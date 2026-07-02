package uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.YamlConfiguration
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.NOTTINGHAM
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo

@SpringBootTest(classes = [YamlConfiguration::class, PrisonPersonalisation::class])
class PrisonPersonalisationTest {
  @Autowired
  private lateinit var personalisation: PrisonPersonalisation

  @Nested
  @TestPropertySource(properties = ["prison.personalisation.file=/config/prison_personalisation.yaml"])
  inner class PrisonPersonalisationLiveConfig {
    @Test
    fun `should match on default settings for unknown prison code`() {
      with(personalisation.forPrison("UNKNOWN")) {
        code isEqualTo "DEFAULT"
        name isEqualTo "Default prison"
      }
    }
  }

  @Nested
  @TestPropertySource(properties = ["prison.personalisation.file=/config/test_prison_personalisation.yaml"])
  inner class PrisonPersonalisationTestConfig {
    @Test
    fun `should match on Nottingham prison code`() {
      with(personalisation.forPrison(NOTTINGHAM)) {
        code isEqualTo "NMI"
        name isEqualTo "HMP & YOI Nottingham"
      }
    }

    @Test
    fun `should match on Moorland prison code`() {
      with(personalisation.forPrison(MOORLAND)) {
        code isEqualTo "MDI"
        name isEqualTo "Moorland (HMP & YOI)"
      }
    }

    @Test
    fun `should match on default settings for unknown prison code`() {
      with(personalisation.forPrison("UNKNOWN")) {
        code isEqualTo "DEFAULT"
        name isEqualTo "Default prison"
      }
    }
  }
}
