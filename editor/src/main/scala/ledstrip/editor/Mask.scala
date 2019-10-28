package ledstrip.editor

import ledstrip.editor.Mask.MaskPoint

case class Mask(width: Int, height: Int, points: Seq[MaskPoint]) {
  def toLine(image: Image): Seq[Color] = {
    val maxIndex = points.map(_.index).max
    (0 to maxIndex).map { i =>
      points.find(_.index == i).map(point => image.pixel(point.x, point.y)).getOrElse(Color.Black)
    }
  }

  def fromLine(line: Seq[Color]): Image = {
    val image = Image.blank(width, height, Color.White)
    line.zipWithIndex.foldLeft(image) {
      case (image, (color, index)) =>
        points.foldLeft(image) {
          case (image, MaskPoint(x, y, `index`)) =>
            image.withPixel(x, y, color)

          case (image, _) =>
            image
        }
      /*points.collectFirst {
        case MaskPoint(x, y, `index`) =>
          image.withPixel(x, y, color)
      }.getOrElse(image)*/
    }
  }
}

object Mask {

  case class MaskPoint(x: Int, y: Int, index: Int)

  def fromImage(image: Image): Mask = {
    val width = image.width
    val height = image.height

    Mask(width, height,
      for {
        y <- 0 until height
        x <- 0 until width
        color = image.pixel(x, y)
        _ <- Some(()).filter(_ => color.r == 255)
      } yield
        MaskPoint(x, y, color.g + color.b * 255)
    )
  }

}
