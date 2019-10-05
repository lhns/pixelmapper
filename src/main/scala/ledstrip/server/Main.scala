package ledstrip.server

import cats.effect.ExitCode
import com.github.mbelling.ws281x.{LedStripType, Ws281xLedStrip}
import ledstrip.ColorRule
import monix.eval.{Task, TaskApp}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.dsl.task._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

object Main extends TaskApp {

  case class LedStrip(ledsCount: Int) {
    val ledStripTask: Task[Ws281xLedStrip] = Task {
      new Ws281xLedStrip(
        ledsCount,
        18,
        800000,
        10,
        255,
        0,
        false,
        LedStripType.WS2811_STRIP_GRB,
        true
      )
    }.memoizeOnSuccess

    def setColors(colorRules: List[ColorRule]): Task[Unit] =
      for {
        ledStrip <- ledStripTask
      } yield {
        colorRules.collectFirst {
          case ColorRule(None, color) =>
            ledStrip.setStrip(color)
        }

        colorRules.collect {
          case ColorRule(Some(leds), color) =>
            for (led <- leds) {
              if (led < ledsCount) ledStrip.setPixel(led, color)
            }
        }

        ledStrip.render()
      }
  }

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
