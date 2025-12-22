package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType

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
  val relationshipType: RelationshipType?,

  @Schema(description = "The visitor relationship code", example = "POM")
  val relationshipCode: String?,

  @Schema(description = "The visitor attendance code, either ABSENT, ATTENDED or null if not recorded.", example = "ABSENT")
  val attendanceCode: AttendanceType?,
)
