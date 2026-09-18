package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfiguration {
  @Bean(name = ["asyncExecutor"])
  fun asyncExecutor(): Executor = ThreadPoolTaskExecutor().apply {
    corePoolSize = 2
    maxPoolSize = 4
    setQueueCapacity(50)
    setThreadNamePrefix("async-thread-")
    initialize()
  }
}
