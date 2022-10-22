package ledstrip.server

import cats.data.OptionT
import cats.effect.std.Queue
import cats.effect.{ExitCode, IO, IOApp, Ref, Resource}
import com.comcast.ip4s.{Host, Port, SocketAddress}
import ledstrip._
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.scalatags._
import org.http4s.dsl.io._
import org.http4s.server.Server
import org.http4s.server.middleware.ErrorAction
import org.log4s.getLogger

import scala.concurrent.duration._

object Main extends IOApp {
  private val logger = getLogger

  def playAnimation(
                     ledStrip: LedStrip,
                     runningAnimation: Queue[IO, Option[Animation]]): IO[Unit] = {
    def playAnimationFrames(remainingFrames: List[Frame]): IO[Unit] =
      remainingFrames.headOption match {
        case Some(frame) =>
          for {
            fiber <- ledStrip.setColors(frame.rules).start
            _ <- IO.sleep(frame.delay.millis)
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
          ledStrip.setColors(List(ColorRule(None, Color.Black)))
      }
    } yield ()) >> loop

    loop
  }

  def service(ledStrip: LedStrip,
              runningAnimation: Queue[IO, Option[Animation]]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
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
          _ <- runningAnimation.offer(Some(animation))
          response <- Ok()
        } yield response

      case request@DELETE -> Root / "animation" =>
        for {
          _ <- runningAnimation.offer(None)
          response <- Ok()
        } yield response

      case GET -> Root / "ui" =>
        Ok(Ui.ui)

    }

  override def run(args: List[String]): IO[ExitCode] = {
    val socketAddress = SocketAddress.fromString(args(0)).get
    val ledStrip = LedStrip(ledsCount = args(1).toInt)

    applicationResource(
      socketAddress,
      ledStrip
    ).use(_ => IO.never)
  }

  def applicationResource(socketAddress: SocketAddress[Host], ledStrip: LedStrip): Resource[IO, Unit] =
    for {
      runningAnimation <- Resource.eval(Queue.circularBuffer[IO, Option[Animation]](1))
      _ <- Resource.eval(playAnimation(ledStrip, runningAnimation).start)
      _ <- serverResource(
        socketAddress,
        service(ledStrip, runningAnimation).orNotFound
      )
    } yield ()

  def serverResource(socketAddress: SocketAddress[Host], http: HttpApp[IO]): Resource[IO, Server] =
    EmberServerBuilder.default[IO]
      .withHost(socketAddress.host)
      .withPort(socketAddress.port)
      .withHttpApp(
        ErrorAction.log(
          http = http,
          messageFailureLogAction = (t, msg) => IO(logger.debug(t)(msg)),
          serviceErrorLogAction = (t, msg) => IO(logger.error(t)(msg))
        ))
      .build
}
