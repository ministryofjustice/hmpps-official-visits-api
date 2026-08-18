package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import java.util.Optional

class VisitReviewServiceTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val checker: VisitReviewChecker = mock()
  private val releaseChecker: VisitReviewReleaseChecker = mock()
  private val transferChecker: VisitReviewTransferChecker = mock()

  private val service = VisitReviewService(
    officialVisitRepository,
    checker,
    releaseChecker,
    transferChecker,
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
}
