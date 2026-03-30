package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OverlappingVisitsCriteriaRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OverlappingContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OverlappingVisitsResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class OverlappingVisitsService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val officialVisitorRepository: OfficialVisitorRepository,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun findOverlappingScheduledVisits(prisonCode: String, request: OverlappingVisitsCriteriaRequest): OverlappingVisitsResponse = run {
    require(request.visitDate!!.atTime(request.startTime!!).isAfter(LocalDateTime.now())) {
      "Cannot overlap visits in the past. Visit date: ${request.visitDate} and start time: ${request.startTime} must be in the future."
    }

    val overlappingPrisonerVisits = officialVisitRepository.findScheduledOverlappingVisitsBy(
      prisonCode = prisonCode,
      prisonerNumber = request.prisonerNumber!!,
      visitDate = request.visitDate,
      startTime = request.startTime,
      endTime = request.endTime!!,
    ).ignoring(request.existingOfficialVisitId)

    val overlappingContactVisits = buildMap {
      request.contactIds
        .orEmpty()
        .distinct()
        .forEach { contactId ->
          officialVisitorRepository.findScheduledOverlappingVisitsBy(
            contactId = contactId,
            visitDate = request.visitDate,
            startTime = request.startTime,
            endTime = request.endTime,
          )
            .map(OfficialVisitorEntity::officialVisit)
            .ignoring(request.existingOfficialVisitId)
            .let { overlappingVisits -> add(contactId, overlappingVisits) }
        }
    }

    OverlappingVisitsResponse(
      prisonerNumber = request.prisonerNumber,
      overlappingPrisonerVisits = overlappingPrisonerVisits.map(OfficialVisitEntity::officialVisitId),
      contacts = request.contactIds.orEmpty().map { OverlappingContact(it, overlappingContactVisits[it].orEmpty()) },
    ).also { logger.info("Overlapping visits $it") }
  }

  private fun Collection<OfficialVisitEntity>.ignoring(visitId: Long?) = filterNot { it.officialVisitId == visitId }

  private fun MutableMap<Long, List<Long>>.add(contactId: Long, overlapping: Collection<OfficialVisitEntity>) {
    put(contactId, overlapping.map(OfficialVisitEntity::officialVisitId))
  }
}
