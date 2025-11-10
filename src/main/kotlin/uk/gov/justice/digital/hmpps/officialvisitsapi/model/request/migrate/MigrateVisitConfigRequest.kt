package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Schema(description = "Request to migrate a day and time slot for a prison and its associated visit slots from NOMIS")
data class MigrateVisitConfigRequest(

  @Schema(description = "The 3-letter agy_loc_id from NOMIS where this visit time slot exists", example = "MDI", required = true)
  @field:NotNull(message = "The prison code for a visit time slot is mandatory")
  val prisonCode: String?,

  @Schema(description = "The 3-letter day of the week code where this slot falls in this prison.", example = "TUE", required = true)
  @field:NotNull(message = "The day code for a visit time slot is mandatory")
  val dayCode: String?,

  @Schema(description = "The time slot sequence in NOMIS. This is used to build a response object for cross-reference", example = "1", required = true)
  @field:NotNull(message = "The time slot sequence for a visit time slot is mandatory")
  val timeSlotSeq: Int?,

  @Schema(description = "The start time for this visit time slot", example = "13:30", required = true)
  @field:NotNull(message = "The start time for a visit time slot is mandatory")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime?,

  @Schema(description = "The end time for this visit time slot", example = "14:30", required = true)
  @field:NotNull(message = "The end time for a visit time slot is mandatory")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(description = "The date from which this row will be effective", example = "1980-01-01", required = true)
  @field:NotNull(message = "The effective date for a visit time slot is mandatory")
  @field:DateTimeFormat(pattern = "yyyy-MM-dd")
  val effectiveDate: LocalDate?,

  @Schema(description = "The date from which this row will no longer be effective", example = "1980-01-01", nullable = true)
  @field:DateTimeFormat(pattern = "yyyy-MM-dd")
  val expiryDate: LocalDate? = null,

  @Schema(description = "The list of visit slots which are linked to this time slot")
  val visitSlots: List<MigrateVisitSlot> = emptyList(),

  @Schema(description = "The data and time the record was created", example = "2022-10-01T16:45:45", nullable = true)
  val createDateTime: LocalDateTime?,

  @Schema(description = "The username who created the record", example = "X999X", nullable = true)
  val createUsername: String?,

  @Schema(description = "The date and time the record was last amended", nullable = true, example = "2022-10-01T16:45:45")
  val modifyDateTime: LocalDateTime? = null,

  @Schema(description = "The username who last modified the record", nullable = true, example = "X999X")
  val modifyUsername: String? = null,
)

data class MigrateVisitSlot(

  @Schema(description = "Unique ID for this visit slot in NOMIS. For building a response to cross-reference with a DPS visit slot.", example = "1", required = true)
  @field:NotNull(message = "The visit slot ID is mandatory")
  val agencyVisitSlotId: Long?,

  @Schema(description = "The internal location ID from NOMIS. Information only", example = "1090909", nullable = true)
  val internalLocationId: Long? = null,

  @Schema(description = "The internal location ID from NOMIS. Information only", example = "MDI-OFFICIAL_VISITS", nullable = true)
  val locationKey: String? = null,

  @Schema(description = "The DPS location ID (mapped from the NOMIS internal location ID)", example = "9485cf4a-750b-4d74-b594-59bacbcda247", required = true)
  @field:NotNull(message = "The DPS location ID for a visit slot is mandatory")
  val dpsLocationId: UUID?,

  @Schema(description = "The maximum number of groups that can be booked into this visit slot. Effectively, the max visits limit for the slot.", example = "8", nullable = true)
  val maxGroups: Int? = null,

  @Schema(description = "The maximum number of adults who can be booked into this visits slot.", example = "22", nullable = true)
  val maxAdults: Int? = null,

  @Schema(description = "The maximum number of video sessions that can be booked into this visits slot.", example = "8", nullable = true)
  val maxVideoSessions: Int? = null,

  @Schema(description = "The data and time the record was created", example = "2022-10-01T16:45:45", nullable = true)
  val createDateTime: LocalDateTime?,

  @Schema(description = "The username who created the record", example = "X999X", nullable = true)
  val createUsername: String?,

  @Schema(description = "The date and time the record was last amended", nullable = true, example = "2022-10-01T16:45:45")
  val modifyDateTime: LocalDateTime? = null,

  @Schema(description = "The username who last modified the record", nullable = true, example = "X999X", required = false)
  val modifyUsername: String? = null,
)
