package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

@Configuration
class ResourceServerConfiguration {

  @Bean
  fun webSecurityCustomizer(): WebSecurityCustomizer {
    // Completely bypass Spring Security filters for these paths
    return WebSecurityCustomizer { web ->
      web.ignoring().requestMatchers(
        "/notify/callback/**",
      )
    }
  }

  @Bean
  fun resourceServerCustomizer() = ResourceServerConfigurationCustomizer {
    // Allow unsecured access to these request paths
    unauthorizedRequestPaths {
      addPaths = setOf(
        "/queue-admin/retry-all-dlqs",
        "/job-admin/**",
      )
    }
  }
}
