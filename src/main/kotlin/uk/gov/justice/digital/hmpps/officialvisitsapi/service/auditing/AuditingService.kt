package uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AuditedEventEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.AuditedEventRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import java.time.LocalDateTime
import kotlin.properties.Delegates

@Service
class AuditingService(private val auditedEventRepository: AuditedEventRepository) {
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
      ),
    )
  }
}

fun auditCreateEvent(initializer: CreateDsl.() -> Unit): AuditEventDto = CreateDsl().apply(initializer).toAuditEvent()

fun auditChangeEvent(initializer: ChangeDsl.() -> Unit): AuditEventDto = ChangeDsl().apply(initializer).toAuditEvent()

@DslMarker
annotation class AuditEventDslMarker

@AuditEventDslMarker
abstract class AuditEventDsl {
  private var officialVisitId by Delegates.notNull<Long>()
  private lateinit var summaryText: String
  private lateinit var eventSource: String
  private lateinit var prisonCode: String
  private lateinit var prisonerNumber: String
  private lateinit var user: User

  fun officialVisitId(ovId: Long) {
    officialVisitId = ovId
  }

  fun summaryText(st: String) {
    summaryText = st
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
      detailText = detailsText(),
    )
  }
}

@AuditEventDslMarker
class CreateDsl : AuditEventDsl() {
  private lateinit var detailsText: String

  fun detailsText(detailsText: String) {
    this.detailsText = detailsText
  }

  override fun detailsText(): String = detailsText
}

class ChangeDsl : AuditEventDsl() {
  private lateinit var changes: Changes

  fun changes(initializer: Changes.() -> Unit) {
    changes = Changes()
    changes.initializer()
  }

  override fun detailsText(): String = run {
    changes.changes().joinToString(
      separator = "; ",
      postfix = ".",
    ) { "${it.descriptiveText} changed from ${it.old()} to ${it.new()}" }
  }

  @AuditEventDslMarker
  class Changes {
    private val changes = mutableListOf<Change<*>>()

    fun change(descriptiveText: String, old: () -> Any?, new: () -> Any?) {
      changes.add(Change(descriptiveText, old, new))
    }

    fun changes() = changes.filter { it.old() != it.new() }

    data class Change<T : Any>(val descriptiveText: String, val old: () -> T?, val new: () -> T?)
  }
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
