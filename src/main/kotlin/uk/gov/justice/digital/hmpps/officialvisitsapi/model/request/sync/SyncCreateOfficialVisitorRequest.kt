package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import java.time.LocalDate
import java.time.LocalDateTime

data class SyncCreateOfficialVisitorRequest(
  @Schema(description = "The NOMIS offender visit visitor ID", example = "133232", required = true)
  @field:NotNull(message = "The NOMIS offender visit visitor ID is mandatory")
  val offenderVisitVisitorId: Long?,

  @Schema(description = "The NOMIS person ID (same as contactId) for this visitor", example = "13989898", required = true)
  @field:NotNull(message = "The NOMIS person ID is mandatory")
  val personId: Long?,

  @Schema(description = "The first name of the visitor", example = "Bob", nullable = true)
  val firstName: String? = null,

  @Schema(description = "The last name of the visitor", example = "Harris", nullable = true)
  val lastName: String? = null,

  @Schema(description = "The visitors date of birth", example = "2011-01-03", nullable = true)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val dateOfBirth: LocalDate? = null,

  @Schema(description = "The relationship type OFFICIAL or SOCIAL. Default is null if not known.", example = "OFFICIAL", nullable = true)
  val relationshipTypeCode: RelationshipType? = null,

  @Schema(description = "The relationship code between visitor and prisoner, from NOMIS reference data. A null value will indicate no relationship.", example = "POL", nullable = true)
  val relationshipToPrisoner: String? = null,

  @Schema(description = "Set to true if this person is the lead visitor. Defaults to false if not supplied.", example = "true", nullable = true)
  val groupLeaderFlag: Boolean? = false,

  @Schema(description = "Set to true if this person requires assistance at the visit. Defaults to false if not supplied.", example = "true", nullable = true)
  val assistedVisitFlag: Boolean? = false,

  @Schema(description = "The visitor comment text from NOMIS", example = "Some comments", nullable = true)
  val commentText: String? = null,

  @Schema(description = "The visitor attendance code (ATTENDED or ABSENT). A null indicates no attendance was added.", example = "ATTENDED", nullable = true)
  val attendanceCode: AttendanceType? = null,

  @Schema(description = "The data and time the record was created", example = "2022-10-01T16:45:45", required = true)
  var createDateTime: LocalDateTime? = null,

  @Schema(description = "The username who created the row", example = "X999X", required = true)
  var createUsername: String? = null,
)
