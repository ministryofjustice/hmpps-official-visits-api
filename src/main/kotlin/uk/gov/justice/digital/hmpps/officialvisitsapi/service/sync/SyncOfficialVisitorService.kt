package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.EntityInUseException
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync.toSyncModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncCreateOfficialVisitorRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.sync.SyncUpdateOfficialVisitorRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitorRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.VisitorEquipmentRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ContactsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.outbound.Source
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsEvents
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.MetricsService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.metrics.VisitorMetricInfo
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.AuditingService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing.auditVisitCreateEvent
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Service
@Transactional
class SyncOfficialVisitorService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val officialVisitorRepository: OfficialVisitorRepository,
  private val contactsService: ContactsService,
  private val visitorEquipmentRepository: VisitorEquipmentRepository,
  private val auditingService: AuditingService,
  private val metricsService: MetricsService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createVisitor(officialVisitId: Long, request: SyncCreateOfficialVisitorRequest): SyncAddVisitorResponse {
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
    ).also {
      metricsService.send(
        MetricsEvents.ADD_VISITOR,
        info = VisitorMetricInfo(
          source = Source.NOMIS,
          username = it.createdBy,
          prisonCode = visit.prisonCode,
          officialVisitId = visit.officialVisitId,
          contactId = it.contactId!!,
          officialVisitorId = it.officialVisitorId,
        ),
      )
    }

    return SyncAddVisitorResponse(
      officialVisitId = visit.officialVisitId,
      officialVisitorId = visitorSaved.officialVisitorId,
      prisonCode = visit.prisonCode,
      prisonerNumber = visit.prisonerNumber,
      visitor = visitorSaved.toSyncModel(),
    ).also {
      auditingService.recordAuditEvent(
        auditVisitCreateEvent {
          officialVisitId(visit.officialVisitId)
          summaryText("${visitorSaved.relationshipType()} visitor added")
          eventSource("NOMIS")
          user(UserService.getServiceAsUser())
          prisonCode(visit.prisonCode)
          prisonerNumber(visit.prisonerNumber)
          detailsText("${visitorSaved.relationshipType()} visitor ${visitorSaved.name()} added to visit for prisoner number ${it.prisonerNumber}")
        },
      )
    }
  }

  private fun OfficialVisitorEntity.name() = "${firstName?.replaceFirstChar { it.uppercase() }} ${lastName?.replaceFirstChar { it.uppercase() }}"

  private fun OfficialVisitorEntity.relationshipType() = relationshipTypeCode?.name?.lowercase()?.replaceFirstChar { it.uppercase() }

  fun deleteVisitor(officialVisitId: Long, officialVisitorId: Long): SyncRemoveVisitorResponse? {
    val visit = officialVisitRepository.findById(officialVisitId).getOrNull() ?: return null
    visit.officialVisitors().forEach { visitor ->
      if (visitor.officialVisitorId == officialVisitorId) {
        if (visitor.visitorEquipment != null) {
          visitorEquipmentRepository.deleteAllByOfficialVisitor(visitor)
        }

        visit.removeVisitor(visitor).also {
          metricsService.send(
            MetricsEvents.REMOVE_VISITOR,
            info = VisitorMetricInfo(
              source = Source.NOMIS,
              username = UserService.getClientAsUser("NOMIS").username,
              prisonCode = visit.prisonCode,
              officialVisitId = visit.officialVisitId,
              contactId = visitor.contactId!!,
              officialVisitorId = visitor.officialVisitorId,
            ),
          )
        }

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

  fun updateVisitor(officialVisitId: Long, officialVisitorId: Long, request: SyncUpdateOfficialVisitorRequest): SyncUpdateVisitorResponse {
    val visit = officialVisitRepository.findById(officialVisitId).orElseThrow {
      EntityNotFoundException("The official visit with id $officialVisitId was not found")
    }

    val visitor = officialVisitorRepository.findById(officialVisitorId).getOrNull()
      ?: throw EntityNotFoundException("The official visitor with id $officialVisitorId was not found")

    // Check if the request has changed the person visiting and if so, get their relationship to the prisoner
    val updatedPrisonerContactId = if (visitor.contactId != request.personId) {
      val contactSummary = contactsService.getPrisonerContactSummary(visit.prisonerNumber, request.personId!!)
      val thisContact = contactSummary.find { it.contactId == request.personId && it.currentTerm }
      if (thisContact != null) {
        if (!thisContact.isApprovedVisitor) {
          log.info("UNAPPROVED VISITOR - Sync updated an unapproved visitor on visitId ${visit.officialVisitId}, contactId: ${request.personId}, prisoner: ${visit.prisonerNumber}")
        }
        thisContact.prisonerContactId
      } else {
        null
      }
    } else {
      visitor.prisonerContactId
    }

    val changes = describeVisitorChanges(old = visitor, new = request)
    log.info(changes)

    val savedVisitorEntity = officialVisitorRepository.saveAndFlush(
      visitor.apply {
        firstName = request.firstName
        lastName = request.lastName
        prisonerContactId = updatedPrisonerContactId
        contactId = request.personId
        relationshipTypeCode = request.relationshipTypeCode ?: RelationshipType.OFFICIAL
        relationshipCode = request.relationshipToPrisoner
        leadVisitor = request.groupLeaderFlag ?: false
        assistedVisit = request.assistedVisitFlag ?: false
        visitorNotes = request.commentText
        offenderVisitVisitorId = request.offenderVisitVisitorId
        attendanceCode = request.attendanceCode
        updatedBy = request.updateUsername
        updatedTime = request.updateDateTime
      },
    )

    return SyncUpdateVisitorResponse(
      officialVisitId = visit.officialVisitId,
      officialVisitorId = visitor.officialVisitorId,
      prisonCode = visit.prisonCode,
      prisonerNumber = visit.prisonerNumber,
      visitor = savedVisitorEntity.toSyncModel(),
    )
  }

  private fun describeVisitorChanges(old: OfficialVisitorEntity, new: SyncUpdateOfficialVisitorRequest): String {
    val changes = mutableListOf<String>()

    if (old.firstName != new.firstName) {
      changes.add("First name changed from '${old.firstName}' to '${new.firstName}'")
    }
    if (old.lastName != new.lastName) {
      changes.add("Last name changed from '${old.lastName}' to '${new.lastName}'")
    }
    if (old.leadVisitor != new.groupLeaderFlag) {
      val status = if (new.groupLeaderFlag ?: false) "Is now the lead visitor" else "Is no longer the lead visitor"
      changes.add(status)
    }
    if (old.assistedVisit != new.assistedVisitFlag) {
      val status = if (new.assistedVisitFlag ?: false) "Is now an assisted visitor" else "Is no longer an assisted visitor"
      changes.add(status)
    }
    if (old.contactId != new.personId) {
      changes.add("Contact ID changed from '${old.contactId}' to '${new.personId}'")
    }
    if (old.relationshipTypeCode != new.relationshipTypeCode) {
      changes.add("Relationship type code changed from '${old.relationshipTypeCode}' to '${new.relationshipTypeCode}'")
    }
    if (old.relationshipCode != new.relationshipToPrisoner) {
      changes.add("Relationship code changed from '${old.relationshipCode}' to '${new.relationshipToPrisoner}'")
    }
    if (old.visitorNotes != new.commentText) {
      changes.add("Visitor notes changed from '${old.visitorNotes}' to '${new.commentText}'")
    }
    if (old.attendanceCode != new.attendanceCode) {
      changes.add("Attendance code changed from '${old.attendanceCode}' to '${new.attendanceCode}'")
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
