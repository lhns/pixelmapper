package ledstrip.editor

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.file.Path

import javax.imageio.ImageIO
import scodec.bits.ByteVector

trait Image {
  def width: Int

  def height: Int

  def pixel(x: Int, y: Int): Color

  def row(y: Int): Seq[Color] = (0 until width).map(x => pixel(x, y))

  def column(x: Int): Seq[Color] = (0 until height).map(y => pixel(x, y))

  def withPixel(x: Int, y: Int, color: Color): Image

  def toBufferedImage: BufferedImage = {
    val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    for {
      y <- 0 until height
      x <- 0 until width
    } image.setRGB(x, y, this.pixel(x, y).toARGB)

    image
  }

  def scale(scaleX: Int, scaleY: Int): Image = new Image {
    override def width: Int = Image.this.width * scaleX

    override def height: Int = Image.this.height * scaleY

    override def pixel(x: Int, y: Int): Color = Image.this.pixel(x / scaleX, y / scaleY)

    override def withPixel(x: Int, y: Int, color: Color): Image = Image.this.withPixel(x / scaleX, y / scaleY, color)
  }
}

object Image {
  private def pixelIndex(x: Int, y: Int, width: Int): Int = x + y * width

  case class ImageImpl(width: Int, height: Int, private val pixels: Array[Color]) extends Image {
    require(pixels.length >= width * height)

    def pixel(x: Int, y: Int): Color = pixels(Image.pixelIndex(x, y, width))

    private def withPixels(pixels: Array[Color]): Image = copy(pixels = pixels)

    def withPixel(x: Int, y: Int, color: Color): Image = withPixels(pixels.updated(Image.pixelIndex(x, y, width), color))
  }

  def blank(width: Int, height: Int, color: Color): Image = {
    val pixels = Array.fill(pixelIndex(0, height, width))(color)
    ImageImpl(width, height, pixels)
  }

  def fromBufferedImage(bufferedImage: BufferedImage): Image = {
    val width = bufferedImage.getWidth
    val height = bufferedImage.getHeight

    ImageImpl(width, height,
      (for {
        y <- 0 until height
        x <- 0 until width
      } yield
        Color.fromARGB(bufferedImage.getRGB(x, y))).toArray
    )
  }

  def read(path: Path): Image = fromBufferedImage(ImageIO.read(path.toFile))

  def read(bytes: ByteVector): Image = {
    val inputStream = new ByteArrayInputStream(bytes.toArray)
    fromBufferedImage(ImageIO.read(inputStream))
  }
}