import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "watson"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "com.codahale" % "jerkson_2.9.1" % "0.6.0-SNAPSHOT"
  )
  
  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += "codahale" at "http://repo.codahale.com/"      
  )

}
