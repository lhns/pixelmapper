package ledstrip

import io.circe._
import io.circe.generic.semiauto._

case class Color(r: Int, g: Int, b: Int) {
  def toStripColor = new com.github.mbelling.ws281x.Color(r, g, b)
}

object Color {
  implicit val colorDecoder: Decoder[Color] = deriveDecoder[Color]
  implicit val colorEncoder: Encoder[Color] = deriveEncoder[Color]

  val Black = Color(0, 0, 0)
  val White = Color(255, 255, 255)
  val Red = Color(255, 0, 0)
  val Green = Color(0, 255, 0)
  val Blue = Color(0, 0, 255)
}
