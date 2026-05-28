package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime
import java.util.UUID

data class NotifyCallbackRequest(
  val id: UUID,
  val status: String,
  @JsonProperty("completed_at")
  val completedAt: OffsetDateTime? = null,
  val reference: String? = null,
  @JsonProperty("notification_type")
  val notificationType: String? = null,
)
