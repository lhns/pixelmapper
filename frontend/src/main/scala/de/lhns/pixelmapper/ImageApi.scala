package de.lhns.pixelmapper

import cats.data.OptionT
import cats.effect.IO
import cats.effect.std.Queue
import cats.syntax.traverse._
import fs2.dom._
import fs2.{Pipe, Stream}
import org.http4s.dom.FetchClientBuilder
import org.http4s.headers.{`Content-Disposition`, `Content-Type`}
import org.http4s.multipart.{Multipart, Multiparts, Part}
import org.http4s.{Headers, Media, MediaType, Method, Request, Uri}
import org.scalajs.dom.File
import org.typelevel.ci._

object ImageApi {
  private def decodeImage(media: Media[IO]): IO[(String, MediaType, Array[Byte])] =
    for {
      bytes <- media.as[Array[Byte]]
      contentType = media.contentType.map(_.mediaType).getOrElse(MediaType.image.png)
      fileName = media.headers.get[`Content-Disposition`].flatMap(_.filename).getOrElse("animation.png")
    } yield
      (fileName, contentType, bytes)

  def getImages: IO[Seq[(String, MediaType, Array[Byte])]] =
    FetchClientBuilder[IO].create.run(Request(
      method = Method.GET,
      uri = Uri.unsafeFromString("/image")
    )).use {
      case response if !response.status.isSuccess =>
        IO.pure(Seq.empty)

      case response if response.contentType.exists(_.mediaType.isMultipart) =>
        for {
          multipart <- response.as[Multipart[IO]]
          images <- multipart.parts.map(decodeImage).sequence
        } yield images

      case response =>
        Seq(response).map(decodeImage).sequence
    }

  def postImages(files: Seq[File]): Stream[IO, Double] = {
    Stream.eval(Queue.bounded[IO, Option[Long]](1)).flatMap { queue =>
      val totalSize = files.map(_.size.toLong).sum
      val reportSize: Pipe[IO, Byte, Byte] =
        _.chunks.evalTap(chunk => queue.offer(Some(chunk.size.toLong))).unchunks

      Stream.fromQueueNoneTerminated(queue)
        .scan(0L)(_ + _)
        .map(_ / totalSize.toDouble)
        .concurrently(Stream.eval {
          val request = Request[IO](
            method = Method.POST,
            uri = Uri.unsafeFromString("/image")
          )

          (files match {
            case Seq() =>
              OptionT.none[IO, Request[IO]]
            case Seq(file) =>
              OptionT.some[IO] {
                request
                  .withEntity(readReadableStream(IO(file.stream)).through(reportSize))
                  .putHeaders(
                    `Content-Disposition`("inline", Map(ci"filename" -> file.name)),
                    `Content-Type`.parse(file.`type`).toTry.get
                  )
              }
            case files =>
              OptionT.liftF {
                for {
                  multiparts <- Multiparts.forSync[IO]
                  multipart <- multiparts.multipart(
                    files.zipWithIndex.map {
                      case (file, i) =>
                        Part.fileData(
                          name = i.toString,
                          filename = file.name,
                          entityBody = readReadableStream(IO(file.stream)).through(reportSize),
                          headers = Headers(`Content-Type`.parse(file.`type`).toTry.get)
                        )
                    }.toVector
                  )
                } yield request
                  .withEntity(multipart)
                  .putHeaders(multipart.headers)
              }
          })
            .semiflatMap(request => FetchClientBuilder[IO].create.status(request))
            .value
            .void
            .guarantee(queue.offer(None))
        })
    }
  }
}
