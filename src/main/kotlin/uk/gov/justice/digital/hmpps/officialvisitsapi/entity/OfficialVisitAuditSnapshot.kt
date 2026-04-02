package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/** Immutable copy of auditable OfficialVisit fields taken after load. */
data class OfficialVisitAuditSnapshot(
  val prisonVisitSlotId: Long,
  val visitDate: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val dpsLocationId: UUID,
  val visitTypeCode: String,
  val prisonCode: String,
  val prisonerNumber: String,
  val currentTerm: Boolean,
  val staffNotes: String?,
  val prisonerNotes: String?,
  val visitorConcernNotes: String?,
  val overrideBanBy: String?,
  val offenderBookId: Long?,
  val offenderVisitId: Long?,
  val visitOrderNumber: Long?,
  val visitStatusCode: String,
  val completionCode: String?,
  val completionNotes: String?,
  val searchTypeCode: String?,
) {
  companion object {
    fun from(visit: OfficialVisitEntity) = OfficialVisitAuditSnapshot(
      prisonVisitSlotId = visit.prisonVisitSlot.prisonVisitSlotId,
      visitDate = visit.visitDate,
      startTime = visit.startTime,
      endTime = visit.endTime,
      dpsLocationId = visit.dpsLocationId,
      visitTypeCode = visit.visitTypeCode.name,
      prisonCode = visit.prisonCode,
      prisonerNumber = visit.prisonerNumber,
      currentTerm = visit.currentTerm,
      staffNotes = visit.staffNotes,
      prisonerNotes = visit.prisonerNotes,
      visitorConcernNotes = visit.visitorConcernNotes,
      overrideBanBy = visit.overrideBanBy,
      offenderBookId = visit.offenderBookId,
      offenderVisitId = visit.offenderVisitId,
      visitOrderNumber = visit.visitOrderNumber,
      visitStatusCode = visit.visitStatusCode.name,
      completionCode = visit.completionCode?.name,
      completionNotes = visit.completionNotes,
      searchTypeCode = visit.searchTypeCode?.name,
    )
  }
}
