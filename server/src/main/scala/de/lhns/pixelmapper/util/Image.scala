package de.lhns.pixelmapper.util

import cats.data.EitherT
import cats.effect.{Async, Sync}
import fs2.Pipe
import org.http4s.headers.`Content-Type`
import org.http4s.{DecodeResult, Entity, EntityDecoder, EntityEncoder, Headers, Media, MediaRange, MediaType}

import java.awt.image.BufferedImage
import javax.imageio.ImageIO

trait Image {
  def width: Int

  def height: Int

  def getColor(x: Int, y: Int): Color

  def getRow(y: Int): IndexedSeq[Color] =
    (0 until width).map(getColor(_, y))

  def toBytes[F[_] : Async]: fs2.Stream[F, Byte]
}

object Image {
  def fromBufferedImage(bufferedImage: BufferedImage): Image = new Image {
    override def width: Int = bufferedImage.getWidth

    override def height: Int = bufferedImage.getHeight

    override def getColor(x: Int, y: Int): Color = {
      val argb = bufferedImage.getRGB(x, y)
      Color(
        (argb >> 16) & 0xFF,
        (argb >> 8) & 0xFF,
        (argb >> 0) & 0xFF
      )
    }

    override def toBytes[F[_] : Async]: fs2.Stream[F, Byte] =
      fs2.io.readOutputStream(1024 * 64)(outputStream => Async[F].blocking {
        ImageIO.write(bufferedImage, "png", outputStream)
      })
  }

  def fromBytes[F[_] : Async]: Pipe[F, Byte, Image] = { stream =>
    stream
      .through(fs2.io.toInputStream)
      .evalMap(inputStream => Sync[F].blocking {
        fromBufferedImage(ImageIO.read(inputStream))
      })
  }

  implicit def entityDecoder[F[_] : Async]: EntityDecoder[F, Image] = new EntityDecoder[F, Image] {
    override def decode(m: Media[F], strict: Boolean): DecodeResult[F, Image] = EitherT.right {
      m.body
        .through(fromBytes)
        .compile
        .lastOrError
    }

    override def consumes: Set[MediaRange] = Set(MediaRange.`image/*`)
  }

  implicit def entityEncoder[F[_] : Async]: EntityEncoder[F, Image] = new EntityEncoder[F, Image] {
    override def toEntity(a: Image): Entity[F] = Entity(a.toBytes)

    override def headers: Headers = Headers(`Content-Type`(MediaType.image.png))
  }
}
