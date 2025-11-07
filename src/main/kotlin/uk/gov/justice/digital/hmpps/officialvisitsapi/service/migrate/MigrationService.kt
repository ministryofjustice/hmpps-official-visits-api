package uk.gov.justice.digital.hmpps.officialvisitsapi.service.migrate

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonTimeSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
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
import java.time.LocalDateTime

@Service
class MigrationService(
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val officialVisitRepository: OfficialVisitRepository,
  private val officialVisitorRepository: OfficialVisitorRepository,
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
      dpsTimeSlotId = 1L,
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
          maxAdults = slot.maxAdults!!,
          maxGroups = slot.maxGroups!!,
          maxVideoSessions = slot.maxVideoSessions!!,
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
      "Migrate official visit ID {} at {} for {} on {} with {} visitors",
      request.offenderVisitId,
      request.prisonCode!!,
      request.prisonerNumber!!,
      request.visitDate!!,
      request.visitors.size,
    )

    // The visit slot must exist prior to the visit being migrated - error if does not
    val visitSlot = prisonVisitSlotRepository.findById(request.prisonVisitSlotId).orElseThrow {
      EntityNotFoundException("Prison visit slot ID ${request.prisonVisitSlotId} not found on offender visit ID ${request.offenderVisitId}")
    }

    val visit = officialVisitRepository.saveAndFlush(
      OfficialVisitEntity(
        prisonVisitSlot = visitSlot,
        prisonCode = request.prisonCode,
        prisonerNumber = request.prisonerNumber,
        visitDate = request.visitDate,
        visitStatusCode = request.visitStatusCode.code,
        visitTypeCode = request.visitTypeCode.code,
        createdBy = request.createUsername ?: "MIGRATION",
        // TODO: Needs a full set of values defined on the entity
      ).apply {
        // this.createdTime = request.createDateTime ?: LocalDateTime.now()
        // TODO: Needs createdTime as a constructor arg or private set var to pass through NOMIS value
        this.updatedTime = request.modifyDateTime
        this.updatedBy = request.modifyUsername
      },
    )

    val visitorsPairs = extractAndSaveVisitors(visit, request)

    // Build the response object to map NOMIS IDs to DPS IDs for each entity created
    return MigrateVisitResponse(
      visit = IdPair(nomisId = request.offenderVisitId, dpsId = visit.officialVisitId, elementType = ElementType.OFFICIAL_VISIT),
      visitors = visitorsPairs.map { IdPair(ElementType.OFFICIAL_VISITOR, it.first, it.second.officialVisitorId) },
    )
  }

  fun extractAndSaveVisitors(dpsVisit: OfficialVisitEntity, request: MigrateVisitRequest) = request.visitors.map { visitor ->
    Pair(
      visitor.offenderVisitVisitorId,
      officialVisitorRepository.saveAndFlush(
        OfficialVisitorEntity(
          officialVisit = dpsVisit,
          visitorTypeCode = "O",
          contactTypeCode = "O",
          contactId = visitor.personId,
          leadVisitor = visitor.groupLeaderFlag ?: false,
          assistedVisit = visitor.assistedVisitFlag ?: false,
          createdBy = visitor.createUsername ?: dpsVisit.createdBy,
          // TODO: More fields required for each visitor
          // TODO: The prisoner contact ID is not available
          // TODO: The relationships between prisoner and visitor is not available
        ),
      ),
    )
  }
}
