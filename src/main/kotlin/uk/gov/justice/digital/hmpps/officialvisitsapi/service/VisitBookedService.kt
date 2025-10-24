package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitBookedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitBookedRepository

@Service
class VisitBookedService(private val visitBookedRepository: VisitBookedRepository) {
  // TODO: Map from entity to a model representation if exposing via API
  fun getVisitsBookedForPrison(prisonCode: String): List<VisitBookedEntity> = visitBookedRepository.findVisitsBookedByPrisonCode(prisonCode)
}
