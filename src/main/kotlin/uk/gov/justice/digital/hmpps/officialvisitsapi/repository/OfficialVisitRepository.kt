package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Repository
interface OfficialVisitRepository : JpaRepository<OfficialVisitEntity, Long> {
  fun findByOfficialVisitIdAndPrisonCode(officialVisitId: Long, prisonCode: String): OfficialVisitEntity?

  @Query("SELECT ov.officialVisitId FROM OfficialVisitEntity ov WHERE (:currentTermOnly is null OR ov.currentTerm = :currentTermOnly)")
  fun findAllOfficialVisitIds(currentTermOnly: Boolean?, pageable: Pageable): Page<Long>

  @Query(
    """
    SELECT ov 
    FROM OfficialVisitEntity ov 
    WHERE ov.prisonerNumber = :prisonerNumber
    AND (:currentTerm = false OR ov.currentTerm = true)
    AND ov.prisonCode = :prisonCode
    AND ov.visitStatusCode = :visitStatusCode
    AND (CAST(:fromDate as date) IS NULL OR ov.visitDate >= :fromDate)
    AND (CAST(:toDate as date) IS NULL OR ov.visitDate <= :toDate)
    ORDER BY ov.visitDate, ov.startTime
   """,
  )
  fun findAllPrisonerVisitsForReleaseCancel(prisonerNumber: String, prisonCode: String, visitStatusCode: VisitStatusType, currentTerm: Boolean, fromDate: LocalDate?, toDate: LocalDate?): List<OfficialVisitEntity>

  @Query(
    """
    SELECT ov 
    FROM OfficialVisitEntity ov 
    WHERE ov.prisonerNumber = :prisonerNumber
    AND (:currentTerm = false OR ov.currentTerm = true)
    AND (CAST(:fromDate as date) IS NULL OR ov.visitDate >= :fromDate)
    AND (CAST(:toDate as date) IS NULL OR ov.visitDate <= :toDate)
    ORDER BY ov.visitDate, ov.startTime
   """,
  )
  fun findAllPrisonerVisits(prisonerNumber: String, currentTerm: Boolean, fromDate: LocalDate?, toDate: LocalDate?): List<OfficialVisitEntity>

  fun existsByPrisonVisitSlotPrisonVisitSlotId(prisonVisitSlotId: Long): Boolean

  fun findByOffenderVisitId(offenderVisitId: Long): OfficialVisitEntity?

  @Query(
    value = """
      SELECT count(distinct ov)
      FROM OfficialVisitEntity ov
      WHERE ov.prisonerNumber = :prisonerNumber
    """,
  )
  fun countOVByPrisonerNumber(prisonerNumber: String): Long

  @Query(value = "UPDATE OfficialVisitEntity ov SET ov.prisonerNumber = :replacementNumber, ov.offenderBookId = :bookingId WHERE ov.prisonerNumber = :removedNumber")
  @Modifying
  fun mergePrisonerNumber(removedNumber: String, replacementNumber: String, bookingId: Long?)

  @Query(
    value = """
      UPDATE OfficialVisitEntity ov
      SET ov.prisonerNumber = :replacementNumber 
      WHERE ov.prisonerNumber = :removedNumber and ov.offenderBookId = :bookingId and ov.createdTime >= :startDateTime
       """,
  )
  @Modifying
  fun bookingMove(removedNumber: String, replacementNumber: String, bookingId: Long, startDateTime: LocalDateTime)

  @Query(
    value = """
      SELECT count(distinct ov)
      FROM OfficialVisitEntity ov
      WHERE ov.prisonerNumber = :prisonerNumber and ov.offenderBookId = :bookingId and ov.createdTime >= :startDateTime
    """,
  )
  fun countOVByPrisonerNumberAndBookingId(prisonerNumber: String, bookingId: Long, startDateTime: LocalDateTime): Long

  fun findByPrisonCodeAndPrisonerNumberAndVisitDate(prisonCode: String, prisonerNumber: String, visitDate: LocalDate): List<OfficialVisitEntity>

  @Query(
    value = """
      SELECT ov
      FROM OfficialVisitEntity ov
      WHERE ov.prisonCode = :prisonCode
        AND ov.prisonerNumber = :prisonerNumber
        AND ov.visitDate = :visitDate
        AND :startTime < ov.endTime
        AND :endTime > ov.startTime
        AND ov.visitStatusCode = 'SCHEDULED'
    """,
  )
  fun findScheduledOverlappingVisitsBy(prisonCode: String, prisonerNumber: String, visitDate: LocalDate, startTime: LocalTime, endTime: LocalTime): List<OfficialVisitEntity>
}
