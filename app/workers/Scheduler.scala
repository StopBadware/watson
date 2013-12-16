package workers

import akka.actor.ActorSystem
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import play.api.libs.concurrent.Execution.Implicits._
import controllers.Blacklist

object Scheduler {
  	
  def main(args: Array[String]): Unit = {
    val interval = new FiniteDuration(30, TimeUnit.SECONDS)
    val system = ActorSystem("CheckBlacklistQueue")
    system.scheduler.schedule(Duration.Zero, interval, CheckBlacklistQueue())
  }
  
  case class CheckBlacklistQueue() extends Runnable {
    
    def run() = {
      //TODO WTSN-39 check redis queue
      //TODO WTSN-39 call Blacklist.importDifferential
      println("IamA worker dyno AMA!")	//DELME WTSN-39
//      Blacklist.importDifferential(reported, source, time)
    }
    
  }

}