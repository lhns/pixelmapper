package de.lhns.pixelmapper

import cats.effect.IO
import cats.syntax.traverse._
import japgolly.scalajs.react.ScalaComponent.BackendScope
import japgolly.scalajs.react.util.EffectCatsEffect._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{ReactEventFromInput, ScalaComponent}
import org.http4s.dom.FetchClientBuilder
import org.http4s.headers.`Content-Type`
import org.http4s.{Method, Request, Uri}

import scala.concurrent.duration._

object MainComponent {
  private val updateInterval: FiniteDuration = 8.seconds

  case class Props()

  case class State(fetched: Boolean)

  object State {
    val empty: State = State(fetched = false)
  }

  class Backend($: BackendScope[Props, State]) {
    private def fetchState: IO[Unit] =
      for {
        test <- IO()
        _ <- $.modStateAsync(_.copy(fetched = true))
      } yield ()

    def componentDidMount: IO[Unit] = {
      lazy val tick: IO[Unit] =
        fetchState >>
          tick.delayBy(updateInterval)

      tick
    }

    def render: VdomElement = {
      <.div(
        ^.cls := "container py-4",
        <.div(
          <.input(
            ^.cls := "form-control form-control-lg",
            ^.tpe := "file",
            ^.onChange ==> { event: ReactEventFromInput =>
              event.target.files.headOption.map { file =>
                FetchClientBuilder[IO].create.status(Request(
                  method = Method.POST,
                  uri = Uri.unsafeFromString("/image"),
                  body = fromReadableStream[IO](file.stream)
                ).withContentType(`Content-Type`.parse(file.`type`).toTry.get))
              }.sequence.void
            },
            ^.onClick ==> { event: ReactEventFromInput =>
              event.target.value = null
              IO.unit
            }
          )
        )
      )
    }
  }

  val Component =
    ScalaComponent.builder[Props]
      .initialState(State.empty)
      .backend(new Backend(_))
      .render(_.backend.render)
      .componentDidMount(_.backend.componentDidMount)
      .build
}
