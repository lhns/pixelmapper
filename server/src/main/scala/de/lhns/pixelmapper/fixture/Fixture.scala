package de.lhns.pixelmapper.fixture

import cats.Monad
import de.lhns.pixelmapper.util.Color

trait Fixture[F[_]] {
  def numPixels: Int

  def setAllPixels(color: Color): F[Unit]

  def setPixels(colors: Seq[(Int, Color)]): F[Unit]

  def setPixel(i: Int, color: Color): F[Unit] = setPixels(Seq((i, color)))

  def setPixels(colors: IndexedSeq[Color], offset: Int, length: Int): F[Unit] =
    setPixels(
      for {
        i <- Math.max(0, offset) until Math.min(numPixels, offset + length)
        color = colors(i * colors.length / length + offset)
        if color != null
      } yield
        (i, color)
    )

  def setPixels(colors: IndexedSeq[Color]): F[Unit] =
    setPixels(colors, 0, colors.length)
}

object Fixture {
  def dummy[F[_] : Monad](_numPixels: Int): Fixture[F] = new Fixture[F] {
    override def numPixels: Int = _numPixels

    override def setAllPixels(color: Color): F[Unit] =
      Monad[F].unit

    override def setPixels(colors: Seq[(Int, Color)]): F[Unit] =
      Monad[F].unit
  }
}
