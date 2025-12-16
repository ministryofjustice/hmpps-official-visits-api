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

  // The WHERE clauses for the two queries must be the same when using Pageable.
  @Query(
    value = """
      SELECT ovs
        FROM OfficialVisitSummaryEntity ovs
       WHERE ovs.prisonCode = :prisonCode
         AND ovs.visitDate BETWEEN :startDate AND :endDate
         AND (:prisonerNumbers is null or ovs.prisonerNumber IN :prisonerNumbers)
         AND (:visitTypes is null or ovs.visitTypeCode IN :visitTypes)
         AND (:visitStatusTypes is null or ovs.visitStatusCode IN :visitStatusTypes)
    """,
    countQuery = """
      SELECT count(ovs)
        FROM OfficialVisitSummaryEntity ovs
       WHERE ovs.prisonCode = :prisonCode
         AND ovs.visitDate BETWEEN :startDate AND :endDate
         AND (:prisonerNumbers is null or ovs.prisonerNumber IN :prisonerNumbers)
         AND (:visitTypes is null or ovs.visitTypeCode IN :visitTypes)
         AND (:visitStatusTypes is null or ovs.visitStatusCode IN :visitStatusTypes)
    """,
  )
  fun findOfficialVisitSummaryEntityBy(
    prisonCode: String,
    prisonerNumbers: Collection<String>?,
    startDate: LocalDate,
    endDate: LocalDate,
    visitTypes: Collection<VisitType>?,
    visitStatusTypes: Collection<VisitStatusType>?,
    locationsId: Collection<UUID>?,
    pageable: Pageable,
  ): Page<OfficialVisitSummaryEntity>
}
