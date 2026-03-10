package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import java.util.UUID

@Service
class LocationsService(private val locationsInsidePrisonClient: LocationsInsidePrisonClient) {

  fun getLocationById(id: UUID): Location? = run {
    locationsInsidePrisonClient.getLocationById(id)
  }

  fun getOfficialVisitLocationsAtPrison(prisonCode: String): List<Location> = run {
    locationsInsidePrisonClient.getOfficialVisitLocationsAtPrison(prisonCode)
  }
}
