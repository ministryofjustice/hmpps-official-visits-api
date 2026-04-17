package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.PrisonerVisitedEntity
import java.time.LocalDateTime

@Repository
interface PrisonerVisitedRepository : JpaRepository<PrisonerVisitedEntity, Long> {

  @Query(
    value = """
    SELECT pv 
    FROM PrisonerVisitedEntity pv 
    WHERE pv.officialVisit.officialVisitId = :officialVisitId
    """,
  )
  fun findByOfficialVisitId(officialVisitId: Long): PrisonerVisitedEntity?

  fun findByOfficialVisit(officialVisitEntity: OfficialVisitEntity): PrisonerVisitedEntity?

  fun findAllByOfficialVisitOfficialVisitIdIn(officialVisitIds: Collection<Long>): List<PrisonerVisitedEntity>

  fun deleteByOfficialVisit(officialVisitEntity: OfficialVisitEntity)

  @Query(
    value = """
    UPDATE PrisonerVisitedEntity pv 
    SET pv.prisonerNumber = :replacementNumber 
    WHERE pv.prisonerNumber = :removedNumber
    """,
  )
  @Modifying
  fun replacePrisonerNumber(removedNumber: String, replacementNumber: String)

  @Query(
    value = """
    UPDATE PrisonerVisitedEntity pv 
    SET pv.prisonerNumber = :replacementNumber 
    WHERE pv.prisonerNumber = :removedNumber
    AND pv.officialVisit.offenderBookId = :bookingId
    AND pv.officialVisit.createdTime > :startDateTime
    """,
  )
  @Modifying
  fun replacePrisonerNumberForBooking(removedNumber: String, replacementNumber: String, bookingId: Long, startDateTime: LocalDateTime)

  @Query(
    value = """
    DELETE FROM PrisonerVisitedEntity pv
    WHERE pv.prisonerNumber = :prisonerNumber
    """,
  )
  @Modifying
  fun deleteAllByPrisonerNumber(prisonerNumber: String)

  fun countByPrisonerNumber(prisonerNumber: String): Long
}
