package uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now

class AuditEventServiceTest {
  @Test
  fun `should be audit create event when recorded changes`() {
    val auditEvent = auditCreateEvent {
      officialVisitId(1)
      summaryText("Test summary create text")
      eventSource("DPS")
      user(MOORLAND_PRISON_USER)
      prisonCode(MOORLAND)
      prisonerNumber("A1234AA")
      detailsText("Details of create event")
    }

    with(auditEvent) {
      officialVisitId isEqualTo 1
      summaryText isEqualTo "Test summary create text"
      eventSource isEqualTo "DPS"
      eventDateTime isCloseTo now()
      username isEqualTo MOORLAND_PRISON_USER.username
      userFullName isEqualTo MOORLAND_PRISON_USER.name
      prisonerNumber isEqualTo "A1234AA"
      detailText isEqualTo "Details of create event"
    }
  }

  @Test
  fun `should be audit change event when recorded changes`() {
    val auditEvent = auditChangeEvent {
      officialVisitId(1)
      summaryText("Test summary change text")
      eventSource("DPS")
      user(MOORLAND_PRISON_USER)
      prisonCode(MOORLAND)
      prisonerNumber("A1234AA")
      changes {
        change("FIELD_1", { 1 }, { 2 })
        change("FIELD_2", { "a" }, { "b" })
      }
    }

    with(auditEvent) {
      officialVisitId isEqualTo 1
      summaryText isEqualTo "Test summary change text"
      eventSource isEqualTo "DPS"
      eventDateTime isCloseTo now()
      username isEqualTo MOORLAND_PRISON_USER.username
      userFullName isEqualTo MOORLAND_PRISON_USER.name
      prisonerNumber isEqualTo "A1234AA"
      detailText isEqualTo "FIELD_1 changed from 1 to 2; FIELD_2 changed from a to b."
    }
  }
}
