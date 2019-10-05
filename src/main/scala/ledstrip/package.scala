import com.github.mbelling.ws281x.Color
import io.circe._
import io.circe.generic.semiauto._

package object ledstrip {

  private case class JsonColor(r: Int, g: Int, b: Int)

  implicit val colorDecoder: Decoder[Color] = deriveDecoder[JsonColor].map(e => new Color(e.r, e.g, e.b))
  implicit val colorEncoder: Encoder[Color] = deriveEncoder[JsonColor].contramap[Color](e => JsonColor(e.getRed, e.getGreen, e.getBlue))

  case class ColorRule(leds: Option[List[Int]], color: Color)

  object ColorRule {
    implicit val colorRuleDecoder: Decoder[ColorRule] = deriveDecoder[ColorRule]
    implicit val colorRuleEncoder: Encoder[ColorRule] = deriveEncoder[ColorRule]
  }

}
