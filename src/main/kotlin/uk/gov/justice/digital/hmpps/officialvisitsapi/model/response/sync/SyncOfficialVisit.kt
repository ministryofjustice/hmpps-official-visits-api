package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class SyncOfficialVisit(

  @Schema(description = "The internal official visit ID", example = "1")
  val officialVisitId: Long,

  @Schema(description = "The visit date", example = "2020-01-02", required = true)
  val visitDate: LocalDate,

  @Schema(description = "The visit start time", example = "09:15")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The visit end time", example = "10:15")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "The visit slot ID", example = "12345")
  val prisonVisitSlotId: Long,

  @Schema(description = "The DPS location ID where the visit is taking place", example = "aaa-ddd-bbb-123455632323")
  val dpsLocationId: UUID,

  @Schema(description = "The prison establishment code", example = "MDI", required = true)
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

  @Schema(description = "The offender visitor details")
  val visitors: List<SyncOfficialVisitor>,
)
