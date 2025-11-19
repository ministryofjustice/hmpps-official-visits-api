package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Configuration
class TimeSourceConfiguration {
  @Bean
  fun timeSource() = TimeSource { LocalDateTime.now(Clock.systemDefaultZone()) }
}

/**
 * To be used for providing dates and times in the application. Enables control of time in the code (and unit tests).
 */
fun interface TimeSource {
  fun now(): LocalDateTime

  fun today(): LocalDate = now().toLocalDate()
}
