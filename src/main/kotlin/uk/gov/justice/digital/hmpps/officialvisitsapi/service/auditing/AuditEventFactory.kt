package uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing

import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AuditedEventEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import kotlin.properties.Delegates

fun auditCreateEvent(initializer: CreateDsl.() -> Unit): AuditedEventEntity = CreateDsl().apply(initializer).toAuditEvent()

fun auditChangeEvent(initializer: ChangeDsl.() -> Unit): AuditedEventEntity = ChangeDsl().apply(initializer).toAuditEvent()

@DslMarker
annotation class AuditEventDslMarker

@AuditEventDslMarker
abstract class AuditEventDsl {
  private var officialVisitId by Delegates.notNull<Long>()
  private lateinit var summaryText: String
  private lateinit var eventSource: String
  private lateinit var user: User
  private lateinit var prisonerDetails: PrisonerDetails

  fun officialVisitId(ovId: Long) {
    officialVisitId = ovId
  }

  fun summaryText(st: String) {
    summaryText = st
  }

  fun eventSource(es: String) {
    eventSource = es
  }

  fun user(u: User) {
    user = u
  }

  fun prisonerDetails(initializer: PrisonerDetails.() -> Unit) {
    prisonerDetails = PrisonerDetails()
    prisonerDetails.initializer()
  }

  abstract fun detailsText(): String

  open fun toAuditEvent(): AuditedEventEntity = run {
    AuditedEventEntity(
      officialVisitId = officialVisitId,
      prisonCode = prisonerDetails.prisonCode,
      prisonDescription = prisonerDetails.prisonDescription,
      prisonerNumber = prisonerDetails.prisonerNumber,
      eventSource = eventSource,
      username = user.username,
      userFullName = user.name,
      summaryText = summaryText,
      detailText = detailsText(),
    )
  }

  @AuditEventDslMarker
  class PrisonerDetails {
    lateinit var prisonCode: String
    lateinit var prisonDescription: String
    lateinit var prisonerNumber: String
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
