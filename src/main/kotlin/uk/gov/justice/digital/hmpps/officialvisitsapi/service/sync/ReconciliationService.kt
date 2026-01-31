package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

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
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class ReconciliationService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
) {
  fun getOfficialVisitIds(currentTermOnly: Boolean, pageable: Pageable): PagedModel<SyncOfficialVisitId> = PagedModel(
    officialVisitRepository.findAllOfficialVisitIds(currentTermOnly.takeIf { it }, pageable).toModelIds(),
  )

  fun getOfficialVisitById(officialVisitId: Long): SyncOfficialVisit {
    val ove = officialVisitRepository.findById(officialVisitId).orElseThrow {
      EntityNotFoundException("Official visit with id $officialVisitId not found")
    }

    val pve = prisonerVisitedRepository.findByOfficialVisit(ove)
      ?: throw EntityNotFoundException("Prisoner visited not found for visit ID $officialVisitId")

    return ove.toSyncModel(pve)
  }

  fun getAllPrisonerVisits(prisonerNumber: String, currentTerm: Boolean, fromDate: LocalDate?, toDate: LocalDate?): List<SyncOfficialVisit> {
    val oves = officialVisitRepository.findAllPrisonerVisits(prisonerNumber, currentTerm, fromDate, toDate)
    return if (oves.isEmpty()) {
      emptyList()
    } else {
      val pves = prisonerVisitedRepository.findAllByOfficialVisitOfficialVisitIdIn(oves.map { it.officialVisitId })
      val pveMap = pves.associateBy { it.officialVisit.officialVisitId }
      oves.map { it.toSyncModel(pveMap[it.officialVisitId]) }
    }
  }
}
