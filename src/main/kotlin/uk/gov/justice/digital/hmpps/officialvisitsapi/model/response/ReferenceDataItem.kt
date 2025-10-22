package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.ReferenceDataGroup

@Schema(description = "Describes the details of a reference code")
data class ReferenceDataItem(

  @Schema(description = "An internally-generated unique identifier for this reference code.", example = "12345")
  val referenceDataId: Long,

  @Schema(description = "The group name for related reference codes.", example = "VIS_STS")
  val groupCode: ReferenceDataGroup,

  @Schema(description = "The code for this reference data", example = "SCH")
  val code: String,

  @Schema(description = "A fuller description of the reference code", example = "Scheduled")
  val description: String,

  @Schema(description = "The default order configured for the reference code, lowest number first.", example = "5")
  val displaySequence: Int,

  @Schema(description = "Whether the reference code is still in use. Old reference codes are maintained for compatability with legacy data.", example = "true")
  val enabled: Boolean,
)
