package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.StringFeature
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.OfficialVisitFacade
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCancellationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.PrisonerReleasedEvent
import java.time.LocalDate

const val DAYS_TO_LOOK_AHEAD = 365L

/**
 * This is a temporary flag which prevents visits being canceled on prisoner release, put in place
 * until further research has been done to confirm it is the right thing to do.
 *
 * There is a concern that automatic cancellation will confuse staff, and they may not be able to identify
 * the visits that need manual action, to inform the visitors that the prisoner is no longer at this establishment.
 */
const val CANCEL_ON_RELEASE = false

@Component
class PrisonerReleasedEventHandler(
  private val featureSwitches: FeatureSwitches,
  private val officialVisitRepository: OfficialVisitRepository,
  private val officialVisitFacade: OfficialVisitFacade,
) : DomainEventHandler<PrisonerReleasedEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerReleasedEvent) {
    val prisonerNumber = event.prisonerNumber()
    val prison = event.prisonId()
    when {
      event.isTemporary() -> log.info("RELEASE EVENT HANDLER: Ignoring temporary release event - $prisonerNumber from $prison")
      event.isTransferred() -> log.info("TRANSFER EVENT HANDLER: Ignoring transfer event (handled by standard sync) - $prisonerNumber from $prison")
      event.isPermanent() -> this.permanentRelease(prison, prisonerNumber)
      else -> log.warn("RELEASE EVENT HANDLER: Ignoring unknown release event $event")
    }
  }

  private fun permanentRelease(prison: String, prisonerNumber: String) {
    // This will always be false at present - but please leave the code in place
    if (enabledPrisons()?.contains(prison) ?: false && CANCEL_ON_RELEASE == true) {
      log.info("RELEASE EVENT HANDLER: Handling permanent release event for - $prisonerNumber from $prison")

      val visitsToCancel = officialVisitRepository.findAllPrisonerVisitsForReleaseCancel(
        prisonerNumber = prisonerNumber,
        prisonCode = prison,
        visitStatusCode = VisitStatusType.SCHEDULED,
        currentTerm = true,
        fromDate = LocalDate.now().plusDays(1),
        toDate = LocalDate.now().plusDays(DAYS_TO_LOOK_AHEAD),
      )
        .map { visit -> VisitCancellationCandidate(visit.prisonCode, visit.officialVisitId, visit.visitDate, visit.visitStatusCode) }

      visitsToCancel.forEach { candidate ->
        // Cancel them via the facade to emit sync events
        officialVisitFacade.cancelOfficialVisit(
          prisonCode = prison,
          officialVisitId = candidate.officialVisitId,
          request = OfficialVisitCancellationRequest(
            cancellationReason = VisitCompletionType.STAFF_CANCELLED,
            cancellationNotes = "Automatically cancelled due to release",
          ),
          user = UserService.getServiceAsUser(),
        )

        log.info("RELEASE EVENT HANDLER: Cancelled visit ${candidate.officialVisitId} for $prisonerNumber on ${candidate.visitDate} status ${candidate.visitStatusCode} at $prison")
      }
    } else {
      log.info("RELEASE EVENT HANDLER: Ignore permanent release event as $prison is not enabled for DPS visits")
    }
  }

  private fun enabledPrisons() = featureSwitches.getValue(StringFeature.FEATURE_DPS_ENABLED_PRISONS, null)?.split(',')
}

data class VisitCancellationCandidate(
  val prisonCode: String,
  val officialVisitId: Long,
  val visitDate: LocalDate,
  val visitStatusCode: VisitStatusType,
)
