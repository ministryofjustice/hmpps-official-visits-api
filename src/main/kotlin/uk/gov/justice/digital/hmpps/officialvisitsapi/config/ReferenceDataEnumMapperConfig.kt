package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.ReferenceDataGroupEnumConverter

/**
 * Used to map reference data group identifier requests to the enum of valid groups
 */
@Configuration
class ReferenceDataEnumMapperConfig : WebMvcConfigurer {
  override fun addFormatters(registry: FormatterRegistry) {
    registry.addConverter(ReferenceDataGroupEnumConverter())
  }
}
