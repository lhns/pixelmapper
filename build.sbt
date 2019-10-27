inThisBuild(Seq(
  name := "led-strip",
  scalaVersion := "2.12.10"
))

name := (ThisBuild / name).value

def settings: Seq[SettingsDefinition] = Seq(
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4"),

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

    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)

lazy val ledStripServer = project.in(file("."))
  .settings(settings: _*)
  .settings(
    name := "led-strip-server",
    version := "0.0.1",

    mainClass := Some("ledstrip.server.Main"),

    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "io.monix" %% "monix" % "3.0.0",
      "io.circe" %% "circe-core" % "0.11.1",
      "io.circe" %% "circe-generic" % "0.11.1",
      "io.circe" %% "circe-parser" % "0.11.1",
      "com.lihaoyi" %% "scalatags" % "0.7.0",
      "org.http4s" %% "http4s-dsl" % "0.20.11",
      "org.http4s" %% "http4s-blaze-server" % "0.20.11",
      "org.http4s" %% "http4s-blaze-client" % "0.20.11",
      "org.http4s" %% "http4s-circe" % "0.20.11",
      "org.http4s" %% "http4s-scalatags" % "0.20.11",
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
    version := "0.0.1",

    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.0.0",
      "org.openjfx" % "javafx-base" % "12.0.2" classifier osName,
      "org.openjfx" % "javafx-controls" % "12.0.2" classifier osName,
      "org.openjfx" % "javafx-graphics" % "12.0.2" classifier osName,
      "org.scalafx" %% "scalafx" % "12.0.2-R18",
      "com.miglayout" % "miglayout-javafx" % "5.2"
    )
  )

lazy val editor = project.in(file("editor"))
  .settings(settings: _*)
  .settings(
    name := "led-strip-editor",
    version := "0.0.1",

    libraryDependencies ++= Seq(
      "io.monix" %% "monix" % "3.0.0",
      "co.fs2" %% "fs2-core" % "2.0.0",
      "org.openjfx" % "javafx-base" % "12.0.2" classifier osName,
      "org.openjfx" % "javafx-controls" % "12.0.2" classifier osName,
      "org.openjfx" % "javafx-graphics" % "12.0.2" classifier osName,
      "org.scalafx" %% "scalafx" % "12.0.2-R18"
    )
  )
