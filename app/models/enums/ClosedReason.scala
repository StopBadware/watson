package models.enums

import anorm.{Column, TypeDoesNotMatch}
import anorm.MayErr.eitherToError

protected class ClosedReason(reason: String) {
  override def toString: String = reason
}

object ClosedReason {
  
  val ABUSIVE: ClosedReason = models.enums.ABUSIVE
  val ADMINISTRATIVE: ClosedReason = models.enums.ADMINISTRATIVE
  val NO_PARTNERS_REPORTING: ClosedReason = models.enums.NO_PARTNERS_REPORTING
  val REVIEWED_BAD: ClosedReason = models.enums.REVIEWED_BAD
  val REVIEWED_CLEAN: ClosedReason = models.enums.REVIEWED_CLEAN
  
  val reasons = Map(
      "ABUSIVE" -> ABUSIVE,
      "ADMINISTRATIVE" -> ADMINISTRATIVE,
      "NO_PARTNERS_REPORTING" -> NO_PARTNERS_REPORTING,
      "REVIEWED_BAD" -> REVIEWED_BAD,
      "REVIEWED_CLEAN" -> REVIEWED_CLEAN
    )
  
  def fromStr(str: String): Option[ClosedReason] = {
    val upper = str.toUpperCase
    return if (reasons.contains(upper)) Some(reasons(upper)) else None
  }
  
  implicit def rowToSource: Column[ClosedReason] = {
    Column.nonNull[ClosedReason] { (value, meta) =>
      val reason = ClosedReason.fromStr(value.toString)
	    if (reason.isDefined) Right(reason.get) else Left(TypeDoesNotMatch(value.toString+" - "+meta))
    }
  }  
  
}

case object ABUSIVE extends ClosedReason("ABUSIVE")
case object ADMINISTRATIVE extends ClosedReason("ADMINISTRATIVE")
case object NO_PARTNERS_REPORTING extends ClosedReason("NO_PARTNERS_REPORTING")
case object REVIEWED_BAD extends ClosedReason("REVIEWED_BAD")
case object REVIEWED_CLEAN extends ClosedReason("REVIEWED_CLEAN")