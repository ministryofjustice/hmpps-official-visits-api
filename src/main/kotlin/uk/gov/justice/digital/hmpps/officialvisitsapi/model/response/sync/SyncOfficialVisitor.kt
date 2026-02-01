package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import java.time.LocalDateTime

data class SyncOfficialVisitor(
  @Schema(description = "The internal official visitor ID", example = "1")
  val officialVisitorId: Long,

  @Schema(description = "The contact ID of the person visiting", example = "123")
  val contactId: Long? = null,

  @Schema(description = "The visitor first Name", example = "John")
  val firstName: String? = null,

  @Schema(description = "The visitor last Name", example = "Smith")
  val lastName: String? = null,

  @Schema(description = "The relationship type for this visitor (OFFICIAL or SOCIAL)", example = "OFFICIAL")
  val relationshipType: RelationshipType? = RelationshipType.OFFICIAL,

  @Schema(description = "The visitor relationship code", example = "POM")
  val relationshipCode: String? = null,

  @Schema(description = "The visitor attendance code, either ABSENT, ATTENDED or null if not recorded.", example = "ABSENT")
  val attendanceCode: AttendanceType? = null,

  @Schema(description = "Set to true if this is the lead visitor", example = "false")
  val leadVisitor: Boolean? = false,

  @Schema(description = "Set to true if this visitor requires an assisted visit. The equivalent of the AVPU flag in NOMIS", example = "false")
  val assistedVisit: Boolean? = false,

  @Schema(description = "Visitor specific notes", example = "Wheelchair access required")
  val visitorNotes: String? = null,

  @Schema(description = "The username of the person who created the visitor", example = "X8393", required = true)
  val createdBy: String,

  @Schema(description = "The date and time the visitor was created", example = "2024-12-01T03:05:00", required = true)
  val createdTime: LocalDateTime,

  @Schema(description = "The username of the person who last updated the visitor", example = "X8393")
  val updatedBy: String? = null,

  @Schema(description = "The date and time the visitor was last updated", example = "2024-12-01T03:05:00")
  val updatedTime: LocalDateTime? = null,
)
