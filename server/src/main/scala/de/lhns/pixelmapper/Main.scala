package de.lhns.pixelmapper

import cats.effect.std.Queue
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.semigroupk._
import com.comcast.ip4s.{Host, SocketAddress}
import de.lhns.pixelmapper.fixture.{Fixture, LedStrip, Translation}
import de.lhns.pixelmapper.route.{AnimationRoutes, UiRoutes}
import de.lhns.pixelmapper.util.{Animation, Image}
import fs2.io.file.{Files, Path}
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.{ErrorAction, GZip}
import org.log4s.getLogger

import scala.util.chaining._

object Main extends IOApp {
  private val logger = getLogger

  override def run(args: List[String]): IO[ExitCode] = {
    val socketAddress = SocketAddress.fromString(args(0)).get
    val translationFilePath = Path(args(1))

    applicationResource(
      socketAddress,
      translationFilePath
    ).use(_ => IO.never)
  }

  def applicationResource(socketAddress: SocketAddress[Host], translationFilePath: Path): Resource[IO, Unit] =
    for {
      translationImage <- Resource.eval {
        Files[IO].readAll(translationFilePath)
          .through(Image.fromBytes)
          .compile
          .lastOrError
      }
      fixture <- Resource.eval {
        LedStrip[IO](translationImage.width)
          .handleError { throwable =>
            logger.error(throwable)(throwable.getMessage)
            Fixture.dummy[IO](translationImage.width)
          }
      }
      fixture <- Resource.pure {
        Translation.fromImage(fixture, translationImage)
      }
      runningAnimation <- Resource.eval(Queue.circularBuffer[IO, Option[Animation]](1))
      animationRoutes <- AnimationRoutes(fixture, runningAnimation)
      uiRoutes = new UiRoutes()
      _ <- serverResource(
        socketAddress,
        (animationRoutes.toRoutes <+> uiRoutes.toRoutes)
          .pipe(GZip(_))
          .orNotFound
      )
    } yield ()

  def serverResource(socketAddress: SocketAddress[Host], http: HttpApp[IO]): Resource[IO, Server] =
    EmberServerBuilder.default[IO]
      .withHost(socketAddress.host)
      .withPort(socketAddress.port)
      .withHttpApp(ErrorAction.log(
        http = http,
        messageFailureLogAction = (t, msg) => IO(logger.debug(t)(msg)),
        serviceErrorLogAction = (t, msg) => IO(logger.error(t)(msg))
      ))
      .build
}
