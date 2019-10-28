package ledstrip.editor

import java.awt.image.BufferedImage

import ledstrip.editor.Image._

case class Image(width: Int, height: Int, private val pixels: Array[Color]) {
  require(pixels.length >= width * height)

  def apply(x: Int, y: Int): Color = pixels(Image.pixelIndex(x, y, width))

  private def withPixels(pixels: Array[Color]): Image = copy(pixels = pixels)

  def withPixel(x: Int, y: Int, color: Color): Image = withPixels(pixels.updated(Image.pixelIndex(x, y, width), color))

  def toBufferedImage: BufferedImage = {
    val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    for {
      y <- 0 until height
      x <- 0 until width
    } image.setRGB(x, y, pixels(pixelIndex(x, y, width)).toARGB)

    image
  }
}

object Image {
  private def pixelIndex(x: Int, y: Int, width: Int): Int = x + y * width

  def blank(width: Int, height: Int, color: Color): Image = {
    val pixels = Array.fill(pixelIndex(0, height, width))(color)
    Image(width, height, pixels)
  }
}