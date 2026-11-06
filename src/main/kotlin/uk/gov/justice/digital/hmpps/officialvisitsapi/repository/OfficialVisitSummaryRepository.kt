package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitSummaryEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitType
import java.time.LocalDate
import java.util.UUID

/**
 * This repository is read-only and accessed via the view v_official_visit_summary.
 */
@Repository
interface OfficialVisitSummaryRepository : ReadOnlyRepository<OfficialVisitSummaryEntity, Long> {

  @Query(
    value = """
      SELECT ovs
        FROM OfficialVisitSummaryEntity ovs
       WHERE ovs.prisonCode = :prisonCode
         AND ovs.visitDate BETWEEN :startDate AND :endDate
         AND (:prisonerNumbers is null or ovs.prisonerNumber IN :prisonerNumbers)
         AND (:visitTypes is null or ovs.visitTypeCode IN :visitTypes)
         AND (:visitStatuses is null or ovs.visitStatusCode IN :visitStatuses)
         AND (:locationIds is null or ovs.dpsLocationId IN :locationIds)
    """,
  )
  fun findOfficialVisitSummaryEntityBy(
    prisonCode: String,
    prisonerNumbers: Collection<String>?,
    startDate: LocalDate,
    endDate: LocalDate,
    visitTypes: Collection<VisitType>?,
    visitStatuses: Collection<VisitStatusType>?,
    locationIds: Collection<UUID>?,
    pageable: Pageable,
  ): Page<OfficialVisitSummaryEntity>

  @Query(
    value = """
      SELECT ovs.officialVisitId
        FROM OfficialVisitSummaryEntity ovs
       WHERE ovs.visitDate BETWEEN :fromDate AND :toDate
         AND (:prisonerNumber is null or ovs.prisonerNumber = :prisonerNumber)
         AND (:prisonCode is null or ovs.prisonCode = :prisonCode)
         AND (:visitStatus is null or ovs.visitStatusCode = :visitStatus)
         AND (:visitType is null or ovs.visitTypeCode = :visitType)
         AND (:locationId is null or ovs.dpsLocationId = :locationId)
    """,
  )
  fun findOfficialVisitIdsBy(
    fromDate: LocalDate,
    toDate: LocalDate,
    prisonerNumber: String?,
    prisonCode: String?,
    visitStatus: VisitStatusType?,
    visitType: VisitType?,
    locationId: UUID?,
    pageable: Pageable,
  ): Page<Long>
}
