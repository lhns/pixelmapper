package ledstrip.client

import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.{Files, Path, Paths}
import cats.effect.{ExitCode, IO, IOApp}

import javax.imageio.ImageIO
import ledstrip.{Animation, Color, ColorRule, Frame}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.jdkhttpclient.JdkHttpClient
import org.http4s.{EntityEncoder, Method, Request, Uri}

import scala.collection.JavaConverters._

object Main extends IOApp {

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


  def sendColors(colorRules: List[ColorRule])(implicit client: Client[IO]): IO[Unit] =
    send(Method.POST, "colors", colorRules)

  def sendAnimation(animationOption: Option[Animation])(implicit client: Client[IO]): IO[Unit] =
    animationOption match {
      case Some(animation) =>
        send(Method.POST, "animation", animation)

      case None =>
        send(Method.DELETE, "animation", ())
    }


  def loadUri(): Uri = {
    def jarPath(clazz: Class[_]): Path = Paths.get(new File(clazz.getProtectionDomain.getCodeSource.getLocation.toURI.getPath).getPath)

    val confPath = jarPath(getClass).resolveSibling("config.txt")
    val uriString = Files.readAllLines(confPath).asScala.toList.mkString("\n").trim
    Uri.unsafeFromString(uriString)
  }

  def send[E](method: Method, endpoint: String, entity: E)(implicit client: Client[IO], encoder: EntityEncoder[IO, E]): IO[Unit] = {
    val uri = loadUri()

    val request: Request[IO] =
      Request(
        method = method,
        uri = uri / endpoint
      )
        .withEntity(entity)

    client.successful(request).map(_ => ())
  }

  def imageRow(image: Image, row: Int): List[ColorRule] = {
    val colors =
      for (i <- 0 until image.width) yield {
        ColorRule(Some(List(i)), image.getColor(i, row))
      }

    ColorRule.compressList(ColorRule(None, Color.Black) +: colors.toList)
  }


  override def run(args: List[String]): IO[ExitCode] = {
    val path: Path = Paths.get(args.head)
    val image: Image = Image.read(path)

    for {
      _ <- JdkHttpClient.simple[IO].use { implicit client =>
        /*def drawImage(row: Int = 0): IO[Unit] = {
          for {
            fiber <- IO.defer {
              val colorRules = imageRow(image, row)
              sendColors(colorRules)
            }.start
            _ <- IO.sleep(30.millis)
            _ <- fiber.join
            _ <- {
              val nextRow = row + 1
              if (nextRow < image.height) drawImage(row + 1)
              else IO.unit
            }
          } yield ()
        }

        drawImage().loopForever*/

        val frames = for (row <- 0 until image.height) yield {
          Frame(imageRow(image, row), if (row % 10 == 0) 30 else 30)
        }

        sendAnimation(Some(Animation(frames.toList, loop = true)))
        //sendAnimation(None)
      }

      /*_ <- IO {
        val frames = for (row <- 0 until image.height) yield {
          Frame(imageRow(image, row), if (row % 10 == 0) 30 else 30)
        }

        val animation = Animation(frames.toList, loop = true)

        println(animation.asJson.noSpaces)
      }*/
    } yield
      ExitCode.Success
  }
}
