package uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class PrisonPersonalisation(
  @Value($$"${prison.personalisation.file}") private val prisonPersonalisationFile: String,
  private val yamlObjectMapper: ObjectMapper,
) {
  private val prisonsPersonalisationDto by lazy {
    this::class.java.getResourceAsStream(prisonPersonalisationFile)
      .use { yamlObjectMapper.readValue(it, PrisonsPersonalisationDto::class.java) }
  }

  fun forPrison(prisonCode: String) = prisonsPersonalisationDto.prisons.singleOrNull { it.code == prisonCode }
    ?: prisonsPersonalisationDto.prisons.single { it.code == "DEFAULT" }
}

data class PrisonsPersonalisationDto(
  @JsonProperty("prisons")
  var prisons: List<PrisonPersonalisationDto>,
)

data class PrisonPersonalisationDto(
  @JsonProperty("code")
  val code: String,
  @JsonProperty("name")
  val name: String,
  @JsonProperty("address")
  val address: Address,
  @JsonProperty("video_visit")
  val videoVisit: String,
  @JsonProperty("in_person_visit")
  val inPersonVisit: String,
)

data class Address(
  @JsonProperty("line1")
  val line1: String?,
  @JsonProperty("line2")
  val line2: String?,
  @JsonProperty("line3")
  val line3: String?,
  @JsonProperty("city")
  val city: String?,
  @JsonProperty("postcode")
  val postcode: String?,
)
