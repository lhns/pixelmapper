package ledstrip.client

import com.github.mbelling.ws281x.Color
import ledstrip.ColorRule
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits.global
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{Request, Uri}

object Main {
  def main(args: Array[String]): Unit = {
    BlazeClientBuilder[Task](Scheduler.global).resource.use { client =>
      val request: Request[Task] =
        Request(uri = Uri.unsafeFromString("http://10.1.15.178:8080/colors"))
          .withEntity(List(ColorRule(None, new Color(255, 0, 0))))

      client.successful(request)
    }
  }
}
