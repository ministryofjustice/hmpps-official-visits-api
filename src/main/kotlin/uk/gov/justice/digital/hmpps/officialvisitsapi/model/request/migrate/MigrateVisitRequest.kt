package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

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

  @Schema(description = "The offender book ID to echo back. It will be stored in DPS against the visit.", example = "74748", required = true)
  @field:NotNull(message = "The offender book ID is mandatory")
  val offenderBookId: Long?,

  @Schema(description = "The prisoner number (NOMS ID)", example = "A1234AA", required = true)
  @field:NotBlank(message = "The prisoner number for the official visit is mandatory")
  @field:Size(max = 7, message = "Prisoner number must not exceed {max} characters")
  val prisonerNumber: String?,

  @Schema(description = "If this visits relates to the current or latest term (booking) in prison true, else false.", example = "true", required = true)
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

  @Schema(description = "The DPS location where the visit takes place.", example = "aaaa-bbbb-xxxxxxxx-yyyyyyyy", required = true)
  @field:NotNull(message = "The DPS location ID is mandatory")
  val dpsLocationId: UUID?,

  @Schema(description = "The DPS visit status code. The Syscon migration service will map the NOMIS state to a value in this enumerated type.", example = "SCHEDULED", required = true)
  @field:NotNull(message = "The visit status code from NOMIS is mandatory")
  val visitStatusCode: VisitStatusType?,

  @Schema(description = "The DPS visit type code. For migrated NOMIS visits this will default to type UNKNOWN. Other values are IN_PERSON, VIDEO, or TELEPHONE.", example = "UNKNOWN", nullable = true)
  val visitTypeCode: VisitType? = VisitType.UNKNOWN,

  @Schema(description = "The visit comment text", example = "This is a comment", nullable = true)
  val commentText: String? = null,

  @Schema(description = "The prisoner search type code. Maps to the same reference code values in both NOMIS and DPS.", example = "RUB_A", nullable = true)
  val searchTypeCode: SearchLevelType? = null,

  @Schema(description = "The DPS visit completion code. Default is NORMAL if not supplied.", example = "NORMAL", nullable = true)
  val visitCompletionCode: VisitCompletionType? = VisitCompletionType.NORMAL,

  @Schema(description = "Visit concern text from NOMIS", example = "I am concerned", nullable = true)
  val visitorConcernText: String? = null,

  @Schema(description = "The staff username who authorised an override for a ban for this visit", example = "X3243H", nullable = true)
  val overrideBanStaffUsername: String? = null,

  @Schema(description = "The visit order number (if present) for the official visit", example = "12344", nullable = true)
  val visitOrderNumber: Long? = null,

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

  @Schema(description = "The date and time the record was last amended", nullable = true, example = "2022-10-01T16:45:45")
  var modifyDateTime: LocalDateTime? = null,

  @Schema(description = "The username who last modified the row", nullable = true, example = "X999X")
  var modifyUsername: String? = null,
)
