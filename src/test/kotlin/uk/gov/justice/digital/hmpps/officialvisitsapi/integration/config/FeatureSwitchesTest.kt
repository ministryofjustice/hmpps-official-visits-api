package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.Feature
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isBool
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase

class FeatureSwitchesTest : IntegrationTestBase() {
  @TestPropertySource(properties = ["feature.events.sns.enabled=true"])
  @Nested
  @DisplayName("Features are enabled when set")
  inner class EnabledFeatures(@Autowired val featureSwitches: FeatureSwitches) {
    @Test
    fun `features are enabled`() {
      Feature.entries.forEach {
        assertThat(featureSwitches.isEnabled(it)).isTrue
      }
    }
  }

  @TestPropertySource(properties = ["feature.events.sns.enabled=false"])
  @Nested
  @DisplayName("Features are disabled when specifically declared so")
  inner class DisabledFeaturesSet(@Autowired val featureSwitches: FeatureSwitches) {
    @Test
    fun `features are disabled when set to false`() {
      Feature.entries.forEach {
        assertThat(featureSwitches.isEnabled(it)).withFailMessage("${it.label} not enabled").isFalse
      }
    }
  }

  @Nested
  @DisplayName("Features are disabled by default")
  inner class DisabledFeatures(@Autowired val featureSwitches: FeatureSwitches) {
    @Test
    fun `features are disabled by default`() {
      Feature.entries.forEach {
        assertThat(featureSwitches.isEnabled(it)).withFailMessage("${it.label} enabled").isFalse
      }
    }
  }

  @Nested
  @DisplayName("Features can be defaulted when not present")
  inner class DefaultedFeatures(@Autowired val featureSwitches: FeatureSwitches) {
    @Test
    fun `different feature types can be defaulted `() {
      featureSwitches.isEnabled(Feature.OUTBOUND_EVENTS_ENABLED, true) isBool true
    }
  }
}
