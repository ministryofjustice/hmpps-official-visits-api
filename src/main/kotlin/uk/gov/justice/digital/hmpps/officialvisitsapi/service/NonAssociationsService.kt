package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.nonassociations.NonAssociationsApiClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.nonassociations.model.OtherPrisonerDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NonAssociationCheckRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.NonAssociationVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository

@Service
@Transactional(readOnly = true)
class NonAssociationsService(
  private val nonAssociationsApiClient: NonAssociationsApiClient,
  private val officialVisitRepository: OfficialVisitRepository,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Returns the scheduled official visits that any of the prisoner's open non-associations have at the same prison
   * on the same date as the visit being checked.
   */
  fun getNonAssociationVisits(prisonCode: String, request: NonAssociationCheckRequest): List<NonAssociationVisitResponse> {
    // Get the prisoner's open non-associations, keyed by the other prisoner's number
    val nonAssociatesByPrisonerNumber = nonAssociationsApiClient
      .getPrisonerNonAssociations(request.prisonerNumber)
      ?.nonAssociations
      .orEmpty()
      .associate { it.otherPrisonerDetails.prisonerNumber to it.otherPrisonerDetails }

    if (nonAssociatesByPrisonerNumber.isEmpty()) {
      logger.info("No open non-associations found for prisoner ${request.prisonerNumber}, no visits to check")
      return emptyList()
    }

    // Get any scheduled visits those non-associates have at this prison on the same date
    val visits = officialVisitRepository.findScheduledVisitsForPrisonersOn(
      prisonCode = prisonCode,
      prisonerNumbers = nonAssociatesByPrisonerNumber.keys,
      visitDate = request.visitDate,
    )

    if (visits.isEmpty()) {
      logger.info("No non-associate visits at $prisonCode on ${request.visitDate} for prisoner ${request.prisonerNumber}")
      return emptyList()
    }

    // Get the locations for visits for this prison
    val locationDescriptions = locationsInsidePrisonClient.getOfficialVisitLocationsAtPrison(prisonCode)
      .associate { location -> location.id to (location.localName ?: location.key) }

    return visits.mapNotNull { visit ->
      nonAssociatesByPrisonerNumber[visit.prisonerNumber]?.let { nonAssociate ->
        visit.toNonAssociationVisitResponse(nonAssociate, locationDescriptions[visit.dpsLocationId] ?: "Unknown")
      }
    }.also { logger.info("Found ${it.size} non-associate visit(s) at $prisonCode on ${request.visitDate} for prisoner ${request.prisonerNumber}") }
  }

  private fun OfficialVisitEntity.toNonAssociationVisitResponse(nonAssociate: OtherPrisonerDetails, locationDescription: String) = NonAssociationVisitResponse(
    prisonCode = prisonCode,
    officialVisitId = officialVisitId,
    visitDate = visitDate,
    startTime = startTime,
    endTime = endTime,
    prisonerNumber = prisonerNumber,
    dpsLocationId = dpsLocationId,
    locationDescription = locationDescription,
    firstName = nonAssociate.firstName,
    lastName = nonAssociate.lastName,
  )
}
