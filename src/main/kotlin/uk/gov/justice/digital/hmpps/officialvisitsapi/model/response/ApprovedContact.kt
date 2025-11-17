package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationship.model.RestrictionsSummary

class ApprovedContact(

  /* The unique identifier for the prisoner contact */
  @Schema(description = "The prisoner contact number", example = "1")
  val prisonerContactId: Long = 0,

  /* The unique identifier for the prisoner contact */
  @Schema(description = "The unique identifier for the prisoner contact", example = "1")
  val contactId: Long = 0,

  @Schema(description = "Prisoner number (NOMS ID)")
  val prisonerNumber: String,

  @Schema(description = "The last name of the contact")
  val lastName: String,

  /* The first name of the contact */
  @Schema(description = "The first name of the contact")
  val firstName: String,

  @Schema(description = "Coded value indicating either a social or official contact")
  val relationshipTypeCode: String,

  @Schema(description = "Relationship type Description")
  val relationshipTypeDescription: String,

  /* The relationship to the prisoner. A code from SOCIAL_RELATIONSHIP or OFFICIAL_RELATIONSHIP reference data groups depending on the relationship type. */

  @Schema(description = "The relationship to the prisoner. A code from SOCIAL_RELATIONSHIP or OFFICIAL_RELATIONSHIP reference data groups depending on the relationship type.")
  val relationshipToPrisonerCode: String,

  /* The description of the relationship to the prisoner */
  @Schema(description = "Relationship type Description")
  val relationshipToPrisonerDescription: String,

  /* Indicates whether the contact is an approved visitor */
  @Schema(description = "Indicates whether the contact is an approved visitor")
  val isApprovedVisitor: Boolean,

  /* Is this contact the prisoner's next of kin? */
  @Schema(description = "Is this contact the prisoner's next of kin?")
  val isNextOfKin: Boolean,

  /* Is this contact the prisoner's emergency contact? */
  @Schema(description = "Is this contact the prisoner's emergency contact?")
  val isEmergencyContact: Boolean,

  /* Is this prisoner's contact relationship active? */
  @Schema(description = "Is this prisoner's contact relationship active?")
  val isRelationshipActive: Boolean,

  /* Is this relationship active for the current booking? */
  @Schema(description = "Is this relationship active for the current booking?")
  val currentTerm: Boolean,

  /* Whether the contact is a staff member */
  @Schema(description = "Whether the contact is a staff member")
  val isStaff: Boolean,

  @Schema(description = "Restriction Summary")
  val restrictionSummary: RestrictionsSummary,

  /*        The title code for the contact.       This is a coded value (from the group code TITLE in reference data).        */
  @Schema(description = "The title code for the contact")
  val titleCode: String? = null,

  /* The description of the title code, if present */
  @Schema(description = "The description of the title code, if present")
  val titleDescription: String? = null,

  /* The middle names of the contact, if any */
  @Schema(description = "The middle names of the contact, if any")
  val middleNames: String? = null,

  /* The date of birth of the contact */
  @Schema(description = "The date of birth of the contact")
  val dateOfBirth: java.time.LocalDate? = null,

  /* The date the contact deceased, if known */
  @Schema(description = "The date the contact deceased, if known")
  val deceasedDate: java.time.LocalDate? = null,

  /* Flat number in the address, if any */
  @Schema(description = "Flat number in the address, if any")
  val flat: String? = null,

  /* Property name or number */
  @Schema(description = "Property name or number")
  val `property`: String? = null,

  /* Street name */
  @Schema(description = "Street Name")
  val street: String? = null,

  /* Area or locality, if any */
  @Schema(description = "Area or locality, if any")
  val area: String? = null,

  /* City code */
  @Schema(description = "City code")
  val cityCode: String? = null,

  /* The description of city code */
  @Schema(description = "The description of city code")
  val cityDescription: String? = null,

  /* County code */
  @Schema(description = "Country code")
  val countyCode: String? = null,

  /* The description of county code */
  @Schema(description = "The description of county code")
  val countyDescription: String? = null,

  /* Postal code */
  @Schema(description = "Postal code")
  val postcode: String? = null,

  /* Country code */
  @Schema(description = "Country Code")
  val countryCode: String? = null,

  /* The description of country code */
  @Schema(description = "Flag to indicate whether this address indicates no fixed address")
  val countryDescription: String? = null,

  /* Flag to indicate whether this address indicates no fixed address */
  @Schema(description = "Flag to indicate whether this address indicates no fixed address")
  val noFixedAddress: Boolean? = null,

  /* If true this address should be considered as the primary residential address */
  @Schema(description = "If true this address should be considered as the primary residential address")
  val primaryAddress: Boolean? = null,

  /* If true this address should be considered for sending mail to */
  @Schema(description = "If true this address should be considered for sending mail to")
  val mailAddress: Boolean? = null,

  /* Type of the latest phone number */
  @Schema(description = "Type of the latest phone number")
  val phoneType: String? = null,

  /* Description of the type of the latest phone number */
  @Schema(description = "Description of the type of the latest phone number")
  val phoneTypeDescription: String? = null,

  /* The latest phone number, if there are any */
  @Schema(description = "The latest phone number, if there are any")
  val phoneNumber: String? = null,

  /* The extension number of the latest phone number */
  @Schema(description = "The extension number of the latest phone number")
  val extNumber: String? = null,

)
