package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitSummarySearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitSummarySearchResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.PrisonerVisitedDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitSummaryRepository
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class OfficialVisitSearchService(private val officialVisitSummaryRepository: OfficialVisitSummaryRepository) {
  fun searchForOfficialVisitSummaries(prisonCode: String, request: OfficialVisitSummarySearchRequest, page: Int, size: Int) = run {
    require(request.endDate!! >= request.startDate) { "End date must be on or after the start date" }
    require(page >= 0) { "Page number must be greater than or equal to zero" }
    require(size > 0) { "Page size must be greater than zero" }

    val results = officialVisitSummaryRepository.findOfficialVisitSummaryEntityBy(
      prisonCode,
      request.prisonerNumbers.takeIf { !it.isNullOrEmpty() }?.toSet(),
      request.startDate!!,
      request.endDate,
      request.visitTypes.takeIf { !it.isNullOrEmpty() }?.toSet(),
      request.visitStatuses.takeIf { !it.isNullOrEmpty() }?.toSet(),
      request.locationIds.takeIf { !it.isNullOrEmpty() }?.toSet(),
      Pageable.ofSize(size).withPage(page),
    ).map { ov ->
      // TODO pull in prisoner and location details

      OfficialVisitSummarySearchResponse(
        officialVisitId = ov.officialVisitId,
        prisonCode = prisonCode,
        prisonDescription = "prison description",
        visitStatus = ov.visitStatusCode,
        visitStatusDescription = "visit status description",
        visitTypeCode = ov.visitTypeCode,
        visitTypeDescription = "visit type description",
        visitDate = ov.visitDate,
        startTime = ov.startTime,
        endTime = ov.endTime,
        dpsLocationId = ov.dpsLocationId,
        locationDescription = "location description",
        visitSlotId = ov.prisonVisitSlotId,
        staffNotes = ov.staffNotes,
        prisonerNotes = ov.prisonerNotes,
        visitorConcernNotes = ov.visitorConcernNotes,
        numberOfVisitors = ov.numberOfVisitors,
        completionCode = ov.completionCode,
        completionDescription = "completion description",
        createdBy = ov.createdBy,
        createdTime = ov.createdTime,
        updatedBy = ov.updatedBy,
        updatedTime = ov.updatedTime,
        prisoner = PrisonerVisitedDetails(
          prisonCode = prisonCode,
          prisonerNumber = ov.prisonerNumber,
          firstName = "${ov.prisonerNumber} first name",
          lastName = "${ov.prisonerNumber} last name",
          middleNames = "${ov.prisonerNumber} middle name",
          dateOfBirth = LocalDate.of(1970, 1, 1),
          cellLocation = "cell location",
          offenderBookId = ov.offenderBookId,
          attendanceCode = null,
          attendanceCodeDescription = null,
        ),
      )
    }

    PagedModel(results)
  }
}
