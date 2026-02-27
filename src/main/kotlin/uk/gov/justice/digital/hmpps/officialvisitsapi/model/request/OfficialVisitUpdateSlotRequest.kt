package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class OfficialVisitUpdateSlotRequest(
  @field:NotNull(message = "The prison visit slot identifier for the official visit is mandatory")
  val prisonVisitSlotId: Long?,

  @field:NotNull(message = "The date for the official visit is mandatory")
  @Schema(description = "The date the official visit will take place", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val visitDate: LocalDate?,

  @field:NotNull(message = "The start time of the official visit is mandatory")
  @Schema(description = "The start time of the official visit", example = "10:00")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val startTime: LocalTime?,

  @field:NotNull(message = "The end time of the official visit is mandatory")
  @Schema(description = "The end time of the official visit", example = "11:00")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val endTime: LocalTime?,

  @field:NotNull(message = "The DPS location ID is mandatory")
  @Schema(description = "The DPS location ID where the official visit is to take place", example = "aaaa-bbbb-9f9f9f9f-9f9f9f9f")
  val dpsLocationId: UUID?,

  @field:NotNull(message = "Visit type code is mandatory")
  @Schema(description = "The visit type code", example = "IN_PERSON", required = true)
  val visitTypeCode: VisitType?,
)
