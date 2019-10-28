package ledstrip.editor

import ledstrip.editor.Mask.MaskPoint

case class Mask(width: Int, height: Int, points: Seq[MaskPoint]) {
  def toLine(image: Image): Seq[Color] = {
    val maxIndex = points.map(_.index).max
    (0 to maxIndex).map { i =>
      points.find(_.index == i).map(point => image(point.x, point.y)).getOrElse(Color.Black)
    }
  }

  def fromLine(line: Seq[Color]): Image = {
    val image = Image.blank(width, height, Color.White)
    line.zipWithIndex.foldLeft(image) {
      case (image, (color, index)) =>
        points.collectFirst {
          case MaskPoint(x, y, `index`) =>
            image.withPixel(x, y, color)
        }.getOrElse(image)
    }
  }
}

object Mask {

  case class MaskPoint(x: Int, y: Int, index: Int)

}
