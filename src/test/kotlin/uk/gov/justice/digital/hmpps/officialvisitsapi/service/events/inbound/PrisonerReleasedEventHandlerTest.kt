package uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.StringFeature
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.OfficialVisitFacade
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.PENTONVILLE_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.createAVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitCompletionType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.OfficialVisitCancellationRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.UserService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.DAYS_TO_LOOK_AHEAD
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.events.inbound.handlers.PrisonerReleasedEventHandler
import java.time.LocalDate

class PrisonerReleasedEventHandlerTest {
  private val featureSwitches: FeatureSwitches = mock()
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val officialVisitFacade: OfficialVisitFacade = mock()

  val permanentReleaseEvent = PrisonerReleasedEvent(
    additionalInformation = ReleaseInformation(
      nomsNumber = PENTONVILLE_PRISONER.number,
      reason = "RELEASED",
      prisonId = PENTONVILLE,
    ),
  )

  private val temporaryReleaseEvent = PrisonerReleasedEvent(
    additionalInformation = ReleaseInformation(
      nomsNumber = PENTONVILLE_PRISONER.number,
      reason = "SENT_TO_COURT",
      prisonId = PENTONVILLE,
    ),
  )

  private val transferEvent = PrisonerReleasedEvent(
    additionalInformation = ReleaseInformation(
      nomsNumber = PENTONVILLE_PRISONER.number,
      reason = "TRANSFERRED",
      prisonId = PENTONVILLE,
    ),
  )

  private val handler = PrisonerReleasedEventHandler(featureSwitches, officialVisitRepository, officialVisitFacade)

  @BeforeEach
  fun setup() {
    whenever(
      officialVisitRepository.findAllPrisonerVisits(
        prisonerNumber = PENTONVILLE_PRISONER.number,
        currentTerm = true,
        fromDate = LocalDate.now().plusDays(1),
        toDate = LocalDate.now().plusDays(DAYS_TO_LOOK_AHEAD),
      ),
    ).thenReturn(
      listOf(
        createAVisitEntity(1L),
        createAVisitEntity(2L),
      ),
    )

    whenever(featureSwitches.getValue(StringFeature.FEATURE_DPS_ENABLED_PRISONS)).thenReturn(PENTONVILLE)
  }

  @Test
  fun `should cancel visits for a permanent release event when enabled for DPS official visits`() {
    handler.handle(permanentReleaseEvent)

    verify(officialVisitRepository).findAllPrisonerVisits(
      prisonerNumber = PENTONVILLE_PRISONER.number,
      currentTerm = true,
      fromDate = LocalDate.now().plusDays(1),
      toDate = LocalDate.now().plusDays(DAYS_TO_LOOK_AHEAD),
    )

    verify(officialVisitFacade).cancelOfficialVisit(
      prisonCode = PENTONVILLE,
      officialVisitId = 1L,
      OfficialVisitCancellationRequest(
        cancellationReason = VisitCompletionType.STAFF_CANCELLED,
        cancellationNotes = "Automatically cancelled due to release",
      ),
      user = UserService.getServiceAsUser(),
    )

    verify(officialVisitFacade).cancelOfficialVisit(
      prisonCode = PENTONVILLE,
      officialVisitId = 2L,
      OfficialVisitCancellationRequest(
        cancellationReason = VisitCompletionType.STAFF_CANCELLED,
        cancellationNotes = "Automatically cancelled due to release",
      ),
      user = UserService.getServiceAsUser(),
    )
  }

  @Test
  fun `should not attempt to cancel visits is the prison is not enabled for DPS visits`() {
    whenever(featureSwitches.getValue(StringFeature.FEATURE_DPS_ENABLED_PRISONS)).thenReturn("")
    handler.handle(permanentReleaseEvent)
    verifyNoInteractions(officialVisitFacade)
  }

  @Test
  fun `should not attempt to cancel visits for transfers`() {
    handler.handle(transferEvent)
    verifyNoInteractions(officialVisitFacade)
  }

  @Test
  fun `should not attempt to cancel visits if the release is temporary`() {
    handler.handle(temporaryReleaseEvent)
    verifyNoInteractions(officialVisitFacade)
  }
}
