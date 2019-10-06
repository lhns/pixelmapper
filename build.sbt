name := "led-strip-server"
version := "0.0.1"

mainClass := Some("ledstrip.server.Main")

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "io.monix" %% "monix" % "3.0.0",
  "io.circe" %% "circe-core" % "0.11.1",
  "io.circe" %% "circe-generic" % "0.11.1",
  "io.circe" %% "circe-parser" % "0.11.1",
  "org.http4s" %% "http4s-dsl" % "0.20.11",
  "org.http4s" %% "http4s-blaze-server" % "0.20.11",
  "org.http4s" %% "http4s-blaze-client" % "0.20.11",
  "org.http4s" %% "http4s-circe" % "0.20.11",
  "com.github.mbelling" % "rpi-ws281x-java" % "2.0.0"
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")


assembly / assemblyJarName := s"${name.value}-${version.value}.sh.bat"

assembly / assemblyOption := (assembly / assemblyOption).value
  .copy(prependShellScript = Some(defaultUniversalScript(shebang = false)))

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

def universalScript(shellCommands: String,
                    cmdCommands: String,
                    shebang: Boolean): String = {
  Seq(
    if (shebang) "#!/usr/bin/env sh" else "",
    "@ 2>/dev/null # 2>nul & echo off & goto BOF\r",
    ":",
    shellCommands.replaceAll("\r\n|\n", "\n"),
    "exit",
    Seq(
      "",
      ":BOF",
      cmdCommands.replaceAll("\r\n|\n", "\r\n"),
      "exit /B %errorlevel%",
      ""
    ).mkString("\r\n")
  ).filterNot(_.isEmpty).mkString("\n")
}

def defaultUniversalScript(shebang: Boolean,
                           javaOpts: Seq[String] = Seq.empty,
                           javaw: Boolean = false): Seq[String] = {
  val javaOptsString = javaOpts.map(_ + " ").mkString
  Seq(universalScript(
    shellCommands = {
      def javaCommand(args: String): String =
        s"exec java $args"

      "if [ -n \"$JAVA_HOME\" ]; then PATH=\"$JAVA_HOME/bin:$PATH\"; fi\n" +
        javaCommand(s"""-jar $javaOptsString$$JAVA_OPTS "$$0" "$$@"""")
    },
    cmdCommands = {
      def javaCommand(args: String): String =
        if (javaw)
          s"""start "" javaw $args"""
        else
          s"java $args"

      "if not \"%JAVA_HOME%\"==\"\" set \"PATH=%JAVA_HOME%\\bin;%PATH%\"\n" +
        javaCommand(s"""-jar $javaOptsString%JAVA_OPTS% "%~dpnx0" %*""")
    },
    shebang = shebang
  ))
}
