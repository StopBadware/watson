package models.enums

import anorm.{Column, TypeDoesNotMatch}
import anorm.MayErr.eitherToError

protected class ReviewStatus(status: String) {
  def isOpen: Boolean = this.isInstanceOf[Open]
  override def toString: String = status
}

object ReviewStatus {
  
  val CLOSED_BAD: ReviewStatus = models.enums.CLOSED_BAD
  val CLOSED_CLEAN: ReviewStatus = models.enums.CLOSED_CLEAN
  val CLOSED_NO_LONGER_REPORTED: ReviewStatus = models.enums.CLOSED_NO_LONGER_REPORTED
  val CLOSED_WITHOUT_REVIEW: ReviewStatus = models.enums.CLOSED_WITHOUT_REVIEW
  val NEW: ReviewStatus = models.enums.NEW
  val PENDING_BAD: ReviewStatus = models.enums.PENDING_BAD
  val REJECTED: ReviewStatus = models.enums.REJECTED
  val REOPENED: ReviewStatus = models.enums.REOPENED
  
  val statuses = Map(
    "CLOSED_BAD" -> CLOSED_BAD,
    "CLOSED_CLEAN" -> CLOSED_CLEAN,
    "CLOSED_NO_LONGER_REPORTED" -> CLOSED_NO_LONGER_REPORTED,
    "CLOSED_WITHOUT_REVIEW" -> CLOSED_WITHOUT_REVIEW,
    "NEW" -> NEW,
    "PENDING_BAD" -> PENDING_BAD,
    "REJECTED" -> REJECTED,
    "REOPENED" -> REOPENED
  )
  
  def fromStr(str: String): Option[ReviewStatus] = {
    val upper = str.toUpperCase
    return if (statuses.contains(upper)) Some(statuses(upper)) else None
  }
  
  implicit def rowToReviewStatus: Column[ReviewStatus] = {
    Column.nonNull[ReviewStatus] { (value, meta) =>
      val status = ReviewStatus.fromStr(value.toString)
	    if (status.isDefined) Right(status.get) else Left(TypeDoesNotMatch(value.toString+" - "+meta))
    }
  }
  
}

case object CLOSED_BAD extends ReviewStatus("CLOSED_BAD")
case object CLOSED_CLEAN extends ReviewStatus("CLOSED_CLEAN")
case object CLOSED_NO_LONGER_REPORTED extends ReviewStatus("CLOSED_NO_LONGER_REPORTED")
case object CLOSED_WITHOUT_REVIEW extends ReviewStatus("CLOSED_WITHOUT_REVIEW")
case object NEW extends ReviewStatus("NEW") with Open
case object PENDING_BAD extends ReviewStatus("PENDING_BAD") with Open
case object REJECTED extends ReviewStatus("REJECTED") with Open
case object REOPENED extends ReviewStatus("REOPENED") with Open

protected trait Open {}