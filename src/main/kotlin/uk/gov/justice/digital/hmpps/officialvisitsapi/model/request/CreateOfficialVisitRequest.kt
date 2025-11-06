package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

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

  @field:NotBlank(message = "The type code for the official visit is mandatory")
  val visitTypeCode: String?,

  val officialVisitors: List<OfficialVisitor> = emptyList(),
)
