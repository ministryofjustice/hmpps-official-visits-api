package uk.gov.justice.digital.hmpps.officialvisitsapi.exception

class OffenderVisitIdConflictException(
  val offenderVisitId: Long,
  val officialVisitId: Long,
  override val message: String,
) : RuntimeException(message)
