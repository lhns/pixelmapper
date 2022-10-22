package ledstrip

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import com.github.mbelling.ws281x.{LedStripType, Ws281xLedStrip}

case class LedStrip(ledsCount: Int) {
  val ledStripTask: IO[Ws281xLedStrip] = IO {
    new Ws281xLedStrip(
      ledsCount,
      18,
      800000,
      10,
      255,
      0,
      false,
      LedStripType.WS2811_STRIP_GRB,
      true
    )
  }.memoize.unsafeRunSync()(IORuntime.global)

  def setColors(colorRules: List[ColorRule]): IO[Unit] =
    for {
      ledStrip <- ledStripTask
    } yield {
      colorRules.collectFirst {
        case ColorRule(None, color) =>
          ledStrip.setStrip(color.toStripColor)
      }

      colorRules.collect {
        case ColorRule(Some(leds), color) =>
          for (led <- leds) {
            if (led < ledsCount) ledStrip.setPixel(led, color.toStripColor)
          }
      }

      ledStrip.render()
    }
}