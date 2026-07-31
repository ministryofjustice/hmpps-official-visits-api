package uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class OfficialVisitCancelledEmailTest {
  @Test
  fun `should populate all personalisation fields correctly when optional fields are not blank`() {
    val emailAddress = "recipient@example.com"
    val prisonerName = "JOHN DOE"
    val visitorNames = "JANE DOE, ALEX DOE"
    val appointmentDate = LocalDate.of(2026, 7, 30)
    val appointmentTime = LocalTime.of(14, 30)
    val appointmentLocation = "Room 4"
    val notes = "Cancelled due to unforeseen circumstances"
    val userName = "Admin User"

    val email = OfficialVisitCancelledEmail(
      emailAddress = emailAddress,
      prisonerName = prisonerName,
      visitorNames = visitorNames,
      appointmentDate = appointmentDate,
      appointmentTime = appointmentTime,
      appointmentLocation = appointmentLocation,
      notes = notes,
      userName = userName,
    )

    val personalisation = email.personalisation()
    assertEquals("John Doe", personalisation["prisoner_name"])
    assertEquals("Jane Doe, Alex Doe", personalisation["visitor_names"])
    assertEquals("30 Jul 2026", personalisation["appointment_date"])
    assertEquals("14:30", personalisation["appointment_time"])
    assertEquals(appointmentLocation, personalisation["appointment_location"])
    assertEquals("yes", personalisation["show_notes"])
    assertEquals(notes, personalisation["notes"])
    assertEquals(userName, personalisation["user_name"])
    assertEquals(EmailType.OFFICIAL_VISIT_CANCELLED, email.type())
  }

  @Test
  fun `should handle blank notes correctly`() {
    // Given
    val emailAddress = "recipient@example.com"
    val prisonerName = "JOHN DOE"
    val visitorNames = "JANE DOE, ALEX DOE"
    val appointmentDate = LocalDate.of(2026, 7, 30)
    val appointmentTime = LocalTime.of(14, 30)
    val appointmentLocation = "Room 4"
    val notes = "  "
    val userName = "Admin User"

    val email = OfficialVisitCancelledEmail(
      emailAddress = emailAddress,
      prisonerName = prisonerName,
      visitorNames = visitorNames,
      appointmentDate = appointmentDate,
      appointmentTime = appointmentTime,
      appointmentLocation = appointmentLocation,
      notes = notes,
      userName = userName,
    )

    val personalisation = email.personalisation()
    assertEquals("no", personalisation["show_notes"])
    assertEquals("", personalisation["notes"])
  }

  @Test
  fun `should handle null notes correctly`() {
    val emailAddress = "recipient@example.com"
    val prisonerName = "JOHN DOE"
    val visitorNames = "JANE DOE, ALEX DOE"
    val appointmentDate = LocalDate.of(2026, 7, 30)
    val appointmentTime = LocalTime.of(14, 30)
    val appointmentLocation = "Room 4"
    val notes: String? = null // Null notes
    val userName = "Admin User"

    val email = OfficialVisitCancelledEmail(
      emailAddress = emailAddress,
      prisonerName = prisonerName,
      visitorNames = visitorNames,
      appointmentDate = appointmentDate,
      appointmentTime = appointmentTime,
      appointmentLocation = appointmentLocation,
      notes = notes,
      userName = userName,
    )

    val personalisation = email.personalisation()
    assertEquals("no", personalisation["show_notes"])
    assertEquals("", personalisation["notes"])
  }
}
