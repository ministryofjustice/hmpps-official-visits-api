package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Defines the migration request received from the Syscon migration service to pass the data for one official visit
 * from NOMIS into this service and store into its DB.
 */

data class MigrateVisitRequest(
  @Schema(description = "The NOMIS offender visit ID", example = "133232")
  @field:NotNull(message = "The NOMIS offender visit ID is mandatory")
  val offenderVisitId: Long,

  @Schema(description = "The DPS visit slot ID - this provides the location, start time and end time via configuration data", example = "123132")
  @field:NotNull(message = "The DPS visit slot ID is mandatory")
  val prisonVisitSlotId: Long,

  @field:NotBlank(message = "The prison code is mandatory")
  @Schema(description = "The prison code where the visit takes place", example = "PVI")
  val prisonCode: String?,

  @field:NotBlank(message = "The prisoner number for the official visit is mandatory")
  @field:Size(max = 7, message = "Prisoner number must not exceed {max} characters")
  @Schema(description = "The prisoner number (NOMS ID)", example = "A1234AA")
  val prisonerNumber: String?,

  @field:NotNull(message = "The current term flag is mandatory")
  @Schema(description = "If this visits relates to the current term (booking) in prison true, else false.", example = "true")
  val currentTerm: Boolean,

  @field:NotNull(message = "The date for the official visit is mandatory")
  @Schema(description = "The date the official visit will take place", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val visitDate: LocalDate?,

  @field:NotNull(message = "The DPS location ID is mandatory")
  @Schema(description = "The DPS location where the visit takes place", example = "aaaa-bbbb-xxxxxxxx-yyyyyyyy")
  val dpsLocationId: UUID,

  @Schema(description = "The visit status code from NOMIS (reference data)", example = "SCH")
  @field:NotNull(message = "The status code from NOMIS is mandatory")
  val visitStatusCode: CodedValue,

  @Schema(description = "The visit type code from NOMIS expected to be O for all(reference data)", example = "O")
  @field:NotNull(message = "The visit type code is mandatory")
  val visitTypeCode: CodedValue,

  @Schema(description = "The comment text from NOMIS", example = "This is a comment", nullable = true)
  val commentText: String? = null,

  @Schema(description = "The search type code from NOMIS (reference data)", example = "RUB_A", nullable = true)
  val searchTypeCode: CodedValue? = null,

  @Schema(description = "The event outcome code NOMIS (reference data)", example = "", nullable = true)
  val eventOutcomeCode: CodedValue? = null,

  @Schema(description = "The outcome reason code NOMIS (reference data)", example = "", nullable = true)
  val outcomeReasonCode: CodedValue? = null,

  @Schema(description = "The raised indcident type code from NOMIS (reference data)", example = "", nullable = true)
  val raisedIncidentTypeCode: CodedValue? = null,

  @Schema(description = "The raised incident number from NOMIS", example = "1333", nullable = true)
  val incidentNumber: Long? = null,

  @Schema(description = "The outcome reason code NOMIS (reference data)", example = "", nullable = true)
  val visitorConcernText: String? = null,

  @Schema(description = "The staff username who authorised an override for a ban", example = "X3243H", nullable = true)
  val overrideBanStaffUsername: String?,

  @Schema(description = "The data and time the record was created", nullable = true, example = "2022-10-01T16:45:45")
  var createDateTime: LocalDateTime? = null,

  @Schema(description = "The username who created the row", nullable = true, example = "X999X")
  var createUsername: String? = null,

  @Schema(description = "The date and time the record was last amended", nullable = true, example = "2022-10-01T16:45:45")
  var modifyDateTime: LocalDateTime? = null,

  @Schema(description = "The username who last modified the row", nullable = true, example = "X999X")
  var modifyUsername: String? = null,

  val visitors: List<MigrateVisitor> = emptyList(),
)

@Schema(description = "The details of an official visitor")
data class MigrateVisitor(
  @field:NotNull(message = "The NOMIS offender visit visitor ID is mandatory")
  @Schema(description = "The NOMIS offender visit visitor ID", example = "133232")
  val offenderVisitVisitorId: Long,

  @field:NotNull(message = "The NOMIS person ID is mandatory")
  @Schema(description = "The NOMIS person ID (same as contactId) for this visitor", example = "13989898")
  val personId: Long,

  @Schema(description = "If this visits relates to the current term (booking) in prison true, else false.", example = "true", nullable = true)
  val groupLeaderFlag: Boolean? = false,

  @field:NotNull(message = "The current term flag is mandatory")
  @Schema(description = "If this visits relates to the current term (booking) in prison true, else false.", example = "true", nullable = true)
  val assistedVisitFlag: Boolean? = false,

  @Schema(description = "The comment text from NOMIS", example = "This is a comment", nullable = true)
  val commentText: String? = null,

  @Schema(description = "The event outcome code NOMIS (reference data)", example = "", nullable = true)
  val eventOutcomeCode: CodedValue? = null,

  @Schema(description = "The outcome reason code NOMIS (reference data)", example = "", nullable = true)
  val outcomeReasonCode: CodedValue? = null,

  @Schema(description = "The data and time the record was created", nullable = true, example = "2022-10-01T16:45:45")
  var createDateTime: LocalDateTime? = null,

  @Schema(description = "The username who created the row", nullable = true, example = "X999X")
  var createUsername: String? = null,

  @Schema(description = "The date and time the record was last amended", nullable = true, example = "2022-10-01T16:45:45")
  var modifyDateTime: LocalDateTime? = null,

  @Schema(description = "The username who last modified the row", nullable = true, example = "X999X")
  var modifyUsername: String? = null,
)

data class CodedValue(
  @Schema(description = "A coded value from NOMIS reference data", maxLength = 12, example = "CODE")
  @field:Size(max = 12, message = "Coded values must be <= 12 characters")
  val code: String,

  @Schema(description = "The description for this coded value in NOMIS", example = "Description")
  val description: String,
)
