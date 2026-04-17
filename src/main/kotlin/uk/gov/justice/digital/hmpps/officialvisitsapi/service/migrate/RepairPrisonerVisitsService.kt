package uk.gov.justice.digital.hmpps.officialvisitsapi.service.migrate

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.RepairPrisonerVisitsRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.RepairPrisonerVisitsResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.AuditedEventRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitorEquipmentRepository

@Service
class RepairPrisonerVisitsService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
  private val officialVisitorRepository: OfficialVisitorRepository,
  private val visitorEquipmentRepository: VisitorEquipmentRepository,
  private val auditedEventRepository: AuditedEventRepository,
  private val migrationService: MigrationService,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * This service method implements a complete replacement of all visits for one prisoner with their
   * visits from NOMIS. It performs the deletions and creations in one transaction.
   *
   * It replaces the visit details in DPS with the latest version of visits from NOMIS and removes all
   * prior data and state, including the audit trail, from DPS.
   *
   * It calls the same service method as migration for each visit, but with an overriding transaction state which
   * applies to all visits in the request.This function implements an emergency repair to be used if the state
   * of visits for one prisoner gets out of sync with NOMIS.
   *
   * It does not emit sync events for the changes.
   */
  @Transactional
  fun repairPrisonerVisits(prisonerNumber: String, request: RepairPrisonerVisitsRequest): RepairPrisonerVisitsResponse {
    logger.info("REPAIR: Received a repair request with ${request.visits.size} prisoner visits")

    val countOfVisitsRemoved = clearAllVisitsForPrisoner(prisonerNumber)

    logger.info("REPAIR: Deleted $countOfVisitsRemoved visits for prisoner $prisonerNumber")

    val migrationResponseList = request.visits.map { visit ->
      migrationService.migrateVisit(visit)
    }

    logger.info("REPAIR: Recreated ${migrationResponseList.size} visits for prisoner $prisonerNumber")

    return RepairPrisonerVisitsResponse(
      prisonerNumber = prisonerNumber,
      visits = migrationResponseList,
    )
  }

  /**
   * This function executes bulk delete operations via SQL directly.
   * It will not CASCADE the deletes downwards from official visit -> official visitor -> visitor equipment
   * as it would with a JPA remove(entity), so operations must be done in the correct order, children first, parents last.
   */

  private fun clearAllVisitsForPrisoner(prisonerNumber: String): Long {
    // Count the number of prisonerVisited rows - this is the same as the number of visits for the prisoner
    val countPrisonerVisited = prisonerVisitedRepository.countByPrisonerNumber(prisonerNumber)

    // Delete the prisonerVisited entities (Child entity of a visit)
    prisonerVisitedRepository.deleteAllByPrisonerNumber(prisonerNumber)
    prisonerVisitedRepository.flush()

    // Delete the visitor equipment (child entity of a visitor)
    visitorEquipmentRepository.deleteAllByPrisonerNumber(prisonerNumber)
    visitorEquipmentRepository.flush()

    // Delete the visitors on these visits (child entity of a visit)
    officialVisitorRepository.deleteAllByPrisonerNumber(prisonerNumber)
    officialVisitorRepository.flush()

    // Delete the visits for this prisoner (it's a bulk operation and JPA cascades are not honored here)
    officialVisitRepository.deleteAllByPrisonerNumber(prisonerNumber)
    officialVisitRepository.flush()

    // Delete the audited events for the visits
    auditedEventRepository.deleteAllByPrisonerNumber(prisonerNumber)
    auditedEventRepository.flush()

    return countPrisonerVisited
  }
}
