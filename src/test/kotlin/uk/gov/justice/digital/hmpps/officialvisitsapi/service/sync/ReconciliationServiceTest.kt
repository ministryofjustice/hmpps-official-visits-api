package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository

class ReconciliationServiceTest {
  private val officialVisitRepository: OfficialVisitRepository = Mockito.mock()
  private val prisonerVisitedRepository: PrisonerVisitedRepository = Mockito.mock()

  private val reconciliationService = ReconciliationService(officialVisitRepository, prisonerVisitedRepository)

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `Get all official visits Ids`() {
    val pageable = PageRequest.of(0, 10)

    val result = listOf<Long>(1L)
    val pageOfficialVisitsIds = PageImpl(result, pageable, 1)

    whenever(officialVisitRepository.findAllOfficialVisitIds(null, pageable)).thenReturn(pageOfficialVisitsIds)

    Assertions.assertThat(
      reconciliationService.getOfficialVisitIds(false, pageable).content.single().officialVisitId isEqualTo 1,
    )
  }
}
