package ledstrip.editor

import java.awt.image.BufferedImage

import fs2._
import javafx.embed.swing.SwingFXUtils
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.canvas.{Canvas, GraphicsContext}

case class CanvasWindow(title: String,
                        width: Double,
                        height: Double,
                        render: Stream[Task, BufferedImage]) {
  def show(): Unit = {
    new JFXApp {
      stage = new PrimaryStage {
        title.value = CanvasWindow.this.title

        scene = new Scene(CanvasWindow.this.width, CanvasWindow.this.height) {
          val canvas = new Canvas(CanvasWindow.this.width, CanvasWindow.this.height)

          content = canvas

          val graphicsContext: GraphicsContext = canvas.graphicsContext2D
          render.map { bufferedImage =>
            val image = SwingFXUtils.toFXImage(bufferedImage, null)
            graphicsContext.drawImage(image, 0, 0)
          }.compile.drain.runToFuture
          //graphicsContext.fill = Color.Black
          //graphicsContext.fillRect(0, 0, graphicsContext.canvas.getWidth, graphicsContext.canvas.getHeight)
        }
      }
    }.main(Array[String]())
  }
}
