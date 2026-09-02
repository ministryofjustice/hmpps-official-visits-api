package uk.gov.justice.digital.hmpps.officialvisitsapi.service.jobs

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewCheckType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.review.VisitReviewService
import java.time.LocalDateTime
import java.util.Optional

class VisitReviewCheckAndDequeueServiceTest {

  private val visitReviewService: VisitReviewService = mock()
  private val visitReviewQueueRepository: VisitReviewQueueRepository = mock()

  private lateinit var service: VisitReviewCheckAndDequeueService

  @BeforeEach
  fun setUp() {
    service = VisitReviewCheckAndDequeueService(visitReviewService, visitReviewQueueRepository)
  }

  @Test
  fun `checks the visit and deletes the queue entry when it exists`() {
    val officialVisitId = 123L
    val queueEntry = VisitReviewQueueEntity(
      visitReviewQueueId = 2L,
      officialVisitId = officialVisitId,
      createdTime = LocalDateTime.now(),
      triggeringEvent = "PROCESS",
    )
    whenever(visitReviewQueueRepository.findById(officialVisitId)).thenReturn(Optional.of(queueEntry))

    service.checkAndDequeue(officialVisitId)

    val inOrder = inOrder(visitReviewService, visitReviewQueueRepository)
    inOrder.verify(visitReviewService).check(officialVisitId, VisitReviewCheckType.CHECK)
    inOrder.verify(visitReviewQueueRepository).findById(officialVisitId)
    inOrder.verify(visitReviewQueueRepository).delete(queueEntry)
  }

  @Test
  fun `does not attempt delete when no queue entry exists`() {
    val officialVisitId = 456L

    whenever(visitReviewQueueRepository.findById(officialVisitId)).thenReturn(Optional.empty())

    service.checkAndDequeue(officialVisitId)

    verify(visitReviewQueueRepository, never()).delete(any())
  }

  @Test
  fun `propagates exception from check and does not delete queue entry`() {
    val officialVisitId = 789L

    whenever(visitReviewService.check(eq(officialVisitId), eq(VisitReviewCheckType.CHECK)))
      .thenThrow(RuntimeException("check failed"))

    assertThatThrownBy { service.checkAndDequeue(officialVisitId) }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("check failed")

    verify(visitReviewQueueRepository, never()).findById(any())
    verify(visitReviewQueueRepository, never()).delete(any())
  }

  @Test
  fun `propagates exception from delete`() {
    val officialVisitId = 999L
    val queueEntry = VisitReviewQueueEntity(
      visitReviewQueueId = 2L,
      officialVisitId = officialVisitId,
      createdTime = LocalDateTime.now(),
      triggeringEvent = "PROCESS",
    )
    whenever(visitReviewQueueRepository.findById(officialVisitId)).thenReturn(Optional.of(queueEntry))
    whenever(visitReviewQueueRepository.delete(queueEntry)).thenThrow(RuntimeException("delete failed"))

    assertThatThrownBy { service.checkAndDequeue(officialVisitId) }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("delete failed")
  }
}
