package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.Immutable
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Entity
@Immutable
@Table(name = "v_official_visits_booked")
data class VisitBookedEntity(
  @Id
  val officialVisitId: Long,

  val prisonCode: String,

  val dayCode: String,

  val dayDescription: String,

  val prisonVisitSlotId: Long,

  val prisonTimeSlotId: Long,

  val visitDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime,

  val visitStatusCode: String? = null,

  val visitTypeCode: String,

  val prisonerNumber: String,

  val contactId: Long? = null,

  val visitorTypeCode: String? = null,

  val relationshipTypeCode: String? = null,

  val relationshipCode: String? = null,

  val firstName: String? = null,

  val lastName: String? = null,

  val dpsLocationId: UUID,
) {
  fun isVisitType(visitType: VisitType) = VisitType.valueOf(visitTypeCode) == visitType
}
