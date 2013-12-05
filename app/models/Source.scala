package models

import anorm._
import play.api.db._
import play.api.Play.current

class Source(shortName: String) {
  
  val abbr = shortName
  
  lazy val fullName: String = DB.withConnection { implicit conn =>
    val name = SQL("SELECT full_name FROM sources WHERE abbr={abbr}::SOURCE").on("abbr"->abbr).apply().headOption
    if (name.isDefined) name.get[String]("full_name") else ""
  }
  
  override def toString: String = abbr
}

object Source {
  
  val GOOG: Source = models.GOOG
  val NSF: Source = models.NSF
  val TTS: Source = models.TTS
  val SBW: Source = models.SBW
  val SBWCR: Source = models.SBWCR
  
  val sources = Map(
      "GOOG" -> GOOG,
      "NSF" -> NSF,
      "TTS" -> TTS,
      "SBW" -> SBW,
      "SBWCR" -> SBWCR
    )
  
  def withAbbr(abbr: String): Option[Source] = {
    val upper = abbr.toUpperCase
    return if (sources.contains(upper)) Some(sources(upper)) else None
  }
  
  implicit def rowToSource: Column[Source] = {
    Column.nonNull[Source] { (value, meta) =>
      val source = Source.withAbbr(value.toString)
	    if (source.isDefined) Right(source.get) else Left(TypeDoesNotMatch(value.toString+" - "+meta))
    }
  }  
  
}

case object GOOG extends Source("GOOG")
case object NSF extends Source("NSF")
case object TTS extends Source("TTS")
case object SBW extends Source("SBW")
case object SBWCR extends Source("SBWCR")
