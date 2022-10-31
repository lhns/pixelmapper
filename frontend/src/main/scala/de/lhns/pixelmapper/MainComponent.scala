package de.lhns.pixelmapper

import cats.effect.IO
import japgolly.scalajs.react.ScalaComponent.BackendScope
import japgolly.scalajs.react.util.EffectCatsEffect._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{ReactEventFromInput, ScalaComponent}
import org.http4s.dom.FetchClientBuilder
import org.http4s.{MediaType, Method, Request, Uri}

import scala.concurrent.duration._

object MainComponent {
  private val updateInterval: FiniteDuration = 8.seconds

  case class Props()

  case class State(
                    images: Seq[(String, MediaType, Array[Byte])],
                    progress: Option[Double]
                  )

  object State {
    val empty: State = State(
      images = Seq.empty,
      progress = None
    )
  }

  class Backend($: BackendScope[Props, State]) {
    private def fetchState: IO[Unit] =
      for {
        images <- ImageApi.getImages
        _ <- $.modStateAsync(_.copy(images = images))
      } yield ()

    def componentDidMount: IO[Unit] = {
      lazy val tick: IO[Unit] =
        fetchState >>
          tick.delayBy(updateInterval)

      tick
    }

    def render(state: State): VdomElement = {
      <.div(
        ^.cls := "container py-4",
        <.div(
          ^.cls := "d-flex flex-column gap-3",
          <.h1(
            ^.cls := "text-center",
            "PixelMapper"
          ),
          <.div(
            ^.cls := "d-flex flex-row gap-2",
            <.input(
              ^.cls := "form-control form-control-lg",
              ^.tpe := "file",
              ^.multiple := true,
              ^.onChange ==> { event: ReactEventFromInput =>
                val target = event.target
                for {
                  _ <- ImageApi.postImages(target.files.toSeq).evalTap(progress => $.modStateAsync(_.copy(progress = Some(progress)))).compile.drain
                  _ = target.value = null
                  _ <- $.modStateAsync(_.copy(progress = None))
                  _ <- fetchState
                } yield ()
              }
            ),
            <.button(
              ^.tpe := "button",
              ^.cls := "btn btn-danger",
              <.i(^.cls := "bi bi-power"),
              ^.onClick --> {
                FetchClientBuilder[IO].create.status(Request[IO](
                  method = Method.DELETE,
                  uri = Uri.unsafeFromString("/animation")
                )) >> fetchState
              }
            )
          ),
          state.progress.map { progress =>
            <.div(
              ^.cls := "progress",
              <.div(
                ^.cls := "progress-bar",
                ^.role := "progressbar",
                ^.width := s"${(progress * 100).toInt}%"
              )
            )
          },
          state.images.toVdomArray {
            case (fileName, mediaType, imageBytes) =>
              val dataUrl = makeDataUrl(mediaType, imageBytes)
              <.div(
                <.a(
                  ^.download := fileName,
                  ^.href := dataUrl,
                  <.img(^.src := dataUrl)
                )
              )
          }
        )
      )
    }
  }

  val Component =
    ScalaComponent.builder[Props]
      .initialState(State.empty)
      .backend(new Backend(_))
      .render(e => e.backend.render(e.state))
      .componentDidMount(_.backend.componentDidMount)
      .build
}
