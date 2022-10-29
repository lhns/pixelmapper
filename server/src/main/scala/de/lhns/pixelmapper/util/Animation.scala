package de.lhns.pixelmapper.util

import cats.kernel.Monoid
import io.circe._
import io.circe.generic.semiauto._

import scala.concurrent.duration.{Duration, FiniteDuration}

case class Animation(frames: Seq[Frame], loop: Boolean)

object Animation {
  val empty: Animation = Animation(Seq.empty, loop = false)

  implicit val codec: Codec[Animation] = deriveCodec[Animation]

  implicit val monoid: Monoid[Animation] = Monoid.instance(
    empty,
    { (a, b) =>
      Animation(a.frames ++ b.frames, a.loop || b.loop)
    }
  )

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

  implicit val codec: Codec[Frame] = deriveCodec[Frame]
}
