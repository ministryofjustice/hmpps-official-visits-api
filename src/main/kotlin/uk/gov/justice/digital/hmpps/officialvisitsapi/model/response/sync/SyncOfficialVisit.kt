package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.SearchLevelType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class SyncOfficialVisit(

  @Schema(description = "The official visit ID", example = "1")
  val officialVisitId: Long,

  @Schema(description = "The date that the visit takes place", example = "2020-01-02", required = true)
  val visitDate: LocalDate,

  @Schema(description = "The start time", example = "09:15")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The end time", example = "10:15")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "The visit slot ID", example = "12345")
  val prisonVisitSlotId: Long,

  @Schema(description = "The DPS location ID where the visit is taking place", example = "aaa-ddd-bbb-123455632323")
  val dpsLocationId: UUID,

  @Schema(description = "The prison code", example = "MDI", required = true)
  val prisonCode: String,

  @Schema(description = "The prisoner number", example = "A1111AA", required = true)
  val prisonerNumber: String,

  @Schema(description = "The visit status code", example = "SCHEDULED", required = true)
  val statusCode: VisitStatusType,

  @Schema(description = "The visit completion code", example = "NORMAL")
  val completionCode: VisitCompletionType? = null,

  @Schema(description = "The offender booking ID in NOMIS", example = "12345")
  val offenderBookId: Long? = null,

  @Schema(description = "The offender visit ID in NOMIS (only present for migrated bookings)", example = "12345")
  val offenderVisitId: Long? = null,

  @Schema(description = "The type of visit. NOMIS assumes in person, but we may want to indicate others via notes", example = "IN_PERSON")
  val visitType: VisitType,

  @Schema(description = "Whether the prisoner attended or not", example = "ATTENDED")
  val prisonerAttendance: AttendanceType? = null,

  @Schema(description = "The prisoner search type", example = "FULL")
  val searchType: SearchLevelType? = null,

  @Schema(description = "Comments provided for the prisoner", example = "These are notes for the prisoner")
  val visitComments: String? = null,

  @Schema(description = "The username of the person who created the visit", example = "X8393", required = true)
  val createdBy: String,

  @Schema(description = "The date and time the visit was created", example = "2024-12-01T03:05:00", required = true)
  val createdTime: LocalDateTime,

  @Schema(description = "The username of the person who last updated the visit", example = "X8393")
  val updatedBy: String? = null,

  @Schema(description = "The date and time the visit was last updated", example = "2024-12-01T03:05:00")
  val updatedTime: LocalDateTime? = null,

  @Schema(description = "The visitor details")
  val visitors: List<SyncOfficialVisitor>,
)
