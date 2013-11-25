package models

case class Host(host: String) {
  
  val reversed = host.split("\\.").reverse.mkString(".")

}

object Host {
  
  def reverse(host: String): String = host.split("\\.").reverse.mkString(".")

}