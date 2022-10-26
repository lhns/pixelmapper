package de.lhns.pixelmapper.fixture

import cats.Monad
import de.lhns.pixelmapper.util.Color

trait Fixture[F[_]] {
  def numPixels: Int

  def setPixel(i: Int, color: Color): F[Unit]

  def setAllPixels(color: Color): F[Unit]

  def setPixels(colors: IndexedSeq[Color], fixtureOffset: Int, fixtureLength: Int, arrayOffset: Int, arrayLength: Int): F[Unit]

  def setPixels(colors: IndexedSeq[Color], fixtureOffset: Int, fixtureLength: Int): F[Unit] =
    setPixels(colors, fixtureOffset, fixtureLength, 0, colors.size)

  def setPixels(colors: IndexedSeq[Color]): F[Unit] =
    setPixels(colors, 0, numPixels, 0, colors.size)
}

object Fixture {
  def dummy[F[_] : Monad](_numPixels: Int): Fixture[F] = new Fixture[F] {
    override def numPixels: Int = _numPixels

    override def setPixel(i: Int, color: Color): F[Unit] =
      Monad[F].unit

    override def setAllPixels(color: Color): F[Unit] =
      Monad[F].unit

    override def setPixels(colors: IndexedSeq[Color], fixtureOffset: Int, fixtureLength: Int, arrayOffset: Int, arrayLength: Int): F[Unit] =
      Monad[F].unit
  }
}
