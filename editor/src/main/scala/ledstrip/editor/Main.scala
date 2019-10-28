package ledstrip.editor

import java.nio.file.Path

import fs2._
import fs2.concurrent.Queue
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration._

object Main {
  def main(args: Array[String]): Unit = {
    def images(color: Color): Stream[Task, Image] = {
      val newColor = color match {
        case Color.Blue => Color.Red
        case Color.Red => Color.Green
        case Color.Green => Color.Blue
      }

      val image = Image.blank(800, 600, color)

      Stream(image) ++ (Stream.sleep[Task](0.millis) >> images(newColor))
    }

    val queue = Queue.bounded[Task, Image](10).runSyncUnsafe()

    images(Color.Blue).through(queue.enqueue).compile.drain.runToFuture

    val canvasWindow = CanvasWindow("Test", 800, 600, queue.dequeue)

    canvasWindow.show()
  }

  def loadMask(path: Path): Unit = ???


}
