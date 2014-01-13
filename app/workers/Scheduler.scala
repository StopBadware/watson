package workers

import java.io.File
import akka.actor.ActorSystem
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import play.api.{DefaultApplication, Logger, Mode, Play}
import play.api.libs.concurrent.Execution.Implicits._
import controllers.{Blacklist, Redis}
import models.Source

object Scheduler {
  	
  def main(args: Array[String]): Unit = {
    Play.start(new DefaultApplication(new File("."), Scheduler.getClass.getClassLoader, None, Mode.Prod))
    val interval = new FiniteDuration(60, TimeUnit.SECONDS)
    val system = ActorSystem("ImportBlacklistQueue")
    system.scheduler.schedule(Duration.Zero, interval, BlacklistQueue())
  }
  
}

case class BlacklistQueue() extends Runnable {
  
  def blacklists: Map[Source, List[Long]] = {
    val sourcesWithDifferential = List(Source.GOOG, Source.TTS)
    return sourcesWithDifferential.map(source => source -> Redis.blacklistTimes(source)).toMap
  }
  
  def importQueue() = {
    blacklists.foreach { case (source, times) =>
      times.sortWith(_ < _).foreach { time =>
        val blacklist = Redis.getBlacklist(source, time)
        val success = Blacklist.importDifferential(blacklist, source, time)
        if (success || blacklist.isEmpty) {
        	Redis.dropBlacklist(source, time)
        } else {
        	Logger.error("Importing "+source+" blacklist "+time+" from queue failed")
        }
      }
    }
  }
  
  def run() = importQueue()
  
}