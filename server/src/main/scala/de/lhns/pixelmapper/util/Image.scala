package de.lhns.pixelmapper.util

import cats.effect.{Async, Sync}
import fs2.Pipe

import java.awt.image.BufferedImage
import javax.imageio.ImageIO

trait Image {
  def width: Int

  def height: Int

  def getColor(x: Int, y: Int): Color

  def getRow(y: Int): IndexedSeq[Color] =
    (0 until width).map(getColor(_, y))
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
  }

  def fromBytes[F[_] : Async]: Pipe[F, Byte, Image] = { stream =>
    stream
      .through(fs2.io.toInputStream)
      .evalMap(inputStream => Sync[F].blocking {
        fromBufferedImage(ImageIO.read(inputStream))
      })
  }
}
