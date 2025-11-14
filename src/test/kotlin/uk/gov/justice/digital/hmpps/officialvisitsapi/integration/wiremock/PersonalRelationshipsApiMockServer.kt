package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class PersonalRelationshipsApiMockServer : MockServer(8099) {
  fun stubApprovedContacts(prisonerNumber: String) {
    val jsonResponse = """
      {
         "content": [
          {
            "prisonerContactId": 123456,
            "contactId": 654321,
            "prisonerNumber": "A1234BC",
            "titleCode": "MR",
            "titleDescription": "Mr",
            "lastName": "Doe",
            "firstName": "John",
            "middleNames": "William",
            "dateOfBirth": "1980-01-01",
            "deceasedDate": "1980-01-01",
            "relationshipTypeCode": "O",
            "relationshipTypeDescription": "Friend",
            "relationshipToPrisonerCode": "FRI",
            "relationshipToPrisonerDescription": "Friend",
            "flat": "Flat 1",
            "property": "123",
            "street": "Baker Street",
            "area": "Marylebone",
            "cityCode": "25343",
            "cityDescription": "Sheffield",
            "countyCode": "S.YORKSHIRE",
            "countyDescription": "South Yorkshire",
            "postcode": "NW1 6XE",
            "countryCode": "ENG",
            "countryDescription": "England",
            "noFixedAddress": false,
            "primaryAddress": true,
            "mailAddress": true,
            "phoneType": "MOB",
            "phoneTypeDescription": "Mobile",
            "phoneNumber": "+1234567890",
            "extNumber": "123",
            "isApprovedVisitor": true,
            "isNextOfKin": false,
            "isEmergencyContact": true,
            "isRelationshipActive": true,
            "currentTerm": true,
            "comments": "Close family friend",
            "isStaff": false,
            "restrictionSummary": {
              "active": [
                {
                  "restrictionType": "string",
                  "restrictionTypeDescription": "string"
                }
              ],
              "totalActive": 0,
              "totalExpired": 0
            }
          }
        ],
        "page": {
          "size": 0,
          "number": 0,
          "totalElements": 0,
          "totalPages": 0
        }
      }
    """.trimIndent()
    stubFor(
      get("/prisoner/$prisonerNumber/contact?relationshipType=O&active=true")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jsonResponse)
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
