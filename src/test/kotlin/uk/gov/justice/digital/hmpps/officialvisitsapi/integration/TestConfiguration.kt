package uk.gov.justice.digital.hmpps.officialvisitsapi.integration

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/*
 * This will configure a stubbed version of the event publisher, overriding the real bean in tests, so that
 * events will not be emitted, but instead stored in memory, and can be queried within the tests.
 */

@TestConfiguration
class TestConfiguration {

  @Primary
  @Bean
  fun stubOutboundEventsPublisher(): StubOutboundEventsPublisher = StubOutboundEventsPublisher()
}
