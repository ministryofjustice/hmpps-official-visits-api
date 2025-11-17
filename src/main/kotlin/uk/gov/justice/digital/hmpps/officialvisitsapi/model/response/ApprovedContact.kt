package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationship.model.RestrictionsSummary
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Describes the details of a prisoner's contact")
data class ApprovedContact(

  @Schema(description = "The unique identifier for the prisoner contact", example = "123456")
  val prisonerContactId: Long,

  @Schema(description = "The unique identifier for the contact", example = "654321")
  val contactId: Long,

  @Schema(description = "Prisoner number (NOMS ID)", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(
    description =
      """
      The title code for the contact.
      This is a coded value (from the group code TITLE in reference data).
      """,
    example = "MR",
    nullable = true,
  )
  val titleCode: String? = null,

  @Schema(
    description = "The description of the title code, if present",
    example = "Mr",
    nullable = true,
  )
  val titleDescription: String? = null,

  @Schema(description = "The last name of the contact", example = "Doe")
  val lastName: String,

  @Schema(description = "The first name of the contact", example = "John")
  val firstName: String,

  @Schema(description = "The middle names of the contact, if any", example = "William", nullable = true)
  val middleNames: String? = null,

  @Schema(description = "The date of birth of the contact", example = "1980-01-01")
  val dateOfBirth: LocalDate?,

  @Schema(description = "The date the contact deceased, if known", example = "1980-01-01", nullable = true)
  val deceasedDate: LocalDate?,

  @Schema(
    description =
      """
      Coded value indicating either a social or official contact (mandatory).
      This is a coded value from the group code CONTACT_TYPE in reference data.
      Known values are (S) Social or (O) official.
      """,
    example = "S",
  )
  val relationshipTypeCode: String,

  @Schema(description = "The description of the relationship type", example = "Friend")
  val relationshipTypeDescription: String,

  @Schema(description = "The relationship to the prisoner. A code from SOCIAL_RELATIONSHIP or OFFICIAL_RELATIONSHIP reference data groups depending on the relationship type.", example = "FRI")
  val relationshipToPrisonerCode: String,

  @Schema(description = "The description of the relationship to the prisoner", example = "Friend")
  val relationshipToPrisonerDescription: String,

  @Schema(description = "Flat number in the address, if any", example = "Flat 1", nullable = true)
  val flat: String?,

  @Schema(description = "Property name or number", example = "123")
  val property: String?,

  @Schema(description = "Street name", example = "Baker Street")
  val street: String?,

  @Schema(description = "Area or locality, if any", example = "Marylebone", nullable = true)
  val area: String?,

  @Schema(description = "City code", example = "25343")
  val cityCode: String?,

  @Schema(description = "The description of city code", example = "Sheffield")
  val cityDescription: String?,

  @Schema(description = "County code", example = "S.YORKSHIRE")
  val countyCode: String?,

  @Schema(description = "The description of county code", example = "South Yorkshire")
  val countyDescription: String?,

  @Schema(description = "Postal code", example = "NW1 6XE")
  val postcode: String?,

  @Schema(description = "Country code", example = "ENG")
  val countryCode: String?,

  @Schema(description = "The description of country code", example = "England")
  val countryDescription: String?,

  @Schema(description = "Flag to indicate whether this address indicates no fixed address", example = "false")
  val noFixedAddress: Boolean?,

  @Schema(description = "If true this address should be considered as the primary residential address", nullable = true, example = "true")
  val primaryAddress: Boolean?,

  @Schema(description = "If true this address should be considered for sending mail to", nullable = true, example = "true")
  val mailAddress: Boolean?,

  @Schema(description = "Type of the latest phone number", example = "MOB", nullable = true)
  val phoneType: String?,

  @Schema(description = "Description of the type of the latest phone number", example = "Mobile", nullable = true)
  val phoneTypeDescription: String?,

  @Schema(description = "The latest phone number, if there are any", example = "+1234567890", nullable = true)
  val phoneNumber: String?,

  @Schema(description = "The extension number of the latest phone number", example = "123", nullable = true)
  val extNumber: String?,

  @Schema(description = "Indicates whether the contact is an approved visitor", example = "true")
  val isApprovedVisitor: Boolean,

  @Schema(description = "Is this contact the prisoner's next of kin?", example = "false")
  val isNextOfKin: Boolean,

  @Schema(description = "Is this contact the prisoner's emergency contact?", example = "true")
  val isEmergencyContact: Boolean,

  @Schema(description = "Is this prisoner's contact relationship active?", example = "true")
  val isRelationshipActive: Boolean,

  @Schema(description = "Is this relationship active for the current booking?", example = "true")
  val currentTerm: Boolean,

  @Schema(description = "Any additional comments", example = "Close family friend", nullable = true)
  val comments: String?,

  @Schema(description = "Whether the contact is a staff member", example = "false", nullable = false)
  val isStaff: Boolean = false,

  val restrictionSummary: RestrictionsSummary,

)
