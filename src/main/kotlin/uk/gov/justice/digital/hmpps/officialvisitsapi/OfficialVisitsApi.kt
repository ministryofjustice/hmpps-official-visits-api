package uk.gov.justice.digital.hmpps.officialvisitsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.ServiceConfig

@EnableConfigurationProperties(ServiceConfig::class)
@SpringBootApplication
class OfficialVisitsApi

fun main(args: Array<String>) {
  runApplication<OfficialVisitsApi>(*args)
}
