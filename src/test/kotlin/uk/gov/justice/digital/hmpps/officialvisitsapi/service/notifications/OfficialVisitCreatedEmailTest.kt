package uk.gov.justice.digital.hmpps.officialvisitsapi.service.notifications

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.officialvisitsapi.common.toTitleCase
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import java.time.LocalDate
import java.time.LocalTime

class OfficialVisitCreatedEmailTest {
  val emailAddress = "test@example.com"
  val prisonerNumber = "A1234AA"
  val prisonerName = "JOHN DOE"
  val visitDate: LocalDate = LocalDate.of(2026, 8, 15)
  val visitStartTime: LocalTime = LocalTime.of(10, 30)
  val visitEndTime: LocalTime = LocalTime.of(11, 30)
  val visitLocation = "Visiting Room A"
  val visitorNames = "Bob French, Nils Bohr"

  val prisonContactDetails = PrisonContactDetails(
    prisonCode = MOORLAND,
    prisonName = "Moorland (HMP)",
    prisonAddressLine1 = "1",
    prisonAddressLine2 = "2",
    prisonTown = "Town",
    prisonCounty = "County",
    prisonPostcode = "postcode",
    prisonEmail = "test@prison.com",
    prisonTelephone = "080808080",
    prisonWebsite = "Website",
  )

  @Test
  fun `should populate personalisation for an in-person visit`() {
    val notes = "This is a note"

    val email = InPersonVisitConfirmedEmail(
      emailAddress = emailAddress,
      prisonerNumber = prisonerNumber,
      prisonerName = prisonerName,
      prisonDetails = prisonContactDetails,
      visitDate = visitDate,
      visitStartTime = visitStartTime,
      visitEndTime = visitEndTime,
      visitLocation = visitLocation,
      visitorNames = visitorNames,
      notes = notes,
    )

    assertThat(email.emailAddress).isEqualTo(emailAddress)
    assertThat(email.type()).isEqualTo(EmailType.IN_PERSON_VISIT_CONFIRMED)

    assertThat(email.personalisation()).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "prisoner_number" to prisonerNumber,
        "prisoner_name" to prisonerName.toTitleCase(),
        "prison_code" to prisonContactDetails.prisonCode,
        "prison_name" to prisonContactDetails.prisonName,
        "prison_address_line1" to prisonContactDetails.prisonAddressLine1,
        "prison_address_line2" to prisonContactDetails.prisonAddressLine2,
        "prison_town" to prisonContactDetails.prisonTown,
        "prison_county" to prisonContactDetails.prisonCounty,
        "prison_postcode" to prisonContactDetails.prisonPostcode,
        "prison_email" to prisonContactDetails.prisonEmail,
        "prison_telephone" to prisonContactDetails.prisonTelephone,
        "prison_website" to prisonContactDetails.prisonWebsite,
        "visit_date" to "15 Aug 2026",
        "visit_start_time" to "10:30",
        "visit_end_time" to "11:30",
        "visit_location" to visitLocation,
        "visitor_names" to visitorNames,
        "show_notes" to "yes",
        "notes" to notes,
      ),
    )
  }

  @Test
  fun `should handle video link URL and notes correctly`() {
    val videoLinkUrl = "http://localhost:8080"
    val notes = "   "

    val email = VideoVisitConfirmedEmail(
      emailAddress = emailAddress,
      prisonerNumber = prisonerNumber,
      prisonerName = prisonerName,
      prisonDetails = prisonContactDetails,
      visitDate = visitDate,
      visitStartTime = visitStartTime,
      visitEndTime = visitEndTime,
      visitLocation = visitLocation,
      visitorNames = visitorNames,
      notes = notes,
      videoLinkUrl = videoLinkUrl,
    )

    assertThat(email.emailAddress).isEqualTo(emailAddress)
    assertThat(email.type()).isEqualTo(EmailType.VIDEO_VISIT_CONFIRMED)

    assertThat(email.personalisation()).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "prisoner_number" to prisonerNumber,
        "prisoner_name" to prisonerName.toTitleCase(),
        "prison_code" to prisonContactDetails.prisonCode,
        "prison_name" to prisonContactDetails.prisonName,
        "prison_address_line1" to prisonContactDetails.prisonAddressLine1,
        "prison_address_line2" to prisonContactDetails.prisonAddressLine2,
        "prison_town" to prisonContactDetails.prisonTown,
        "prison_county" to prisonContactDetails.prisonCounty,
        "prison_postcode" to prisonContactDetails.prisonPostcode,
        "prison_email" to prisonContactDetails.prisonEmail,
        "prison_telephone" to prisonContactDetails.prisonTelephone,
        "prison_website" to prisonContactDetails.prisonWebsite,
        "visit_date" to "15 Aug 2026",
        "visit_start_time" to "10:30",
        "visit_end_time" to "11:30",
        "visit_location" to visitLocation,
        "visitor_names" to visitorNames,
        "show_video_link" to "yes",
        "video_link_url" to videoLinkUrl,
        "show_notes" to "no",
        "notes" to "",
      ),
    )
  }
}
