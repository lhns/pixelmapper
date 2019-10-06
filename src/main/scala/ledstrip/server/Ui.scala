package ledstrip.server

import scalatags.Text.TypedTag
import scalatags.Text.all._

object Ui extends ExtraTags {
  def ui: TypedTag[String] = html(
    head(`class` := "h-100",
      meta(charset := "UTF8"),

      pageTitle("Led Strip Server"),

      link(rel := "stylesheet", href := "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css", integrity := "sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T", crossorigin := "anonymous"),

      script(src := "https://code.jquery.com/jquery-3.4.1.min.js", integrity := "sha256-CSXorXvZcTkaix6Yvo6HppcZGetbYMGWSFlBw8HfCJo=", crossorigin := "anonymous"),
      script(src := "https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js", integrity := "sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1", crossorigin := "anonymous"),
      script(src := "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js", integrity := "sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM", crossorigin := "anonymous"),
      script(src := "https://use.fontawesome.com/2171858466.js"),

      pageStyle(
        """.btn {
          |    padding: 0;
          |    touch-action: manipulation;
          |}
          |
          |.flex-fill {
          |    flex-basis: 0 !important;
          |}
          |""".stripMargin
      )
    ),
    body(`class` := "h-100",
      div(`class` := "h-100 d-flex flex-column",
        div(`class` := "flex-fill"),
        div(`class` := "flex-fill d-flex flex-row",
          div(`class` := "flex-fill"),
          button(`type` := "submit", `class` := "btn btn-danger flex-fill fa fa-power-off"),
          div(`class` := "flex-fill")
        ),
        div(`class` := "flex-fill")
      )
    )
  )
}
