package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.SentNotificationEntity
import java.time.LocalDateTime

@Repository
interface NotificationSearchRepository : ReadOnlyRepository<SentNotificationEntity, Long> {

  fun findByPrisonCodeOrderBySentDateTimeDesc(
    prisonCode: String,
    pageable: Pageable,
  ): Page<SentNotificationEntity>

  fun findByPrisonCodeAndSentDateTimeGreaterThanEqualOrderBySentDateTimeDesc(
    prisonCode: String,
    fromDateTime: LocalDateTime,
    pageable: Pageable,
  ): Page<SentNotificationEntity>

  fun findByPrisonCodeAndSentDateTimeLessThanOrderBySentDateTimeDesc(
    prisonCode: String,
    toDateTimeExclusive: LocalDateTime,
    pageable: Pageable,
  ): Page<SentNotificationEntity>

  fun findByPrisonCodeAndSentDateTimeGreaterThanEqualAndSentDateTimeLessThanOrderBySentDateTimeDesc(
    prisonCode: String,
    fromDateTime: LocalDateTime,
    toDateTimeExclusive: LocalDateTime,
    pageable: Pageable,
  ): Page<SentNotificationEntity>
}
