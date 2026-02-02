package uk.gov.justice.digital.hmpps.officialvisitsapi.resource.sync

import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.facade.sync.SyncFacade
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.resource.AuthApiResponses

@Tag(name = "Synchronisation")
@RestController
@RequestMapping(value = ["sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class OfficialVisitSyncController(private val syncFacade: SyncFacade) {

  @Operation(summary = "Endpoint to return one official visit by ID")
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
  ): SyncOfficialVisit = syncFacade.getOfficialVisitById(officialVisitId)
}
