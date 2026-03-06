package uk.gov.justice.digital.hmpps.officialvisitsapi.facade.admin

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.VisitLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.LocationsService

@Component
class LocationsFacade(private val locationsService: LocationsService) {
  fun getOfficialVisitLocationsAtPrison(prisonCode: String): List<VisitLocation> = locationsService.getOfficialVisitLocationsAtPrison(prisonCode).map { loc ->
    VisitLocation(locationId = loc.id, locationName = loc.localName)
  }
}
