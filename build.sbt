name := "RedisScalaCombinators"

version := "1.0"

scalaVersion := "2.11.7"

parallelExecution in Test := false

libraryDependencies ++= Seq(
  "net.debasishg" %% "redisclient" % "3.0",
  "org.scalaz" %% "scalaz-core" % "7.1.4",
  "io.reactivex" %% "rxscala" % "0.25.0",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test"
)
