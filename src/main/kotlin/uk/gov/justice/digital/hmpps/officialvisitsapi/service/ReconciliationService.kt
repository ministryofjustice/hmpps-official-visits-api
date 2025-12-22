package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toModelIds
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toSyncModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitId
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository

@Service
@Transactional(readOnly = true)
class ReconciliationService(private val officialVisitRepository: OfficialVisitRepository) {
  fun getOfficialVisitIds(currentTerm: Boolean, pageable: Pageable): PagedModel<SyncOfficialVisitId> = PagedModel(officialVisitRepository.findAllOfficialVisitIds(currentTerm, pageable).toModelIds())

  fun getOfficialVisitById(officialVisitId: Long): SyncOfficialVisit {
    val syncOfficialVisit = officialVisitRepository.findById(officialVisitId).orElseThrow {
      EntityNotFoundException("Official visit with id $officialVisitId not found")
    }
    return syncOfficialVisit.toSyncModel()
  }
}
