package uk.gov.justice.digital.hmpps.officialvisitsapi.service.admin

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonTimeSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.admin.toVisitSlotModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.CreateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.admin.UpdateVisitSlotRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.VisitSlot
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonTimeSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.LocationsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import java.time.LocalDateTime

@Service
@Transactional
class VisitSlotService(
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val prisonTimeSlotRepository: PrisonTimeSlotRepository,
  private val officialVisitRepository: OfficialVisitRepository,
  private val locationsService: LocationsService,
) {

  fun getById(prisonVisitSlotId: Long): VisitSlot {
    val visitSlotEntity = prisonVisitSlotRepository.findById(prisonVisitSlotId)
      .orElseThrow { EntityNotFoundException("Prison visit slot with ID $prisonVisitSlotId was not found") }
    val timeSlotEntity = prisonTimeSlotRepository.findById(visitSlotEntity.prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID ${visitSlotEntity.prisonTimeSlotId} was not found for visit slot") }
    val enhanceVisitSlotDetails = enhanceVisitSlotDetails(visitSlotEntity, timeSlotEntity)
    return enhanceVisitSlotDetails.copy(hasVisit = officialVisitsExistFor(enhanceVisitSlotDetails.visitSlotId))
  }

  fun create(prisonTimeSlotId: Long, request: CreateVisitSlotRequest, user: User): VisitSlot {
    val timeSlotEntity = prisonTimeSlotRepository.findById(prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID $prisonTimeSlotId was not found for visit slot") }

    val entity = PrisonVisitSlotEntity(
      prisonVisitSlotId = 0L,
      prisonTimeSlotId = prisonTimeSlotId,
      dpsLocationId = request.dpsLocationId,
      maxAdults = request.maxAdults,
      maxGroups = request.maxGroups,
      maxVideoSessions = request.maxVideo,
      createdTime = LocalDateTime.now(),
      createdBy = user.username,
    )

    val toVisitSlotModel = prisonVisitSlotRepository.saveAndFlush(entity)
    return enhanceVisitSlotDetails(toVisitSlotModel, timeSlotEntity)
  }

  fun update(prisonVisitSlotId: Long, request: UpdateVisitSlotRequest, user: User): VisitSlot {
    val visitSlotEntity = prisonVisitSlotRepository.findById(prisonVisitSlotId)
      .orElseThrow { EntityNotFoundException("Prison visit slot with ID $prisonVisitSlotId was not found") }

    val timeSlotEntity = prisonTimeSlotRepository.findById(visitSlotEntity.prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID ${visitSlotEntity.prisonTimeSlotId} was not found for visit slot") }

    // Only capacities may be changed; dpsLocationId and prisonTimeSlotId must remain the same
    val changed = visitSlotEntity.copy(
      maxAdults = request.maxAdults,
      maxGroups = request.maxGroups,
      maxVideoSessions = request.maxVideo,
      dpsLocationId = request.dpsLocationId ?: visitSlotEntity.dpsLocationId,
      updatedBy = user.username,
      updatedTime = LocalDateTime.now(),
    )

    val toVisitSlotModel = prisonVisitSlotRepository.saveAndFlush(changed)
    val enhanceVisitSlotDetails = enhanceVisitSlotDetails(toVisitSlotModel, timeSlotEntity)
    return enhanceVisitSlotDetails.copy(hasVisit = officialVisitsExistFor(enhanceVisitSlotDetails.visitSlotId))
  }

  fun delete(prisonVisitSlotId: Long): VisitSlot {
    val visitSlotEntity = prisonVisitSlotRepository.findById(prisonVisitSlotId)
      .orElseThrow { EntityNotFoundException("Prison visit slot with ID $prisonVisitSlotId was not found") }

    if (officialVisitsExistFor(visitSlotEntity.prisonVisitSlotId)) {
      throw EntityInUseException("The prison visit slot has visits associated with it and cannot be deleted.")
    }

    val timeSlotEntity = prisonTimeSlotRepository.findById(visitSlotEntity.prisonTimeSlotId)
      .orElseThrow { EntityNotFoundException("Prison time slot with ID ${visitSlotEntity.prisonTimeSlotId} was not found for visit slot") }

    prisonVisitSlotRepository.deleteById(prisonVisitSlotId)

    return visitSlotEntity.toVisitSlotModel(timeSlotEntity.prisonCode)
  }

  private fun enhanceVisitSlotDetails(
    visitSlotEntity: PrisonVisitSlotEntity,
    timeSlotEntity: PrisonTimeSlotEntity,
  ): VisitSlot {
    val officialVisitLocation = locationsService.getLocationById(visitSlotEntity.dpsLocationId)
    val toVisitSlotModel = visitSlotEntity.toVisitSlotModel(timeSlotEntity.prisonCode)
    return toVisitSlotModel.copy(locationDescription = officialVisitLocation?.localName ?: "Unknown")
  }

  private fun officialVisitsExistFor(prisonVisitSlotId: Long): Boolean = officialVisitRepository.existsByPrisonVisitSlotPrisonVisitSlotId(prisonVisitSlotId)
}
