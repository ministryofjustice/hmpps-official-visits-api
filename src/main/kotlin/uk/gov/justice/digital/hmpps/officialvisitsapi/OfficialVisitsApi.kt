package uk.gov.justice.digital.hmpps.officialvisitsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OfficialVisitsApi

fun main(args: Array<String>) {
  runApplication<OfficialVisitsApi>(*args)
}
