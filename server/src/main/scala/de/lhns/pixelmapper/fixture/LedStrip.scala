package de.lhns.pixelmapper.fixture

import cats.effect.kernel.Async
import com.github.mbelling.ws281x.{LedStripType, Ws281xLedStrip}
import de.lhns.pixelmapper.util.Color

class LedStrip[F[_] : Async] private(ledStrip: Ws281xLedStrip) extends Fixture[F] {
  override def numPixels: Int = ledStrip.getLedsCount

  private implicit def toLedStripColor(color: Color): com.github.mbelling.ws281x.Color =
    new com.github.mbelling.ws281x.Color(color.r, color.g, color.b)

  override def setPixel(i: Int, color: Color): F[Unit] = Async[F].blocking {
    if (i >= 0 && i < numPixels) {
      ledStrip.setPixel(i, color)
      ledStrip.render()
    }
  }

  override def setAllPixels(color: Color): F[Unit] = Async[F].blocking {
    ledStrip.setStrip(color)
    ledStrip.render()
  }

  override def setPixels(colors: IndexedSeq[Color], fixtureOffset: Int, fixtureLength: Int, arrayOffset: Int, arrayLength: Int): F[Unit] = Async[F].blocking {
    for (i <- Math.max(0, fixtureOffset) until Math.min(numPixels, fixtureOffset + fixtureLength)) {
      ledStrip.setPixel(i, colors(i * arrayLength / fixtureLength + arrayOffset))
    }
    ledStrip.render()
  }
}

object LedStrip {
  def apply[F[_] : Async](numLeds: Int, gpioPin: Int = 18): F[LedStrip[F]] = Async[F].blocking {
    new LedStrip(new Ws281xLedStrip(
      numLeds,
      gpioPin,
      800000,
      10,
      255,
      0,
      false,
      LedStripType.WS2811_STRIP_GRB,
      true
    ))
  }
}
