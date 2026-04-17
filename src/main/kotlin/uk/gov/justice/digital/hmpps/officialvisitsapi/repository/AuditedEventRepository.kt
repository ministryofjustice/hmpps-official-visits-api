package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.AuditedEventEntity

@Repository
interface AuditedEventRepository : JpaRepository<AuditedEventEntity, Long> {

  @Query(
    value = """
    DELETE FROM AuditedEventEntity ae
    WHERE ae.prisonerNumber = :prisonerNumber
    """,
  )
  @Modifying
  fun deleteAllByPrisonerNumber(prisonerNumber: String)
}
