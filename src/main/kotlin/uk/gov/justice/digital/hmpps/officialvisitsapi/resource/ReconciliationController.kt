package uk.gov.justice.digital.hmpps.officialvisitsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.converters.models.PageableAsQueryParam
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitId
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ReconciliationService

@Tag(name = "Reconciliation")
@RestController
@RequestMapping(value = ["reconcile"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class ReconciliationController(private val officialVisitReconciliationService: ReconciliationService) {

  @Operation(summary = "Endpoint to return a paged list of all official visit IDs")
  @GetMapping(value = ["/official-visits/identifiers"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('OFFICIAL_VISITS_MIGRATION', 'OFFICIAL_VISITS_ADMIN')")
  @PageableAsQueryParam
  fun getAllOfficialVisitIds(
    @Parameter(hidden = true)
    @PageableDefault(size = 200, page = 0, direction = Sort.Direction.ASC)
    pageable: Pageable,
    @RequestParam(name = "currentTerm", required = true, defaultValue = "false")
    currentTerm: Boolean = false,
  ): PagedModel<SyncOfficialVisitId> = officialVisitReconciliationService.getOfficialVisitIds(currentTerm, pageable)

  @Operation(summary = "Endpoint to return one official visit by ID for reconciliation")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Official visit details",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncOfficialVisit::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/official-visit/id/{officialVisitId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('OFFICIAL_VISITS_MIGRATION', 'OFFICIAL_VISITS_ADMIN')")
  fun getOfficialVisitById(
    @PathVariable(name = "officialVisitId", required = true)
    officialVisitId: Long,
  ): SyncOfficialVisit = officialVisitReconciliationService.getOfficialVisitById(officialVisitId)
}
