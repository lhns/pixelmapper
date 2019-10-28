package ledstrip.editor

import fs2._
import javafx.embed.swing.SwingFXUtils
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.execution.{CancelableFuture, Scheduler}
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.canvas.Canvas

case class CanvasWindow(title: String,
                        width: Double,
                        height: Double,
                        render: Stream[Task, Image]) {
  def show(): CancelableFuture[Unit] = Task {
    new JFXApp {
      stage = new PrimaryStage {
        title.value = CanvasWindow.this.title

        scene = new Scene(CanvasWindow.this.width, CanvasWindow.this.height) {
          private val canvas = new Canvas(CanvasWindow.this.width, CanvasWindow.this.height)
          private val graphics2d = canvas.graphicsContext2D

          content = canvas

          render.map { image =>
            val fxImage = SwingFXUtils.toFXImage(image.toBufferedImage, null)
            graphics2d.drawImage(fxImage, 0, 0)
          }.compile.drain.runToFuture
        }
      }
    }.main(Array[String]())
  }.runToFuture(Scheduler.singleThread("render"))
}
