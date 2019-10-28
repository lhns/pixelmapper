package ledstrip.editor

import java.awt

case class Color(r: Int, g: Int, b: Int) {
  def toARGB: Int = {
    (0xFF << 24) |
      ((r & 0xFF) << 16) |
      ((g & 0xFF) << 8) |
      ((b & 0xFF))
  }

  def toAwtColor: awt.Color = new awt.Color(r, g, b)
}

object Color {
  def fromARGB(argb: Int): Color = {
    val r = (argb >> 16) & 0xFF
    val g = (argb >> 8) & 0xFF
    val b = argb & 0xFF
    Color(r, g, b)
  }

  object Black extends Color(0, 0, 0)

  object White extends Color(255, 255, 255)

  object Red extends Color(255, 0, 0)

  object Green extends Color(0, 255, 0)

  object Blue extends Color(0, 0, 255)

}
