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
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ContactsService

@Tag(name = "Personal Relationships contacts")
@RestController
@RequestMapping(value = ["prisoner-contact"], produces = [MediaType.APPLICATION_JSON_VALUE])
open class ContactsController(private val contactsService: ContactsService) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Operation(summary = "Endpoint to return the approved Available Contacts for Official and Social visits")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of   Approved contacts",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = PrisonerContactSummary::class)),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/prison/{prisonerNumber}/contact-relationships"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS__R')")
  fun getApprovedContacts(
    @PathVariable("prisonerNumber", required = true)
    prisonNumber: String,
    @Parameter(description = "Type should be S or  O", required = false)
    @RequestParam(name = "type", required = true, defaultValue = "O")
    type: String,
  ): List<PrisonerContactSummary> {
    logger.info("Received request for Approved contacts for  prisoner code $prisonNumber")
    return contactsService.getApprovedContacts(prisonNumber, type)
  }
}
