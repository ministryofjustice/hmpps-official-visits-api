package uk.gov.justice.digital.hmpps.officialvisitsapi.exception

class DuplicateOffenderVisitIdConflictException(
  val offenderVisitId: Long,
  val officialVisitId: Long,
  override val message: String,
) : RuntimeException(message)
