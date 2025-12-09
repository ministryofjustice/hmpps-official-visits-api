package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.Prisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class OfficialVisitDetails(

  @Schema(description = "The official visit id", example = "1")
  val officialVisitId: Long,

  @Schema(description = "The prisoner code")
  val prisonCode: String,
  @Schema(description = "The Official visit prison description")
  val prisonDescription: String?,
  @Schema(description = "The Official visit status type ")
  val visitStatus: VisitStatusType,
  @Schema(description = "The Official visit status description", example = "Visit Status")
  val visitStatusDescription: String,
  @Schema(description = "The Official visit visit type", example = "AP")
  val visitTypeCode: VisitType,
  @Schema(description = "The Official visit type description")
  val visitTypeDescription: String, // from reference_data
  @Schema(description = "The Official visit date")
  val visitDate: LocalDate,
  @Schema(description = "The Official visit start time")
  val startTime: LocalTime,
  @Schema(description = "The Official visit end time")
  val endTime: LocalTime,
  @Schema(description = "The Official visit Location Id")
  val dpsLocationId: UUID,
  @Schema(description = "The Official visit location description")
  val locationDescription: String?,
  @Schema(description = "The Official visit - visitor slot slot identifier for the official visit takes place")
  val visitSlotId: Long,
  @Schema(description = "The Official visit - staff notes")
  val staffNotes: String?,
  @Schema(description = "The Official visit - prisoner notes")
  val prisonerNotes: String?,
  @Schema(description = "The Official visit - visitor concern notes")
  val visitorConcernNotes: String?,
  @Schema(description = "The Official visit completion type")
  val completionCode: VisitCompletionType?,
  @Schema(description = "The Official visit creation description")
  val completionDescription: String?, // from reference data
  @Schema(description = "The Official visit Search Level type")
  val searchTypeCode: SearchLevelType?,
  @Schema(description = "The Official visit search type")
  val searchTypeDescription: String?, // from reference data
  @Schema(description = "The Official visit creation time")
  val createdTime: LocalDateTime,
  @Schema(description = "The Official visit created by User")
  val createdBy: String,
  @Schema(description = "The Official visit updated date time")
  val updatedTime: LocalDateTime?,
  @Schema(description = "The Official visit updated by user")
  val updatedBy: String?,
  @Schema(description = "The Official visit updated by user")
  val officialVisitors: List<OfficialVisitorDetails>?,
  @Schema(description = "The Prisoner Information")
  val prisoner: Prisoner?,
)
