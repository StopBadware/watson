package models

case class Host(host: String) {

}

object Host {
  
  def reverse(host: String): String = host.split("\\.").reverse.mkString(".")

}