package uk.gov.justice.digital.hmpps.officialvisitsapi.service.slotavailability

import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AvailableSlot

object AvailableSlotSpecificationFactory {
  fun getAvailableSlotSpecification(request: CreateOfficialVisitRequest): AvailableSlotSpecification = when (request.visitTypeCode!!) {
    VisitType.IN_PERSON -> InPersonSpecification(request)
    VisitType.VIDEO -> VideoSpecification(request)
    VisitType.TELEPHONE -> TelephoneSpecification(request)
    else -> { _, _ -> false }
  }
}

fun interface AvailableSlotSpecification {
  fun isSatisfiedBy(slot: AvailableSlot, prisonVisitSlot: PrisonVisitSlotEntity): Boolean
}

class InPersonSpecification(private val request: CreateOfficialVisitRequest) : AvailableSlotSpecification {
  override fun isSatisfiedBy(slot: AvailableSlot, prisonVisitSlot: PrisonVisitSlotEntity): Boolean = run {
    slot.visitSlotId == prisonVisitSlot.prisonVisitSlotId &&
      slot.startTime == request.startTime &&
      slot.dpsLocationId == request.dpsLocationId &&
      slot.availableAdults >= request.officialVisitors.count() &&
      slot.availableGroups > 0
  }
}

class VideoSpecification(private val request: CreateOfficialVisitRequest) : AvailableSlotSpecification {
  override fun isSatisfiedBy(slot: AvailableSlot, prisonVisitSlot: PrisonVisitSlotEntity): Boolean = run {
    slot.visitSlotId == prisonVisitSlot.prisonVisitSlotId &&
      slot.startTime == request.startTime &&
      slot.dpsLocationId == request.dpsLocationId &&
      slot.availableVideoSessions > 0
  }
}

class TelephoneSpecification(private val request: CreateOfficialVisitRequest) : AvailableSlotSpecification {
  override fun isSatisfiedBy(slot: AvailableSlot, prisonVisitSlot: PrisonVisitSlotEntity): Boolean = run {
    slot.visitSlotId == prisonVisitSlot.prisonVisitSlotId &&
      slot.startTime == request.startTime &&
      slot.dpsLocationId == request.dpsLocationId &&
      slot.availableGroups > 0 &&
      slot.availableAdults > 0
  }
}
