package de.lhns

import org.http4s.MediaType
import org.http4s.util.Renderer

import java.util.Base64

package object pixelmapper {
  def makeDataUrl(mediaType: MediaType, data: Array[Byte]): String =
    s"data:${Renderer.renderString(mediaType)};base64,${Base64.getEncoder.encodeToString(data)}"
}
