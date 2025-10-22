package uk.gov.justice.digital.hmpps.officialvisitsapi.service.migrate

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonTimeSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.MigrateVisitConfigRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.ElementType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.IdPair
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.MigrateVisitConfigResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import java.time.LocalDateTime

@Service
class MigrationService(
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
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

  private fun extractAndSaveVisitSlots(timeSlotId: Long, request: MigrateVisitConfigRequest) = request.visitSlots.map { slot ->
    Pair(
      timeSlotId,
      prisonVisitSlotRepository.saveAndFlush(
        PrisonVisitSlotEntity(
          prisonTimeSlotId = timeSlotId,
          dpsLocationId = slot.dpsLocationId!!,
          maxAdults = slot.maxAdults!!,
          maxGroups = slot.maxGroups!!,
          maxVideoSessions = slot.maxVideoSessions!!,
          createdTime = slot.createDateTime ?: LocalDateTime.now(),
          createdBy = slot.createUsername ?: "MIGRATION",
        ),
      ),
    )
  }
}
