package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository
import java.time.LocalDateTime

class VisitReviewExpireServiceTest {

  private val visitReviewRepository: VisitReviewRepository = mock()
  private val visitReviewExpireService: VisitReviewExpireService = VisitReviewExpireService(visitReviewRepository)

  @Test
  fun expire() {
    val review = VisitReviewEntity(
      officialVisitId = 123L,
      raisedTime = LocalDateTime.now(),
    )
    whenever(visitReviewRepository.findByOfficialVisitId(review.officialVisitId)).thenReturn(listOf(review))

    visitReviewExpireService.expire(review.officialVisitId)

    verify(visitReviewRepository).findByOfficialVisitId(review.officialVisitId)
  }
}
