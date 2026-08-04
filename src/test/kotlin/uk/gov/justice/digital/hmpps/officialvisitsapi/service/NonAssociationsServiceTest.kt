package uk.gov.justice.digital.hmpps.officialvisitsapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.nonassociations.NonAssociationsApiClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.nonassociations.model.PrisonerNonAssociation
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.nonassociations.model.PrisonerNonAssociations
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonVisitSlotEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.MOORLAND_PRISONER
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.Prisoner
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.hasSize
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.nonAssociation
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.now
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.prisonerNonAssociations
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.request.NonAssociationCheckRequest
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class NonAssociationsServiceTest {
  private val nonAssociationsApiClient: NonAssociationsApiClient = mock()
  private val officialVisitRepository: OfficialVisitRepository = mock()
  private val locationsService: LocationsService = mock()
  private val service = NonAssociationsService(nonAssociationsApiClient, officialVisitRepository, locationsService)

  private val nonAssociateOne = Prisoner(MOORLAND, "A1232DD", 2, "Steve", "Smith")
  private val nonAssociateTwo = Prisoner(MOORLAND, "A1233EE", 3, "Alan", "Jones")

  private val request = NonAssociationCheckRequest(
    prisonerNumber = MOORLAND_PRISONER.number,
    visitDate = tomorrow(),
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
  )

  @Test
  fun `should return no visits and not query visits when prisoner is unknown to non-associations`() {
    stubNonAssociations(null)

    service.getNonAssociationVisits(MOORLAND, request) hasSize 0

    verifyNoInteractions(officialVisitRepository)
  }

  @Test
  fun `should return no visits and not query visits when prisoner has no non-associations`() {
    stubNonAssociations(prisonerNonAssociations(nonAssociations = emptyList()))

    service.getNonAssociationVisits(MOORLAND, request) hasSize 0

    verifyNoInteractions(officialVisitRepository)
  }

  @Test
  fun `should return no visits when non-associates have no visits on the date`() {
    stubNonAssociations(listOf(nonAssociation(nonAssociateOne)))
    stubVisits(emptyList())

    service.getNonAssociationVisits(MOORLAND, request) hasSize 0

    verifyVisitsQueriedFor(setOf(nonAssociateOne.number))
  }

  @Test
  fun `should return the visit and prisoner details of a non-associate with a visit on the date`() {
    val visit = visitFor(nonAssociateOne, officialVisitId = 23232323)

    stubNonAssociations(listOf(nonAssociation(nonAssociateOne)))
    stubVisits(listOf(visit))
    stubLocations()

    val response = service.getNonAssociationVisits(MOORLAND, request)

    response hasSize 1
    with(response.single()) {
      prisonCode isEqualTo MOORLAND
      officialVisitId isEqualTo 23232323
      visitDate isEqualTo visit.visitDate
      startTime isEqualTo visit.startTime
      endTime isEqualTo visit.endTime
      prisonerNumber isEqualTo nonAssociateOne.number
      dpsLocationId isEqualTo visit.dpsLocationId
      locationDescription isEqualTo "Moorland area 1"
      firstName isEqualTo "Steve"
      lastName isEqualTo "Smith"
    }
  }

  @Test
  fun `should describe the location as unknown when the prison locations do not include it`() {
    stubNonAssociations(listOf(nonAssociation(nonAssociateOne)))
    stubVisits(listOf(visitFor(nonAssociateOne, 1, dpsLocationId = UUID.randomUUID())))
    stubLocations()

    service.getNonAssociationVisits(MOORLAND, request).single().locationDescription isEqualTo "Unknown"
  }

  @Test
  fun `should look the prison locations up only once for several non-associate visits`() {
    stubNonAssociations(
      listOf(nonAssociation(nonAssociateOne, id = 1), nonAssociation(nonAssociateTwo, id = 2)),
    )
    stubVisits(listOf(visitFor(nonAssociateOne, 1), visitFor(nonAssociateTwo, 2)))
    stubLocations()

    service.getNonAssociationVisits(MOORLAND, request) hasSize 2

    verify(locationsService, times(1)).getOfficialVisitLocationsAtPrison(MOORLAND)
  }

  @Test
  fun `should not look up locations when no non-associate has a visit`() {
    stubNonAssociations(listOf(nonAssociation(nonAssociateOne)))
    stubVisits(emptyList())

    service.getNonAssociationVisits(MOORLAND, request) hasSize 0

    verifyNoInteractions(locationsService)
  }

  @Test
  fun `should return a visit for each non-associate that has one`() {
    stubNonAssociations(
      listOf(
        nonAssociation(nonAssociateOne, id = 1),
        nonAssociation(nonAssociateTwo, id = 2),
      ),
    )
    stubVisits(listOf(visitFor(nonAssociateOne, 1), visitFor(nonAssociateTwo, 2)))

    val response = service.getNonAssociationVisits(MOORLAND, request)

    response.map { it.prisonerNumber } containsExactly listOf(nonAssociateOne.number, nonAssociateTwo.number)
    response.map { it.lastName } containsExactly listOf("Smith", "Jones")

    verifyVisitsQueriedFor(setOf(nonAssociateOne.number, nonAssociateTwo.number))
  }

  @Test
  fun `should ignore a returned visit that does not belong to a non-associate`() {
    val unrelatedPrisoner = Prisoner(MOORLAND, "A9999ZZ", 9, "Unrelated", "Prisoner")

    stubNonAssociations(listOf(nonAssociation(nonAssociateOne)))
    stubVisits(listOf(visitFor(unrelatedPrisoner, 1)))

    service.getNonAssociationVisits(MOORLAND, request) hasSize 0
  }

  @Test
  fun `should query visits without times when the request has none`() {
    stubNonAssociations(listOf(nonAssociation(nonAssociateOne)))
    stubVisits(emptyList())

    service.getNonAssociationVisits(MOORLAND, request.copy(startTime = null, endTime = null)) hasSize 0

    verifyVisitsQueriedFor(setOf(nonAssociateOne.number), startTime = null, endTime = null)
  }

  @Test
  fun `should reject a start time without an end time`() {
    val exception = assertThrows<IllegalArgumentException> {
      service.getNonAssociationVisits(MOORLAND, request.copy(endTime = null))
    }

    exception.message isEqualTo "The start and end times must be supplied together"

    verifyNoInteractions(nonAssociationsApiClient, officialVisitRepository)
  }

  @Test
  fun `should reject an end time before the start time`() {
    val exception = assertThrows<IllegalArgumentException> {
      service.getNonAssociationVisits(MOORLAND, request.copy(endTime = LocalTime.of(9, 0)))
    }

    exception.message isEqualTo "The end time must be after the start time"

    verifyNoInteractions(nonAssociationsApiClient, officialVisitRepository)
  }

  @Test
  fun `should reject an end time equal to the start time`() {
    val exception = assertThrows<IllegalArgumentException> {
      service.getNonAssociationVisits(MOORLAND, request.copy(endTime = request.startTime))
    }

    exception.message isEqualTo "The end time must be after the start time"

    verifyNoInteractions(nonAssociationsApiClient, officialVisitRepository)
  }

  private fun stubNonAssociations(nonAssociations: List<PrisonerNonAssociation>) = stubNonAssociations(prisonerNonAssociations(nonAssociations = nonAssociations))

  private fun stubNonAssociations(response: PrisonerNonAssociations?) {
    whenever(nonAssociationsApiClient.getPrisonerNonAssociations(MOORLAND_PRISONER.number)).thenReturn(response)
  }

  private fun stubVisits(visits: List<OfficialVisitEntity>) {
    whenever(officialVisitRepository.findScheduledVisitsForPrisonersOn(any(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(visits)
  }

  private fun verifyVisitsQueriedFor(prisonerNumbers: Set<String>, startTime: LocalTime? = request.startTime, endTime: LocalTime? = request.endTime) {
    verify(officialVisitRepository).findScheduledVisitsForPrisonersOn(
      prisonCode = eq(MOORLAND),
      prisonerNumbers = eq(prisonerNumbers),
      visitDate = eq(request.visitDate),
      startTime = eq(startTime),
      endTime = eq(endTime),
    )
  }

  private fun stubLocations(locations: List<Location> = listOf(moorlandLocation)) {
    whenever(locationsService.getOfficialVisitLocationsAtPrison(MOORLAND)).thenReturn(locations)
  }

  private fun visitFor(
    prisoner: Prisoner,
    officialVisitId: Long,
    visitDate: LocalDate = tomorrow(),
    dpsLocationId: UUID = moorlandLocation.id,
  ): OfficialVisitEntity {
    val prisonVisitSlot = PrisonVisitSlotEntity(
      prisonVisitSlotId = officialVisitId,
      prisonTimeSlotId = 1,
      dpsLocationId = dpsLocationId,
      maxAdults = 1,
      maxGroups = 1,
      maxVideoSessions = 1,
      createdBy = "test-helper",
      createdTime = now(),
    )

    return OfficialVisitEntity(
      officialVisitId = officialVisitId,
      prisonVisitSlot = prisonVisitSlot,
      prisonCode = prisoner.prison,
      prisonerNumber = prisoner.number,
      visitDate = visitDate,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      dpsLocationId = prisonVisitSlot.dpsLocationId,
      visitTypeCode = VisitType.IN_PERSON,
      createdBy = "test-helper",
    )
  }
}
