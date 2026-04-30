package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import uk.gov.service.notify.NotificationClient

@Component
@ConfigurationProperties(prefix = "notify")
class NotifyConfig {
  var apiKey: String? = null
}

object NotifyTemplates {
  const val OV_TEST_TEMPLATE = "e4eac011-e12a-4f6c-ba94-b6e35ed0d080"
}

@Configuration
class NotifyClientConfig {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Bean("notificationClient")
  fun notificationClient(notifyConfig: NotifyConfig) = run {
    log.info("Notify Api Key secret is: " + notifyConfig.apiKey?.length + " characters")
    val client = NotificationClient(notifyConfig.apiKey)
    log.info("Notify Api Service Id is: " + client.serviceId)
    client
  }
}
