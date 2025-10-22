package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.ReferenceDataGroup
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.ReferenceDataItem
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.ReferenceDataRepository

class ReferenceDataServiceTest {
  private val referenceDataRepository: ReferenceDataRepository = mock()
  private val service = ReferenceDataService(referenceDataRepository)

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `Should return a list of references codes that are active values only`() {
    val groupCode = ReferenceDataGroup.VIS_TYPE_CODE
    val listOfCodes = listOf(
      ReferenceDataEntity(1L, groupCode, "IN_PERSON", "In person", 0, true),
      ReferenceDataEntity(2L, groupCode, "VIDEO", "Video", 1, true),
      ReferenceDataEntity(3L, groupCode, "TELEPHONE", "Telephone", 2, true),
    )

    whenever(referenceDataRepository.findAllByGroupCodeAndEnabledEquals(groupCode, true, Sort.unsorted())).thenReturn(listOfCodes)

    assertThat(service.getReferenceDataByGroup(groupCode, Sort.unsorted(), true)).isEqualTo(listOfCodes.toModel())

    verify(referenceDataRepository).findAllByGroupCodeAndEnabledEquals(groupCode, true, Sort.unsorted())
  }

  @Test
  fun `Should return a list of references codes for active and inactive values`() {
    val groupCode = ReferenceDataGroup.VIS_TYPE_CODE
    val listOfCodes = listOf(
      ReferenceDataEntity(1L, groupCode, "IN_PERSON", "In person", 0, true),
      ReferenceDataEntity(2L, groupCode, "VIDEO", "Video", 1, true),
      ReferenceDataEntity(3L, groupCode, "TELEPHONE", "Telephone", 2, true),
    )

    whenever(referenceDataRepository.findAllByGroupCodeAndEnabledEquals(groupCode, true, Sort.unsorted())).thenReturn(listOfCodes)

    assertThat(service.getReferenceDataByGroup(groupCode, Sort.unsorted(), true)).isEqualTo(listOfCodes.toModel())

    verify(referenceDataRepository).findAllByGroupCodeAndEnabledEquals(groupCode, true, Sort.unsorted())
  }

  @Test
  fun `Should return an empty list when no reference codes are enabled for a group`() {
    val groupCode = ReferenceDataGroup.TEST_TYPE
    whenever(referenceDataRepository.findAllByGroupCodeEquals(groupCode, Sort.unsorted())).thenReturn(emptyList())

    assertThat(service.getReferenceDataByGroup(groupCode, Sort.unsorted(), false)).isEmpty()

    verify(referenceDataRepository).findAllByGroupCodeEquals(groupCode, Sort.unsorted())
  }

  @Test
  fun `should return a reference code when enabled`() {
    val entity = ReferenceDataEntity(1L, ReferenceDataGroup.TEST_TYPE, "TEST", "test", 0, true)

    whenever(referenceDataRepository.findByGroupCodeAndCode(ReferenceDataGroup.TEST_TYPE, "TEST")).thenReturn(entity)

    val code = service.validateReferenceData(ReferenceDataGroup.TEST_TYPE, "TEST", allowInactive = false)

    assertThat(code).isEqualTo(ReferenceDataItem(1L, ReferenceDataGroup.TEST_TYPE, "TEST", "test", 0, true))
  }

  @Test
  fun `should return reference code if not enabled and not enabled is allowed`() {
    val entity = ReferenceDataEntity(1L, ReferenceDataGroup.VIS_COMPLETE, "CANC", "Cancelled", 0, false)

    whenever(referenceDataRepository.findByGroupCodeAndCode(ReferenceDataGroup.VIS_COMPLETE, "CANC")).thenReturn(entity)

    val code = service.validateReferenceData(ReferenceDataGroup.VIS_COMPLETE, "CANC", allowInactive = true)

    assertThat(code).isEqualTo(ReferenceDataItem(1L, ReferenceDataGroup.VIS_COMPLETE, "CANC", "Cancelled", 0, false))
  }

  @Test
  fun `should throw exception if reference code is not enabled and inactive not allowed`() {
    val entity = ReferenceDataEntity(1L, ReferenceDataGroup.VIS_COMPLETE, "CANC", "Cancelled", 0, false)

    whenever(referenceDataRepository.findByGroupCodeAndCode(ReferenceDataGroup.VIS_COMPLETE, "CANC")).thenReturn(entity)

    val exception = assertThrows<ValidationException> {
      service.validateReferenceData(ReferenceDataGroup.VIS_COMPLETE, "CANC", allowInactive = false)
    }

    assertThat(exception.message).isEqualTo("Unsupported visit completion code (CANC). This code is no longer active.")
  }

  @Test
  fun `should throw exception if reference code is not found`() {
    whenever(referenceDataRepository.findByGroupCodeAndCode(ReferenceDataGroup.VIS_COMPLETE, "UNKNOWN")).thenReturn(null)

    val exception = assertThrows<ValidationException> {
      service.validateReferenceData(ReferenceDataGroup.VIS_COMPLETE, "UNKNOWN", allowInactive = true)
    }

    assertThat(exception.message).isEqualTo("Unsupported visit completion code (UNKNOWN)")
  }
}
