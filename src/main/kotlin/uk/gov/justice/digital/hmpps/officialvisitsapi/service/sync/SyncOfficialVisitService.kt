package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
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
  @Transactional(readOnly = true)
  fun getVisitById(officialVisitId: Long): SyncOfficialVisit {
    val ove = officialVisitRepository.findById(officialVisitId).orElseThrow {
      EntityNotFoundException("Official visit with id $officialVisitId not found")
    }

    val pve = prisonerVisitedRepository.findByOfficialVisit(ove)
      ?: throw EntityNotFoundException("Prisoner visited not found for visit ID $officialVisitId")

    return ove.toSyncModel(pve)
  }

  fun createVisit(request: SyncCreateOfficialVisitRequest): SyncOfficialVisit {
    val visitSlot = prisonVisitSlotRepository.findById(request.prisonVisitSlotId!!).orElseThrow {
      EntityNotFoundException("Prison visit slot ID ${request.prisonVisitSlotId} does not exist")
    }

    // When an offenderVisitId is supplied perform a check for duplicates
    request.offenderVisitId?.let { offenderVisitId ->
      officialVisitRepository.findByOffenderVisitId(offenderVisitId)?.let { duplicate ->
        throw EntityInUseException(
          "Official visit with offenderVisitId $offenderVisitId already exists (DPS ID ${duplicate.officialVisitId})",
        )
      }
    }

    val visit = officialVisitRepository.saveAndFlush(OfficialVisitEntity.synchronised(visitSlot, request))

    val prisonerVisited = prisonerVisitedRepository.saveAndFlush(
      PrisonerVisitedEntity(
        officialVisit = visit,
        prisonerNumber = visit.prisonerNumber,
        createdBy = visit.createdBy,
        createdTime = visit.createdTime,
      ),
    )

    return visit.toSyncModel(prisonerVisited)
  }

  fun updateVisit(officialVisitId: Long, request: SyncUpdateOfficialVisitRequest): SyncOfficialVisit {
    val visit = officialVisitRepository.findById(officialVisitId).orElseThrow {
      EntityNotFoundException("Official visit with ID $officialVisitId not found")
    }

    val pve = prisonerVisitedRepository.findByOfficialVisit(visit)
      ?: throw EntityNotFoundException("Prisoner visited not found for visit ID $officialVisitId")

    // Deal with a change of prisoner
    val prisonerVisited = if (request.prisonerNumber != pve.prisonerNumber) {
      prisonerVisitedRepository.deleteById(pve.prisonerVisitedId)
      prisonerVisitedRepository.saveAndFlush(
        PrisonerVisitedEntity(
          officialVisit = visit,
          prisonerNumber = request.prisonerNumber,
          createdBy = request.updateUsername,
          createdTime = request.updateDateTime,
        ),
      )
    } else {
      pve
    }

    // Deal with a change of visit slot
    val visitSlot = if (request.prisonVisitSlotId != visit.prisonVisitSlot.prisonVisitSlotId) {
      prisonVisitSlotRepository.findById(request.prisonVisitSlotId).orElseThrow {
        EntityNotFoundException("Visit slot with ID ${request.prisonVisitSlotId} not found")
      }
    } else {
      visit.prisonVisitSlot
    }

    visit.apply {
      prisonVisitSlot = visitSlot
      visitDate = request.visitDate
      startTime = request.startTime
      endTime = request.endTime
      dpsLocationId = request.dpsLocationId
      prisonCode = request.prisonCode
      prisonerNumber = request.prisonerNumber
      prisonerNotes = request.commentText
      visitorConcernNotes = request.visitorConcernText
      overrideBanBy = request.overrideBanStaffUsername
      offenderBookId = request.offenderBookId
      offenderVisitId = request.offenderVisitId
      visitOrderNumber = request.visitOrderNumber
      visitStatusCode = request.visitStatusCode
      completionCode = request.visitCompletionCode
      searchTypeCode = request.searchTypeCode
      updatedTime = request.updateDateTime
      updatedBy = request.updateUsername
    }

    val savedVisit = officialVisitRepository.saveAndFlush(visit)

    return savedVisit.toSyncModel(prisonerVisited)
  }

  fun deleteVisit(officialVisitId: Long) = officialVisitRepository.findByIdOrNull(officialVisitId)?.also { officialVisit ->
    officialVisitorRepository.deleteByOfficialVisit(officialVisit)
    prisonerVisitedRepository.deleteByOfficialVisit(officialVisit)
    officialVisitRepository.deleteById(officialVisit.officialVisitId)
  }?.toSyncModel()
}
