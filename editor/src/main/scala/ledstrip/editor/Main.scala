package ledstrip.editor

import java.awt.Color
import java.awt.image.BufferedImage

import fs2._
import monix.eval.Task

import scala.concurrent.duration._

object Main {
  def main(args: Array[String]): Unit = {
    def images(color: Color): Stream[Task, BufferedImage] = {
      val newColor = color match {
        case Color.BLUE => Color.RED
        case Color.RED => Color.GREEN
        case Color.GREEN => Color.BLUE
      }

      val image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB)
      val graphics = image.createGraphics()
      graphics.setPaint(color)
      graphics.fillRect(0, 0, image.getWidth, image.getHeight)
      Stream(image) ++ (Stream.sleep[Task](0.millis) >> images(newColor))
    }

    CanvasWindow("Test", 800, 600, images(Color.BLUE)).show()
  }
}
