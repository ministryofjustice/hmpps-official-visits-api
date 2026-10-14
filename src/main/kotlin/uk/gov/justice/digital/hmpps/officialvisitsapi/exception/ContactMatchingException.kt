package uk.gov.justice.digital.hmpps.officialvisitsapi.exception

class ContactMatchingException(
  val officialVisitId: Long,
  val officialVisitorId: Long,
  val contactId: Long? = null,
  override val message: String,
) : RuntimeException(message)
