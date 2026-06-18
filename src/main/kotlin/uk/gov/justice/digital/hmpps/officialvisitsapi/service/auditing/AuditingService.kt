package uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AuditedEventEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AuditedEventChange
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.AuditedEventResponse
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.AuditedEventRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.properties.Delegates

@Service
class AuditingService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val auditedEventRepository: AuditedEventRepository,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(readOnly = true)
  fun findByOfficialVisitId(officialVisitId: Long): List<AuditedEventResponse> {
    officialVisitRepository.findById(officialVisitId).orElseThrow { throw EntityNotFoundException("Official visit with id $officialVisitId not found") }

    return auditedEventRepository.findAllByOfficialVisitId(officialVisitId).sortedBy { it.eventDateTime }.mapNotNull { event ->
      val eventType = AuditEventType.entries.single { it.summaryText == event.summaryText }

      if (eventType.isStaffFacing) {
        if (event.version() == 2) {
          // Version level is 2 so treating event as new audited event.
          when (eventType) {
            AuditEventType.VISIT_CREATED -> {
              AuditedEventResponse(
                auditedEventId = event.auditedEventId,
                officialVisitId = officialVisitId,
                eventSource = event.eventSource,
                eventSummary = event.summaryText,
                eventDetail = event.detailText,
                eventType = "CREATE",
                eventDateTime = event.eventDateTime,
                eventUsername = event.userName,
                eventUserFullName = event.userFullName,
                eventVersion = event.version(),
              )
            }
            AuditEventType.VISIT_UPDATED -> {
              AuditedEventResponse(
                auditedEventId = event.auditedEventId,
                officialVisitId = officialVisitId,
                eventSource = event.eventSource,
                eventSummary = event.summaryText,
                eventDetail = event.detailText,
                eventType = "UPDATE",
                eventDateTime = event.eventDateTime,
                eventUsername = event.userName,
                eventUserFullName = event.userFullName,
                eventChanges = event.toAuditEventChanges(),
                eventVersion = event.version(),
              )
            }
            AuditEventType.VISIT_CANCELLED -> {
              AuditedEventResponse(
                auditedEventId = event.auditedEventId,
                officialVisitId = officialVisitId,
                eventSource = event.eventSource,
                eventSummary = event.summaryText,
                eventDetail = event.detailText,
                eventType = "CANCELLED",
                eventDateTime = event.eventDateTime,
                eventUsername = event.userName,
                eventUserFullName = event.userFullName,
                eventVersion = event.version(),
              )
            }
            AuditEventType.VISIT_COMPLETED -> {
              AuditedEventResponse(
                auditedEventId = event.auditedEventId,
                officialVisitId = officialVisitId,
                eventSource = event.eventSource,
                eventSummary = event.summaryText,
                eventDetail = event.detailText,
                eventType = "COMPLETED",
                eventDateTime = event.eventDateTime,
                eventUsername = event.userName,
                eventUserFullName = event.userFullName,
                eventVersion = event.version(),
              )
            }
            AuditEventType.VISITOR_CHANGED -> {
              AuditedEventResponse(
                auditedEventId = event.auditedEventId,
                officialVisitId = officialVisitId,
                eventSource = event.eventSource,
                eventSummary = event.summaryText,
                eventDetail = event.detailText,
                eventType = "UPDATE",
                eventDateTime = event.eventDateTime,
                eventUsername = event.userName,
                eventUserFullName = event.userFullName,
                eventChanges = event.toAuditEventChanges(),
                eventVersion = event.version(),
              )
            }
            AuditEventType.PRISONER_MERGED -> {
              AuditedEventResponse(
                auditedEventId = event.auditedEventId,
                officialVisitId = officialVisitId,
                eventSource = event.eventSource,
                eventSummary = event.summaryText,
                eventDetail = event.detailText,
                eventType = "UPDATE",
                eventDateTime = event.eventDateTime,
                eventUsername = event.userName,
                eventUserFullName = event.userFullName,
                eventChanges = event.toAuditEventChanges(),
                eventVersion = event.version(),
              )
            }
            else -> null.also { logger.info("Ignoring audit event type : $eventType") }
          }
        } else {
          // Version level is 1 so treating event as old audited event.
          AuditedEventResponse(
            auditedEventId = event.auditedEventId,
            officialVisitId = officialVisitId,
            eventSource = event.eventSource,
            eventSummary = event.summaryText,
            eventDetail = event.detailText,
            eventType = "OTHER",
            eventDateTime = event.eventDateTime,
            eventUsername = event.userName,
            eventUserFullName = event.userFullName,
            eventVersion = event.version(),
          )
        }
      } else {
        null.also { logger.info("Audit event type is not visible : $eventType") }
      }
    }
  }

  private fun AuditedEventEntity.toAuditEventChanges() = this.detailText
    .split(';')
    .filter { it.isNotBlank() }
    .map {
      val (field, oldValue, newValue) = it.split('|').let { element -> Triple(element[0], element[1], element[2]) }

      AuditedEventChange(
        field = field,
        oldValue = oldValue.ifBlank { null },
        newValue = newValue.ifBlank { null },
        significantChange = isSignificantChange(field),
      )
    }

  private fun isSignificantChange(field: String) = listOf("visit_date", "start_time", "end_time", "location", "visit_status").contains(field)

  fun recordAuditEvent(auditEvent: AuditEventDto) {
    auditedEventRepository.saveAndFlush(
      AuditedEventEntity(
        officialVisitId = auditEvent.officialVisitId,
        prisonCode = auditEvent.prisonCode,
        prisonerNumber = auditEvent.prisonerNumber,
        eventSource = auditEvent.eventSource,
        userName = auditEvent.username,
        userFullName = auditEvent.userFullName,
        summaryText = auditEvent.summaryText,
        detailText = auditEvent.detailText,
        eventDateTime = auditEvent.eventDateTime,
        versionNumber = 2,
      ),
    )
  }
}

fun auditVisitCreateEvent(initializer: CreateVisitDsl.() -> Unit): AuditEventDto = CreateVisitDsl().apply(initializer).toAuditEvent()

fun auditVisitChangeEvent(initializer: ChangeVisitDsl.() -> Unit): AuditEventDto = ChangeVisitDsl().apply(initializer).toAuditEvent()

fun auditVisitCancellationEvent(initializer: CancelVisitDsl.() -> Unit): AuditEventDto = CancelVisitDsl().apply(initializer).toAuditEvent()

fun auditVisitDeletedEvent(initializer: DeleteVisitDsl.() -> Unit): AuditEventDto = DeleteVisitDsl().apply(initializer).toAuditEvent()

fun auditVisitorChangedEvent(initializer: ChangedVisitorDsl.() -> Unit): AuditEventDto = ChangedVisitorDsl().apply(initializer).toAuditEvent()

fun auditVisitCompletionEvent(initializer: CompleteVisitDsl.() -> Unit): AuditEventDto = CompleteVisitDsl().apply(initializer).toAuditEvent()

fun auditVisitCurrentTermEvent(initializer: NewBookingVisitDsl.() -> Unit): AuditEventDto = NewBookingVisitDsl().apply(initializer).toAuditEvent()

@DslMarker
annotation class AuditEventDslMarker

@AuditEventDslMarker
abstract class AuditEventDsl {
  private var officialVisitId by Delegates.notNull<Long>()
  private lateinit var summaryText: String
  private lateinit var eventSource: String
  private lateinit var prisonCode: String
  private lateinit var prisonerNumber: String
  protected lateinit var user: User

  fun officialVisitId(ovId: Long) {
    officialVisitId = ovId
  }

  fun summaryText(auditEventType: AuditEventType) {
    summaryText = auditEventType.summaryText
  }

  fun summaryText(st: String) {
    summaryText = st.trim()
  }

  fun eventSource(es: String) {
    eventSource = es
  }

  fun prisonCode(pc: String) {
    prisonCode = pc
  }

  fun prisonerNumber(pn: String) {
    prisonerNumber = pn
  }

  fun user(u: User) {
    user = u
  }

  abstract fun detailsText(): String

  open fun toAuditEvent(): AuditEventDto = run {
    AuditEventDto(
      officialVisitId = officialVisitId,
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      eventSource = eventSource,
      username = user.username,
      userFullName = user.name,
      summaryText = summaryText,
      detailText = detailsText().take(1000),
    )
  }
}

@AuditEventDslMarker
class CreateVisitDsl : AuditEventDsl() {
  private lateinit var detailsText: String

  fun detailsText(detailsText: String) {
    this.detailsText = detailsText
  }

  override fun detailsText(): String = detailsText
}

class ChangeVisitDsl : AuditEventDsl() {
  private lateinit var changes: Changes

  fun changes(initializer: Changes.() -> Unit) {
    changes = Changes()
    changes.initializer()
  }

  override fun detailsText(): String = run {
    if (changes.changes().isEmpty()) return@run "No recorded changes."

    changes.changes().joinToString(
      separator = ";",
      postfix = ";",
    ) { it.alternativeText(it.old, it.new) ?: "${it.descriptiveText}|${it.old ?: ""}|${it.new ?: ""}" }
  }

  @AuditEventDslMarker
  class Changes {
    private val changes = mutableListOf<Change<*>>()

    fun change(descriptiveText: String, old: Any?, new: Any?, alternativeText: (old: Any?, new: Any?) -> String? = { _, _ -> null }) {
      changes.add(Change(descriptiveText, old, new, alternativeText))
    }

    fun change(descriptiveText: String, old: LocalDate?, new: LocalDate?, alternativeText: (old: Any?, new: Any?) -> String? = { _, _ -> null }) {
      changes.add(Change(descriptiveText, old?.toMediumFormatStyle(), new?.toMediumFormatStyle(), alternativeText))
    }

    fun changes() = changes.filter { it.hasChanged }

    data class Change<T : Any>(val descriptiveText: String, val old: T?, val new: T?, val alternativeText: (old: Any?, new: Any?) -> String? = { _, _ -> null }) {
      val hasChanged: Boolean = old != new
    }
  }
}

class ChangedVisitorDsl : AuditEventDsl() {
  private lateinit var changes: Changes

  fun changes(initializer: Changes.() -> Unit) {
    changes = Changes()
    changes.initializer()
  }

  override fun detailsText(): String = run {
    if (changes.changes().isEmpty()) return@run "No recorded changes."

    changes.changes().joinToString(
      separator = ";",
      postfix = ";",
    ) { "${it.descriptiveText}|${it.old ?: ""}|${it.new ?: ""}" }
  }

  @AuditEventDslMarker
  class Changes {
    private val changes = mutableListOf<Change<*>>()

    fun visitorAdded(visitorName: String) {
      changes.add(Change("visitor_added", null, visitorName))
    }

    fun visitorUpdated(visitorName: String) {
      changes.add(Change("visitor_updated", null, visitorName))
    }

    fun visitorRemoved(visitorName: String) {
      changes.add(Change("visitor_removed", null, visitorName))
    }

    fun changes() = changes.filter { it.hasChanged }

    data class Change<T : Any>(val descriptiveText: String, val old: T?, val new: T?) {
      val hasChanged: Boolean = old != new
    }
  }
}

class CancelVisitDsl : AuditEventDsl() {
  override fun detailsText(): String = "Visit cancelled"
}

class DeleteVisitDsl : AuditEventDsl() {
  override fun detailsText(): String = "Visit deleted"
}

class CompleteVisitDsl : AuditEventDsl() {
  override fun detailsText(): String = "Visit completed"
}

class NewBookingVisitDsl : AuditEventDsl() {
  private var currentTerm by Delegates.notNull<Boolean>()

  fun currentTerm(value: Boolean) {
    currentTerm = value
  }

  override fun detailsText(): String = "current_term|${currentTerm.not()}|$currentTerm"
}

data class AuditEventDto(
  val officialVisitId: Long,
  val prisonCode: String,
  val prisonerNumber: String,
  val eventSource: String,
  val username: String,
  val userFullName: String,
  val summaryText: String,
  val detailText: String,
  val eventDateTime: LocalDateTime = LocalDateTime.now(),
)

enum class AuditEventType(val summaryText: String, val isStaffFacing: Boolean) {
  VISIT_CREATED("Visit created", true),
  VISIT_UPDATED("Visit updated", true),
  VISIT_CANCELLED("Visit cancelled", true),
  VISIT_COMPLETED("Visit completed", true),
  VISITOR_CHANGED("Visitor changed", true),
  PRISONER_MERGED("Prisoner merged", true),
  VISIT_DELETED("Visit deleted", false),
  PRISONER_BOOKING_MOVED("Prisoner booking moved", false),
  CURRENT_TERM_CHANGED("Current term changed", false),
}
