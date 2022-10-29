package de.lhns.pixelmapper

import cats.effect.IO
import cats.syntax.traverse._
import fs2.dom._
import japgolly.scalajs.react.ScalaComponent.BackendScope
import japgolly.scalajs.react.util.EffectCatsEffect._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{ReactEventFromInput, ScalaComponent}
import org.http4s.dom.FetchClientBuilder
import org.http4s.headers.{`Content-Disposition`, `Content-Type`}
import org.http4s.util.Renderer
import org.http4s.{Headers, MediaType, Method, Request, Uri}
import org.typelevel.ci._

import java.util.Base64
import scala.concurrent.duration._

object MainComponent {
  private val updateInterval: FiniteDuration = 8.seconds

  case class Props()

  case class State(imageOption: Option[(String, `Content-Type`, Array[Byte])])

  object State {
    val empty: State = State(imageOption = None)
  }

  class Backend($: BackendScope[Props, State]) {
    private def fetchState: IO[Unit] =
      for {
        imageOption <- FetchClientBuilder[IO].create.run(Request(
          method = Method.GET,
          uri = Uri.unsafeFromString("/image")
        )).use { response =>
          if (response.status.isSuccess)
            response.as[Array[Byte]].map { bytes =>
              val contentType = response.contentType.getOrElse(`Content-Type`(MediaType.image.png))
              val fileName = response.headers.get[`Content-Disposition`].flatMap(_.filename).getOrElse("animation.png")
              Some((fileName, contentType, bytes))
            }
          else
            IO(None)
        }
        _ <- $.modStateAsync(_.copy(imageOption = imageOption))
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
              ^.onChange ==> { event: ReactEventFromInput =>
                val target = event.target
                target.files.headOption.map { file =>
                  FetchClientBuilder[IO].create.status(Request(
                    method = Method.POST,
                    uri = Uri.unsafeFromString("/image"),
                    body = readReadableStream(IO(file.stream)),
                    headers = Headers(`Content-Disposition`("inline", Map(ci"filename" -> file.name)))
                  ).withContentType(`Content-Type`.parse(file.`type`).toTry.get))
                }.sequence.void >> IO {
                  target.value = null
                } >> fetchState
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
          <.div(
            state.imageOption.map {
              case (fileName, contentType, imageBytes) =>
                val dataUrl: String = s"data:${Renderer.renderString(contentType.mediaType)};base64,${Base64.getEncoder.encodeToString(imageBytes)}"
                <.a(
                  ^.download := fileName,
                  ^.href := dataUrl,
                  <.img(^.src := dataUrl)
                )
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
      .render(e => e.backend.render(e.state))
      .componentDidMount(_.backend.componentDidMount)
      .build
}
