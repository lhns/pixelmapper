package de.lhns.pixelmapper.util

import io.circe._
import io.circe.generic.semiauto._

import scala.concurrent.duration.{Duration, FiniteDuration}

case class Animation(frames: Seq[Frame], loop: Boolean)

object Animation {
  implicit val animationCodec: Codec[Animation] = deriveCodec[Animation]

  def fromImage(image: Image, delay: FiniteDuration, loop: Boolean): Animation =
    Animation(
      frames = (0 until image.height)
        .map(image.getRow)
        .map(row => Frame(ColorRule.fromColorSeq(row), row.size, delay)),
      loop = loop
    )
}

case class Frame(rules: Seq[ColorRule], width: Int, delay: FiniteDuration)

object Frame {
  private implicit val finiteDurationCodec: Codec[FiniteDuration] = Codec.from(
    Decoder.decodeString.map(Duration(_)).map {
      case finiteDuration: FiniteDuration => finiteDuration
      case _ => throw new RuntimeException("duration must be finite")
    },
    Encoder.encodeString.contramap(_.toString)
  )

  implicit val frameCodec: Codec[Frame] = deriveCodec[Frame]
}
