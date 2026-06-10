package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.DownstreamServiceException
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.DuplicateOffenderVisitIdConflictException
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toSyncModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditEventType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitChangeEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitCreateEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitDeletedEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsEvents
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.VisitMetricInfo

@Service
@Transactional
class SyncOfficialVisitService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val officialVisitorRepository: OfficialVisitorRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val metricsService: MetricsService,
  private val auditingService: AuditingService,
  private val userService: UserService,
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
    val createdBy = userService.getUser(request.createUsername!!)
      ?: throw DownstreamServiceException("Cannot retrieve user details for ${request.createUsername}")

    val visitSlot = prisonVisitSlotRepository.findById(request.prisonVisitSlotId).orElseThrow {
      EntityNotFoundException("Prison visit slot ID ${request.prisonVisitSlotId} does not exist")
    }

    // When an offenderVisitId is supplied perform a check for duplicates
    request.offenderVisitId.let { offenderVisitId ->
      officialVisitRepository.findByOffenderVisitId(offenderVisitId)?.let { duplicate ->
        throw DuplicateOffenderVisitIdConflictException(
          offenderVisitId,
          duplicate.officialVisitId,
          "Official visit with offenderVisitId $offenderVisitId already exists (DPS ID ${duplicate.officialVisitId})",
        )
      }
    }

    val visit = officialVisitRepository.saveAndFlush(OfficialVisitEntity.synchronised(visitSlot, request)).also {
      metricsService.send(
        MetricsEvents.CREATE,
        VisitMetricInfo(
          username = it.createdBy,
          officialVisitId = it.officialVisitId,
          prisonCode = it.prisonCode,
          prisonerNumber = it.prisonerNumber,
          numberOfVisitors = it.officialVisitors().size.toLong(),
          startTime = request.startTime,
          source = Source.NOMIS,
        ),
      )
    }

    val prisonerVisited = prisonerVisitedRepository.saveAndFlush(
      PrisonerVisitedEntity(
        officialVisit = visit,
        prisonerNumber = visit.prisonerNumber,
        createdBy = visit.createdBy,
        createdTime = visit.createdTime,
      ),
    )

    return visit.toSyncModel(prisonerVisited).also {
      auditingService.recordAuditEvent(
        auditVisitCreateEvent {
          officialVisitId(visit.officialVisitId)
          summaryText(AuditEventType.VISIT_CREATED)
          eventSource("NOMIS")
          user(createdBy)
          prisonCode(visit.prisonCode)
          prisonerNumber(visit.prisonerNumber)
          detailsText("Official visit created for prisoner number ${it.prisonerNumber}")
        },
      )
    }
  }

  fun updateVisit(officialVisitId: Long, request: SyncUpdateOfficialVisitRequest): SyncOfficialVisit {
    val updatedBy = userService.getUser(request.updateUsername)
      ?: throw DownstreamServiceException("Cannot retrieve user details for ${request.updateUsername}")

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

    val auditChangeEvent = auditVisitChangeEvent {
      officialVisitId(visit.officialVisitId)
      summaryText(AuditEventType.VISIT_UPDATED)
      eventSource("NOMIS")
      user(updatedBy)
      prisonCode(request.prisonCode)
      prisonerNumber(request.prisonerNumber)
      changes {
        change("visit _slot", visit.prisonVisitSlot.prisonVisitSlotId, request.prisonVisitSlotId)
        change("visit_date", visit.visitDate, request.visitDate)
        change("start_time", visit.startTime, request.startTime)
        change("end_time", visit.endTime, request.endTime)
        change("location", visit.dpsLocationId, request.dpsLocationId)
        change("prison_code", visit.prisonCode, request.prisonCode)
        change("prisoner_number", visit.prisonerNumber, request.prisonerNumber)
        change("prisoner_notes", visit.prisonerNotes, request.commentText)
        change("prisoner_current_term", visit.currentTerm, request.currentTerm)
        change("visitor_concern_notes", visit.visitorConcernNotes, request.visitorConcernText)
        change("override_ban_by", visit.overrideBanBy, request.overrideBanStaffUsername)
        change("offender_book_id", visit.offenderBookId, request.offenderBookId)
        change("offender_visit_id", visit.offenderVisitId, request.offenderVisitId)
        change("visit_order_number", visit.visitOrderNumber, request.visitOrderNumber)
        change("visit_status_code", visit.visitStatusCode, request.visitStatusCode)
        change("visit_completion_code", visit.completionCode, request.visitCompletionCode)
        change("search_type_code", visit.searchTypeCode, request.searchTypeCode)
      }
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
      currentTerm = request.currentTerm
      visitorConcernNotes = request.visitorConcernText
      overrideBanBy = request.overrideBanStaffUsername
      offenderBookId = request.offenderBookId
      offenderVisitId = request.offenderVisitId
      visitOrderNumber = request.visitOrderNumber
      visitStatusCode = request.visitStatusCode
      completionCode = request.visitCompletionCode
      searchTypeCode = request.searchTypeCode
      updatedTime = request.updateDateTime
      this.updatedBy = request.updateUsername
    }

    val savedVisit = officialVisitRepository.saveAndFlush(visit).also {
      metricsService.send(
        MetricsEvents.AMEND,
        VisitMetricInfo(
          username = request.updateUsername,
          officialVisitId = it.officialVisitId,
          prisonCode = it.prisonCode,
          prisonerNumber = it.prisonerNumber,
          startTime = request.startTime,
          source = Source.NOMIS,
        ),
      )
    }

    auditingService.recordAuditEvent(auditChangeEvent)

    return savedVisit.toSyncModel(prisonerVisited)
  }

  fun deleteVisit(officialVisitId: Long) = officialVisitRepository.findByIdOrNull(officialVisitId)?.also { officialVisit ->
    officialVisitorRepository.deleteByOfficialVisit(officialVisit)
    prisonerVisitedRepository.deleteByOfficialVisit(officialVisit)
    officialVisitRepository.deleteById(officialVisit.officialVisitId)

    auditingService.recordAuditEvent(
      auditVisitDeletedEvent {
        officialVisitId(officialVisit.officialVisitId)
        summaryText(AuditEventType.VISIT_DELETED)
        eventSource("NOMIS")
        user(UserService.getClientAsUser("NOMIS"))
        prisonCode(officialVisit.prisonCode)
        prisonerNumber(officialVisit.prisonerNumber)
      },
    )
  }?.toSyncModel()
}
