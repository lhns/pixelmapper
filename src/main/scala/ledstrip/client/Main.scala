package ledstrip.client

import cats.effect.ExitCode
import com.github.mbelling.ws281x.Color
import ledstrip.ColorRule
import monix.eval.{Task, TaskApp}
import monix.execution.Scheduler
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{Method, Request, Uri}

object Main extends TaskApp {
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
        /*def cycle(color: Color): Task[Unit] = {
          for {
            _ <- sendColors(List(ColorRule(None, color)))
            _ <- cycle(new Color(if (color.getBlue > 0) 255 else 0, if (color.getRed > 0) 255 else 0, if (color.getGreen > 0) 255 else 0))
          } yield ()
        }

        cycle(new Color(255, 0, 0))*/

        (for {
          _ <- Task.sequence {
            for (i <- 0 until 150) yield
              sendColors(List(ColorRule(None, Color.BLACK), ColorRule(Some((0 to i).toList), Color.BLUE)))
          }
          _ <- Task.sequence {
            for (i <- 0 until 150) yield
              sendColors(List(ColorRule(None, Color.BLACK), ColorRule(Some((i to 150).toList), Color.BLUE)))
          }

          _ <- Task.sequence {
            for (i <- 0 until 150) yield
              sendColors(List(ColorRule(None, Color.BLACK), ColorRule(Some((0 to i).toList), Color.RED)))
          }
          _ <- Task.sequence {
            for (i <- 0 until 150) yield
              sendColors(List(ColorRule(None, Color.BLACK), ColorRule(Some((i to 150).toList), Color.RED)))
          }

          _ <- Task.sequence {
            for (i <- 0 until 150) yield
              sendColors(List(ColorRule(None, Color.BLACK), ColorRule(Some((0 to i).toList), Color.GREEN)))
          }
          _ <- Task.sequence {
            for (i <- 0 until 150) yield
              sendColors(List(ColorRule(None, Color.BLACK), ColorRule(Some((i to 150).toList), Color.GREEN)))
          }
        } yield ()).loopForever
      }.onErrorRestartIf(_ => true)
    } yield
      ExitCode.Success
}
