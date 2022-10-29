package de.lhns.pixelmapper.route

import cats.data.OptionT
import cats.effect.std.Queue
import cats.effect.{IO, Ref, Resource}
import cats.kernel.Monoid
import cats.syntax.traverse._
import de.lhns.pixelmapper.fixture.Fixture
import de.lhns.pixelmapper.util._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.dsl.io._
import org.http4s.headers.`Content-Disposition`
import org.http4s.multipart.{Multipart, Multiparts, Part}
import org.http4s.{EntityEncoder, HttpRoutes}
import org.typelevel.ci._

import scala.concurrent.duration._

class AnimationRoutes private(
                               fixture: Fixture[IO],
                               runningAnimation: Queue[IO, Option[Animation]],
                               imagesRef: Ref[IO, Seq[(String, Image)]]
                             ) {
  def runAnimation: IO[Unit] = {
    def playAnimationFrames(remainingFrames: Seq[Frame]): IO[Unit] =
      remainingFrames.headOption match {
        case Some(frame) =>
          for {
            fiber <- fixture.setPixels(ColorRule.toColorSeq(frame.rules, frame.width)).start
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
          _ <- imagesRef.set(Seq.empty)
          response <- Ok()
        } yield response

      case request@POST -> Root / "animation" =>
        for {
          animation <- request.as[Animation]
          _ <- runningAnimation.offer(Some(animation))
          _ <- imagesRef.set(Seq.empty)
          response <- Ok()
        } yield response

      case request@DELETE -> Root / "animation" =>
        for {
          _ <- runningAnimation.offer(None)
          _ <- imagesRef.set(Seq.empty)
          response <- Ok()
        } yield response

      case request@POST -> Root / "image" =>
        val defaultFileName = "animation.png"
        for {
          images <- if (request.contentType.exists(_.mediaType.isMultipart)) {
            for {
              multipart <- request.as[Multipart[IO]]
              images <- multipart.parts.map { part =>
                for {
                  image <- part.as[Image]
                  fileName = part.filename.getOrElse(defaultFileName)
                } yield
                  (fileName, image)
              }.sequence
            } yield images
          } else {
            for {
              image <- request.as[Image]
              fileName = request.headers.get[`Content-Disposition`].flatMap(_.filename).getOrElse(defaultFileName)
            } yield Seq((fileName, image))
          }
          animation = Monoid[Animation].combineAll {
            images.map {
              case (_, image) =>
                Animation.fromImage(image, delay = 30.millis, loop = true)
            }
          }
          _ <- runningAnimation.offer(Some(animation))
          _ <- imagesRef.set(images)
          response <- Ok()
        } yield response

      case GET -> Root / "image" =>
        imagesRef.get.flatMap {
          case Seq() => NotFound()
          case Seq((fileName, image)) =>
            Ok(image)
              .map(_.putHeaders(`Content-Disposition`("inline", Map(ci"filename" -> fileName))))
          case seq =>
            for {
              multiparts <- Multiparts.forSync[IO]
              multipart <- multiparts.multipart(
                seq.zipWithIndex.map {
                  case ((fileName, image), i) =>
                    Part.fileData(
                      name = i.toString,
                      filename = fileName,
                      entityBody = EntityEncoder[IO, Image].toEntity(image).body,
                      headers = EntityEncoder[IO, Image].headers
                    )
                }.toVector
              )
              response <- Ok(multipart)
                .map(_.putHeaders(multipart.headers))
            } yield response
        }
    }
}

object AnimationRoutes {
  def apply(
             fixture: Fixture[IO],
             runningAnimation: Queue[IO, Option[Animation]]
           ): Resource[IO, AnimationRoutes] =
    for {
      lastImageRef <- Resource.eval(Ref.of[IO, Seq[(String, Image)]](Seq.empty))
      animationRoutes = new AnimationRoutes(
        fixture,
        runningAnimation,
        lastImageRef
      )
      _ <- Resource.make(
        animationRoutes.runAnimation.start
      )(fiber =>
        fiber.cancel
      )
    } yield
      animationRoutes
}
