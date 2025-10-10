package uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch

import jakarta.validation.ValidationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo

class PrisonerValidatorTest {
  private val prisoner: Prisoner = mock()
  private val client: PrisonerSearchClient = mock()
  private val validator = PrisonerValidator(client)

  @Test
  fun `should pass validation when prisoner found`() {
    prisoner.stub { on { prisonId } doReturn BIRMINGHAM }

    whenever(client.getPrisoner("123456")) doReturn prisoner

    val returnedPrisoner = validator.validatePrisonerAtPrison("123456", BIRMINGHAM)

    returnedPrisoner isEqualTo prisoner
  }

  @Test
  fun `should fail validation when prisoner found but not at prison`() {
    prisoner.stub { on { prisonId } doReturn WANDSWORTH }

    whenever(client.getPrisoner("123456")) doReturn prisoner

    val error = assertThrows<ValidationException> { validator.validatePrisonerAtPrison("123456", BIRMINGHAM) }

    error.message isEqualTo "Prisoner 123456 not found at prison BMI"
  }

  @Test
  fun `should fail validation when prisoner not found`() {
    whenever(client.getPrisoner("123456")) doReturn null

    val error = assertThrows<ValidationException> { validator.validatePrisonerAtPrison("123456", BIRMINGHAM) }

    error.message isEqualTo "Prisoner 123456 not found at prison BMI"
  }
}
