package uk.gov.justice.digital.hmpps.officialvisitsapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.ConflictException

class OfficialVisitsApiExceptionHandlerTest {
  private val handler = OfficialVisitsApiExceptionHandler()

  @Test
  fun `should map conflict exception to 409 response`() {
    val response = handler.handleConflictException(
      ConflictException("Prisoner A1234AA already has an official visit booked at MDI on 2026-10-21 at 09:00"),
    )

    assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    assertThat(response.body?.userMessage).isEqualTo("Prisoner A1234AA already has an official visit booked at MDI on 2026-10-21 at 09:00")
  }
}
