package de.lhns.pixelmapper.util

import io.circe._
import io.circe.generic.semiauto._

case class Color(r: Int, g: Int, b: Int)

object Color {
  implicit val colorCodec: Codec[Color] = deriveCodec[Color]

  val Black = Color(0, 0, 0)
  val White = Color(255, 255, 255)
  val Red = Color(255, 0, 0)
  val Green = Color(0, 255, 0)
  val Blue = Color(0, 0, 255)
}
