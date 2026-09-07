package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.TimeSource
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitForReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitReviewQueueEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.today
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitForReviewRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewQueueRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitReviewRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitsRetrievalService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class VisitReviewServiceTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val checker: VisitReviewChecker = mock()
  private val releaseChecker: VisitReviewReleaseChecker = mock()
  private val transferChecker: VisitReviewTransferChecker = mock()
  private val visitReviewRepository: VisitReviewRepository = mock()
  private val visitReviewQueueRepository: VisitReviewQueueRepository = mock()
  private val now = LocalDateTime.now()
  private val timeSource = TimeSource { now }
  private val visitForReviewRepository: VisitForReviewRepository = mock()
  private val officialVisitsRetrievalService: OfficialVisitsRetrievalService = mock()

  private val service = VisitReviewService(
    officialVisitRepository,
    checker,
    releaseChecker,
    transferChecker,
    visitReviewRepository,
    visitReviewQueueRepository,
    timeSource,
    visitForReviewRepository,
    officialVisitsRetrievalService,
  )

  private val scheduledVisit = mock<OfficialVisitEntity>().stub {
    on { visitStatusCode } doReturn VisitStatusType.SCHEDULED
    on { visitDate } doReturn today()
  }

  @Test
  fun `should be no-op when visit not found`() {
    whenever(officialVisitRepository.findById(1)) doReturn Optional.empty()

    service.visitCheck(1, VisitReviewCheckType.CHECK)

    verify(officialVisitRepository).findById(1)
    verifyNoInteractions(checker, releaseChecker, transferChecker)
  }

  @Test
  fun `should be no-op for completed, cancelled and expired visits`() {
    val officialVisit: OfficialVisitEntity = mock()

    setOf(VisitStatusType.COMPLETED, VisitStatusType.CANCELLED, VisitStatusType.EXPIRED).forEach {
      whenever { officialVisit.visitStatusCode } doReturn it
      whenever(officialVisitRepository.findById(1)) doReturn Optional.of(officialVisit)

      service.visitCheck(1, VisitReviewCheckType.CHECK)
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

    service.visitCheck(1, VisitReviewCheckType.CHECK)

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

    service.visitCheck(1, VisitReviewCheckType.CHECK)

    verify(officialVisitRepository).findById(1)
    verifyNoInteractions(checker, releaseChecker, transferChecker)
  }

  @Test
  fun `should invoke CHECK checker`() {
    whenever(officialVisitRepository.findById(1)) doReturn Optional.of(scheduledVisit)

    service.visitCheck(1, VisitReviewCheckType.CHECK)

    verify(officialVisitRepository).findById(1)
    verify(checker).check(scheduledVisit)
    verifyNoInteractions(releaseChecker, transferChecker)
  }

  @Test
  fun `should invoke RECHECK checker`() {
    whenever(officialVisitRepository.findById(1)) doReturn Optional.of(scheduledVisit)

    service.visitCheck(1, VisitReviewCheckType.RECHECK)

    verify(officialVisitRepository).findById(1)
    verify(checker).check(scheduledVisit)
    verifyNoInteractions(releaseChecker, transferChecker)
  }

  @Test
  fun `should invoke UPDATE checker`() {
    whenever(officialVisitRepository.findById(1)) doReturn Optional.of(scheduledVisit)

    service.visitCheck(1, VisitReviewCheckType.UPDATE)

    verify(officialVisitRepository).findById(1)
    verify(checker).check(scheduledVisit)
    verifyNoInteractions(releaseChecker, transferChecker)
  }

  @Test
  fun `should invoke TRANSFER checker`() {
    whenever(officialVisitRepository.findById(1)) doReturn Optional.of(scheduledVisit)

    service.visitCheck(1, VisitReviewCheckType.TRANSFER)

    verify(officialVisitRepository).findById(1)
    verify(transferChecker).check(scheduledVisit)
    verifyNoInteractions(checker, releaseChecker)
  }

  @Test
  fun `should invoke RELEASE checker`() {
    whenever(officialVisitRepository.findById(1)) doReturn Optional.of(scheduledVisit)

    service.visitCheck(1, VisitReviewCheckType.RELEASE)

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

    service.visitCheck(officialVisitId, VisitReviewCheckType.CHECK)

    inOrder(visitReviewQueueRepository) {
      verify(visitReviewQueueRepository).findById(officialVisitId)
      verify(visitReviewQueueRepository).delete(queueEntry)
    }
  }

  @Test
  fun `does not attempt delete when no queue entry exists`() {
    val officialVisitId = 456L

    whenever(visitReviewQueueRepository.findById(officialVisitId)).thenReturn(Optional.empty())

    service.visitCheck(officialVisitId, VisitReviewCheckType.CHECK)

    verify(visitReviewQueueRepository, never()).delete(any())
  }

  @Test
  fun `propagates exception from check and does not delete queue entry`() {
    val officialVisitId = 789L

    whenever(officialVisitRepository.findById((eq(officialVisitId))))
      .thenThrow(RuntimeException("check failed"))

    assertThatThrownBy { service.visitCheck(officialVisitId, VisitReviewCheckType.CHECK) }
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

    assertThatThrownBy { service.visitCheck(officialVisitId, VisitReviewCheckType.CHECK) }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("delete failed")
  }

  @Test
  fun `should get page of visits for review with grouped issues`() {
    val pageable = PageRequest.of(0, 10)
    val visitDetails: OfficialVisitDetails = org.mockito.kotlin.mock()

    whenever(
      visitForReviewRepository.findVisitIdsForReview(
        prisonCode = eq("MDI"),
        visitStatus = eq(VisitStatusType.SCHEDULED),
        fromDate = any(),
        pageable = eq(pageable),
      ),
    ).thenReturn(PageImpl(listOf(1L), pageable, 1))
    whenever(
      visitForReviewRepository.findCurrentReviewDetailsForVisitIds(
        officialVisitIds = eq(listOf(1L)),
        visitStatus = eq(VisitStatusType.SCHEDULED),
        fromDate = any(),
      ),
    ).thenReturn(
      listOf(
        visitForReviewEntity(1, 11, IssueType.VISITOR_NOT_APPROVED, LocalDateTime.of(2026, 8, 21, 9, 0)),
        visitForReviewEntity(1, 12, IssueType.PRISONER_TRANSFERRED, LocalDateTime.of(2026, 8, 21, 10, 0)),
      ),
    )
    whenever(officialVisitsRetrievalService.getOfficialVisitByPrisonCodeAndId("MDI", 1)).thenReturn(visitDetails)

    val result = service.getVisitsForReview("MDI", pageable)

    result.metadata.totalElements isEqualTo 1
    result.content.size isEqualTo 1
    result.content.single().visit isEqualTo visitDetails
    result.content.single().issues.map { it.issueType } isEqualTo listOf(
      IssueType.VISITOR_NOT_APPROVED,
      IssueType.PRISONER_TRANSFERRED,
    )
    verify(officialVisitsRetrievalService).getOfficialVisitByPrisonCodeAndId("MDI", 1)
  }

  @Test
  fun `should acknowledge visit review`() {
    val visitReview: VisitReviewEntity = mock()
    whenever(visitReviewRepository.findCurrentByOfficialVisitIdAndPrisonCode(1, "MDI")).thenReturn(visitReview)

    service.acknowledgeVisitReview("MDI", 1, MOORLAND_PRISON_USER)

    verify(visitReview).updateAcknowledgedDetails(any(), eq(MOORLAND_PRISON_USER.username))
  }

  @Test
  fun `should throw EntityNotFoundException when visit review not found`() {
    whenever(visitReviewRepository.findCurrentByOfficialVisitIdAndPrisonCode(1, "MDI")).thenReturn(null)

    assertThrows<EntityNotFoundException> {
      service.acknowledgeVisitReview("MDI", 1, MOORLAND_PRISON_USER)
    }.message isEqualTo "Visit review for official visit id 1 and prison code MDI not found"
  }

  private fun visitForReviewEntity(
    officialVisitId: Long,
    visitReviewDetailId: Long,
    issueType: IssueType,
    detailRaisedTime: LocalDateTime,
  ) = VisitForReviewEntity(
    officialVisitId = officialVisitId,
    prisonCode = "MDI",
    prisonerNumber = "A1234BC",
    visitDate = LocalDate.of(2026, 8, 22),
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 0),
    dpsLocationId = UUID.fromString("9485cf4a-750b-4d74-b594-59bacbcda247"),
    visitStatusCode = VisitStatusType.SCHEDULED,
    visitTypeCode = VisitType.IN_PERSON,
    visitReviewId = 1,
    raisedTime = LocalDateTime.of(2026, 8, 21, 8, 0),
    visitReviewDetailId = visitReviewDetailId,
    issueType = issueType,
    detailRaisedTime = detailRaisedTime,
  )
}
