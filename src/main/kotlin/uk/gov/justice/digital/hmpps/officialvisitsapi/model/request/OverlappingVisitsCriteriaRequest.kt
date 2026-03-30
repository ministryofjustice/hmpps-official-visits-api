package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalTime

data class OverlappingVisitsCriteriaRequest(
  @field:NotBlank(message = "The prisoner number is mandatory")
  @field:Size(max = 7, message = "Prisoner number must not exceed {max} characters")
  @Schema(description = "The prisoner number (NOMIS ID) to check for overlapping", example = "A1234AA")
  val prisonerNumber: String?,

  @field:NotNull(message = "The date is mandatory")
  @Schema(description = "The date on which to check for overlapping", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val visitDate: LocalDate?,

  @field:NotNull(message = "The start time is mandatory")
  @Schema(description = "The start time on which to check for overlapping", example = "10:00")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val startTime: LocalTime?,

  @field:NotNull(message = "The end time is mandatory")
  @Schema(description = "The end time on which to check for overlapping", example = "11:00")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(description = "One or more unique identifier for the prisoner contacts, can be null")
  val contactIds: List<Long>?,

  @Schema(description = "The unique identifier of the official visit to exclude from the check. Would be provided for an amend check, otherwise null")
  val existingOfficialVisitId: Long? = null,
)
