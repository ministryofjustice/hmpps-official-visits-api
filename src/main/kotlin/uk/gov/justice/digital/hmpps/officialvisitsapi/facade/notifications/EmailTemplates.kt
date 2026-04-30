package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications

class EmailTemplates(private val templates: Set<EmailTemplate>) {
  fun templateIdFor(emailType: EmailType): TemplateId? = templates.singleOrNull { it.emailType == emailType }?.templateId
}

data class EmailTemplate(val templateId: TemplateId, val emailType: EmailType)

enum class EmailType {
  DUMMY_EMAIL,
}
