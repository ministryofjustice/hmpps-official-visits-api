package uk.gov.justice.digital.hmpps.officialvisitsapi.resource.sync

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.converters.models.PageableAsQueryParam
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitId
import uk.gov.justice.digital.hmpps.officialvisitsapi.resource.AuthApiResponses
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.OfficialVisitReconciliationService

@Tag(name = "Official visits Reconciliation")
@RestController
@RequestMapping(value = ["reconcile"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class OfficialVisitReconciliationController(private val officialVisitReconciliationService: OfficialVisitReconciliationService) {

  @Operation(summary = "Endpoint to return the all the official visit ids for reconciliation")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of  paged list of DPS visit IDs for the current term",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = SyncOfficialVisitId::class)),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/official-visits/identifiers"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN', 'ROLE_OFFICIAL_VISITS__R', 'ROLE_OFFICIAL_VISITS__RW')")
  @PageableAsQueryParam
  fun getAllOfficialVisits(
    @Parameter(hidden = true)
    pageable: Pageable,
    @RequestParam(name = "currentTerm", required = true, defaultValue = "true")
    currentTerm: Boolean,
  ): PagedModel<SyncOfficialVisitId> = officialVisitReconciliationService.getOfficialVisitsIds(currentTerm, pageable)
}
