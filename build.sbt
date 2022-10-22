inThisBuild(Seq(
  name := "led-strip",
  scalaVersion := "2.13.10"
))


val V = new {
  val betterMonadicFor = "0.3.1"
  val cats = "2.6.1"
  val catsEffect = "3.3.14"
  val circe = "0.14.3"
  val fs2 = "3.3.0"
  val http4s = "0.23.16"
  val http4sJdkHttpClient = "0.7.0"
  val http4sScalatags = "0.25.0"
  val logbackClassic = "1.4.4"
}

name := (ThisBuild / name).value

def settings: Seq[SettingsDefinition] = Seq(
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % V.betterMonadicFor),

  assembly / assemblyJarName := s"${name.value}-${version.value}.sh.bat",

  assembly / assemblyOption := (assembly / assemblyOption).value
    .copy(prependShellScript = Some(AssemblyPlugin.defaultUniversalScript(shebang = false))),

  assembly / assemblyMergeStrategy := {
    case PathList("module-info.class") =>
      MergeStrategy.discard

    case PathList("META-INF", "jpms.args") =>
      MergeStrategy.discard

    case PathList("META-INF", "io.netty.versions.properties") =>
      MergeStrategy.first

    case PathList("libws281x.so") =>
      MergeStrategy.last

    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)

lazy val ledStripServer = project.in(file("."))
  .settings(settings: _*)
  .settings(
    name := "led-strip-server",
    version := "0.1.2",

    Compile / mainClass := Some("ledstrip.client.Main"),

    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % V.logbackClassic,
      "io.circe" %% "circe-core" % V.circe,
      "io.circe" %% "circe-generic" % V.circe,
      "io.circe" %% "circe-parser" % V.circe,
      "org.http4s" %% "http4s-circe" % V.http4s,
      "org.http4s" %% "http4s-dsl" % V.http4s,
      "org.http4s" %% "http4s-ember-server" % V.http4s,
      "org.http4s" %% "http4s-jdk-http-client" % V.http4sJdkHttpClient,
      "org.http4s" %% "http4s-scalatags" % V.http4sScalatags,
      "org.typelevel" %% "cats-effect" % V.catsEffect,
      "com.github.mbelling" % "rpi-ws281x-java" % "2.0.0"
    )
  )

def osName: String =
  if (scala.util.Properties.isLinux) "linux"
  else if (scala.util.Properties.isMac) "mac"
  else if (scala.util.Properties.isWin) "win"
  else throw new Exception("Unknown platform!")

lazy val simulator = project.in(file("simulator"))
  .settings(settings: _*)
  .settings(
    name := "led-strip-simulator",
    version := "0.0.3",

    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % V.cats,
      "org.openjfx" % "javafx-base" % "11.0.2" classifier osName,
      "org.openjfx" % "javafx-controls" % "11.0.2" classifier osName,
      "org.openjfx" % "javafx-graphics" % "11.0.2" classifier osName,
      "org.openjfx" % "javafx-media" % "11.0.2" classifier osName,
      "org.scalafx" %% "scalafx" % "11-R16",
      "com.miglayout" % "miglayout-javafx" % "5.2"
    )
  )

/*lazy val editor = project.in(file("editor"))
  .settings(settings: _*)
  .settings(
    name := "led-strip-editor",
    version := "0.0.1",

    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-io" % V.fs2,
      "org.openjfx" % "javafx-base" % "11.0.2" classifier osName,
      "org.openjfx" % "javafx-controls" % "11.0.2" classifier osName,
      "org.openjfx" % "javafx-graphics" % "11.0.2" classifier osName,
      "org.openjfx" % "javafx-media" % "11.0.2" classifier osName,
      "org.scalafx" %% "scalafx" % "11-R16",
      "org.typelevel" %% "cats-effect" % V.catsEffect
    )
  )*/
