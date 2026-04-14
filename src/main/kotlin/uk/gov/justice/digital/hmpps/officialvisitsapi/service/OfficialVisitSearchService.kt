package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.PrisonerContactDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PrisonerContactRelationship
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.SummaryRelationship
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.StringFeature
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitSummaryEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.ReferenceDataGroup
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitSummarySearchRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitSummarySearchResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.PrisonerVisitedDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitSummaryRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsEvents
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.SearchInfo
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OfficialVisitSearchService(
  private val officialVisitSummaryRepository: OfficialVisitSummaryRepository,
  private val referenceDataService: ReferenceDataService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
  private val visitsWithApprovalIssues: VisitsWithApprovalIssues,
  private val metricsService: MetricsService,
) {
  fun searchForOfficialVisitSummaries(prisonCode: String, request: OfficialVisitSummarySearchRequest, user: User, page: Int, size: Int): PagedModel<OfficialVisitSummarySearchResponse> = run {
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

    metricsService.send(
      eventType = MetricsEvents.SEARCH,
      info = SearchInfo(
        prisonCode = prisonCode,
        username = user.username,
        startDate = request.startDate,
        searchTerm = mayBeSearchTerm.orEmpty(),
        endDate = request.endDate,
        visitTypes = request.visitTypes.takeUnless { it.isNullOrEmpty() },
        locationIds = request.locationIds.takeUnless { it.isNullOrEmpty() },
        visitStatuses = request.visitStatuses.takeUnless { it.isNullOrEmpty() },
        numberOfResults = results.content.size,
      ),
    )
    if (results.isEmpty) {
      return PagedModel(Page.empty())
    }

    val matchingPrisoners = if (prisoners.isNotEmpty()) {
      prisoners.filter { it.prisonerNumber in results.map { ov -> ov.prisonerNumber } }.distinctBy { it.prisonerNumber }
    } else {
      results.map { it.prisonerNumber }.toSet().let { prisonerSearchClient.findByPrisonerNumbers(it.toList(), it.size) }
    }

    // Get the prisoner details for the full page of results
    val prisonerMap = matchingPrisoners.associateBy { it.prisonerNumber }

    // Get the locations for visits for this prison (it's a cacheable endpoint)
    val locations = locationsInsidePrisonClient.getOfficialVisitLocationsAtPrison(prisonCode)
    val locationMap = locations.map { LocationDescription(it.id, it.localName, it.locationType, it.key) }.associateBy { it.id }
    // TODO temporary disable until we can re-enable this
//    val visitIdsWithIssues = visitsWithApprovalIssues.identify(prisonCode, results.content.visitsScheduleAfter(LocalDateTime.now()).map { it.officialVisitId })
    val visitIdsWithIssues = emptySet<Long>()

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
        completionNotes = ov.completionNotes,
        createdBy = ov.createdBy,
        createdTime = ov.createdTime,
        updatedBy = ov.updatedBy,
        updatedTime = ov.updatedTime,
        visitorIssues = visitIdsWithIssues.contains(ov.officialVisitId),
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

  private fun List<OfficialVisitSummaryEntity>.visitsScheduleAfter(dateTime: LocalDateTime) = filter { it.isAfter(dateTime) && it.isScheduled() }

  private fun OfficialVisitSummaryEntity.isAfter(dateTime: LocalDateTime) = visitDate.atTime(startTime) > dateTime

  private fun OfficialVisitSummaryEntity.isScheduled() = visitStatusCode == VisitStatusType.SCHEDULED
}

@Component
class VisitsWithApprovalIssues(
  private val officialVisitRepository: OfficialVisitRepository,
  private val contactsService: ContactsService,
  private val featureSwitches: FeatureSwitches,
) {
  /**
   * Identifies visits with potential approval issues. Only those with potential issues are returned.
   */
  fun identify(prisonCode: String, candidatesVisitIdentifiers: Collection<Long>): Collection<Long> {
    val visitsOfInterest = officialVisitRepository.findAllById(candidatesVisitIdentifiers)

    val visitsToPrisonerContacts = buildSet {
      visitsOfInterest.forEach { visit ->
        visit.officialVisitors().filterNot { it.contactId == null }.forEach { visitor ->
          add(VisitPrisonerContactDto(visit.officialVisitId, visit.prisonerNumber, visitor.contactId!!))
        }
      }
    }

    val relationships = contactsService.getPrisonerContactRelationships(
      visitsToPrisonerContacts.map {
        PrisonerContactDto(
          it.prisonerNumber,
          it.contactId,
        )
      }.toSet(),
    )

    return buildSet {
      visitsToPrisonerContacts.forEach { visit ->
        if (relationships[visit.prisonerNumber].isNullOrEmpty() || relationships[visit.prisonerNumber]!!.any { it.hasIssues(prisonCode) }) {
          add(visit.officialVisitId)
        }
      }
    }
  }

  private data class VisitPrisonerContactDto(val officialVisitId: Long, val prisonerNumber: String, val contactId: Long)

  private fun socialPrisons() = featureSwitches.getValue(StringFeature.FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS, null)?.split(',')?.toSet() ?: emptySet()

  private fun PrisonerContactRelationship.hasIssues(prisonCode: String) = this.relationships.isEmpty() ||
    this.relationships.any {
      it.isNotAnApprovedVisitor() || this.relationships.any { it.isSocialVisitor() && !socialPrisons().contains(prisonCode) }
    }

  private fun SummaryRelationship.isNotAnApprovedVisitor() = !isApprovedVisitor

  private fun SummaryRelationship.isSocialVisitor() = relationshipTypeCode == "S"
}
