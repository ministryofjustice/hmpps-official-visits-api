package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.NonResidentialLocationDTO

@Service
class LocationsService(private val locationsInsidePrisonClient: LocationsInsidePrisonClient) {
  fun getOfficialVisitLocationsAtPrison(prisonCode: String): List<NonResidentialLocationDTO>? = run {
    locationsInsidePrisonClient.getNonResidentialOfficialVisitLocationsAtPrison(prisonCode)?.locations?.content
  }
}
