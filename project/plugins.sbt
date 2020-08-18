// format: off
val ScalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).getOrElse("1.1.1")

addSbtPlugin("ch.epfl.scala"        % "sbt-scalafix"                  % "0.9.19")
addSbtPlugin("org.scala-js"         % "sbt-scalajs"                   % ScalaJSVersion)
addSbtPlugin("org.portable-scala"   % "sbt-scalajs-crossproject"      % "1.0.0")
addSbtPlugin("com.jsuereth"         % "sbt-pgp"                       % "2.0.1")
addSbtPlugin("com.dwijnand"         % "sbt-dynver"                    % "4.1.1")
addSbtPlugin("org.xerial.sbt"       % "sbt-sonatype"                  % "3.9.4")
addSbtPlugin("org.scalameta"        % "sbt-scalafmt"                  % "2.4.2")
addSbtPlugin("org.scoverage"        % "sbt-scoverage"                 % "1.6.1")
addSbtPlugin("org.scalameta"        % "sbt-mdoc"                      % "2.2.5")
addSbtPlugin("org.jetbrains"        % "sbt-idea-plugin"               % "3.7.2")
