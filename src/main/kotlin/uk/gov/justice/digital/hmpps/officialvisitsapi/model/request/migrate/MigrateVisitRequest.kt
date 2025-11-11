package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/**
 * Defines the migration request received from the Syscon migration service to pass the data for one official visit
 * from NOMIS into this service and store into its DB.
 */

data class MigrateVisitRequest(
  @Schema(description = "The NOMIS offender visit ID", example = "133232", required = true)
  @field:NotNull(message = "The NOMIS offender visit ID is mandatory")
  val offenderVisitId: Long?,

  @Schema(description = "The DPS visit slot ID - this provides the location, start time and end time via configuration data", example = "123132", required = true)
  @field:NotNull(message = "The DPS visit slot ID is mandatory")
  val prisonVisitSlotId: Long?,

  @Schema(description = "The prison code where the visit takes place", example = "PVI", required = true)
  @field:NotBlank(message = "The prison code is mandatory")
  val prisonCode: String?,

  @Schema(description = "The offender book ID to echo back", example = "74748", required = true)
  @field:NotNull(message = "The offender book ID is mandatory")
  val offenderBookId: Long?,

  @Schema(description = "The prisoner number (NOMS ID)", example = "A1234AA", required = true)
  @field:NotBlank(message = "The prisoner number for the official visit is mandatory")
  @field:Size(max = 7, message = "Prisoner number must not exceed {max} characters")
  val prisonerNumber: String?,

  @Schema(description = "If this visits relates to the current term (booking) in prison true, else false.", example = "true", required = true)
  @field:NotNull(message = "The current term flag is mandatory")
  val currentTerm: Boolean?,

  @Schema(description = "The date the official visit will take place", example = "2022-12-23", required = true)
  @field:NotNull(message = "The date for the official visit is mandatory")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val visitDate: LocalDate?,

  @Schema(description = "The start time for this official visit", example = "09:15", required = true)
  @field:NotNull(message = "The start time is mandatory")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime?,

  @Schema(description = "The end time for this official visit", example = "10:15", required = true)
  @field:NotNull(message = "The end time is mandatory")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(description = "The DPS location where the visit takes place", example = "aaaa-bbbb-xxxxxxxx-yyyyyyyy", required = true)
  @field:NotNull(message = "The DPS location ID is mandatory")
  val dpsLocationId: UUID?,

  @Schema(description = "The visit status code from NOMIS (reference data - VIS_STS)", example = "SCH", required = true)
  @field:NotNull(message = "The status code from NOMIS is mandatory")
  val visitStatusCode: CodedValue?,

  @Schema(description = "The visit type code from NOMIS expected to be O for all(reference data - VISIT_TYPE)", example = "O", required = true)
  @field:NotNull(message = "The visit type code is mandatory")
  val visitTypeCode: CodedValue?,

  @Schema(description = "The comment text from NOMIS", example = "This is a comment", nullable = true)
  val commentText: String? = null,

  @Schema(description = "The search type code from NOMIS (reference data - SEARCH_TYPE)", example = "RUB_A", nullable = true)
  val searchTypeCode: CodedValue? = null,

  @Schema(description = "The event outcome code NOMIS (reference data - VIS_COMPLETE)", example = "", nullable = true)
  val eventOutcomeCode: CodedValue? = null,

  @Schema(description = "The outcome reason code NOMIS (reference data - MOVE_CANC_RS)", example = "", nullable = true)
  val outcomeReasonCode: CodedValue? = null,

  @Schema(description = "The raised indcident type code from NOMIS (reference data - INC_TYPE)", example = "", nullable = true)
  val raisedIncidentTypeCode: CodedValue? = null,

  @Schema(description = "The raised incident number from NOMIS", example = "1333", nullable = true)
  val incidentNumber: Long? = null,

  @Schema(description = "Concerns raised by the visitor", example = "Visitor concern notes", nullable = true)
  val visitorConcernText: String? = null,

  @Schema(description = "The staff username who authorised an override for a ban", example = "X3243H", nullable = true)
  val overrideBanStaffUsername: String?,

  @Schema(description = "The data and time the record was created", example = "2022-10-01T16:45:45", required = true)
  var createDateTime: LocalDateTime? = null,

  @Schema(description = "The username who created the row", example = "X999X", required = true)
  var createUsername: String? = null,

  @Schema(description = "The date and time the record was last amended", nullable = true, example = "2022-10-01T16:45:45")
  var modifyDateTime: LocalDateTime? = null,

  @Schema(description = "The username who last modified the row", nullable = true, example = "X999X")
  var modifyUsername: String? = null,

  val visitors: List<MigrateVisitor> = emptyList(),
)

@Schema(description = "The details of an official visitor")
data class MigrateVisitor(
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

  @Schema(description = "The coded relationships between this visitor and the prisoner", nullable = true)
  val relationshipToPrisoner: CodedValue? = null,

  @Schema(description = "Set to true if this person is the lead visitor. Defaults to false if not supplied.", example = "true", nullable = true)
  val groupLeaderFlag: Boolean? = false,

  @Schema(description = "Set to true if this person requires assistance at the visit. Defaults to false if not supplied.", example = "true", nullable = true)
  val assistedVisitFlag: Boolean? = false,

  @Schema(description = "The comment text from NOMIS", example = "Comment text", nullable = true)
  val commentText: String? = null,

  @Schema(description = "The event outcome code NOMIS (reference data)", example = "", nullable = true)
  val eventOutcomeCode: CodedValue? = null,

  @Schema(description = "The outcome reason code NOMIS (reference data)", example = "", nullable = true)
  val outcomeReasonCode: CodedValue? = null,

  @Schema(description = "The data and time the record was created", example = "2022-10-01T16:45:45", required = true)
  var createDateTime: LocalDateTime? = null,

  @Schema(description = "The username who created the row", example = "X999X", required = true)
  var createUsername: String? = null,

  @Schema(description = "The date and time the record was last amended", nullable = true, example = "2022-10-01T16:45:45")
  var modifyDateTime: LocalDateTime? = null,

  @Schema(description = "The username who last modified the row", nullable = true, example = "X999X")
  var modifyUsername: String? = null,
)

data class CodedValue(
  @Schema(description = "A coded value from NOMIS reference data", maxLength = 12, example = "CODE")
  @field:Size(max = 12, message = "Coded values must be <= 12 characters")
  val code: String?,

  @Schema(description = "The description for this coded value in NOMIS", example = "Description")
  val description: String?,
)
