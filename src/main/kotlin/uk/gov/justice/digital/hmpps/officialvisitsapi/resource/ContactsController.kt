package uk.gov.justice.digital.hmpps.officialvisitsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationship.model.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.ApprovedContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ContactsService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Personal Relationships contacts")
@RestController
@RequestMapping(value = ["prisoner"], produces = [MediaType.APPLICATION_JSON_VALUE])
open class ContactsController(private val contactsService: ContactsService) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Operation(summary = "Get the approved contacts for a prisoner for official or social visits")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of all Approved contacts related to the prisoner",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = PrisonerContactSummary::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The Prisoner was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/{prisonerNumber}/contact-relationships"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS__R')")
  fun getApprovedContacts(
    @PathVariable("prisonerNumber", required = true)
    prisonerNumber: String,
    @Parameter(description = "Relationship Type should be S for social or  O for official", required = false)
    @RequestParam(name = "relationshipType", required = true, defaultValue = "O")
    relationshipType: String,
  ): List<ApprovedContact> {
    logger.info("Received request for Approved contacts for  prisoner code $prisonerNumber")
    return contactsService.getApprovedContacts(prisonerNumber, relationshipType)
  }
}
