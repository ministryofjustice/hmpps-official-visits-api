package uk.gov.justice.digital.hmpps.officialvisitsapi.service.migrate

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.migrate.MigrateVisitConfigRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.ElementType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.IdPair
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate.MigrateVisitConfigResponse

@Service
class MigrationService {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun migrateVisitConfiguration(request: MigrateVisitConfigRequest): MigrateVisitConfigResponse {
    logger.info(
      "Migrate timeslot in {} day {} timeSlotSeq {} with {} visit slots",
      request.prisonCode,
      request.dayCode,
      request.timeSlotSeq,
      request.visitSlots.size,
    )

    // TODO: Logic to map request to entities, save them and return their IDs

    return MigrateVisitConfigResponse(
      prisonCode = request.prisonCode,
      dayCode = request.dayCode,
      timeSlotSeq = request.timeSlotSeq,
      dpsTimeSlotId = 1L,
      visitSlots = listOf(
        IdPair(ElementType.PRISON_VISIT_SLOT, 1L, 2L),
        IdPair(ElementType.PRISON_VISIT_SLOT, 2L, 3L),
      ),
    )
  }
}
