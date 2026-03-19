package uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AuditedEventEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.AuditedEventRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import java.time.LocalDate
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

fun auditVisitCreateEvent(initializer: CreateVisitDsl.() -> Unit): AuditEventDto = CreateVisitDsl().apply(initializer).toAuditEvent()

fun auditVisitChangeEvent(initializer: ChangeVisitDsl.() -> Unit): AuditEventDto = ChangeVisitDsl().apply(initializer).toAuditEvent()

fun auditVisitCancellationEvent(initializer: CancelVisitDsl.() -> Unit): AuditEventDto = CancelVisitDsl().apply(initializer).toAuditEvent()

fun auditVisitCompletionEvent(initializer: CompleteVisitDsl.() -> Unit): AuditEventDto = CompleteVisitDsl().apply(initializer).toAuditEvent()

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
      separator = "; ",
      postfix = ".",
    ) { "${it.descriptiveText} changed from ${it.old ?: "''"} to ${it.new ?: "''"}" }
  }

  @AuditEventDslMarker
  class Changes {
    private val changes = mutableListOf<Change<*>>()

    fun change(descriptiveText: String, old: Any?, new: Any?) {
      changes.add(Change(descriptiveText, old, new))
    }

    fun change(descriptiveText: String, old: LocalDate?, new: LocalDate?) {
      changes.add(Change(descriptiveText, old?.toMediumFormatStyle(), new?.toMediumFormatStyle()))
    }

    fun changes() = changes.filter { it.hasChanged }

    data class Change<T : Any>(val descriptiveText: String, val old: T?, val new: T?) {
      val hasChanged: Boolean = old != new
    }
  }
}

class CancelVisitDsl : AuditEventDsl() {
  override fun detailsText(): String = "Visit cancelled by user ${user.name}"
}

class CompleteVisitDsl : AuditEventDsl() {
  override fun detailsText(): String = "Visit completed by user ${user.name}"
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
