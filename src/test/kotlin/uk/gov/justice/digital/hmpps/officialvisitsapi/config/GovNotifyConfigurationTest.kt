package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications.EmailType

class GovNotifyConfigurationTest {
  @Test
  fun `should map configured templates to matching email types`() {
    val configuration = GovNotifyConfiguration(
      apiKey = "test-api-key",
      officialVisitCreatedTemplateId = "created-template-id",
      officialVisitCancelledTemplateId = "cancelled-template-id",
      officialVisitUpdatedTemplateId = "updated-template-id",
    )

    val templates = configuration.emailTemplates()

    templates.templateIdFor(EmailType.OFFICIAL_VISIT_CREATED) isEqualTo "created-template-id"
    templates.templateIdFor(EmailType.OFFICIAL_VISIT_CANCELLED) isEqualTo "cancelled-template-id"
    templates.templateIdFor(EmailType.OFFICIAL_VISIT_UPDATED) isEqualTo "updated-template-id"
  }
}
