package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Schema(description = "Request to migrate a day and time slot for a prison and its associated visit slots from NOMIS")
data class MigrateVisitConfigRequest(

  @field:NotNull(message = "The prison code for a visit time slot is mandatory")
  @Schema(description = "The 3-letter agy_loc_id from NOMIS where this visit time slot exists", example = "MDI", required = true)
  val prisonCode: String? = null,

  @field:NotNull(message = "The day code for a visit time slot is mandatory")
  @Schema(description = "The 3-letter day of the week code where this slot falls in this prison.", example = "TUE", required = true)
  val dayCode: String? = null,

  @field:NotNull(message = "The time slot sequence for a visit time slot is mandatory")
  @Schema(description = "The time slot sequence in NOMIS. This is used to build a response object for cross-reference", example = "1", required = true)
  val timeSlotSeq: Long? = null,

  @field:NotNull(message = "The start time for a visit time slot is mandatory")
  @Schema(description = "The start time for this visit time slot", example = "13:30", required = true)
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime? = null,

  @field:NotNull(message = "The end time for a visit time slot is mandatory")
  @Schema(description = "The end time for this visit time slot", example = "14:30", required = true)
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime? = null,

  @field:NotNull(message = "The effective date for a visit time slot is mandatory")
  @Schema(description = "The date from which this row will be effective", example = "1980-01-01", required = true)
  @field:DateTimeFormat(pattern = "yyyy-MM-dd")
  val effectiveDate: LocalDate? = null,

  @Schema(description = "The date from which this row will no longer be effective", example = "1980-01-01", nullable = true, required = false)
  @field:DateTimeFormat(pattern = "yyyy-MM-dd")
  val expiryDate: LocalDate? = null,

  @Schema(description = "The list of visit slots which are linked to this time slot")
  val visitSlots: List<MigrateVisitSlot> = emptyList(),

) : AbstractAuditable()

data class MigrateVisitSlot(

  @field:NotNull(message = "The visit slot ID is mandatory")
  @Schema(description = "Unique ID for this visit slot in NOMIS. For building a response to cross-reference with a DPS visit slot.", example = "1", required = true)
  val agencyVisitSlotId: Long? = null,

  @Schema(description = "The internal location ID from NOMIS. Information only", example = "1090909", nullable = true, required = false)
  val internalLocationId: Long? = null,

  @Schema(description = "The internal location ID from NOMIS. Information only", example = "MDI-OFFICIAL_VISITS", nullable = true, required = false)
  val locationKey: String? = null,

  @field:NotNull(message = "The DPS location ID for a visit slot is mandatory")
  @Schema(description = "The DPS location ID (mapped from the NOMIS internal location ID)", example = "9485cf4a-750b-4d74-b594-59bacbcda247", required = true)
  val dpsLocationId: UUID? = null,

  @field:NotNull(message = "The maximum number of groups who can attend this visit slot is mandatory")
  @field:Max(value = 200, message = "Maximum groups must be {max} or less")
  @field:Min(value = 0, message = "Maximum groups must be {min} or above")
  @Schema(description = "The maximum number of groups that can be booked into this visit slot. Effectively, the max visits limit for the slot.", example = "8", required = true)
  val maxGroups: Int? = null,

  @field:NotNull(message = "The maximum number of adults who can attend this visit slot is mandatory.")
  @field:Max(value = 400, message = "Maximum adults must be {max} or less")
  @field:Min(value = 0, message = "Maximum adults must be {min} or above")
  @Schema(description = "The maximum number of adults who can be booked into this visits slot.", example = "22", required = true)
  val maxAdults: Int? = null,

  @field:Max(value = 200, message = "Maximum video sessions must be {max} or less")
  @field:Min(value = 0, message = "Maximum video sessions must be {min} or above")
  @Schema(description = "The maximum number of video sessions that can be booked into this visits slot.", example = "8", nullable = true, required = false)
  val maxVideoSessions: Int? = null,

) : AbstractAuditable()
