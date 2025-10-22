package uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

abstract class AbstractAuditable(
  @Schema(description = "The data and time the record was created", nullable = true, example = "2022-10-01T16:45:45")
  var createDateTime: LocalDateTime? = null,

  @Schema(description = "The username who created the record", nullable = true, example = "X999X")
  var createUsername: String? = null,

  @Schema(description = "The date and time the record was last amended", nullable = true, example = "2022-10-01T16:45:45")
  var modifyDateTime: LocalDateTime? = null,

  @Schema(description = "The username who last modified the record", nullable = true, example = "X999X")
  var modifyUsername: String? = null,
)
