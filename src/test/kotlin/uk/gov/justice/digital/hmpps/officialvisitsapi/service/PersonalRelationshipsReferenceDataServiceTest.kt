package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.PersonalRelationshipsApiClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ReferenceCode
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo

class PersonalRelationshipsReferenceDataServiceTest {
  private val personalRelationshipsApiClient: PersonalRelationshipsApiClient = mock()
  private val personalRelationshipsReferenceDataService = PersonalRelationshipsReferenceDataService(personalRelationshipsApiClient)

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `getReferenceDataByGroupCode should return List of Refence data based on group code for  personal relationships`() {
    val listOfCodes = listOf(
      ReferenceCode(
        1L,
        ReferenceCodeGroup.OFFICIAL_RELATIONSHIP,
        "TEST",
        "description",
        1,
        true,
      ),
    )
    whenever(personalRelationshipsApiClient.getReferenceDataByGroup(ReferenceCodeGroup.OFFICIAL_RELATIONSHIP.toString())).thenReturn(listOfCodes)
    assertThat(personalRelationshipsReferenceDataService.getReferenceDataByGroupCode(ReferenceCodeGroup.OFFICIAL_RELATIONSHIP)?.single()?.description isEqualTo "description")
  }

  @Test
  fun `personalRelationshipsRDService should return Refence data based on group code and reference data code  for  personal relationships`() {
    val listOfCodes = listOf(
      ReferenceCode(
        1,
        ReferenceCodeGroup.OFFICIAL_RELATIONSHIP,
        "TEST",
        "description",
        1,
        true,
      ),
    )
    whenever(personalRelationshipsApiClient.getReferenceDataByGroup(ReferenceCodeGroup.OFFICIAL_RELATIONSHIP.toString())).thenReturn(listOfCodes)
    assertThat(personalRelationshipsReferenceDataService.getReferenceDataByCode(ReferenceCodeGroup.OFFICIAL_RELATIONSHIP.toString(), "TEST")?.description isEqualTo "description")
  }
}
