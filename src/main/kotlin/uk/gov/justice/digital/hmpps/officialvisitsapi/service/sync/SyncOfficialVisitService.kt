package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toSyncModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitorRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateOfficialVisitorRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonVisitSlotRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitorEquipmentRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ContactsService
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Service
@Transactional
class SyncOfficialVisitService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val officialVisitorRepository: OfficialVisitorRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
  private val prisonVisitSlotRepository: PrisonVisitSlotRepository,
  private val contactsService: ContactsService,
  private val visitorEquipmentRepository: VisitorEquipmentRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  // ----------------- official visits -------------------------

  @Transactional(readOnly = true)
  fun getOfficialVisitById(officialVisitId: Long): SyncOfficialVisit {
    val ove = officialVisitRepository.findById(officialVisitId).orElseThrow {
      EntityNotFoundException("Official visit with id $officialVisitId not found")
    }

    val pve = prisonerVisitedRepository.findByOfficialVisit(ove)
      ?: throw EntityNotFoundException("Prisoner visited not found for visit ID $officialVisitId")

    return ove.toSyncModel(pve)
  }

  fun createOfficialVisit(request: SyncCreateOfficialVisitRequest): SyncOfficialVisit {
    val visitSlot = prisonVisitSlotRepository.findById(request.prisonVisitSlotId!!).orElseThrow {
      EntityNotFoundException("Prison visit slot ID ${request.prisonVisitSlotId} does not exist")
    }

    // TODO: Check whether NOMIS includes the visitCompletionType or searchType at the point of creation, and prisoner attendance

    val visit = officialVisitRepository.saveAndFlush(OfficialVisitEntity.synchronised(visitSlot, request))

    val prisonVisited = prisonerVisitedRepository.saveAndFlush(
      PrisonerVisitedEntity(
        officialVisit = visit,
        prisonerNumber = visit.prisonerNumber,
        createdBy = visit.createdBy,
        createdTime = visit.createdTime,
      ),
    )

    return visit.toSyncModel(prisonVisited)
  }

  fun deleteOfficialVisit(officialVisitId: Long) = officialVisitRepository.findByIdOrNull(officialVisitId)?.also { officialVisit ->
    officialVisitorRepository.deleteByOfficialVisit(officialVisit)
    prisonerVisitedRepository.deleteByOfficialVisit(officialVisit)
    officialVisitRepository.deleteById(officialVisit.officialVisitId)
  }?.toSyncModel()

  // ----------------- official visitors -------------------------

  fun createOfficialVisitor(officialVisitId: Long, request: SyncCreateOfficialVisitorRequest): SyncAddVisitorResponse {
    val visit = officialVisitRepository.findById(officialVisitId).orElseThrow {
      EntityNotFoundException("The official visit with id $officialVisitId was not found")
    }

    // Check if the visitor is already present on this visit to prevent a duplicate person being added
    visit.officialVisitors().forEach { visitor ->
      if (visitor.offenderVisitVisitorId == request.offenderVisitVisitorId || visitor.contactId == request.personId) {
        throw EntityInUseException("The person ID ${request.personId} or offenderVisitVisitorId ${request.offenderVisitVisitorId}) is already on the visit ${visit.offenderVisitId} and cannot be added again")
      }
    }

    // Get the prisoner contact relationship to check if this person is approved to visit
    val contactSummary = contactsService.getPrisonerContactSummary(visit.prisonerNumber, request.personId!!)
    val thisContact = contactSummary.find { it.contactId == request.personId && it.currentTerm }
    if (thisContact != null) {
      if (!thisContact.isApprovedVisitor) {
        log.info("UNAPPROVED VISITOR - Sync added an unapproved visitor to visitId ${visit.officialVisitId}, contactId: ${request.personId}, prisoner: ${visit.prisonerNumber}")
      }
      if (!thisContact.currentTerm) {
        log.info("CURRENT TERM VISITOR - Sync added an old relationship visitor to visitId ${visit.officialVisitId}, contactId: ${request.personId}, prisoner: ${visit.prisonerNumber}")
      }
    }

    // Add the visitor directly (rather than through the visit entity which requires a user look-up via manage users service)
    val visitorSaved = officialVisitorRepository.saveAndFlush(
      OfficialVisitorEntity(
        officialVisitorId = 0L,
        officialVisit = visit,
        visitorTypeCode = VisitorType.CONTACT,
        relationshipTypeCode = request.relationshipTypeCode ?: RelationshipType.OFFICIAL,
        relationshipCode = request.relationshipToPrisoner ?: thisContact?.relationshipToPrisonerCode ?: "",
        contactId = request.personId,
        prisonerContactId = thisContact?.prisonerContactId,
        firstName = request.firstName,
        lastName = request.lastName,
        leadVisitor = request.groupLeaderFlag ?: false,
        assistedVisit = request.assistedVisitFlag ?: false,
        visitorNotes = request.commentText,
        createdBy = request.createUsername ?: "SYNC",
        createdTime = request.createDateTime ?: LocalDateTime.now(),
      ),
    )

    return SyncAddVisitorResponse(
      officialVisitId = visit.officialVisitId,
      officialVisitorId = visitorSaved.officialVisitorId,
      prisonCode = visit.prisonCode,
      prisonerNumber = visit.prisonerNumber,
      visitor = visitorSaved.toSyncModel(),
    )
  }

  fun removeOfficialVisitor(officialVisitId: Long, officialVisitorId: Long): SyncRemoveVisitorResponse? {
    val visit = officialVisitRepository.findById(officialVisitId).getOrNull() ?: return null
    visit.officialVisitors().forEach { visitor ->
      if (visitor.officialVisitorId == officialVisitorId) {
        if (visitor.visitorEquipment != null) {
          visitorEquipmentRepository.deleteAllByOfficialVisitor(visitor)
        }

        visit.removeVisitor(visitor)

        return SyncRemoveVisitorResponse(
          officialVisitId = visit.officialVisitId,
          officialVisitorId = visitor.officialVisitorId,
          prisonCode = visit.prisonCode,
          prisonerNumber = visit.prisonerNumber,
          contactId = visitor.contactId,
        )
      }
    }
    return null
  }

  fun updateOfficialVisitor(officialVisitId: Long, officialVisitorId: Long, request: SyncUpdateOfficialVisitorRequest): SyncUpdateVisitorResponse {
    val visit = officialVisitRepository.findById(officialVisitId).orElseThrow {
      EntityNotFoundException("The official visit with id $officialVisitId was not found")
    }

    val visitor = officialVisitorRepository.findById(officialVisitorId).getOrNull()
      ?: throw EntityNotFoundException("The official visitor with id $officialVisitorId was not found")

    val changedVisitorEntity = buildChangedEntity(visit, visitor, request)
    val changes = describeVisitorChanges(old = visitor, new = changedVisitorEntity)
    log.info(changes)

    val savedVisitorEntity = officialVisitorRepository.saveAndFlush(
      visitor.copy(
        visitorTypeCode = changedVisitorEntity.visitorTypeCode,
        firstName = changedVisitorEntity.firstName,
        lastName = changedVisitorEntity.lastName,
        contactId = changedVisitorEntity.contactId,
        prisonerContactId = changedVisitorEntity.prisonerContactId,
        relationshipTypeCode = changedVisitorEntity.relationshipTypeCode,
        relationshipCode = changedVisitorEntity.relationshipCode,
        leadVisitor = changedVisitorEntity.leadVisitor,
        assistedVisit = changedVisitorEntity.assistedVisit,
        visitorNotes = changedVisitorEntity.visitorNotes,
        offenderVisitVisitorId = changedVisitorEntity.offenderVisitVisitorId,
        visitorEquipment = visitor.visitorEquipment,
        updatedBy = changedVisitorEntity.updatedBy,
        updatedTime = changedVisitorEntity.updatedTime,
      ),
    )

    // TODO: Check whether this is needed - integration test will confirm it - try with and without
    // Get the visit again - to check the list of visitors has now updated - don't want to save the original list
    val visitChanged = officialVisitRepository.findById(officialVisitId).orElseThrow {
      EntityNotFoundException("The official visit with id $officialVisitId was not found")
    }

    log.info("Visitors on the visit (after update) = ${visitChanged.officialVisitors().size}")

    return SyncUpdateVisitorResponse(
      officialVisitId = visit.officialVisitId,
      officialVisitorId = visitor.officialVisitorId,
      prisonCode = visit.prisonCode,
      prisonerNumber = visit.prisonerNumber,
      visitor = savedVisitorEntity.toSyncModel(),
    )
  }

  private fun buildChangedEntity(
    visit: OfficialVisitEntity,
    visitor: OfficialVisitorEntity,
    request: SyncUpdateOfficialVisitorRequest,
  ) = OfficialVisitorEntity(
    officialVisitorId = visitor.officialVisitorId,
    officialVisit = visit,
    visitorTypeCode = visitor.visitorTypeCode,
    firstName = request.firstName,
    lastName = request.lastName,
    contactId = request.personId,
    prisonerContactId = if (visitor.contactId == request.personId) visitor.prisonerContactId else null,
    relationshipTypeCode = request.relationshipTypeCode,
    relationshipCode = request.relationshipToPrisoner,
    leadVisitor = request.groupLeaderFlag ?: false,
    assistedVisit = request.assistedVisitFlag ?: false,
    visitorNotes = request.commentText,
    createdBy = visitor.createdBy,
    createdTime = visitor.createdTime,
  ).apply {
    visitorEquipment = visitor.visitorEquipment
    attendanceCode = request.attendanceCode
    updatedTime = request.updateDateTime
    updatedBy = request.updateUsername
  }

  private fun describeVisitorChanges(old: OfficialVisitorEntity, new: OfficialVisitorEntity): String {
    val changes = mutableListOf<String>()

    if (old.firstName != new.firstName) {
      changes.add("First name changed from '${old.firstName}' to '${new.firstName}'")
    }
    if (old.lastName != new.lastName) {
      changes.add("Last name changed from '${old.lastName}' to '${new.lastName}'")
    }
    if (old.leadVisitor != new.leadVisitor) {
      val status = if (new.leadVisitor) "Is now the lead visitor" else "Is no longer the lead visitor"
      changes.add(status)
    }
    if (old.assistedVisit != new.assistedVisit) {
      val status = if (new.assistedVisit) "Is now an assisted visitor" else "Is no longer an assisted visitor"
      changes.add(status)
    }
    if (old.visitorTypeCode != new.visitorTypeCode) {
      changes.add("Visitor type changed from '${old.visitorTypeCode}' to '${new.visitorTypeCode}'")
    }
    if (old.contactId != new.contactId) {
      changes.add("Contact ID changed from '${old.contactId}' to '${new.contactId}'")
    }
    if (old.prisonerContactId != new.prisonerContactId) {
      changes.add("Prisoner contact ID changed from '${old.prisonerContactId}' to '${new.prisonerContactId}'")
    }
    if (old.relationshipTypeCode != new.relationshipTypeCode) {
      changes.add("Relationship type code changed from '${old.relationshipTypeCode}' to '${new.relationshipTypeCode}'")
    }
    if (old.relationshipCode != new.relationshipCode) {
      changes.add("Relationship code changed from '${old.relationshipCode}' to '${new.relationshipCode}'")
    }
    if (old.visitorNotes != new.visitorNotes) {
      changes.add("Visitor notes changed from '${old.visitorNotes}' to '${new.visitorNotes}'")
    }
    if (old.attendanceCode != new.attendanceCode) {
      changes.add("Attendance code changed from '${old.attendanceCode}' to '${new.attendanceCode}'")
    }
    if (old.visitorEquipment?.description != new.visitorEquipment?.description) {
      changes.add("Visitor equipment changed from '${old.visitorEquipment?.description}' to '${new.visitorEquipment?.description}'")
    }
    if (old.offenderVisitVisitorId != new.offenderVisitVisitorId) {
      changes.add("NOMIS offender visit visitor ID changed from '${old.offenderVisitVisitorId}' to '${new.offenderVisitVisitorId}'")
    }

    return if (changes.isEmpty()) {
      "No changes detected."
    } else {
      "Changes: ${changes.joinToString(separator = "; ", postfix = ".")}"
    }
  }
}

data class SyncAddVisitorResponse(
  val officialVisitId: Long,
  val officialVisitorId: Long,
  val prisonCode: String,
  val prisonerNumber: String,
  val visitor: SyncOfficialVisitor,
)

data class SyncUpdateVisitorResponse(
  val officialVisitId: Long,
  val officialVisitorId: Long,
  val prisonCode: String,
  val prisonerNumber: String,
  val visitor: SyncOfficialVisitor,
)

data class SyncRemoveVisitorResponse(
  val officialVisitId: Long,
  val officialVisitorId: Long,
  val prisonCode: String,
  val prisonerNumber: String,
  val contactId: Long? = null,
)
