package uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing

import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AuditedEventEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.User
import kotlin.properties.Delegates

fun auditEvent(initializer: AuditEventDsl.() -> Unit): AuditedEventEntity? = AuditEventDsl().apply(initializer).toAuditEvent()

@DslMarker
annotation class AuditEventDslMarker

@AuditEventDslMarker
class AuditEventDsl {
  private var officialVisitId by Delegates.notNull<Long>()
  private lateinit var summaryText: String
  private lateinit var eventSource: String
  private lateinit var user: User
  private lateinit var changes: Changes
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

  fun changes(initializer: Changes.() -> Unit) {
    changes = Changes()
    changes.initializer()
  }

  fun prisonerDetails(initializer: PrisonerDetails.() -> Unit) {
    prisonerDetails = PrisonerDetails()
    prisonerDetails.initializer()
  }

  fun toAuditEvent(): AuditedEventEntity? = run {
    val changes = this.changes.changes()

    if (changes.isEmpty()) return null

    AuditedEventEntity(
      officialVisitId = officialVisitId,
      prisonCode = prisonerDetails.prisonCode,
      prisonDescription = prisonerDetails.prisonDescription,
      prisonerNumber = prisonerDetails.prisonerNumber,
      eventSource = eventSource,
      username = user.username,
      userFullName = user.name,
      summaryText = summaryText,
      detailText = changes.joinToString(
        separator = "; ",
        postfix = ".",
      ) { "${it.descriptiveText} changed from ${it.old()} to ${it.new()}" },
    )
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

  @AuditEventDslMarker
  class PrisonerDetails {
    lateinit var prisonCode: String
    lateinit var prisonDescription: String
    lateinit var prisonerNumber: String
  }
}
