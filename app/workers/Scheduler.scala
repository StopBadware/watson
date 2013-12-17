package workers

import akka.actor.ActorSystem
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import play.api.libs.concurrent.Execution.Implicits._
import controllers.{Blacklist, Redis}
import models.Source

object Scheduler {
  	
  def main(args: Array[String]): Unit = {
    val interval = new FiniteDuration(30, TimeUnit.SECONDS)
    val system = ActorSystem("CheckBlacklistQueue")
    val sourcesWithDifferential = List(Source.GOOG, Source.NSF)
    sourcesWithDifferential.foreach { source =>
    	system.scheduler.schedule(Duration.Zero, interval, CheckQueue(source))
    }
  }
  
}

case class CheckQueue(source: Source) extends Runnable {
  
  def blacklistQueue: List[Blacklist] = {
    val foo = Redis.blacklistTimes(source).foldLeft(List.empty[Blacklist]) { (list, time) =>
      //TODO WTSN-39 get vals for keys
    	//TODO WTSN-39 umarshal list
      list :+ Blacklist(source, time, List())
    }
    println(foo)	//DELME WTSN-39
    //TODO WTSN-39 return list of blacklist
    List()	//DELME WTSN-39
  }
  
  def run() = {
		val blacklists = blacklistQueue
		blacklistQueue.foreach { blacklist =>
		  Blacklist.importDifferential(blacklist.urls, blacklist.source, blacklist.time)
		  //TODO WTSN-39 delete from queue
		}
  }
  
}