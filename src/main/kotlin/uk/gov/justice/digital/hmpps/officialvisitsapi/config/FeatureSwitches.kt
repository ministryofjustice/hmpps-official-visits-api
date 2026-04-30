package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.OutboundEvent

@Component
class FeatureSwitches(private val environment: Environment) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun isEnabled(feature: Feature, defaultValue: Boolean = false): Boolean = get(feature.label, Boolean::class.java, defaultValue) == true

  fun isEnabled(outboundEvent: OutboundEvent, defaultValue: Boolean = false): Boolean = get("feature.event.${outboundEvent.eventType}", Boolean::class.java, defaultValue) == true

  fun getValue(feature: StringFeature, defaultValue: String? = null): String? = get(feature.label, String::class.java, defaultValue)

  private inline fun <reified T : Any> get(property: String, type: Class<T>, defaultValue: T?) = environment.getProperty(property, type).let {
    if (it == null) {
      log.info("property '$property' not configured, defaulting to $defaultValue")
      defaultValue
    } else {
      it
    }
  }
}

enum class Feature(val label: String) {
  OUTBOUND_EVENTS_ENABLED("feature.events.sns.enabled"),
  FEATURE_NOTIFICATIONS_ENABLE("feature.notifications.enable"),
}

enum class StringFeature(val label: String) {
  FEATURE_DPS_ENABLED_PRISONS("feature.dps.enabled.prisons"),
  FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS("feature.allow.social.visitors.prisons"),
}
