package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitForReviewEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitForReviewRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.OfficialVisitsRetrievalService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class VisitForReviewServiceTest {
  private val visitForReviewRepository: VisitForReviewRepository = mock()
  private val officialVisitsRetrievalService: OfficialVisitsRetrievalService = mock()

  private val service = VisitForReviewService(visitForReviewRepository, officialVisitsRetrievalService)

  @Test
  fun `should get page of visits for review with grouped issues`() {
    val pageable = PageRequest.of(0, 10)
    val visitDetails: OfficialVisitDetails = mock()

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
