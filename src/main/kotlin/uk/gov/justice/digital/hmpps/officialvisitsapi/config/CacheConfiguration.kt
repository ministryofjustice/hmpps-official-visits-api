package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit

@Profile("!test")
@Configuration
@EnableCaching
@EnableScheduling
class CacheConfiguration {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val OFFICIAL_VISIT_LOCATIONS_BY_PRISON_CACHE = "official-visit-locations-by-prison"
    const val FIFTEEN = 15L
  }

  @Bean
  fun cacheManager(): CacheManager = ConcurrentMapCacheManager(
    OFFICIAL_VISIT_LOCATIONS_BY_PRISON_CACHE,
  )

  @CacheEvict(value = [OFFICIAL_VISIT_LOCATIONS_BY_PRISON_CACHE], allEntries = true)
  @Scheduled(fixedDelay = FIFTEEN, timeUnit = TimeUnit.MINUTES)
  fun cacheEvictOfficialVisitLocationsByPrison() {
    log.info("Evicting cache: $OFFICIAL_VISIT_LOCATIONS_BY_PRISON_CACHE after $FIFTEEN mins")
  }
}
