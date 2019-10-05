import cats.effect.ExitCode
import com.github.mbelling.ws281x.{Color, LedStripType, Ws281xLedStrip}
import io.circe.generic.semiauto._
import io.circe.{Decoder, _}
import monix.eval.{Task, TaskApp}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.dsl.task._
import org.http4s.implicits._
import org.http4s.server.blaze._

object Main extends TaskApp {
  val ledStripTask: Task[Ws281xLedStrip] = Task {
    new Ws281xLedStrip(
      150,
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

  def setLedStripColors(colors: Map[Int, Color]): Task[Unit] =
    for {
      ledStrip <- ledStripTask
    } yield {
      for ((led, color) <- colors) ledStrip.setPixel(led, color)
      ledStrip.render()
    }

  private case class JsonColor(r: Int, g: Int, b: Int)

  implicit val colorDecoder: Decoder[Color] = deriveDecoder[JsonColor].map(e => new Color(e.r, e.g, e.b))
  implicit val colorEncoder: Encoder[Color] = deriveEncoder[JsonColor].contramap[Color](e => JsonColor(e.getRed, e.getGreen, e.getBlue))

  val service: HttpRoutes[Task] = HttpRoutes.of[Task] {
    case GET -> Root / "ping" =>
      Ok("pong")

    case request@POST -> Root / "colors" =>
      for {
        colors <- request.as[Map[Int, Color]]
        _ <- setLedStripColors(colors)
        response <- Ok()
      } yield response
  }

  override def run(args: List[String]): Task[ExitCode] =
    BlazeServerBuilder[Task]
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(service.orNotFound)
      .resource
      .use(_ => Task.never)
      .map(_ => ExitCode.Success)
}