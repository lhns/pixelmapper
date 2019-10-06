package ledstrip.server

import scalatags.Text.TypedTag
import scalatags.Text.all._

trait ExtraTags {
  protected val pageTitle: TypedTag[String] = tag("title")
  protected val pageStyle: TypedTag[String] = tag("style")
  protected val nav: TypedTag[String] = tag("nav")
  protected val integrity: Attr = attr("integrity")
  protected val crossorigin: Attr = attr("crossorigin")
  protected val `data-toggle`: Attr = data("toggle")
  protected val `data-target`: Attr = data("target")
  protected val `data-width`: Attr = data("width")
  protected val `data-height`: Attr = data("height")
  protected val `aria-orientation`: Attr = attr("aria-orientation")
  protected val `aria-controls`: Attr = attr("aria-controls")
  protected val `aria-selected`: Attr = attr("aria-selected")
  protected val `aria-labelledby`: Attr = attr("aria-labelledby")
  protected val `aria-disabled`: Attr = attr("aria-disabled")
  protected val `aria-valuenow`: Attr = attr("aria-valuenow")
  protected val `aria-valuemin`: Attr = attr("aria-valuemin")
  protected val `aria-valuemax`: Attr = attr("aria-valuemax")
  protected val `aria-live`: Attr = attr("aria-live")
  protected val `aria-atomic`: Attr = attr("aria-atomic")
  protected val `aria-label`: Attr = attr("aria-label")
  protected val `aria-hidden`: Attr = attr("aria-hidden")
  protected val `aria-describedby`: Attr = attr("aria-describedby")
  protected val `aria-expanded`: Attr = attr("aria-expanded")
  protected val `aria-haspopup`: Attr = attr("aria-haspopup")
}
