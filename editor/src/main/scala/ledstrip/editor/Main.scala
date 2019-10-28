package ledstrip.editor

import java.nio.file.{Path, Paths}

import cats.effect.Blocker
import fs2._
import fs2.concurrent.Queue
import fs2.io._
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits.global
import scodec.bits.ByteVector

import scala.concurrent.duration._

object Main {
  def main(args: Array[String]): Unit = {
    def images(color: Color): Stream[Task, Image] = {
      val newColor = color match {
        case Color.Blue => Color.Red
        case Color.Red => Color.Green
        case Color.Green => Color.Blue
      }

      val image = Image.blank(1000, 800, color)

      Stream(image) ++ (Stream.sleep[Task](10.millis) >> images(newColor))
    }

    val queue = Queue.bounded[Task, Image](10).runSyncUnsafe()

    /*{
      val scheduler: Scheduler = Scheduler.singleThread("images")
      images(Color.Blue).through(queue.enqueue).compile.drain.runToFuture(scheduler)
    }*/

    val path: Path = Paths.get("D:\\pierr\\Downloads\\animation2.png")
    val image: Image = Image.read(path)

    val image2: Image = Image.read(loadFromClasspath("test.png"))

    val mask = Mask.fromImage(image2)

    val image3 = mask.fromLine(image2.row(0))

    queue.enqueue1(image3.scale(4, 4)).runSyncUnsafe()

    val canvasWindow = CanvasWindow("Test", 1900, 1000, queue.dequeue)

    canvasWindow.show()
  }


  private val blocker = Blocker.liftExecutionContext(Scheduler.singleThread("read"))

  def loadFromClasspath(name: String): ByteVector = {
    readInputStream(Task(getClass.getResourceAsStream("/" + name)), chunkSize = 4096, blocker)
      .compile.toChunk.map(_.toArray).map(ByteVector.view)
      .runSyncUnsafe()
  }


}
