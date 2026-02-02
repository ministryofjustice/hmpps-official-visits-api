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
import uk.gov.justice.digital.hmpps.officialvisitsapi.resource.AuthApiResponses
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.sync.ReconciliationService
import java.time.LocalDate

@Tag(name = "Reconciliation")
@RestController
@RequestMapping(value = ["reconcile"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class ReconciliationController(private val reconciliationService: ReconciliationService) {

  @Operation(summary = "Return a paged list of all official visit IDs")
  @GetMapping(value = ["/official-visits/identifiers"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('OFFICIAL_VISITS_MIGRATION', 'OFFICIAL_VISITS_ADMIN')")
  @PageableAsQueryParam
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A paginated list of all official visit IDs",
      ),
    ],
  )
  fun getAllOfficialVisitIds(
    @Parameter(hidden = true)
    @PageableDefault(size = 200, page = 0, direction = Sort.Direction.ASC)
    pageable: Pageable,
    @RequestParam(name = "currentTermOnly", defaultValue = "true")
    currentTermOnly: Boolean = true,
  ): PagedModel<SyncOfficialVisitId> = reconciliationService.getOfficialVisitIds(currentTermOnly, pageable)

  @Operation(summary = "Return one official visit by ID")
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
    @PathVariable(required = true) officialVisitId: Long,
  ): SyncOfficialVisit = reconciliationService.getOfficialVisitById(officialVisitId)

  @Operation(summary = "Return all official visits for a prisoner between two dates with optional current term flag")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of official visits",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = SyncOfficialVisit::class)),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/prisoner/{prisonerNumber}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('OFFICIAL_VISITS_MIGRATION', 'OFFICIAL_VISITS_ADMIN')")
  fun getAllOfficialVisitForPrisoner(
    @PathVariable(required = true) @Parameter(description = "Prisoner number", required = true)
    prisonerNumber: String,
    @Parameter(description = "Current term only, true of false. Defaults to true.")
    @RequestParam(name = "currentTermOnly", defaultValue = "true")
    currentTermOnly: Boolean = true,
    @Parameter(description = "The from date in ISO format (YYYY-MM-DD)")
    @RequestParam(name = "fromDate", required = false)
    fromDate: LocalDate?,
    @Parameter(description = "The to date in ISO format (YYYY-MM-DD)")
    @RequestParam(name = "toDate", required = false)
    toDate: LocalDate?,
  ): List<SyncOfficialVisit> = reconciliationService.getAllPrisonerVisits(prisonerNumber, currentTermOnly, fromDate, toDate)
}
