package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.DayType
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "Request to create a new prison time slot for official visits")
data class CreateTimeSlotRequest(
  @Schema(description = "Prison code", example = "MDI", required = true)
  val prisonCode: String,

  @Schema(description = "Day code MON-SUN", example = "MON", required = true)
  val dayCode: DayType,

  @Schema(description = "Start time", example = "09:00", required = true)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "End time", example = "11:00", required = true)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "Effective date. The date from which this time slot will be active", example = "2026-01-21", required = true)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val effectiveDate: LocalDate,

  @Schema(description = "Expiry date. The date from which this time slot will no longer be considered active", example = "2027-01-21")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val expiryDate: LocalDate? = null,

)
