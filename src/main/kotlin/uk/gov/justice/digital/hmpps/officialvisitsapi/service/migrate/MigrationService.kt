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

    // TODO: Get the contact and relationships from the personal relationships API -- CurrentTerm? Old ones?

    val visit = officialVisitRepository.saveAndFlush(
      OfficialVisitEntity(
        prisonVisitSlot = visitSlot,
        prisonCode = request.prisonCode,
        prisonerNumber = request.prisonerNumber,
        currentTerm = request.currentTerm ?: false,
        visitDate = request.visitDate!!,
        startTime = request.startTime!!,
        endTime = request.endTime!!,
        dpsLocationId = request.dpsLocationId!!,
        visitStatusCode = request.visitStatusCode?.code ?: "ACTIVE", // TODO: Map this value to DPS code
        visitTypeCode = request.visitTypeCode?.code ?: "IN_PERSON", // TODO: Map this value to DPS code
        publicNotes = request.commentText,
        searchTypeCode = request.searchTypeCode?.code, // TODO: Map this value to DPS code
        visitorConcernText = request.visitorConcernText,
        completionCode = request.eventOutcomeCode?.code, // TODO: Map this from outcome code and outcome reason code
        overrideBanBy = request.overrideBanStaffUsername,
        overrideBanTime = null, // TODO: Investigate whether Syscon can send this?
        createdTime = request.createDateTime ?: LocalDateTime.now(),
        createdBy = request.createUsername ?: "MIGRATION",
        updatedTime = request.modifyDateTime,
        updatedBy = request.modifyUsername,
        offenderVisitId = request.offenderVisitId!!,
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
          contactId = visitor.personId,
          visitorTypeCode = "CONTACT", // TODO: Will there be other types? e.g. PRISONER, OPV?
          contactTypeCode = "O", // TODO: Get from contacts
          leadVisitor = visitor.groupLeaderFlag ?: false,
          assistedVisit = visitor.assistedVisitFlag ?: false,
          visitorNotes = visitor.commentText,
          firstName = visitor.firstName,
          lastName = visitor.lastName,
          prisonerContactId = null, // TODO: Get from contacts
          relationshipCode = visitor.relationshipToPrisoner?.code,
          attendanceCode = visitor.eventOutcomeCode?.code, // TODO: Map from event outcome/outcome reason code
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
      attendanceCode = dpsVisit.completionCode, // TODO: Check this
      attendanceBy = null, // TODO: Don't think we can get this for migrated visits?
      createdBy = dpsVisit.createdBy,
      createdTime = dpsVisit.createdTime,
      updatedBy = dpsVisit.updatedBy,
      updatedTime = dpsVisit.updatedTime,
    ),
  )
}
