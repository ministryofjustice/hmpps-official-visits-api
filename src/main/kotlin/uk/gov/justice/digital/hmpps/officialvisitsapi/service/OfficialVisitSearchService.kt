package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.ReferenceDataGroup
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitSummarySearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitSummarySearchResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.PrisonerVisitedDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitSummaryRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OfficialVisitSearchService(
  private val officialVisitSummaryRepository: OfficialVisitSummaryRepository,
  private val referenceDataService: ReferenceDataService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
) {
  fun searchForOfficialVisitSummaries(prisonCode: String, request: OfficialVisitSummarySearchRequest, page: Int, size: Int): PagedModel<OfficialVisitSummarySearchResponse> = run {
    require(request.endDate!! >= request.startDate) { "End date must be on or after the start date" }
    require(page >= 0) { "Page number must be greater than or equal to zero" }
    require(size > 0) { "Page size must be greater than zero" }

    val mayBeSearchTerm = request.searchTerm?.trim()
    require(mayBeSearchTerm == null || mayBeSearchTerm.length >= 2) { "Search term must be a minimum of 2 characters if provided" }
    val prisoners = mayBeSearchTerm?.let { st -> prisonerSearchClient.findPrisonersBySearchTerm(prisonCode, mayBeSearchTerm) } ?: emptyList()

    // Avoid an unnecessary query if no prisoners found in search above
    if (mayBeSearchTerm != null && prisoners.isEmpty()) {
      return PagedModel(Page.empty())
    }

    val results = officialVisitSummaryRepository.findOfficialVisitSummaryEntityBy(
      prisonCode = prisonCode,
      prisonerNumbers = mayBeSearchTerm?.let { prisoners.map { it.prisonerNumber }.toSet() },
      startDate = request.startDate!!,
      endDate = request.endDate,
      visitTypes = request.visitTypes.takeIf { !it.isNullOrEmpty() }?.toSet(),
      visitStatuses = request.visitStatuses.takeIf { !it.isNullOrEmpty() }?.toSet(),
      locationIds = request.locationIds.takeIf { !it.isNullOrEmpty() }?.toSet(),
      pageable = PageRequest.of(page, size, Sort.by("visitDate", "startTime").ascending()),
    )

    // Get the prisoner details for the full page of results
    val matchingPrisoners = prisoners.filter { it.prisonerNumber in results.map { ov -> ov.prisonerNumber } }.distinctBy { it.prisonerNumber }
    val prisonerMap = matchingPrisoners.associateBy { it.prisonerNumber }

    // Get the locations for visits for this prison (it's a cacheable endpoint)
    val locations = locationsInsidePrisonClient.getOfficialVisitLocationsAtPrison(prisonCode)
    val locationMap = locations.map { LocationDescription(it.id, it.localName, it.locationType, it.key) }.associateBy { it.id }

    // Enrich the page of results
    val response = results.map { ov ->

      // Check that the prisoner and visit are still at the same prison
      val prisoner = prisonerMap[ov.prisonerNumber]
      val prisonName = if (prisoner != null && prisoner.prisonId == ov.prisonCode) {
        prisoner.prisonName
      } else {
        "Check location"
      }

      // Get the location of this visit from the map by dpsLocationId
      val location = locationMap[ov.dpsLocationId]

      // Get the prisoner's attendance at this visit - row should exist for every visit
      val prisonerVisited = prisonerVisitedRepository.findByOfficialVisitId(ov.officialVisitId)
        ?: throw EntityNotFoundException("Prisoner visited for visit ID ${ov.officialVisitId} - ${ov.prisonerNumber} was not found")

      OfficialVisitSummarySearchResponse(
        officialVisitId = ov.officialVisitId,
        prisonCode = prisonCode,
        prisonDescription = prisonName!!,
        visitStatus = ov.visitStatusCode,
        visitStatusDescription = getReferenceDescription(ReferenceDataGroup.VIS_STATUS, ov.visitStatusCode.name)!!,
        visitTypeCode = ov.visitTypeCode,
        visitTypeDescription = getReferenceDescription(ReferenceDataGroup.VIS_TYPE, ov.visitTypeCode.name)!!,
        visitDate = ov.visitDate,
        startTime = ov.startTime,
        endTime = ov.endTime,
        dpsLocationId = ov.dpsLocationId,
        locationDescription = location?.localName ?: location?.key ?: "Unknown",
        visitSlotId = ov.prisonVisitSlotId,
        staffNotes = ov.staffNotes,
        prisonerNotes = ov.prisonerNotes,
        visitorConcernNotes = ov.visitorConcernNotes,
        numberOfVisitors = ov.numberOfVisitors,
        completionCode = ov.completionCode,
        completionDescription = getReferenceDescription(ReferenceDataGroup.VIS_COMPLETION, ov.completionCode?.name),
        createdBy = ov.createdBy,
        createdTime = ov.createdTime,
        updatedBy = ov.updatedBy,
        updatedTime = ov.updatedTime,
        prisoner = PrisonerVisitedDetails(
          prisonCode = prisonCode,
          prisonerNumber = ov.prisonerNumber,
          firstName = prisoner?.firstName,
          lastName = prisoner?.lastName,
          middleNames = prisoner?.middleNames,
          dateOfBirth = prisoner?.dateOfBirth,
          cellLocation = prisoner?.cellLocation,
          offenderBookId = ov.offenderBookId,
          attendanceCode = prisonerVisited.attendanceCode?.name,
          attendanceCodeDescription = getReferenceDescription(ReferenceDataGroup.ATTENDANCE, prisonerVisited.attendanceCode?.name),
        ),
      )
    }

    PagedModel(response)
  }

  private fun getReferenceDescription(group: ReferenceDataGroup, code: String?) = code?.let {
    referenceDataService.getReferenceDataByGroupAndCode(group, code)?.description ?: "Reference code $code not found"
  }

  private data class LocationDescription(
    val id: UUID,
    val localName: String?,
    val locationType: Location.LocationType,
    val key: String?,
  )
}
