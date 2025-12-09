package uk.gov.justice.digital.hmpps.officialvisitsapi.service.migrate

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonTimeSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.AttendanceType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.MigrateVisitConfigRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.MigrateVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.ElementType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.IdPair
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.MigrateVisitConfigResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.MigrateVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import java.time.LocalDateTime

@Service
class MigrationService(
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val officialVisitRepository: OfficialVisitRepository,
  private val officialVisitorRepository: OfficialVisitorRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun migrateVisitConfiguration(request: MigrateVisitConfigRequest): MigrateVisitConfigResponse {
    logger.info(
      "Migrate time slot in {} day {} timeSlotSeq {} start {} end {} effective {} with {} visit slots",
      request.prisonCode!!,
      request.dayCode!!,
      request.timeSlotSeq!!,
      request.startTime!!,
      request.endTime!!,
      request.effectiveDate!!,
      request.visitSlots.size,
    )

    val timeSlot = prisonTimeSlotRepository.saveAndFlush(
      PrisonTimeSlotEntity(
        prisonCode = request.prisonCode,
        dayCode = request.dayCode,
        startTime = request.startTime,
        endTime = request.endTime,
        effectiveDate = request.effectiveDate,
        expiryDate = request.expiryDate,
        createdTime = request.createDateTime ?: LocalDateTime.now(),
        createdBy = request.createUsername ?: "MIGRATION",
        updatedTime = request.modifyDateTime,
        updatedBy = request.modifyUsername,
      ),
    )

    val visitSlots = extractAndSaveVisitSlots(timeSlot.prisonTimeSlotId, request)

    return MigrateVisitConfigResponse(
      prisonCode = request.prisonCode,
      dayCode = request.dayCode,
      timeSlotSeq = request.timeSlotSeq,
      dpsTimeSlotId = timeSlot.prisonTimeSlotId,
      visitSlots = visitSlots.map { IdPair(ElementType.PRISON_VISIT_SLOT, it.first, it.second.prisonVisitSlotId) },
    )
  }

  fun extractAndSaveVisitSlots(timeSlotId: Long, request: MigrateVisitConfigRequest) = request.visitSlots.map { slot ->
    Pair(
      slot.agencyVisitSlotId!!,
      prisonVisitSlotRepository.saveAndFlush(
        PrisonVisitSlotEntity(
          prisonTimeSlotId = timeSlotId,
          dpsLocationId = slot.dpsLocationId!!,
          maxAdults = slot.maxAdults,
          maxGroups = slot.maxGroups,
          maxVideoSessions = slot.maxVideoSessions,
          createdTime = slot.createDateTime ?: LocalDateTime.now(),
          createdBy = slot.createUsername ?: "MIGRATION",
          updatedTime = slot.modifyDateTime,
          updatedBy = slot.modifyUsername,
        ),
      ),
    )
  }

  @Transactional
  fun migrateVisit(request: MigrateVisitRequest): MigrateVisitResponse {
    logger.info(
      "Migrate official visit ID {} at {} for {} (bookId {}) on {} with {} visitors",
      request.offenderVisitId,
      request.prisonCode!!,
      request.prisonerNumber!!,
      request.offenderBookId,
      request.visitDate,
      request.visitors.size,
    )

    // The visit slot must exist prior to related visits being migrated
    val visitSlot = prisonVisitSlotRepository.findById(request.prisonVisitSlotId!!).orElseThrow {
      EntityNotFoundException("Prison visit slot ID ${request.prisonVisitSlotId} not found on offender visit ID ${request.offenderVisitId}")
    }

    val visit = officialVisitRepository.saveAndFlush(
      OfficialVisitEntity(
        prisonVisitSlot = visitSlot,
        visitDate = request.visitDate!!,
        startTime = request.startTime!!,
        endTime = request.endTime!!,
        dpsLocationId = request.dpsLocationId!!,
        visitStatusCode = request.visitStatusCode!!,
        visitTypeCode = request.visitTypeCode!!,
        prisonCode = request.prisonCode,
        prisonerNumber = request.prisonerNumber,
        currentTerm = request.currentTerm!!,
        staffNotes = request.commentText,
        prisonerNotes = null, // Never supplied
        visitorConcernNotes = request.visitorConcernText,
        searchTypeCode = request.searchTypeCode,
        completionCode = request.visitCompletionCode,
        overrideBanTime = null, // Never supplied
        overrideBanBy = request.overrideBanStaffUsername,
        createdBy = request.createUsername ?: "MIGRATION",
        createdTime = request.createDateTime ?: LocalDateTime.now(),
        updatedBy = request.modifyUsername,
        updatedTime = request.modifyDateTime,
        offenderBookId = request.offenderBookId,
        offenderVisitId = request.offenderVisitId!!,
        visitOrderNumber = request.visitOrderNumber,
      ),
    )

    val prisoner = extractAndSavePrisonerVisited(visit, request)

    val visitorPairs = extractAndSaveVisitors(visit, request)

    return MigrateVisitResponse(
      visit = IdPair(elementType = ElementType.OFFICIAL_VISIT, nomisId = request.offenderVisitId, dpsId = visit.officialVisitId),
      prisoner = IdPair(elementType = ElementType.PRISONER_VISITED, nomisId = request.offenderBookId!!, dpsId = prisoner.prisonerVisitedId),
      visitors = visitorPairs.map { IdPair(elementType = ElementType.OFFICIAL_VISITOR, nomisId = it.first, dpsId = it.second.officialVisitorId) },
    )
  }

  fun extractAndSaveVisitors(dpsVisit: OfficialVisitEntity, request: MigrateVisitRequest) = request.visitors.map { visitor ->
    Pair(
      visitor.offenderVisitVisitorId!!,
      officialVisitorRepository.saveAndFlush(
        OfficialVisitorEntity(
          officialVisit = dpsVisit,
          visitorTypeCode = VisitorType.CONTACT,
          firstName = visitor.firstName,
          lastName = visitor.lastName,
          contactId = visitor.personId,
          prisonerContactId = null, // Not supplied via migration
          relationshipTypeCode = visitor.relationshipTypeCode,
          relationshipCode = visitor.relationshipToPrisoner,
          leadVisitor = visitor.groupLeaderFlag ?: false,
          assistedVisit = visitor.assistedVisitFlag ?: false,
          visitorNotes = visitor.commentText,
          attendanceCode = visitor.attendanceCode,
          createdBy = visitor.createUsername ?: "MIGRATION",
          createdTime = visitor.createDateTime ?: LocalDateTime.now(),
          updatedBy = visitor.modifyUsername,
          updatedTime = visitor.modifyDateTime,
          offenderVisitVisitorId = visitor.offenderVisitVisitorId,
        ),
      ),
    )
  }

  fun extractAndSavePrisonerVisited(
    dpsVisit: OfficialVisitEntity,
    request: MigrateVisitRequest,
  ): PrisonerVisitedEntity = prisonerVisitedRepository.saveAndFlush(
    PrisonerVisitedEntity(
      officialVisit = dpsVisit,
      prisonerNumber = dpsVisit.prisonerNumber,
      attendanceCode = mapPrisonerAttendance(request),
      createdBy = dpsVisit.createdBy,
      createdTime = dpsVisit.createdTime,
      updatedBy = dpsVisit.updatedBy,
      updatedTime = dpsVisit.updatedTime,
    ),
  )

  private fun mapPrisonerAttendance(request: MigrateVisitRequest) = when (request.visitCompletionCode) {
    null,
    VisitCompletionType.VISITOR_CANCELLED,
    VisitCompletionType.VISITOR_NO_SHOW,
    VisitCompletionType.PRISONER_CANCELLED,
    VisitCompletionType.VISITOR_DENIED,
    VisitCompletionType.STAFF_CANCELLED,
    -> null

    VisitCompletionType.PRISONER_REFUSED,
    -> AttendanceType.ABSENT

    VisitCompletionType.VISITOR_EARLY,
    VisitCompletionType.STAFF_EARLY,
    VisitCompletionType.NORMAL,
    VisitCompletionType.PRISONER_EARLY,
    -> AttendanceType.ATTENDED
  }
}
