import NativePackagerHelper.directory
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

val http4sVersion = "0.21.2"
val circeVersion = "0.13.0"
val circeConfigVersion = "0.7.0"
val fs2Version = "2.2.2"
val doobieVersion = "0.9.0"

lazy val root = (project in file("."))
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .settings(
    name := "taskapi",
    version:= "1.0",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions += "-target:jvm-1.8",
    version in Docker := "latest",
    packageName in Docker := "taskmanager",
    dockerExposedPorts in Docker := Seq(8080),
    scalaVersion := "2.13.1",
    mainClass in Compile := Some("examples.engine.Engine"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-core" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "com.github.fd4s" %% "fs2-kafka" % "1.1.0",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-fs2" % circeVersion,
      "io.circe" %% "circe-config" % circeConfigVersion,
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "org.scalatest" %% "scalatest" % "3.1.1" % "test"
    ),
    unmanagedClasspath in Runtime += baseDirectory.value / "conf",
    mappings in Universal ++= directory("conf"),
    scriptClasspath in bashScriptDefines ~= {cp => "/opt/docker/conf" +: cp},

  )
