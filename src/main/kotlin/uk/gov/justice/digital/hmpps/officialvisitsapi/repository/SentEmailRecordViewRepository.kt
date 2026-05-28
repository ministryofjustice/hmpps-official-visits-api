package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.SentEmailRecordViewEntity
import java.time.LocalDateTime

@Repository
interface SentEmailRecordViewRepository : ReadOnlyRepository<SentEmailRecordViewEntity, Long> {

  fun findByPrisonCodeOrderBySentDateTimeDesc(
    prisonCode: String,
    pageable: Pageable,
  ): Page<SentEmailRecordViewEntity>

  fun findByPrisonCodeAndSentDateTimeGreaterThanEqualOrderBySentDateTimeDesc(
    prisonCode: String,
    fromDateTime: LocalDateTime,
    pageable: Pageable,
  ): Page<SentEmailRecordViewEntity>

  fun findByPrisonCodeAndSentDateTimeLessThanOrderBySentDateTimeDesc(
    prisonCode: String,
    toDateTimeExclusive: LocalDateTime,
    pageable: Pageable,
  ): Page<SentEmailRecordViewEntity>

  fun findByPrisonCodeAndSentDateTimeGreaterThanEqualAndSentDateTimeLessThanOrderBySentDateTimeDesc(
    prisonCode: String,
    fromDateTime: LocalDateTime,
    toDateTimeExclusive: LocalDateTime,
    pageable: Pageable,
  ): Page<SentEmailRecordViewEntity>
}
