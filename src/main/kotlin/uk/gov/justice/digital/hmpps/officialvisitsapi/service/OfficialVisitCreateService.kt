package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.CreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.CreateOfficialVisitResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository

@Service
class OfficialVisitCreateService(
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val officialVisitRepository: OfficialVisitRepository,
) {
  fun create(request: CreateOfficialVisitRequest, user: User): CreateOfficialVisitResponse = run {
    // TODO this is very stripped down. There is no checking of the data provided e.g. prisoner and associated prison code
    officialVisitRepository.saveAndFlush(
      OfficialVisitEntity(
        prisonVisitSlot = prisonVisitSlotRepository.findById(request.prisonVisitSlotId).orElseThrow(),
        prisonCode = request.prisonCode!!,
        prisonerNumber = request.prisonerNumber!!,
        visitDate = request.visitDate!!,
        visitStatusCode = "ACTIVE",
        visitTypeCode = request.visitTypeCode!!,
        createdBy = user.username,
      ),
    ).let {
      CreateOfficialVisitResponse(it.officialVisitId)
    }
  }
}
