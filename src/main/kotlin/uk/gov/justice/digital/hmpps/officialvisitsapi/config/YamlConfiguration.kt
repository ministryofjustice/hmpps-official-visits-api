package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class YamlConfiguration {
  @Bean
  fun yamlObjectMapper() = ObjectMapper(YAMLFactory())
}
