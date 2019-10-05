package ledstrip

import com.github.mbelling.ws281x.{LedStripType, Ws281xLedStrip}
import monix.eval.Task

case class LedStrip(ledsCount: Int) {
  val ledStripTask: Task[Ws281xLedStrip] = Task {
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
  }.memoizeOnSuccess

  def setColors(colorRules: List[ColorRule]): Task[Unit] =
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