package models.enums

import anorm._
import play.api.db._
import play.api.Play.current
import anorm.MayErr.eitherToError

protected class Source(shortName: String) {
  
  val abbr = shortName
  
  lazy val fullName: String = DB.withConnection { implicit conn =>
    val name = SQL("SELECT full_name FROM sources WHERE abbr={abbr}::SOURCE").on("abbr"->abbr).apply().headOption
    if (name.isDefined) name.get[String]("full_name") else ""
  }
  
  override def toString: String = abbr
}

object Source {
  
  val GOOG: Source = models.enums.GOOG
  val NSF: Source = models.enums.NSF
  val TTS: Source = models.enums.TTS
  val SBW: Source = models.enums.SBW
  val SBWCR: Source = models.enums.SBWCR
  
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
