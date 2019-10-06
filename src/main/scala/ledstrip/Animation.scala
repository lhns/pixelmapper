package ledstrip

import io.circe._
import io.circe.generic.semiauto._

case class Animation(frames: List[Frame], loop: Boolean)

object Animation {
  implicit val animationDecoder: Decoder[Animation] = deriveDecoder[Animation]
  implicit val animationEncoder: Encoder[Animation] = deriveEncoder[Animation]
}

case class Frame(delay: Int, rules: List[ColorRule])

object Frame {
  implicit val frameDecoder: Decoder[Frame] = deriveDecoder[Frame]
  implicit val frameEncoder: Encoder[Frame] = deriveEncoder[Frame]
}
