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
@Transactional(readOnly = true)
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

  fun getOfficialVisitById(officialVisitId: Long): SyncOfficialVisit {
    val ove = officialVisitRepository.findById(officialVisitId).orElseThrow {
      EntityNotFoundException("Official visit with id $officialVisitId not found")
    }

    val pve = prisonerVisitedRepository.findByOfficialVisit(ove)
      ?: throw EntityNotFoundException("Prisoner visited not found for visit ID $officialVisitId")

    return ove.toSyncModel(pve)
  }

  @Transactional
  fun createOfficialVisit(request: SyncCreateOfficialVisitRequest): SyncOfficialVisit {
    val visitSlot = prisonVisitSlotRepository.findById(request.prisonVisitSlotId!!).orElseThrow {
      EntityNotFoundException("Prison visit slot ID ${request.prisonVisitSlotId} does not exist")
    }

    // TODO: Check whether NOMIS includes the visitCompletionType at the point of creation, and prisoner attendance
    // NOMIS may set these values by default when creating a visit? Question for Andy.

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

  @Transactional
  fun deleteOfficialVisit(officialVisitId: Long) = officialVisitRepository.findByIdOrNull(officialVisitId)?.also { officialVisit ->
    officialVisitorRepository.deleteByOfficialVisit(officialVisit)
    prisonerVisitedRepository.deleteByOfficialVisit(officialVisit)
    officialVisitRepository.deleteById(officialVisit.officialVisitId)
  }?.toSyncModel()

  @Transactional
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

  @Transactional
  fun removeOfficialVisitor(officialVisitId: Long, officialVisitorId: Long): SyncRemoveVisitorResponse? {
    val visit = officialVisitRepository.findById(officialVisitId).getOrNull() ?: return null
    visit.officialVisitors().forEach { visitor ->
      if (visitor.officialVisitorId == officialVisitorId) {
        if (visitor.visitorEquipment != null) {
          visitorEquipmentRepository.deleteAllByOfficialVisitor(visitor)
        }
        officialVisitorRepository.deleteById(visitor.officialVisitorId)
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
}

data class SyncAddVisitorResponse(
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
