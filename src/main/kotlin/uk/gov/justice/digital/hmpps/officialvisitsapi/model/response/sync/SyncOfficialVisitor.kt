package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType

data class SyncOfficialVisitor(
  @Schema(description = "The official visitor id", example = "1")
  val officialVisitorId: Long,

  @Schema(description = "The official contact id")
  val contactId: Long? = null,

  @Schema(description = "The official visitor first Name")
  val firstName: String? = null,

  @Schema(description = "The official visitor last Name")
  val lastName: String? = null,

  @Schema(description = "The official visitor relationship type")
  val relationshipType: RelationshipType?,

  @Schema(description = "The official visitor relationship code")
  val relationshipCode: String?,

  @Schema(description = "The official visitor attendance code")
  val attendanceCode: AttendanceType?,
)
