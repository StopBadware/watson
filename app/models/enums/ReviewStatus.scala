package models.enums

import anorm.{Column, TypeDoesNotMatch}
import anorm.MayErr.eitherToError

protected class ReviewStatus(status: String) {
  override def toString: String = status
}

object ReviewStatus {
  
  val BAD: ReviewStatus = models.enums.BAD
  val CLEAN: ReviewStatus = models.enums.CLEAN
  val CLOSED_WITHOUT_REVIEW: ReviewStatus = models.enums.CLOSED_WITHOUT_REVIEW
  val NEW: ReviewStatus = models.enums.NEW
  val PENDING: ReviewStatus = models.enums.PENDING
  val REJECTED: ReviewStatus = models.enums.REJECTED
  
  val statuses = Map(
      "BAD" -> BAD,
      "CLEAN" -> CLEAN,
      "CLOSED_WITHOUT_REVIEW" -> CLOSED_WITHOUT_REVIEW,
      "NEW" -> NEW,
      "PENDING" -> PENDING,
      "REJECTED" -> REJECTED
    )
  
  def fromStr(str: String): Option[ReviewStatus] = {
    val upper = str.toUpperCase
    return if (statuses.contains(upper)) Some(statuses(upper)) else None
  }
  
  implicit def rowToSource: Column[ReviewStatus] = {
    Column.nonNull[ReviewStatus] { (value, meta) =>
      val status = ReviewStatus.fromStr(value.toString)
	    if (status.isDefined) Right(status.get) else Left(TypeDoesNotMatch(value.toString+" - "+meta))
    }
  }  
  
}

case object BAD extends ReviewStatus("BAD")
case object CLEAN extends ReviewStatus("CLEAN")
case object CLOSED_WITHOUT_REVIEW extends ReviewStatus("CLOSED_WITHOUT_REVIEW")
case object NEW extends ReviewStatus("NEW")
case object PENDING extends ReviewStatus("PENDING")
case object REJECTED extends ReviewStatus("REJECTED")