package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class SyncOfficialVisit(

  @Schema(description = "The official visit id", example = "1")
  val officialVisitId: Long,

  @Schema(description = "The Official visit date")
  val visitDate: LocalDate,

  @Schema(description = "The Official visit start time")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The Official visit end time")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "The prisoner visit slotid")
  val prisonVisitSlotId: Long,

  @Schema(description = "The Official visit Location Id")
  val dpsLocationId: UUID,

  @Schema(description = "The prisoner code")
  val prisonCode: String,

  @Schema(description = "The Official visit prisoner number")
  val prisonerNumber: String,

  @Schema(description = "The Official visit status type")
  val statusCode: VisitStatusType,

  @Schema(description = "The Official visit outcome status")
  val completionCode: VisitCompletionType? = null,

  @Schema(description = "The offender book id")
  val offenderBookId: Long? = null,

  @Schema(description = "The offender visit id")
  val offenderVisitId: Long,

  @Schema(description = "The offender visitor details")
  val visitors: List<SyncOfficialVisitor>,
)
