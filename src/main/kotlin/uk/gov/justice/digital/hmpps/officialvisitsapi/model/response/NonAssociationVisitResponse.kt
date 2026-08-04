package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class NonAssociationVisitResponse(
  @Schema(description = "The prison code the non-associate's official visit is at", example = "MDI")
  val prisonCode: String,

  @Schema(description = "The unique identifier of the non-associate's official visit", example = "23232323")
  val officialVisitId: Long,

  @Schema(description = "The date of the non-associate's official visit", example = "2026-03-02")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val visitDate: LocalDate,

  @Schema(description = "The start time of the non-associate's official visit", example = "10:00")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The end time of the non-associate's official visit", example = "11:00")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "The prisoner number (NOMIS ID) of the non-associate", example = "A1232DD")
  val prisonerNumber: String,

  @Schema(description = "The DPS location identifier of the non-associate's official visit")
  val dpsLocationId: UUID,

  @Schema(description = "The description of the prison location the non-associate's official visit is in", example = "Legal visits room 8")
  val locationDescription: String,

  @Schema(description = "The first name of the non-associate", example = "Steve")
  val firstName: String,

  @Schema(description = "The last name of the non-associate", example = "Smith")
  val lastName: String,
)
