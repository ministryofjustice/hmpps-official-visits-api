package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toModelIds
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toSyncModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitId
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository

@Service
class OfficialVisitReconciliationService(
  private val officialVisitRepository: OfficialVisitRepository,
) {

  fun getOfficialVisitsIds(currentTerm: Boolean, pageable: Pageable): PagedModel<SyncOfficialVisitId> = PagedModel(officialVisitRepository.findAllOfficialVisitIds(currentTerm, pageable).toModelIds())
  fun getOfficialVisitsById(officialVisitId: Long): SyncOfficialVisit = officialVisitRepository.findById(officialVisitId).get().toSyncModel()
}
