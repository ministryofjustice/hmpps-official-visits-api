package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class PrisonerVisitedDetails(
  @Schema(description = "The official visitor - prisoner number", example = "1")
  val prisonerNumber: String,

  @Schema(description = "The official visitor Prisoner code")
  val prisonCode: String,

  @Schema(description = "The official visitor - Prisoner first name")
  val firstName: String?,

  @Schema(description = "The official visitor prisoner last name")
  val lastName: String?,

  @Schema(description = "The official visitor - Prisoner date of birth")
  val dateOfBirth: LocalDate?,

  @Schema(description = "Prisoner Cell location")
  val cellLocation: String?,

  @Schema(description = "Prisoner middle name")
  val middleNames: String?,

  @Schema(description = "Prisoner offender booking id")
  val offenderBookId: Long?,

  @Schema(description = "Prisoner attendance code")
  val attendanceCode: String?,

  @Schema(description = "Prisoner attendance code description")
  val attendanceCodeDescription: String?,
)
