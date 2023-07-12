ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

lazy val macros =
  (project in file("macros"))
    .settings(
      name := "Macros",
      libraryDependencies ++= List(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"),
      libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.10")

lazy val main =
  (project in file("main"))
    .dependsOn(macros)
    .settings(name := "Main")

val vZio = "2.0.15"
val vAkka = "2.6.21"
val vFs2 = "3.7.0"
lazy val bench =
  (project in file("bench"))
    .enablePlugins(JmhPlugin)
    .settings(name := "Benchmark")
//    .settings(
//      Jmh / sourceDirectory := (Test / sourceDirectory).value,
//      Jmh / classDirectory := (Test / classDirectory).value,
//      Jmh / dependencyClasspath := (Test / dependencyClasspath).value,
//      // rewire tasks, so that 'bench/Jmh/run' automatically invokes 'bench/Jmh/compile' (otherwise a clean 'bench/Jmh/run' would fail)
//      Jmh / compile := (Jmh / compile).dependsOn(Test / compile).value,
//      Jmh / run := (Jmh / run).dependsOn(Jmh / compile).evaluated
//    )
    .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % vZio,
      "dev.zio" %% "zio-streams" % vZio,
      "com.typesafe.akka" %% "akka-actor" % vAkka,
      "com.typesafe.akka" %% "akka-stream" % vAkka,
      "co.fs2" %% "fs2-core" % vFs2,
      "co.fs2" %% "fs2-io" % vFs2
    )
  )

lazy val root =
  (project in file("."))
    .aggregate(macros, main)
    .settings(
      name := "Sandbox"
    )

ThisBuild / libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % vAkka
ThisBuild / libraryDependencies += "dev.zio" %% "zio" % vZio
//ThisBuild / scalacOptions += "-Vprint:typer"