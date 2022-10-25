package de.lhns.pixelmapper.route

import cats.data.OptionT
import cats.effect.std.Queue
import cats.effect.{IO, Resource}
import de.lhns.pixelmapper.fixture.Fixture
import de.lhns.pixelmapper.util.{Animation, Color, ColorRule, Frame}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.dsl.io._

class AnimationRoutes private(
                               fixture: Fixture[IO],
                               runningAnimation: Queue[IO, Option[Animation]]
                             ) {
  def runAnimation: IO[Unit] = {
    def playAnimationFrames(remainingFrames: Seq[Frame]): IO[Unit] =
      remainingFrames.headOption match {
        case Some(frame) =>
          for {
            fiber <- fixture.setPixels(ColorRule.toColorSeq(frame.rules, fixture.numPixels)).start
            _ <- IO.sleep(frame.delay)
            _ <- fiber.join
            _ <- playAnimationFrames(remainingFrames.tail)
          } yield ()

        case None =>
          IO.unit
      }

    lazy val loop: IO[Unit] = (for {
      animationOption <- runningAnimation.take
      newAnimationOption <- IO.race[Option[Animation], Option[Animation]](
        (for {
          animation <- OptionT.fromOption[IO](animationOption)
          _ <- OptionT.liftF(playAnimationFrames(animation.frames))
          _ <- OptionT.some[IO](animation).filter(_.loop)
        } yield animation).value,
        runningAnimation.take
      ).map(_.merge)
      _ <- newAnimationOption match {
        case Some(newAnimation) =>
          runningAnimation.tryOffer(Some(newAnimation))

        case None =>
          fixture.setAllPixels(Color.Black)
      }
    } yield ()) >> loop

    loop
  }

  def toRoutes: HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "health" =>
        Ok("Ok")

      case request@POST -> Root / "colors" =>
        for {
          colors <- request.as[List[ColorRule]]
          _ <- fixture.setPixels(ColorRule.toColorSeq(colors, fixture.numPixels))
          response <- Ok()
        } yield response

      case request@POST -> Root / "animation" =>
        for {
          animation <- request.as[Animation]
          _ <- runningAnimation.offer(Some(animation))
          response <- Ok()
        } yield response

      case request@DELETE -> Root / "animation" =>
        for {
          _ <- runningAnimation.offer(None)
          response <- Ok()
        } yield response
    }
}

object AnimationRoutes {
  def apply(
             fixture: Fixture[IO],
             runningAnimation: Queue[IO, Option[Animation]]
           ): Resource[IO, AnimationRoutes] = {
    val animationRoutes = new AnimationRoutes(
      fixture,
      runningAnimation
    )

    Resource.make(
      animationRoutes.runAnimation.start
    )(fiber =>
      fiber.cancel
    ).map(_ =>
      animationRoutes
    )
  }
}
