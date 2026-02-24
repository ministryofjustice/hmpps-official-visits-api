package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class SyncUpdateOfficialVisitRequest(
  @Schema(description = "The NOMIS offender visit ID", example = "133232", required = true)
  var offenderVisitId: Long,

  @Schema(description = "The DPS visit slot ID - this provides the location, start time and end time via configuration data", example = "123132", required = true)
  var prisonVisitSlotId: Long,

  @Schema(description = "The prison code where the visit takes place", example = "PVI", required = true)
  val prisonCode: String,

  @Schema(description = "The offender book ID which is stored in DPS against the visit.", example = "74748", required = true)
  var offenderBookId: Long,

  @Schema(description = "The prisoner number (NOMS ID)", example = "A1234AA", required = true)
  val prisonerNumber: String,

  @Schema(description = "The date the official visit will take place", example = "2022-12-23", required = true)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  var visitDate: LocalDate,

  @Schema(description = "The start time for this official visit", example = "09:15", required = true)
  @JsonFormat(pattern = "HH:mm")
  var startTime: LocalTime,

  @Schema(description = "The end time for this official visit", example = "10:15", required = true)
  @JsonFormat(pattern = "HH:mm")
  var endTime: LocalTime,

  @Schema(description = "The DPS location where the visit takes place.", example = "aaaa-bbbb-xxxxxxxx-yyyyyyyy", required = true)
  var dpsLocationId: UUID,

  @Schema(description = "The DPS visit status code. The Syscon sync service will map the NOMIS state to a value in this enumerated type.", example = "SCHEDULED", required = true)
  var visitStatusCode: VisitStatusType,

  @Schema(description = "The visit comment text", example = "This is a comment", nullable = true)
  val commentText: String? = null,

  @Schema(description = "The prisoner search type code. Maps to the same reference code values in both NOMIS and DPS.", example = "RUB_A", nullable = true)
  val searchTypeCode: SearchLevelType? = null,

  @Schema(description = "The DPS visit completion code. Default is null/not set if not provided.", example = "NORMAL", nullable = true)
  val visitCompletionCode: VisitCompletionType? = null,

  @Schema(description = "Visit concern text from NOMIS", example = "I am concerned", nullable = true)
  val visitorConcernText: String? = null,

  @Schema(description = "The staff username who authorised an override for a ban for this visit", example = "X3243H", nullable = true)
  val overrideBanStaffUsername: String? = null,

  @Schema(description = "The visit order number (if present) for the official visit", example = "12344", nullable = true)
  val visitOrderNumber: Long? = null,

  @Schema(description = "The data and time the record was created", example = "2022-10-01T16:45:45", required = true)
  var updateDateTime: LocalDateTime,

  @Schema(description = "The username who created the visit in NOMIS", example = "X999X", required = true)
  var updateUsername: String,
)
