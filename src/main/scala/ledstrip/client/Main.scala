package ledstrip.client

import java.awt.image.BufferedImage
import java.nio.file.{Path, Paths}

import cats.effect.ExitCode
import javax.imageio.ImageIO
import ledstrip.{Color, ColorRule}
import monix.eval.{Task, TaskApp}
import monix.execution.Scheduler
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{Method, Request, Uri}

import scala.concurrent.duration._

object Main extends TaskApp {

  case class Image(bufferedImage: BufferedImage) {
    val width: Int = bufferedImage.getWidth
    val height: Int = bufferedImage.getHeight

    def getColor(x: Int, y: Int): Color = {
      val argb = bufferedImage.getRGB(x, y)
      Color(
        (argb >> 16) & 0xFF,
        (argb >> 8) & 0xFF,
        (argb >> 0) & 0xFF
      )
    }
  }

  object Image {
    def read(path: Path): Image = Image(ImageIO.read(path.toFile))
  }

  val path: Path = Paths.get("D:\\pierr\\Downloads\\animation2.png")
  var image: Image = Image.read(path)


  def sendColors(colorRules: List[ColorRule])(implicit client: Client[Task]): Task[Unit] = {
    val request: Request[Task] =
      Request(
        method = Method.POST,
        uri = Uri.unsafeFromString("http://10.1.15.178:8080/colors")
      )
        .withEntity(colorRules)

    client.successful(request).map(_ => ())
  }

  override def run(args: List[String]): Task[ExitCode] =
    for {
      _ <- BlazeClientBuilder[Task](Scheduler.global).resource.use { implicit client =>
        def drawImage(row: Int = 0): Task[Unit] = {
          for {
            fiber <- Task.defer {
              val colors =
                for (i <- 0 until image.width) yield {
                  ColorRule(Some(List(i)), image.getColor(i, row))
                }

              val colorRules = ColorRule.compressList(ColorRule(None, Color.Black) +: colors.toList)

              sendColors(colorRules)
            }.start
            _ <- Task.sleep(30.millis)
            _ <- fiber.join
            _ <- {
              val nextRow = row + 1
              if (nextRow < image.height) drawImage(row + 1)
              else Task.unit
            }
          } yield ()
        }

        drawImage().loopForever
      }.onErrorRestartIf { throwable =>
        throwable.printStackTrace()
        true
      }
    } yield
      ExitCode.Success
}
