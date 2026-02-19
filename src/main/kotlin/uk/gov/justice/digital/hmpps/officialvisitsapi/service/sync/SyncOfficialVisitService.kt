package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toSyncModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository

@Service
@Transactional
class SyncOfficialVisitService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val officialVisitorRepository: OfficialVisitorRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(readOnly = true)
  fun getOfficialVisitById(officialVisitId: Long): SyncOfficialVisit {
    val ove = officialVisitRepository.findById(officialVisitId).orElseThrow {
      EntityNotFoundException("Official visit with id $officialVisitId not found")
    }

    val pve = prisonerVisitedRepository.findByOfficialVisit(ove)
      ?: throw EntityNotFoundException("Prisoner visited not found for visit ID $officialVisitId")

    return ove.toSyncModel(pve)
  }

  fun createOfficialVisit(request: SyncCreateOfficialVisitRequest): SyncOfficialVisit {
    val visitSlot = prisonVisitSlotRepository.findById(request.prisonVisitSlotId!!).orElseThrow {
      EntityNotFoundException("Prison visit slot ID ${request.prisonVisitSlotId} does not exist")
    }

    // TODO: Check whether NOMIS includes the visitCompletionType or searchType at the point of creation, and prisoner attendance

    val visit = officialVisitRepository.saveAndFlush(OfficialVisitEntity.synchronised(visitSlot, request))

    val prisonVisited = prisonerVisitedRepository.saveAndFlush(
      PrisonerVisitedEntity(
        officialVisit = visit,
        prisonerNumber = visit.prisonerNumber,
        createdBy = visit.createdBy,
        createdTime = visit.createdTime,
      ),
    )

    return visit.toSyncModel(prisonVisited)
  }

  fun updateOfficialVisit(officialVisitId: Long, request: SyncUpdateOfficialVisitRequest): SyncOfficialVisit {
    val ove = officialVisitRepository.findById(officialVisitId).orElseThrow {
      EntityNotFoundException("Official visit with id $officialVisitId not found")
    }

    val pve = prisonerVisitedRepository.findByOfficialVisit(ove)
      ?: throw EntityNotFoundException("Prisoner visited not found for visit ID $officialVisitId")

    // Implement the updates
    // Do not include anything about the visitors here
    // Check what has changed?
    // All fields are nullable - take the original value if request value is null
    // How would they set something to null that previously had a value?
    // Record the changes?
    // Implement a copy command on the official visit entity

    return ove.toSyncModel(pve)
  }

  fun deleteOfficialVisit(officialVisitId: Long) = officialVisitRepository.findByIdOrNull(officialVisitId)?.also { officialVisit ->
    officialVisitorRepository.deleteByOfficialVisit(officialVisit)
    prisonerVisitedRepository.deleteByOfficialVisit(officialVisit)
    officialVisitRepository.deleteById(officialVisit.officialVisitId)
  }?.toSyncModel()
}
