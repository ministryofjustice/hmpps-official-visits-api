package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ContactDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ContactEmailDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ContactPhoneDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.RestrictionTypeDetails
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.RestrictionsSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.pagedModelPrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.referenceCode

class PersonalRelationshipsApiMockServer : MockServer(8094) {
  fun stubApprovedContacts(prisonerNumber: String) {
    stubFor(
      get(urlPathEqualTo("/prisoner/$prisonerNumber/contact"))
        .withQueryParam("relationshipType", equalTo("O"))
        .withQueryParam("active", equalTo("true"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(pagedModelPrisonerContactSummary(prisonerNumber, "O")))
            .withStatus(200),
        ),
    )
  }

  fun stubReferenceGroup() {
    stubFor(
      get(urlPathEqualTo("/reference-codes/group/OFFICIAL_RELATIONSHIP"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(referenceCode()))
            .withStatus(200),
        ),
    )
  }

  fun stubAllApprovedContacts(contact: PrisonerContactSummary) = stubAllApprovedContacts(prisonerNumber = contact.prisonerNumber, contactId = contact.contactId, prisonerContactId = contact.prisonerContactId)

  fun stubAllApprovedContacts(prisonerNumber: String, contactId: Long = 1, prisonerContactId: Long = 1) {
    stubFor(
      get(urlPathEqualTo("/prisoner/$prisonerNumber/contact"))
        .withQueryParam("active", equalTo("true"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                pagedModelPrisonerContactSummary(
                  prisonerContact(
                    prisonerNumber = prisonerNumber,
                    type = "O",
                    contactId = contactId,
                    prisonerContactId = prisonerContactId,
                  ),
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubAllApprovedContact(contact: PrisonerContactSummary) {
    stubFor(
      get(urlPathEqualTo("/prisoner/${contact.prisonerNumber}/contact"))
        .withQueryParam("active", equalTo("true"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                pagedModelPrisonerContactSummary(contact),
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubForContactById(contact: PrisonerContactSummary, emailAddress: String? = null) {
    stubFor(
      get(urlPathEqualTo("/contact/${contact.contactId}"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                ContactDetails(
                  id = contact.contactId,
                  lastName = "first name",
                  firstName = "last name",
                  isStaff = false,
                  interpreterRequired = false,
                  addresses = emptyList(),
                  phoneNumbers = contact.phoneNumber?.let {
                    listOf(
                      ContactPhoneDetails(
                        contactPhoneId = 1,
                        contactId = contact.contactId,
                        phoneType = "",
                        phoneTypeDescription = "",
                        phoneNumber = contact.phoneNumber,
                        createdBy = "integration test",
                        createdTime = now(),
                      ),
                    )
                  } ?: emptyList(),
                  emailAddresses = emailAddress?.let {
                    listOf(
                      ContactEmailDetails(
                        contactEmailId = 1,
                        contactId = contact.contactId,
                        emailAddress = emailAddress,
                        createdBy = "integration test",
                        createdTime = now(),
                      ),
                    )
                  } ?: emptyList(),
                  identities = emptyList(),
                  employments = emptyList(),
                  createdBy = "integration test",
                  createdTime = now(),
                  titleCode = "title code",
                  titleDescription = "title description",
                  middleNames = null,
                  dateOfBirth = null,
                  deceasedDate = null,
                  languageCode = null,
                  languageDescription = null,
                  domesticStatusCode = null,
                  domesticStatusDescription = null,
                  genderCode = null,
                  genderDescription = null,
                  staff = null,
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubPrisonerContactRelationships(prisonerNumber: String, contactId: Long = 1) {
    stubFor(
      get(urlPathEqualTo("/prisoner/$prisonerNumber/contact/$contactId"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                listOf(
                  PrisonerContactSummary(
                    prisonerContactId = 2L,
                    contactId = contactId,
                    prisonerNumber = prisonerNumber,
                    lastName = "Last",
                    firstName = "First",
                    relationshipTypeCode = "OFFICIAL",
                    relationshipTypeDescription = "Official",
                    relationshipToPrisonerCode = "POL",
                    relationshipToPrisonerDescription = "Police officer",
                    isApprovedVisitor = true,
                    isNextOfKin = false,
                    isEmergencyContact = false,
                    isRelationshipActive = true,
                    currentTerm = true,
                    isStaff = false,
                    restrictionSummary = RestrictionsSummary(
                      active = emptySet<RestrictionTypeDetails>(),
                      totalActive = 0,
                      totalExpired = 0,
                    ),
                  ),
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }
}

class PersonalRelationshipsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val server = PersonalRelationshipsApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    server.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    server.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    server.stop()
  }
}
