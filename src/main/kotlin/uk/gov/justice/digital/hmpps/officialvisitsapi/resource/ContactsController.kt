package uk.gov.justice.digital.hmpps.officialvisitsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.ApprovedContact
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ContactsService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Contacts")
@RestController
@AuthApiResponses
@RequestMapping(value = ["prisoner"], produces = [MediaType.APPLICATION_JSON_VALUE])
class ContactsController(private val contactsService: ContactsService) {
  @Operation(summary = "Get the approved contacts for a prisoner for official or social visits")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of approved contacts of the prisoner",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ApprovedContact::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prisoner was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/{prisonerNumber}/approved-relationships"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__R', 'ROLE_OFFICIAL_VISITS__RW')")
  fun getApprovedContacts(
    @PathVariable("prisonerNumber", required = true)
    prisonerNumber: String,
    @Parameter(description = "The relationship type should be S for social or O for official", required = false)
    @RequestParam(name = "relationshipType", required = true, defaultValue = "O")
    relationshipType: String,
  ): List<ApprovedContact> = contactsService.getApprovedContacts(prisonerNumber, relationshipType)
}
