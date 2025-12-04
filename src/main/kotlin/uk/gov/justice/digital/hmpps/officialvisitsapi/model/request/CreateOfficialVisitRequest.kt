package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class CreateOfficialVisitRequest(
  @field:NotNull(message = "The prison visit slot identifier for the official visit is mandatory")
  val prisonVisitSlotId: Long,

  @field:NotBlank(message = "The prison code for the official visit is mandatory")
  @field:Size(max = 3, message = "Prison code must not exceed {max} characters")
  @Schema(description = "The prison code for the prisoner", example = "PVI")
  val prisonCode: String?,

  @field:NotBlank(message = "The prisoner number for the official visit is mandatory")
  @field:Size(max = 7, message = "Prisoner number must not exceed {max} characters")
  @Schema(description = "The prisoner number (NOMIS ID)", example = "A1234AA")
  val prisonerNumber: String?,

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

  @Schema(description = "The visit type code (VIDEO, IN_PERSON, TELEPHONE)", example = "IN_PERSON", required = true)
  val visitTypeCode: VisitType,

  @Schema(description = "Notes for staff that will not be shared on movement slips", example = "Staff notes")
  val staffNotes: String?,

  @Schema(description = "Notes for prisoners that may be shared on movement slips", example = "Prisoner notes")
  val prisonerNotes: String?,

  // TODO: More values to add here

  val officialVisitors: List<OfficialVisitor> = emptyList(),
)
