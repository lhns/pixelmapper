package ledstrip

import io.circe._
import io.circe.generic.semiauto._

case class ColorRule(leds: Option[List[Int]], color: Color)

object ColorRule {
  implicit val colorRuleDecoder: Decoder[ColorRule] = deriveDecoder[ColorRule]
  implicit val colorRuleEncoder: Encoder[ColorRule] = deriveEncoder[ColorRule]

  def compressList(rules: List[ColorRule]): List[ColorRule] = {
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