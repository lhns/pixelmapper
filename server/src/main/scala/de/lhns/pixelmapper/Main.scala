package de.lhns.pixelmapper

import cats.effect.std.Queue
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.semigroupk._
import com.comcast.ip4s.{Host, SocketAddress}
import de.lhns.pixelmapper.fixture.LedStrip
import de.lhns.pixelmapper.route.{AnimationRoutes, UiRoutes}
import de.lhns.pixelmapper.util.Animation
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
    val numLeds = args(1).toInt

    applicationResource(
      socketAddress,
      numLeds
    ).use(_ => IO.never)
  }

  def applicationResource(socketAddress: SocketAddress[Host], numLeds: Int): Resource[IO, Unit] =
    for {
      fixture <- Resource.eval(LedStrip[IO](numLeds))
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
