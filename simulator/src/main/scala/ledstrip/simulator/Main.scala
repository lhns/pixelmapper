package ledstrip.simulator

import java.nio.file.{Path, Paths}

import javax.imageio.ImageIO
import scalafx.animation.AnimationTimer
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.canvas.Canvas
import scalafx.scene.paint.Color

object Main {
  val path: Path = Paths.get("D:\\pierr\\Downloads\\animation2.png")

  var image = ImageIO.read(path.toFile)

  def doReread(): Unit = {
    image = ImageIO.read(path.toFile)
  }

  val (imgWidth, imgHeight) = (image.getWidth, image.getHeight)
  val (pixelWidth, pixelHeight) = (20, 20)

  val (sceneWidth, sceneHeight) = (imgWidth * pixelWidth, pixelHeight)

  def getColor(x: Int, y: Int): Color = {
    val argb = image.getRGB(x, y)
    Color(
      ((argb >> 16) & 0xFF) / 255D,
      ((argb >> 8) & 0xFF) / 255D,
      ((argb >> 0) & 0xFF) / 255D,
      ((argb >> 24) & 0xFF) / 255D
    )
  }

  def render(canvas: Canvas, offset: Int): Unit = {
    val graphicsContext = canvas.graphicsContext2D

    graphicsContext.fill = Color.Black
    graphicsContext.fillRect(0, 0, graphicsContext.canvas.getWidth, graphicsContext.canvas.getHeight)

    for {
      y <- 0 until 1
      x <- 0 until imgWidth
    } {
      val color = getColor(x, y + offset)
      graphicsContext.fill = color
      graphicsContext.fillRect(x * pixelWidth, y * pixelHeight, pixelWidth, pixelHeight)
    }
  }

  var offset = 0
  var lastFrame = 0L
  val frameRateMillis = 50
  var reread = 0

  class JFXMain extends JFXApp {
    stage = new PrimaryStage {
      title.value = "LED Strip"

      scene = new Scene(sceneWidth, sceneHeight) {
        val canvas = new Canvas(sceneWidth, sceneHeight)


        content = canvas

        val animationTimer = AnimationTimer { t =>
          if (t - lastFrame > frameRateMillis * 1000000L) {
            reread += 1
            if (reread == 10) {
              reread = 0;
              doReread()
            }

            lastFrame = t
            offset = (offset + 1) % imgHeight
            render(canvas, offset)
          }
        }

        animationTimer.start()
      }
    }
  }

  def main(args: Array[String]): Unit = {
    new JFXMain().main(Array[String]())
  }
}
