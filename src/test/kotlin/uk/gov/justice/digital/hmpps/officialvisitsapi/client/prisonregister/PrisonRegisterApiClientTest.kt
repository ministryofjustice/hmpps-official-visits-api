package uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonregister

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonregisterapi.model.AddressDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonregisterapi.model.ContactDetailsDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonregisterapi.model.PrisonDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonregisterapi.model.PrisonOperatorDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonregisterapi.model.PrisonTypeDto
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock.PrisonRegisterApiMockServer

class PrisonRegisterApiClientTest {
  private val prisonId = "MDI"
  private val department = "SOCIAL_VISIT"

  private val server = PrisonRegisterApiMockServer().also { it.start() }
  private val client = PrisonRegisterClient(WebClient.create("http://localhost:${server.port()}"))

  @AfterEach
  fun after() {
    server.stop()
  }

  @Test
  fun `should return the prison details by prison code`() {
    val prisonDetails = PrisonDto(
      prisonId = prisonId,
      prisonName = "Moorland",
      active = true,
      male = true,
      female = false,
      contracted = false,
      lthse = false,
      types = listOf(PrisonTypeDto(code = PrisonTypeDto.Code.HMP, description = "Description")),
      categories = setOf(PrisonDto.Categories.B),
      addresses = listOf(AddressDto(id = 1, town = "Doncaster", postcode = "DN4 3NJ", country = "UK")),
      operators = listOf(PrisonOperatorDto(name = "HMPPS")),
    )

    server.stubGetPrisonDetails(prisonId, prisonDetails)

    val response = client.getPrisonDetails(prisonId)

    response?.prisonId isEqualTo prisonDetails.prisonId
    response?.prisonName isEqualTo prisonDetails.prisonName
  }

  @Test
  fun `should return not found if the prison code is unrecognised`() {
    server.stubGetPrisonDetailsNotFound(prisonId)

    val response = client.getPrisonDetails(prisonId)

    response isEqualTo null
  }

  @Test
  fun `should return the contact details for the official visits department at a prison`() {
    val departmentContact = ContactDetailsDto(
      // TODO: There is no contact type for OFFICIAL_VISIT as yet - needs a change to the prison register
      type = ContactDetailsDto.Type.SOCIAL_VISIT,
      emailAddress = "test@official-visit.com",
      phoneNumber = "1111111111",
      webAddress = "test.com",
    )

    server.stubGetPrisonContactDetails(prisonId, department, departmentContact)

    val response = client.getPrisonContactDetails(prisonId, department)

    response?.type isEqualTo departmentContact.type
    response?.emailAddress isEqualTo departmentContact.emailAddress
    response?.phoneNumber isEqualTo departmentContact.phoneNumber
    response?.webAddress isEqualTo departmentContact.webAddress
  }
}
