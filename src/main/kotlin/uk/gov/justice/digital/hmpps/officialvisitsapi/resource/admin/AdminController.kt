package uk.gov.justice.digital.hmpps.officialvisitsapi.resource.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.admin.TimeSlotSummary
import uk.gov.justice.digital.hmpps.officialvisitsapi.resource.AuthApiResponses
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.admin.AdminService

@Tag(name = "Admin")
@RestController
@RequestMapping(value = ["admin"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class AdminController(private val adminService: AdminService) {

  @GetMapping(path = ["/time-slots/prison/{prisonCode}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Return summary of prison time slot and associated visit slots based on the prison code",
    description = """
      Requires role: OFFICIAL_VISITS_ADMIN.
      Used to get the summary of prison time slot and associated visit slots based on the prison code.
      """,
  )
  @PreAuthorize("hasAnyRole('ROLE_OFFICIAL_VISITS_ADMIN')")
  fun getAllTimeSlotsAndVisitSlots(
    @PathVariable prisonCode: String,
    @RequestParam(name = "activeOnly", required = false, defaultValue = "true")
    activeOnly: Boolean = true,
  ): TimeSlotSummary = adminService.getAllPrisonTimeSlotsAndAssociatedVisitSlots(prisonCode, activeOnly)
}
