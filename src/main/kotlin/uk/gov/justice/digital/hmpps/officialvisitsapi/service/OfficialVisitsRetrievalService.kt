package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.ReferenceDataGroup
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.PrisonerVisitedDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository

@Service
class OfficialVisitsRetrievalService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val referenceDataService: ReferenceDataService,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
) {
  @Transactional(readOnly = true)
  fun getOfficialVisitByPrisonCodeAndId(prisonCode: String, id: Long): OfficialVisitDetails {
    val officialVisitEntity = officialVisitRepository.findByOfficialVisitIdAndPrisonCode(id, prisonCode)
      .orElseThrow { EntityNotFoundException("Official visit with id $id and prison code $prisonCode not found") }

    val prisoner = prisonerSearchClient.getPrisoner(officialVisitEntity.prisonerNumber)
    val prisonerVisitedEntity = prisonerVisitedRepository.findByOfficialVisitId(id)
    return populateOfficialVisitDetails(officialVisitEntity, prisoner, prisonerVisitedEntity)
  }

  private fun populateOfficialVisitDetails(
    officialVisitEntity: OfficialVisitEntity?,
    prisoner: uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.Prisoner?,
    prisonerVisitedEntity: PrisonerVisitedEntity?,
  ): OfficialVisitDetails = OfficialVisitDetails(
    officialVisitId = officialVisitEntity!!.officialVisitId,
    prisonCode = officialVisitEntity.prisonCode,
    prisonDescription = prisoner!!.prisonName,
    visitStatus = officialVisitEntity.visitStatusCode,
    visitStatusDescription = referenceDataService.getReferenceDataByGroupAndCode(
      ReferenceDataGroup.VIS_STATUS,
      officialVisitEntity.visitStatusCode.toString(),
    )?.description
      ?: officialVisitEntity.visitStatusCode.toString(),
    visitTypeCode = officialVisitEntity.visitTypeCode,
    visitTypeDescription = referenceDataService.getReferenceDataByGroupAndCode(
      ReferenceDataGroup.VIS_TYPE,
      officialVisitEntity.visitTypeCode.toString(),
    )?.description ?: officialVisitEntity.visitTypeCode.toString(),
    visitDate = officialVisitEntity.visitDate,
    startTime = officialVisitEntity.startTime,
    endTime = officialVisitEntity.endTime,
    dpsLocationId = officialVisitEntity.dpsLocationId,
    locationDescription = prisoner.locationDescription,
    visitSlotId = officialVisitEntity.prisonVisitSlot.prisonVisitSlotId,
    staffNotes = officialVisitEntity.staffNotes,
    prisonerNotes = officialVisitEntity.prisonerNotes,
    visitorConcernNotes = officialVisitEntity.visitorConcernNotes,
    completionCode = officialVisitEntity.completionCode,
    completionDescription = referenceDataService.getReferenceDataByGroupAndCode(
      ReferenceDataGroup.VIS_COMPLETION,
      officialVisitEntity.completionCode.toString(),
    )?.description ?: officialVisitEntity.completionCode.toString(),
    searchTypeCode = officialVisitEntity.searchTypeCode,
    searchTypeDescription = referenceDataService.getReferenceDataByGroupAndCode(
      ReferenceDataGroup.SEARCH_LEVEL,
      officialVisitEntity.searchTypeCode.toString(),
    )?.description ?: officialVisitEntity.searchTypeCode.toString(),
    createdTime = officialVisitEntity.createdTime,
    createdBy = officialVisitEntity.createdBy,
    updatedTime = officialVisitEntity.updatedTime,
    updatedBy = officialVisitEntity.updatedBy,
    officialVisitors = officialVisitEntity.officialVisitors().toModel(referenceDataService),
    prisoner = PrisonerVisitedDetails(
      prisonerNumber = prisoner.prisonerNumber,
      prisonCode = prisoner.prisonId!!,
      firstName = prisoner.firstName,
      lastName = prisoner.lastName,
      dateOfBirth = prisoner.dateOfBirth,
      middleNames = prisoner.middleNames,
      offenderBookId = prisoner.offenderBookId,
      cellLocation = prisoner.cellLocation,
      attendanceCode = prisonerVisitedEntity?.attendanceCode.toString(),
      attendanceCodeDescription = referenceDataService.getReferenceDataByGroupAndCode(
        ReferenceDataGroup.ATTENDANCE,
        prisonerVisitedEntity?.attendanceCode.toString(),
      )?.description ?: prisonerVisitedEntity?.attendanceCode.toString(),
    ),
  )
}
