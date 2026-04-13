package uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing

import jakarta.annotation.PostConstruct
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.PostUpdate
import jakarta.persistence.PreRemove
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.LocalRequestContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitAuditSnapshot
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ServiceUser
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService

class OfficialVisitAuditEntityListener {
  @PostLoad
  fun onLoad(visit: OfficialVisitEntity) {
    visit.auditSnapshot = OfficialVisitAuditSnapshot.from(visit)
    visit.auditChildChanges = true
  }

  @PostPersist
  fun onCreate(visit: OfficialVisitEntity) {
    OfficialVisitAuditDelegateHolder.delegate.recordCreated(visit)
    visit.auditSnapshot = OfficialVisitAuditSnapshot.from(visit)
  }

  @PostUpdate
  fun onUpdate(visit: OfficialVisitEntity) {
    OfficialVisitAuditDelegateHolder.delegate.recordUpdated(visit, visit.auditSnapshot)
    visit.auditSnapshot = OfficialVisitAuditSnapshot.from(visit)
  }

  @PreRemove
  fun onDelete(visit: OfficialVisitEntity) {
    OfficialVisitAuditDelegateHolder.delegate.recordDeleted(visit, visit.auditSnapshot)
  }
}

class OfficialVisitorAuditEntityListener {
  @PostPersist
  fun onCreate(visitor: OfficialVisitorEntity) {
    if (!visitor.officialVisit.auditChildChanges) return

    OfficialVisitorAuditDelegateHolder.delegate.recordCreated(visitor)
  }
}

private object OfficialVisitAuditDelegateHolder {
  lateinit var delegate: OfficialVisitAuditDelegate
}

private object OfficialVisitorAuditDelegateHolder {
  lateinit var delegate: OfficialVisitorAuditDelegate
}

@Component
class OfficialVisitAuditDelegateRegistrar(private val delegate: OfficialVisitAuditDelegate) {
  @PostConstruct
  fun register() {
    OfficialVisitAuditDelegateHolder.delegate = delegate
  }
}

@Component
class OfficialVisitorAuditDelegateRegistrar(private val delegate: OfficialVisitorAuditDelegate) {
  @PostConstruct
  fun register() {
    OfficialVisitorAuditDelegateHolder.delegate = delegate
  }
}

abstract class OfficialVisitAuditSupport(private val userService: UserService) {
  protected fun eventSource(): String = currentRequestAttributes()
    ?.request
    ?.requestURI
    ?.let { if (it.startsWith("/sync/")) "NOMIS" else "DPS" }
    ?: "DPS"

  protected fun actorFor(username: String?): User = requestUser()
    ?: username?.let { userService.getUser(it) ?: ServiceUser(it, it) }
    ?: ServiceUser("SYSTEM", "SYSTEM")

  private fun requestUser(): User? = (
    currentRequestAttributes()
      ?.request
      ?.getAttribute(LocalRequestContext::class.simpleName) as? LocalRequestContext
    )?.user

  private fun currentRequestAttributes(): ServletRequestAttributes? = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
}

@Component
class OfficialVisitAuditDelegate(
  private val auditingService: AuditingService,
  userService: UserService,
) : OfficialVisitAuditSupport(userService) {
  fun recordCreated(visit: OfficialVisitEntity) {
    val actor = actorFor(visit.createdBy)
    auditingService.recordAuditEvent(
      auditVisitCreateEvent {
        officialVisitId(visit.officialVisitId)
        summaryText("Official visit created")
        eventSource(eventSource())
        user(actor)
        prisonCode(visit.prisonCode)
        prisonerNumber(visit.prisonerNumber)
        detailsText("Official visit created for prisoner number ${visit.prisonerNumber} with ${visit.officialVisitors().size} visitor(s)")
      },
    )
  }

  fun recordUpdated(visit: OfficialVisitEntity, previous: OfficialVisitAuditSnapshot?) {
    val actor = actorFor(visit.updatedBy ?: visit.createdBy)
    val before = previous ?: OfficialVisitAuditSnapshot.from(visit)

    auditingService.recordAuditEvent(
      auditVisitChangeEvent {
        officialVisitId(visit.officialVisitId)
        summaryText("Official visit updated")
        eventSource(eventSource())
        user(actor)
        prisonCode(visit.prisonCode)
        prisonerNumber(visit.prisonerNumber)
        changes {
          change("Visit slot", before.prisonVisitSlotId, visit.prisonVisitSlot.prisonVisitSlotId)
          change("Visit date", before.visitDate, visit.visitDate)
          change("Start time", before.startTime, visit.startTime)
          change("End time", before.endTime, visit.endTime)
          change("Location", before.dpsLocationId, visit.dpsLocationId)
          change("Visit type", before.visitTypeCode, visit.visitTypeCode.name)
          change("Prison code", before.prisonCode, visit.prisonCode)
          change("Prisoner number", before.prisonerNumber, visit.prisonerNumber)
          change("Current term", before.currentTerm, visit.currentTerm)
          change("Prisoner notes", before.prisonerNotes, visit.prisonerNotes)
          change("Staff notes", before.staffNotes, visit.staffNotes)
          change("Visitor concern notes", before.visitorConcernNotes, visit.visitorConcernNotes)
          change("Override ban by", before.overrideBanBy, visit.overrideBanBy)
          change("Offender book ID", before.offenderBookId, visit.offenderBookId)
          change("Offender visit ID", before.offenderVisitId, visit.offenderVisitId)
          change("Visit order number", before.visitOrderNumber, visit.visitOrderNumber)
          change("Visit status code", before.visitStatusCode, visit.visitStatusCode.name)
          change("Visit completion code", before.completionCode, visit.completionCode?.name)
          change("Completion notes", before.completionNotes, visit.completionNotes)
          change("Search type code", before.searchTypeCode, visit.searchTypeCode?.name)
        }
      },
    )
  }

  fun recordDeleted(visit: OfficialVisitEntity, previous: OfficialVisitAuditSnapshot?) {
    val actor = actorFor(visit.updatedBy ?: visit.createdBy)
    val details = previous?.let { "Official visit deleted for prisoner number ${it.prisonerNumber}" }
      ?: "Official visit deleted for prisoner number ${visit.prisonerNumber}"

    auditingService.recordAuditEvent(
      auditVisitCreateEvent {
        officialVisitId(visit.officialVisitId)
        summaryText("Official visit deleted")
        eventSource(eventSource())
        user(actor)
        prisonCode(visit.prisonCode)
        prisonerNumber(visit.prisonerNumber)
        detailsText(details)
      },
    )
  }
}

@Component
class OfficialVisitorAuditDelegate(
  private val auditingService: AuditingService,
  userService: UserService,
) : OfficialVisitAuditSupport(userService) {
  fun recordCreated(visitor: OfficialVisitorEntity) {
    val visit = visitor.officialVisit
    val relationshipType = visitor.relationshipType()

    auditingService.recordAuditEvent(
      auditVisitCreateEvent {
        officialVisitId(visit.officialVisitId)
        summaryText("$relationshipType visitor added")
        eventSource(eventSource())
        user(actorFor(visitor.createdBy))
        prisonCode(visit.prisonCode)
        prisonerNumber(visit.prisonerNumber)
        detailsText("$relationshipType visitor ${visitor.name()} added to visit for prisoner number ${visit.prisonerNumber}")
      },
    )
  }

  private fun OfficialVisitorEntity.relationshipType(): String = relationshipTypeCode
    ?.name
    ?.lowercase()
    ?.replaceFirstChar { it.uppercase() }
    ?: "Unknown"

  private fun OfficialVisitorEntity.name(): String = listOfNotNull(firstName?.formatNamePart(), lastName?.formatNamePart())
    .joinToString(" ")
    .ifBlank { "Unknown visitor" }

  private fun String.formatNamePart(): String = lowercase().replaceFirstChar { it.uppercase() }
}
