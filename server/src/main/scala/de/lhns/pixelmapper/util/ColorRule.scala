package de.lhns.pixelmapper.util

import io.circe._
import io.circe.generic.semiauto._

case class ColorRule(leds: Option[Seq[Int]], color: Color)

object ColorRule {
  implicit val colorRuleCodec: Codec[ColorRule] = deriveCodec[ColorRule]

  def compressList(rules: Seq[ColorRule]): Seq[ColorRule] = {
    val backgroundColor = rules.collectFirst {
      case ColorRule(None, color) => color
    }

    backgroundColor.map(ColorRule(None, _)).toList ++ rules.collect {
      case ColorRule(Some(leds), color) if !backgroundColor.contains(color) =>
        leds.map(_ -> color)
    }.flatten.groupBy(_._2).map {
      case (color, ledsAndColors) => ColorRule(Some(ledsAndColors.map(_._1)), color)
    }.toList
  }

  def fromColorSeq(colorSeq: IndexedSeq[Color]): Seq[ColorRule] =
    ColorRule.compressList(
      ColorRule(None, Color.Black) +:
        colorSeq.indices.map { i =>
          val color = colorSeq(i)
          ColorRule(Some(Seq(i)), color)
        }
    )

  def toColorSeq(rules: Seq[ColorRule], length: Int): IndexedSeq[Color] = {
    val array = Array.fill[Color](length)(Color.Black)
    for {
      rule <- rules
      i <- rule.leds.getOrElse(0 until length)
    } {
      array(i) = rule.color
    }
    array
  }
}