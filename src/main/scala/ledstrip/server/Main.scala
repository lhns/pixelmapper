package ledstrip.server

import cats.effect.ExitCode
import ledstrip.{ColorRule, LedStrip}
import monix.eval.{Task, TaskApp}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.dsl.task._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

object Main extends TaskApp {
  def service(ledStrip: LedStrip): HttpRoutes[Task] = HttpRoutes.of[Task] {
    case GET -> Root / "ping" =>
      Ok("pong")

    case request@POST -> Root / "colors" =>
      for {
        colors <- request.as[List[ColorRule]]
        _ <- ledStrip.setColors(colors)
        response <- Ok()
      } yield response
  }

  override def run(args: List[String]): Task[ExitCode] = {
    val List(host, portString) = args(0).split(":").toList
    val port = portString.toInt

    val ledStrip = LedStrip(args(1).toInt)

    val httpApp = service(ledStrip).orNotFound

    BlazeServerBuilder[Task]
      .bindHttp(port, host)
      .withHttpApp(httpApp)
      .resource
      .use(_ => Task.never)
      .map(_ => ExitCode.Success)
  }
}
