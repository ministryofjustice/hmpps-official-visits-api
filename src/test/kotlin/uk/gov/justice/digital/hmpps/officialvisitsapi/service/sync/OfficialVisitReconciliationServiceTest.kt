package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitId
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository

class OfficialVisitReconciliationServiceTest {
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val officialVisitReconciliationService = OfficialVisitReconciliationService(officialVisitRepository)

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `Get all official visits Ids`() {
    val pageable = PageRequest.of(0, 10)

    val result = listOf<SyncOfficialVisitId>(SyncOfficialVisitId(officialVisitId = 1L))
    val pageOfficialVisitsIds = PageImpl(result, pageable, 1)

    whenever(officialVisitRepository.findAllOfficialVisitIds(false, pageable)).thenReturn(pageOfficialVisitsIds)

    assertThat(officialVisitReconciliationService.getOfficialVisitsIds(false, pageable).content.single().officialVisitId isEqualTo 1)
  }
}
