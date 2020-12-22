// format: off
val ScalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).getOrElse("1.3.1")

addSbtPlugin("ch.epfl.scala"        % "sbt-scalafix"                  % "0.9.24")
addSbtPlugin("org.scala-js"         % "sbt-scalajs"                   % ScalaJSVersion)
addSbtPlugin("com.eed3si9n"         % "sbt-projectmatrix"             % "0.7.0")
addSbtPlugin("com.jsuereth"         % "sbt-pgp"                       % "2.1.1")
addSbtPlugin("com.dwijnand"         % "sbt-dynver"                    % "4.1.1")
addSbtPlugin("org.xerial.sbt"       % "sbt-sonatype"                  % "3.9.5")
addSbtPlugin("org.scalameta"        % "sbt-scalafmt"                  % "2.4.2")
addSbtPlugin("org.scoverage"        % "sbt-scoverage"                 % "1.6.1")
addSbtPlugin("org.scalameta"        % "sbt-mdoc"                      % "2.2.14")
addSbtPlugin("com.eed3si9n"         % "sbt-buildinfo"                 % "0.10.0")
addSbtPlugin("ch.epfl.lamp"         % "sbt-dotty"                     % "0.5.1")
