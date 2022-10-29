package de.lhns.pixelmapper.fixture

import de.lhns.pixelmapper.util.{Color, Image}

class Translation[F[_]](
                         fixture: Fixture[F],
                         translate: IndexedSeq[Seq[Int]]
                       ) extends Fixture[F] {
  override def numPixels: Int = translate.length

  override def setAllPixels(color: Color): F[Unit] =
    fixture.setPixels(translate.flatten.distinct.map((_, color)))

  override def setPixels(colors: Seq[(Int, Color)]): F[Unit] =
    fixture.setPixels(colors.flatMap { case (i, color) => translate(i).map((_, color)) })
}

object Translation {
  /*
  Image:
  y = input
  x = output
   */
  def fromImage[F[_]](fixture: Fixture[F], image: Image): Translation[F] = {
    val translation = Array.tabulate(image.height) { y =>
      (0 until image.width).filter(x => image.getColor(x, y) == Color.White)
    }
    new Translation[F](fixture, translation)
  }
}
