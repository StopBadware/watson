import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "watson"
  val appVersion      = "1.0"

  val appDependencies = Seq(
    jdbc,
    anorm,
    "com.yuvimasory" % "jerkson_2.10" % "0.6.1",
    "net.debasishg" % "redisclient_2.10" % "2.10",
    "com.typesafe" % "play-plugins-redis_2.10" % "2.1.1",
    "com.typesafe.play" %% "play-cache" % "2.2.0",
    "postgresql" % "postgresql" % "9.1-901.jdbc4",
    "org.mockito" % "mockito-core" % "1.9.5"
  )
  
  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "pk11 repo" at "http://pk11-scratch.googlecode.com/svn/trunk/"
  )

}
