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

  fun findByOfficialVisitId(officialVisitId: Long): OfficialVisitEntity?

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

  fun findAllByPrisonerNumber(prisonerNumber: String): List<OfficialVisitEntity>

  fun findAllByPrisonerNumberAndOffenderBookIdAndCreatedTimeGreaterThanEqual(
    prisonerNumber: String,
    offenderBookId: Long,
    createdTime: LocalDateTime,
  ): List<OfficialVisitEntity>

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

  @Query(
    value = """
      SELECT ov
      FROM OfficialVisitEntity ov
      WHERE ov.prisonCode = :prisonCode
        AND ov.prisonerNumber IN :prisonerNumbers
        AND ov.visitDate = :visitDate
        AND (CAST(:startTime as time) IS NULL OR (:startTime < ov.endTime AND :endTime > ov.startTime))
        AND ov.visitStatusCode = 'SCHEDULED'
      ORDER BY ov.startTime, ov.prisonerNumber
    """,
  )
  fun findScheduledVisitsForPrisonersOn(prisonCode: String, prisonerNumbers: Collection<String>, visitDate: LocalDate, startTime: LocalTime?, endTime: LocalTime?): List<OfficialVisitEntity>

  @Query(
    value = """
      DELETE FROM OfficialVisitEntity ov
      WHERE ov.prisonerNumber = :prisonerNumber
    """,
  )
  @Modifying()
  fun deleteAllByPrisonerNumber(prisonerNumber: String)

  fun findAllByPrisonerNumberAndOffenderBookId(prisonerNumber: String, bookingId: Long): List<OfficialVisitEntity>

  fun findAllByPrisonerNumberAndOffenderBookIdNot(prisonerNumber: String, bookingId: Long): List<OfficialVisitEntity>

  fun findAllByPrisonerNumberAndVisitDateBetween(
    prisonerNumber: String,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): List<OfficialVisitEntity>

  @Query(
    value = """
        SELECT ov FROM OfficialVisitEntity ov
        WHERE ov.visitDate >= :today
          AND ov.visitDate < :weekFromNow
          AND ov.visitStatusCode = 'SCHEDULED'
          AND ov.officialVisitId NOT IN (SELECT vrq.officialVisitId FROM VisitReviewQueueEntity vrq)
          AND ov.officialVisitId NOT IN (SELECT vr.officialVisitId FROM VisitReviewEntity vr)
        
        """,
  )
  fun findCandidateVisitsForReview(
    today: LocalDate?,
    weekFromNow: LocalDate?,
  ): Collection<OfficialVisitEntity>

  @Query(
    value = """
        SELECT ov FROM OfficialVisitEntity ov
        JOIN VisitReviewQueueEntity vrq ON vrq.officialVisitId = ov.officialVisitId
        WHERE vrq.createdTime = (
            SELECT MIN(vrq2.createdTime) FROM VisitReviewQueueEntity vrq2
            WHERE vrq2.officialVisitId = ov.officialVisitId
        )
        ORDER BY vrq.createdTime ASC
        """,
  )
  fun findCandidatesOrderedByQueueTime(): Collection<OfficialVisitEntity>

  @Query(
    value = """
        SELECT DISTINCT ov FROM OfficialVisitEntity ov
        JOIN VisitReviewEntity vr ON vr.officialVisitId = ov.officialVisitId
        JOIN VisitReviewDetailEntity vrd ON vrd.visitReview.visitReviewId = vr.visitReviewId
        WHERE ov.visitDate < :date
          AND vr.expiredTime IS NULL
          AND vrd.acknowledgedTime IS NULL
          AND vrd.acknowledgedBy IS NULL
        ORDER BY ov.visitDate ASC, ov.startTime ASC
        """,
  )
  fun findOverdueVisitsWithUnacknowledgedReviewDetailsBefore(date: LocalDate): Collection<OfficialVisitEntity>
}
