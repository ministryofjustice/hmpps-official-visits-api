package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareTokenConverter

@Configuration
class ResourceServerConfiguration {
  @Bean
  @Order(1)
  fun notifyCallbackSecurityFilterChain(
    http: HttpSecurity,
    notifyCallbackAuthorizationManager: NotifyCallbackAuthorizationManager,
  ): SecurityFilterChain {
    http {
      securityMatcher("/notify/callback/**")
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      csrf { disable() }
      authorizeHttpRequests {
        authorize(anyRequest, notifyCallbackAuthorizationManager)
      }
      exceptionHandling {
        authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
        accessDeniedHandler =
          org.springframework.security.web.access.AccessDeniedHandler { _, response, _ ->
            response.sendError(HttpStatus.UNAUTHORIZED.value())
          }
      }
    }

    return http.build()
  }

  @Bean
  @Order(2)
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http {
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      headers { frameOptions { sameOrigin = true } }
      csrf { disable() }
      authorizeHttpRequests {
        listOf(
          "/health/**",
          "/info",
          "/v3/api-docs/**",
          "/swagger-ui/**",
          "/swagger-ui.html",
          "/swagger-resources",
          "/swagger-resources/configuration/ui",
          "/swagger-resources/configuration/security",
          "/queue-admin/retry-all-dlqs",
          "/job-admin/**",
        ).forEach { authorize(it, permitAll) }
        authorize(anyRequest, authenticated)
      }
      oauth2ResourceServer {
        jwt { jwtAuthenticationConverter = AuthAwareTokenConverter() }
      }
    }

    return http.build()
  }
}
