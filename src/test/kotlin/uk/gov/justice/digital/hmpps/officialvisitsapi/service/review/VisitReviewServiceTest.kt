package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository
import java.time.LocalDateTime
import java.util.Optional

class VisitReviewServiceTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val checker: VisitReviewChecker = mock()
  private val releaseChecker: VisitReviewReleaseChecker = mock()
  private val transferChecker: VisitReviewTransferChecker = mock()
  private val visitReviewRepository: VisitReviewRepository = mock()
  private val visitReviewQueueRepository: VisitReviewQueueRepository = mock()

  private val service = VisitReviewService(
    officialVisitRepository,
    checker,
    releaseChecker,
    transferChecker,
    visitReviewRepository,
    visitReviewQueueRepository,
  )

  private val scheduledVisit = mock<OfficialVisitEntity>().stub {
    on { visitStatusCode } doReturn VisitStatusType.SCHEDULED
    on { visitDate } doReturn today()
  }

  @Test
  fun `should be no-op when visit not found`() {
    whenever(officialVisitRepository.findById(1)) doReturn Optional.empty()

    service.check(1, VisitReviewCheckType.CHECK)

    verify(officialVisitRepository).findById(1)
    verifyNoInteractions(checker, releaseChecker, transferChecker)
  }

  @Test
  fun `should be no-op for completed, cancelled and expired visits`() {
    val officialVisit: OfficialVisitEntity = mock()

    setOf(VisitStatusType.COMPLETED, VisitStatusType.CANCELLED, VisitStatusType.EXPIRED).forEach {
      whenever { officialVisit.visitStatusCode } doReturn it
      whenever(officialVisitRepository.findById(1)) doReturn Optional.of(officialVisit)

      service.check(1, VisitReviewCheckType.CHECK)
    }

    verify(officialVisitRepository, times(3)).findById(1)
    verifyNoInteractions(checker, releaseChecker, transferChecker)
  }

  @Test
  fun `should be no-op for visit in the past`() {
    val officialVisit = mock<OfficialVisitEntity>().stub {
      on { visitStatusCode } doReturn VisitStatusType.SCHEDULED
      on { visitDate } doReturn today().minusDays(1)
    }

    whenever(officialVisitRepository.findById(1)) doReturn Optional.of(officialVisit)

    service.check(1, VisitReviewCheckType.CHECK)

    verify(officialVisitRepository).findById(1)
    verifyNoInteractions(checker, releaseChecker, transferChecker)
  }

  @Test
  fun `should be no-op for visit more than 7 days in the future`() {
    val officialVisit = mock<OfficialVisitEntity>().stub {
      on { visitStatusCode } doReturn VisitStatusType.SCHEDULED
      on { visitDate } doReturn today().plusDays(8)
    }

    whenever(officialVisitRepository.findById(1)) doReturn Optional.of(officialVisit)

    service.check(1, VisitReviewCheckType.CHECK)

    verify(officialVisitRepository).findById(1)
    verifyNoInteractions(checker, releaseChecker, transferChecker)
  }

  @Test
  fun `should invoke CHECK checker`() {
    whenever(officialVisitRepository.findById(1)) doReturn Optional.of(scheduledVisit)

    service.check(1, VisitReviewCheckType.CHECK)

    verify(officialVisitRepository).findById(1)
    verify(checker).check(scheduledVisit)
    verifyNoInteractions(releaseChecker, transferChecker)
  }

  @Test
  fun `should invoke RECHECK checker`() {
    whenever(officialVisitRepository.findById(1)) doReturn Optional.of(scheduledVisit)

    service.check(1, VisitReviewCheckType.RECHECK)

    verify(officialVisitRepository).findById(1)
    verify(checker).check(scheduledVisit)
    verifyNoInteractions(releaseChecker, transferChecker)
  }

  @Test
  fun `should invoke UPDATE checker`() {
    whenever(officialVisitRepository.findById(1)) doReturn Optional.of(scheduledVisit)

    service.check(1, VisitReviewCheckType.UPDATE)

    verify(officialVisitRepository).findById(1)
    verify(checker).check(scheduledVisit)
    verifyNoInteractions(releaseChecker, transferChecker)
  }

  @Test
  fun `should invoke TRANSFER checker`() {
    whenever(officialVisitRepository.findById(1)) doReturn Optional.of(scheduledVisit)

    service.check(1, VisitReviewCheckType.TRANSFER)

    verify(officialVisitRepository).findById(1)
    verify(transferChecker).check(scheduledVisit)
    verifyNoInteractions(checker, releaseChecker)
  }

  @Test
  fun `should invoke RELEASE checker`() {
    whenever(officialVisitRepository.findById(1)) doReturn Optional.of(scheduledVisit)

    service.check(1, VisitReviewCheckType.RELEASE)

    verify(officialVisitRepository).findById(1)
    verify(releaseChecker).check(scheduledVisit)
    verifyNoInteractions(checker, transferChecker)
  }

  @Test
  fun expire() {
    val review = VisitReviewEntity(
      officialVisitId = 123L,
      raisedTime = LocalDateTime.now(),
    )
    whenever(visitReviewRepository.findByOfficialVisitId(review.officialVisitId)).thenReturn(listOf(review))

    service.expire(review.officialVisitId)

    verify(visitReviewRepository).findByOfficialVisitId(review.officialVisitId)
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

    service.visitCheck(officialVisitId)

    inOrder(visitReviewQueueRepository) {
      verify(visitReviewQueueRepository).findById(officialVisitId)
      verify(visitReviewQueueRepository).delete(queueEntry)
    }
  }

  @Test
  fun `does not attempt delete when no queue entry exists`() {
    val officialVisitId = 456L

    whenever(visitReviewQueueRepository.findById(officialVisitId)).thenReturn(Optional.empty())

    service.visitCheck(officialVisitId)

    verify(visitReviewQueueRepository, never()).delete(any())
  }

  @Test
  fun `propagates exception from check and does not delete queue entry`() {
    val officialVisitId = 789L

    whenever(officialVisitRepository.findById((eq(officialVisitId))))
      .thenThrow(RuntimeException("check failed"))

    assertThatThrownBy { service.visitCheck(officialVisitId) }
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

    assertThatThrownBy { service.visitCheck(officialVisitId) }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("delete failed")
  }
}
