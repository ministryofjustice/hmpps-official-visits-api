package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import java.time.LocalDateTime

data class OfficialVisitorDetails(

  @Schema(description = "The Official visitor Visit Type code")
  val visitorTypeCode: VisitorType,

  @Schema(description = "The Official visitor TypeDescription")
  val visitorTypeDescription: String?,

  @Schema(description = "The Official visitor first name")
  val firstName: String?,

  @Schema(description = "The Official visitor last name")
  val lastName: String?,

  @Schema(description = "The Official visitor contact id")
  val contactId: Long?,

  @Schema(description = "The prisoner contact id")
  val prisonerContactId: Long?,

  @Schema(description = "The Official visitor relationship type code")
  val relationshipTypeCode: RelationshipType?,

  @Schema(description = "The Official visitor relationship Type Description")
  val relationshipTypeDescription: String?,

  @Schema(description = "The Official visitor relationship code")
  val relationshipCode: String?,

  @Schema(description = "The Official visitor relationship description")
  val relationshipDescription: String?,

  @Schema(description = "The Official visitor - is lead visitor")
  val leadVisitor: Boolean,

  @Schema(description = "The Official visitor - is assisted visit")
  val assistedVisit: Boolean,

  @Schema(description = "The Official visitor visitor notes")
  val visitorNotes: String?,

  @Schema(description = "The Official visitor attendance type")
  val attendanceCode: AttendanceType?,

  @Schema(description = "The Official visitor created by user")
  val createdBy: String,

  @Schema(description = "The Official visitor created date time")
  val createdTime: LocalDateTime,

  @Schema(description = "The Official visitor updated by user")
  val updatedBy: String?,

  @Schema(description = "The Official visitor updated date time")
  val updatedTime: LocalDateTime?,

  @Schema(description = "The Official visitor offender visit visitor id")
  val offenderVisitVisitorId: Long?,

  @Schema(description = "The Official visitor attendance description")
  val attendanceDescription: String?,
)
