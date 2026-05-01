package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications

class OfficialVisitCreatedEmail(
  override val emailAddress: String,
  private val officialVisitId: Long,
  private val prisonerNumber: String,
) : Email {
  override val type: EmailType = EmailType.OFFICIAL_VISIT_CREATED

  override fun personalisation(): Map<String, String> = mapOf(
    "officialVisitId" to officialVisitId.toString(),
    "prisonerNumber" to prisonerNumber,
  )
}

