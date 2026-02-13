package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location

@Service
class LocationsService(private val locationsInsidePrisonClient: LocationsInsidePrisonClient) {
  fun getActiveVisitLocationsAtPrison(prisonCode: String): List<Location> = run {
    locationsInsidePrisonClient.getOfficialVisitLocationsAtPrison(prisonCode).filter { loc -> loc.status == Location.Status.ACTIVE }
  }

  fun getAllVisitLocationsAtPrison(prisonCode: String): List<Location> = run {
    locationsInsidePrisonClient.getOfficialVisitLocationsAtPrison(prisonCode)
  }
}
