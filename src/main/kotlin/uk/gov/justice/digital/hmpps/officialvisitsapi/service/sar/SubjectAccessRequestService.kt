package uk.gov.justice.digital.hmpps.officialvisitsapi.service.sar

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sar.SarEvent
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sar.SarVisit
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sar.SarVisitor
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sar.SubjectAccessResponseData
import uk.gov.justice.digital.hmpps.officialvisitsapi.repository.AuditedEventRepository
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
  private val auditedEventRepository: AuditedEventRepository,
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
      val auditEvents = auditedEventRepository.findAllByOfficialVisitId(visit.officialVisitId)

      val prisonerVisited = prisonerVisitedRepository.findByOfficialVisit(visit)

      SarVisit(
        officialVisitId = visit.officialVisitId,
        prisonCode = visit.prisonCode,
        prisonerNumber = visit.prisonerNumber,
        visitDate = visit.visitDate,
        startTime = visit.startTime,
        endTime = visit.endTime,
        dpsLocationId = visit.dpsLocationId,
        visitType = visit.visitTypeCode,
        visitStatus = visit.visitStatusCode,
        completionCode = visit.completionCode,
        prisonerAttendance = prisonerVisited?.attendanceCode,
        staffNotes = visit.staffNotes,
        prisonerNotes = visit.prisonerNotes,
        visitors = visit.officialVisitors().map { visitor ->
          SarVisitor(
            officialVisitorId = visitor.officialVisitorId,
            firstName = visitor.firstName,
            lastName = visitor.lastName,
            relationshipType = visitor.relationshipTypeCode,
            relationshipCode = visitor.relationshipCode,
            relationshipDescription = "TODO - lookup relationship description",
            visitorNotes = visitor.visitorNotes,
            visitorAttendance = visitor.attendanceCode,
          )
        },
        events = auditEvents.map { event ->
          SarEvent(
            auditedEventId = event.auditedEventId,
            eventDateTime = event.eventDateTime,
            eventType = event.summaryText,
            eventDescription = event.detailText,
            staffCode = event.userName,
            staffName = event.userFullName,
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
