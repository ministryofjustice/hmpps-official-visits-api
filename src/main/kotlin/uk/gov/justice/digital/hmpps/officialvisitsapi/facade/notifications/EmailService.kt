package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.notifications

import java.util.UUID

typealias NotificationId = UUID
typealias TemplateId = String

fun interface EmailService {
  fun send(email: Email): Result<Pair<NotificationId, TemplateId>>
}

// TODO this may be better as an abstract class
interface Email {
  val emailAddress: String
  val type: EmailType

  fun personalisation(): Map<String, String>
}

class DummyEmail(override val emailAddress: String) : Email {
  override val type: EmailType = EmailType.DUMMY_EMAIL

  override fun personalisation(): Map<String, String> = emptyMap()
}
