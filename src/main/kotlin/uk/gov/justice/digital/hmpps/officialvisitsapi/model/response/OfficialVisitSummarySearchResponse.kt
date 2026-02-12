package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class OfficialVisitSummarySearchResponse(
  @Schema(description = "The official visit id", example = "1")
  val officialVisitId: Long,

  @Schema(description = "The 3 letter code for the prison", example = "MDI")
  val prisonCode: String,

  @Schema(description = "The official visit prison description", example = "Moorland (HMP & YOI)")
  val prisonDescription: String,

  @Schema(description = "The official visit status type", example = "SCHEDULED")
  val visitStatus: VisitStatusType,

  @Schema(description = "The official visit status description", example = "Visit status description")
  val visitStatusDescription: String,

  @Schema(description = "The official visit type code", example = "IN_PERSON")
  val visitTypeCode: VisitType,

  @Schema(description = "The official visit type description")
  val visitTypeDescription: String,

  @Schema(description = "The date the official visit takes place", example = "2022-12-23")
  val visitDate: LocalDate,

  @Schema(description = "The start time of the official visit", example = "10:00")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The end time of the official visit", example = "11:00")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "The DPS location ID where the official visit takes place", example = "aaaa-bbbb-9f9f9f9f-9f9f9f9f")
  val dpsLocationId: UUID,

  @Schema(description = "The official visit location description", example = "Legal visits ward")
  val locationDescription: String,

  @Schema(description = "The prison visit slot identifier for the official visit slot", example = "1")
  val visitSlotId: Long,

  @Schema(description = "Notes for staff that will not be shared on movement slips", example = "Legal representation details")
  val staffNotes: String?,

  @Schema(description = "Notes for prisoners that may be shared on movement slips", example = "Please arrive 10 minutes early")
  val prisonerNotes: String?,

  val visitorConcernNotes: String?,

  @Schema(description = "The number of visitors attending the official visit", example = "2")
  val numberOfVisitors: Int,

  @Schema(description = "The official visit completion type", example = "VISITOR_CANCELLED")
  val completionCode: VisitCompletionType?,

  @Schema(description = "The official visit completion description")
  val completionDescription: String?,

  @Schema(description = "Optional notes captured when a visit is either cancelled or completed", example = "Cancelled due to prisoner in hospital")
  val completionNotes: String?,

  @Schema(description = "The name of the user who created the official visit", example = "Fred Bloggs")
  val createdBy: String,

  @Schema(description = "The date and time the official visit was created", example = "2025-12-02 14:45")
  val createdTime: LocalDateTime,

  @Schema(description = "The name of the last user who updated the official visit", example = "Jane Bloggs")
  val updatedBy: String?,

  @Schema(description = "The date and time the official visit was last updated", example = "22025-12-04 09:50")
  val updatedTime: LocalDateTime?,

  @Schema(description = "The details of the prisoner being visited")
  val prisoner: PrisonerVisitedDetails,
)
