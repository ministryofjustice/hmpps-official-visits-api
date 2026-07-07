package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sar

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sar.SarVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sar.SarVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sar.SubjectAccessResponseData
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.OfficialVisitRepository
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.PrisonerVisitedRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

/**
 * Prisoners have the right to access and receive a copy of their personal data and other supplementary information.
 * This is commonly referred to as a subject access request or ‘SAR’.
 * The purpose of this service is to surface all relevant prisoner-specific information for a subject access request.
 * By extending HmppsPrisonSubjectAccessRequestService the endpoint needed for this is automatically included, no
 * additional controller (endpoint) is required.
 */
@Service
@Transactional(readOnly = true)
class SubjectAccessRequestService(
  private val officialVisitRepository: OfficialVisitRepository,
  private val prisonerVisitedRepository: PrisonerVisitedRepository,
) : HmppsPrisonSubjectAccessRequestService {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun getPrisonContentFor(prn: String, fromDate: LocalDate?, toDate: LocalDate?): HmppsSubjectAccessRequestContent? = run {
    val from = fromDate ?: LocalDate.EPOCH
    val to = toDate ?: LocalDate.now()

    log.info("SAR: processing subject access request for prisoner $prn, from $from to $to.")

    val officialVisits = officialVisitRepository.findAllByPrisonerNumberAndVisitDateBetween(
      prisonerNumber = prn,
      fromDate = from,
      toDate = to,
    )

    if (officialVisits.isEmpty()) {
      log.info("SAR: no matches found for prisoner $prn, from $from to $to.")
      return null
    }

    log.info("SAR: matches found for prisoner $prn, from $from to $to.")

    val sarVisits = officialVisits.map { visit ->
      val prisonerVisited = prisonerVisitedRepository.findByOfficialVisit(visit)

      SarVisit(
        completionCode = visit.completionCode,
        completionNotes = visit.completionNotes,
        endTime = visit.endTime,
        prisonCode = visit.prisonCode,
        prisonerNotes = visit.prisonerNotes,
        searchTypeCode = visit.searchTypeCode?.name,
        staffNotes = visit.staffNotes,
        startTime = visit.startTime,
        visitDate = visit.visitDate,
        visitorConcernNotes = visit.visitorConcernNotes,
        visitStatus = visit.visitStatusCode,
        visitType = visit.visitTypeCode,
        prisonerAttendance = prisonerVisited?.attendanceCode,
        visitors = visit.officialVisitors().map { visitor ->
          SarVisitor(
            visitorAttendance = visitor.attendanceCode,
            relationshipType = visitor.relationshipTypeCode,
            relationshipCode = visitor.relationshipCode,
          )
        },
      )
    }

    log.info("SAR: Returning SAR response for prisoner $prn, from $from to $to.")

    HmppsSubjectAccessRequestContent(
      content = SubjectAccessResponseData(
        prisonerNumber = prn,
        fromDate = from,
        toDate = to,
        officialVisits = sarVisits,
      ),
    )
  }
}
