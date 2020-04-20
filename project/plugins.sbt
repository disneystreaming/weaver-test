// format: off
val ScalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).getOrElse("0.6.31")

addSbtPlugin("org.scala-js"         % "sbt-scalajs"                   % ScalaJSVersion)
addSbtPlugin("org.portable-scala"   % "sbt-scalajs-crossproject"      % "0.6.1")
addSbtPlugin("com.jsuereth"         % "sbt-pgp"                       % "2.0.1")
addSbtPlugin("com.typesafe"         % "sbt-mima-plugin"               % "0.6.1")
addSbtPlugin("com.dwijnand"         % "sbt-dynver"                    % "4.0.0")
addSbtPlugin("org.xerial.sbt"       % "sbt-sonatype"                  % "3.8.1")
addSbtPlugin("de.heikoseeberger"    % "sbt-header"                    % "5.4.0")
addSbtPlugin("org.scalameta"        % "sbt-scalafmt"                  % "2.3.0")
addSbtPlugin("org.scoverage"        % "sbt-scoverage"                 % "1.6.1")
addSbtPlugin("org.scalameta"        % "sbt-mdoc"                      % "2.1.1")
addSbtPlugin("net.virtual-void"     % "sbt-dependency-graph"          % "0.10.0-RC1")
