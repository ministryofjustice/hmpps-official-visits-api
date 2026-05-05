package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications

import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.Email
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.emails.EmailType

class OfficialVisitCreatedEmail(
  emailAddress: String,
) : Email(emailAddress) {

  override fun type() = EmailType.OFFICIAL_VISIT_CREATED
}
