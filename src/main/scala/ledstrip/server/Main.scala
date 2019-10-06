package ledstrip.server

import cats.data.OptionT
import cats.effect.ExitCode
import ledstrip.{Animation, ColorRule, Frame, LedStrip}
import monix.catnap.MVar
import monix.eval.{Task, TaskApp}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.dsl.task._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.duration._

object Main extends TaskApp {
  def playAnimation(ledStrip: LedStrip, animationVar: MVar[Task, Option[Animation]]): Task[Unit] = {
    def playAnimationFrames(remainingFrames: List[Frame]): Task[Unit] =
      remainingFrames.headOption match {
        case Some(frame) =>
          for {
            fiber <- ledStrip.setColors(frame.rules).start
            _ <- Task.sleep(frame.delay.millis)
            _ <- fiber.join
            _ <- playAnimationFrames(remainingFrames.tail)
          } yield ()

        case None =>
          Task.unit
      }

    (for {
      animationOption <- animationVar.take
      newAnimationOption <- Task.race[Option[Animation], Option[Animation]](
        (for {
          animation <- OptionT.fromOption[Task](animationOption)
          _ <- OptionT.liftF(playAnimationFrames(animation.frames))
          _ <- OptionT.some(animation).filter(_.loop)
        } yield animation).value,
        animationVar.take
      ).map(_.merge)
      _ <- newAnimationOption match {
        case Some(newAnimation) =>
          animationVar.tryPut(Some(newAnimation))

        case None =>
          Task.unit
      }
    } yield ()).loopForever
  }

  def service(ledStrip: LedStrip,
              runningAnimation: MVar[Task, Option[Animation]]): HttpRoutes[Task] =
    HttpRoutes.of[Task] {
      case GET -> Root / "ping" =>
        Ok("pong")

      case request@POST -> Root / "colors" =>
        for {
          colors <- request.as[List[ColorRule]]
          _ <- ledStrip.setColors(colors)
          response <- Ok()
        } yield response

      case request@POST -> Root / "animation" =>
        for {
          animation <- request.as[Animation]
          _ <- runningAnimation.put(Some(animation))
          response <- Ok()
        } yield response

      case request@DELETE -> Root / "animation" =>
        for {
          _ <- runningAnimation.put(None)
          response <- Ok()
        } yield response
    }

  override def run(args: List[String]): Task[ExitCode] = {
    val List(host, portString) = args(0).split(":").toList
    val port = portString.toInt

    val ledStrip = LedStrip(args(1).toInt)

    for {
      runningAnimation <- MVar.empty[Task, Option[Animation]]()
      _ <- playAnimation(ledStrip, runningAnimation).start
      httpApp = service(ledStrip, runningAnimation).orNotFound
      _ <- BlazeServerBuilder[Task]
        .bindHttp(port, host)
        .withHttpApp(httpApp)
        .resource
        .use(_ => Task.never)
    } yield
      ExitCode.Success
  }
}
