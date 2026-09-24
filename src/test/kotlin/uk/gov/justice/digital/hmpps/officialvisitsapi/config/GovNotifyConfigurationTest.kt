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
      inPersonVisitConfirmedTemplateId = "in-person-visit-confirmed-template-id",
      inPersonVisitAmendedTemplateId = "in-person-visit-amended-template-id",
      inPersonVisitCancelledTemplateId = "in-person-visit-cancelled-template-id",
      telephoneVisitConfirmedTemplateId = "telephone-visit-confirmed-template-id",
      telephoneVisitAmendedTemplateId = "telephone-visit-amended-template-id",
      telephoneVisitCancelledTemplateId = "telephone-visit-cancelled-template-id",
      videoVisitConfirmedTemplateId = "video-visit-confirmed-template-id",
      videoVisitAmendedTemplateId = "video-visit-amended-template-id",
      videoVisitCancelledTemplateId = "video-visit-cancelled-template-id",
    )

    val templates = configuration.emailTemplates()

    templates.templateIdFor(EmailType.OFFICIAL_VISIT_CREATED) isEqualTo "created-template-id"
    templates.templateIdFor(EmailType.OFFICIAL_VISIT_CANCELLED) isEqualTo "cancelled-template-id"
    templates.templateIdFor(EmailType.OFFICIAL_VISIT_UPDATED) isEqualTo "updated-template-id"
    templates.templateIdFor(EmailType.IN_PERSON_VISIT_CONFIRMED) isEqualTo "in-person-visit-confirmed-template-id"
    templates.templateIdFor(EmailType.IN_PERSON_VISIT_AMENDED) isEqualTo "in-person-visit-amended-template-id"
    templates.templateIdFor(EmailType.IN_PERSON_VISIT_CANCELLED) isEqualTo "in-person-visit-cancelled-template-id"
    templates.templateIdFor(EmailType.TELEPHONE_VISIT_CONFIRMED) isEqualTo "telephone-visit-confirmed-template-id"
    templates.templateIdFor(EmailType.TELEPHONE_VISIT_AMENDED) isEqualTo "telephone-visit-amended-template-id"
    templates.templateIdFor(EmailType.TELEPHONE_VISIT_CANCELLED) isEqualTo "telephone-visit-cancelled-template-id"
    templates.templateIdFor(EmailType.VIDEO_VISIT_CONFIRMED) isEqualTo "video-visit-confirmed-template-id"
    templates.templateIdFor(EmailType.VIDEO_VISIT_AMENDED) isEqualTo "video-visit-amended-template-id"
    templates.templateIdFor(EmailType.VIDEO_VISIT_CANCELLED) isEqualTo "video-visit-cancelled-template-id"
  }
}
