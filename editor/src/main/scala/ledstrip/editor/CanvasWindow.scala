package ledstrip.editor

import cats.effect.{FiberIO, IO}
import cats.effect.std.Queue
import cats.effect.unsafe.IORuntime
import fs2._
import javafx.embed.swing.SwingFXUtils
import javafx.scene.input.MouseEvent
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.canvas.Canvas

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

case class CanvasWindow(title: String,
                        width: Double,
                        height: Double,
                        render: Stream[IO, Image]) {
  private val onClickQueue = Queue.bounded[IO, MouseEvent](10).unsafeRunSync()(IORuntime.global)

  def onClickStream: Stream[IO, MouseEvent] = Stream.fromQueueUnterminated(onClickQueue)

  def show(): FiberIO[Unit] = IO.blocking {
    new JFXApp {
      private val renderImagesContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

      stage = new PrimaryStage {
        title.value = CanvasWindow.this.title

        scene = new Scene(CanvasWindow.this.width, CanvasWindow.this.height) {
          private val canvas = new Canvas(CanvasWindow.this.width, CanvasWindow.this.height) {
            resizable = true
          }

          private val graphics2d = canvas.graphicsContext2D

          content = canvas

          render.map { image =>
            val fxImage = SwingFXUtils.toFXImage(image.toBufferedImage, null)
            graphics2d.drawImage(fxImage, 0, 0)
          }.compile.drain.startOn(renderImagesContext)

          canvas.onMouseClicked = (event: MouseEvent) => onClickQueue.tryOffer(event).unsafeRunSync()(IORuntime.global)
        }
      }
    }.main(Array[String]())
  }
    .startOn(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
    .unsafeRunSync()(IORuntime.global)
}
