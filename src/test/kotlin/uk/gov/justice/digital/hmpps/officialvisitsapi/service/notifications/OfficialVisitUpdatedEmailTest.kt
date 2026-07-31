package uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class OfficialVisitUpdatedEmailTest {
  @Test
  fun `should populate personalisation when video link and notes are provided`() {
    val emailAddress = "test@example.com"
    val prisonerName = "JOHN DOE"
    val appointmentDate = LocalDate.of(2026, 8, 15)
    val appointmentTime = LocalTime.of(10, 30)
    val appointmentLocation = "Visiting Room A"
    val videoLinkUrl = "http://example.com/video-link"
    val notes = "This is a note"
    val userName = "Jane Smith"

    val email = OfficialVisitUpdatedEmail(
      emailAddress = emailAddress,
      prisonerName = prisonerName,
      appointmentDate = appointmentDate,
      appointmentTime = appointmentTime,
      appointmentLocation = appointmentLocation,
      videoLinkUrl = videoLinkUrl,
      notes = notes,
      userName = userName,
    )

    assertThat(email.emailAddress).isEqualTo(emailAddress)
    assertThat(email.type()).isEqualTo(EmailType.OFFICIAL_VISIT_UPDATED)
    assertThat(email.personalisation()).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "prisoner_name" to "John Doe",
        "appointment_date" to "15 Aug 2026",
        "appointment_time" to "10:30",
        "appointment_location" to appointmentLocation,
        "show_video_link" to "yes",
        "video_link_url" to videoLinkUrl,
        "show_notes" to "yes",
        "notes" to notes,
        "user_name" to userName,
      ),
    )
  }

  @Test
  fun `should handle missing video link and notes correctly`() {
    val emailAddress = "test@example.com"
    val prisonerName = "JOHN DOE"
    val appointmentDate = LocalDate.of(2026, 8, 15)
    val appointmentTime = LocalTime.of(10, 30)
    val appointmentLocation = "Visiting Room A"
    val userName = "Jane Smith"

    val email = OfficialVisitUpdatedEmail(
      emailAddress = emailAddress,
      prisonerName = prisonerName,
      appointmentDate = appointmentDate,
      appointmentTime = appointmentTime,
      appointmentLocation = appointmentLocation,
      videoLinkUrl = "",
      notes = "",
      userName = userName,
    )

    assertThat(email.emailAddress).isEqualTo(emailAddress)
    assertThat(email.type()).isEqualTo(EmailType.OFFICIAL_VISIT_UPDATED)
    assertThat(email.personalisation()).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "prisoner_name" to "John Doe",
        "appointment_date" to "15 Aug 2026",
        "appointment_time" to "10:30",
        "appointment_location" to appointmentLocation,
        "show_video_link" to "no",
        "video_link_url" to "",
        "show_notes" to "no",
        "notes" to "",
        "user_name" to userName,
      ),
    )
  }
}
