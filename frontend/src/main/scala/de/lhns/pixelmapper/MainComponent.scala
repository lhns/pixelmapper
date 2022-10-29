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
import org.http4s.multipart.{Multipart, Multiparts, Part}
import org.http4s.util.Renderer
import org.http4s.{Headers, MediaType, Method, Request, Uri}
import org.typelevel.ci._

import java.util.Base64
import scala.concurrent.duration._

object MainComponent {
  private val updateInterval: FiniteDuration = 8.seconds

  case class Props()

  case class State(images: Seq[(String, `Content-Type`, Array[Byte])])

  object State {
    val empty: State = State(images = Seq.empty)
  }

  class Backend($: BackendScope[Props, State]) {
    private def fetchState: IO[Unit] =
      for {
        images <- FetchClientBuilder[IO].create.run(Request(
          method = Method.GET,
          uri = Uri.unsafeFromString("/image")
        )).use { response =>
          if (response.status.isSuccess) {
            val defaultFileName = "animation.png"
            if (response.contentType.exists(_.mediaType.isMultipart)) {
              for {
                multipart <- response.as[Multipart[IO]]
                images <- multipart.parts.map { part =>
                  for {
                    bytes <- part.as[Array[Byte]]
                    contentType = part.contentType.getOrElse(`Content-Type`(MediaType.image.png))
                    fileName = part.filename.getOrElse(defaultFileName)
                  } yield
                    (fileName, contentType, bytes)
                }.sequence
              } yield images
            } else {
              for {
                bytes <- response.as[Array[Byte]]
                contentType = response.contentType.getOrElse(`Content-Type`(MediaType.image.png))
                fileName = response.headers.get[`Content-Disposition`].flatMap(_.filename).getOrElse(defaultFileName)
              } yield
                Seq((fileName, contentType, bytes))
            }
          } else {
            IO.pure(Seq.empty)
          }
        }
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
                val uri = Uri.unsafeFromString("/image")
                for {
                  requestOption <- target.files.toSeq match {
                    case Seq() => IO.pure(None)
                    case Seq(file) =>
                      IO.pure(Some(Request(
                        method = Method.POST,
                        uri = uri,
                        body = readReadableStream(IO(file.stream)),
                        headers = Headers(
                          `Content-Disposition`("inline", Map(ci"filename" -> file.name)),
                          `Content-Type`.parse(file.`type`).toTry.get)
                      )))
                    case files =>
                      for {
                        multiparts <- Multiparts.forSync[IO]
                        multipart <- multiparts.multipart(
                          files.zipWithIndex.map {
                            case (file, i) =>
                              Part.fileData(
                                name = i.toString,
                                filename = file.name,
                                entityBody = readReadableStream(IO(file.stream)),
                                headers = Headers(`Content-Type`.parse(file.`type`).toTry.get)
                              )
                          }.toVector
                        )
                      } yield Some(Request(
                        method = Method.POST,
                        uri = uri,
                        headers = multipart.headers
                      ).withEntity(multipart))
                  }
                  _ <- requestOption.map(request => FetchClientBuilder[IO].create.status(request)).sequence
                  _ = target.value = null
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
          state.images.toVdomArray {
            case (fileName, contentType, imageBytes) =>
              val dataUrl: String = s"data:${Renderer.renderString(contentType.mediaType)};base64,${Base64.getEncoder.encodeToString(imageBytes)}"
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
