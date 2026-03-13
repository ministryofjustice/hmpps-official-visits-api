package uk.gov.justice.digital.hmpps.officialvisitsapi.service.auditing

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISON_USER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now

class AuditEventFactoryTest {

  @Test
  fun `should be no audit event when no recorded changes`() {
    val auditEvent = auditEvent {
      officialVisitId(1)
      summaryText("Test summary")
      eventSource("DPS")
      user(MOORLAND_PRISON_USER)
      prisonerDetails {
        prisonCode = "MDI"
        prisonerNumber = "A1234AA"
        prisonDescription = "Moorland"
      }
      changes {
        change("ID", { 1 }, { 1 })
      }
    }

    auditEvent isEqualTo null
  }

  @Test
  fun `should be audit event when recorded changes`() {
    val auditEvent = auditEvent {
      officialVisitId(1)
      summaryText("Test summary text")
      eventSource("DPS")
      user(MOORLAND_PRISON_USER)
      prisonerDetails {
        prisonCode = "MDI"
        prisonerNumber = "A1234AA"
        prisonDescription = "Moorland"
      }
      changes {
        change("FIELD_1", { 1 }, { 2 })
        change("FIELD_2", { "a" }, { "b" })
      }
    }

    with(auditEvent!!) {
      officialVisitId isEqualTo 1
      summaryText isEqualTo "Test summary text"
      eventSource isEqualTo "DPS"
      eventDateTime isCloseTo now()
      username isEqualTo MOORLAND_PRISON_USER.username
      userFullName isEqualTo MOORLAND_PRISON_USER.name
      prisonCode isEqualTo MOORLAND
      prisonerNumber isEqualTo "A1234AA"
      prisonDescription isEqualTo "Moorland"
      detailText isEqualTo "FIELD_1 changed from 1 to 2; FIELD_2 changed from a to b."
    }
  }
}
