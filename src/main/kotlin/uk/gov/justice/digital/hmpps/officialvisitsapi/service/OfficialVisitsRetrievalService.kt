package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ContactDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.VisitorContactInformation
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.ReferenceDataGroup
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.OfficialVisitDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.PrisonerVisitedDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository

@Service
@Transactional(readOnly = true)
class OfficialVisitsRetrievalService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val referenceDataService: ReferenceDataService,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
  private val personalRelationshipsReferenceDataService: PersonalRelationshipsReferenceDataService,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
  private val personalRelationshipsApiClient: PersonalRelationshipsApiClient,
) {
  fun getOfficialVisitByPrisonCodeAndId(prisonCode: String, id: Long): OfficialVisitDetails {
    val ove = officialVisitRepository.findByOfficialVisitIdAndPrisonCode(id, prisonCode)
      ?: throw EntityNotFoundException("Official visit with id $id and prison code $prisonCode not found")

    val prisoner = prisonerSearchClient.getPrisoner(ove.prisonerNumber)
      ?: throw EntityNotFoundException("Prisoner not found ${ove.prisonerNumber}")

    val pve = prisonerVisitedRepository.findByOfficialVisit(ove)
      ?: throw EntityNotFoundException("Prisoner visited not found for visit ID $id")

    return populateOfficialVisitDetails(ove, prisoner, pve)
  }

  private fun populateOfficialVisitDetails(
    ove: OfficialVisitEntity,
    prisoner: Prisoner,
    pve: PrisonerVisitedEntity,
  ): OfficialVisitDetails = run {
    val officialVisitLocation = locationsInsidePrisonClient.getLocationById(ove.dpsLocationId)
    val visitorContactInformation = visitorContactInformation(ove)

    OfficialVisitDetails(
      officialVisitId = ove.officialVisitId,
      prisonCode = ove.prisonCode,
      prisonDescription = prisoner.prisonName ?: "Unknown",
      visitStatus = ove.visitStatusCode,
      visitStatusDescription = getReferenceDescription(ReferenceDataGroup.VIS_STATUS, ove.visitStatusCode.name)!!,
      visitTypeCode = ove.visitTypeCode,
      visitTypeDescription = getReferenceDescription(ReferenceDataGroup.VIS_TYPE, ove.visitTypeCode.name)!!,
      visitDate = ove.visitDate,
      startTime = ove.startTime,
      endTime = ove.endTime,
      dpsLocationId = ove.dpsLocationId,
      locationDescription = officialVisitLocation?.localName ?: "Unknown",
      visitSlotId = ove.prisonVisitSlot.prisonVisitSlotId,
      staffNotes = ove.staffNotes,
      prisonerNotes = ove.prisonerNotes,
      visitorConcernNotes = ove.visitorConcernNotes,
      completionCode = ove.completionCode,
      completionNotes = ove.completionNotes,
      completionDescription = getReferenceDescription(ReferenceDataGroup.VIS_COMPLETION, ove.completionCode?.name),
      searchTypeCode = ove.searchTypeCode,
      searchTypeDescription = getReferenceDescription(ReferenceDataGroup.SEARCH_LEVEL, ove.searchTypeCode?.name),
      createdTime = ove.createdTime,
      createdBy = ove.createdBy,
      updatedTime = ove.updatedTime,
      updatedBy = ove.updatedBy,
      officialVisitors = ove.officialVisitors().toModel(referenceDataService, personalRelationshipsReferenceDataService, visitorContactInformation),
      prisonerVisited = PrisonerVisitedDetails(
        prisonerNumber = prisoner.prisonerNumber,
        prisonCode = prisoner.prisonId!!,
        firstName = prisoner.firstName,
        lastName = prisoner.lastName,
        dateOfBirth = prisoner.dateOfBirth,
        middleNames = prisoner.middleNames,
        offenderBookId = prisoner.offenderBookId?.toLong(),
        cellLocation = prisoner.cellLocation,
        attendanceCode = pve.attendanceCode?.name,
        attendanceCodeDescription = getReferenceDescription(ReferenceDataGroup.ATTENDANCE, pve.attendanceCode?.name),
      ),
    )
  }

  private fun getReferenceDescription(group: ReferenceDataGroup, code: String?) = code?.let {
    referenceDataService.getReferenceDataByGroupAndCode(group, code)?.description ?: code
  }

  private fun visitorContactInformation(ove: OfficialVisitEntity): VisitorContactInformation = run {
    val (visitorPhoneNumbers, visitorEmailAddresses) = run {
      ove.officialVisitors().getVisitorsDetails().let { it.getVisitorsMainPhoneNumber() to it.getVisitorsMainEmailAddress() }
    }

    object : VisitorContactInformation {
      override fun phoneNumber(visitorContactId: Long): String? = visitorPhoneNumbers[visitorContactId]
      override fun emailAddress(visitorContactId: Long): String? = visitorEmailAddresses[visitorContactId]
    }
  }

  private fun List<OfficialVisitorEntity>.getVisitorsDetails(): List<ContactDetails> = run {
    filter { visitor -> visitor.contactId != null }
      .map { visitor -> personalRelationshipsApiClient.getContactById(visitor.contactId!!) ?: throw EntityNotFoundException("Contact with ID ${visitor.contactId} not found") }
  }

  // Strictly speaking, it would be better if the personal relationship API provided easier access to this information instead of putting (fragile) logic in the service here to try and work it out.
  private fun List<ContactDetails>.getVisitorsMainPhoneNumber() = run {
    associateBy(
      { contact -> contact.id },
      { contact ->
        if (contact.phoneNumbers.isEmpty()) {
          contact.addresses.singleOrNull { it.primaryAddress || it.mailFlag }?.phoneNumbers?.maxByOrNull { it.createdTime }?.phoneNumber
        } else {
          contact.phoneNumbers.maxByOrNull { it.createdTime }?.phoneNumber
        }
      },
    )
  }

  private fun List<ContactDetails>.getVisitorsMainEmailAddress() = run {
    associateBy(
      { contact -> contact.id },
      { contact ->
        contact.emailAddresses.takeIf { emailAddresses -> emailAddresses.isNotEmpty() }?.maxByOrNull { it.createdTime }?.emailAddress
      },
    )
  }
}
