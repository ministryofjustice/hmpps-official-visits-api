package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.StringFeature
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ContactsService

@Service
class VisitorIssueChecker(
  private val contactsService: ContactsService,
  private val featureSwitches: FeatureSwitches,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  private val socialPrisons = featureSwitches.getValue(StringFeature.FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS, null)?.split(',')?.toSet() ?: emptySet()

  fun checkVisitorIssues(officialVisit: OfficialVisitEntity): Set<Issue> {
    val prisonerNumber = officialVisit.prisonerNumber

    val contactsById = contactsService.getAllPrisonerContacts(prisonerNumber, approved = null, currentTerm = true)
      .associateBy { it.contactId }

    val visitors = officialVisit.officialVisitors().map { Visitor(it.contactId, it.firstName, it.lastName) }

    logger.info("checkVisitorIssues: all contacts is size ${contactsById.size} for ${officialVisit.prisonerNumber} on ${officialVisit.officialVisitId}")

    val issues = buildSet {
      var anyVisitorMissingRelationship = false
      visitors.forEach { visitor ->

        // TODO - these are not Contacts, but PrisonerContacts i.e. relationships - rename
        val contact = contactsById[visitor.contactId]
        if (contact == null) {
          logger.info("checkVisitorIssues: Missing relationship for ${visitor.fullName()} on visit ${officialVisit.prisonerNumber} on ${officialVisit.officialVisitId}")
          anyVisitorMissingRelationship = true
          return@forEach
        }

        logger.info("checkVisitorIssues: Checking ${contact.relationshipTypeCode} against 'S' and ${officialVisit.prisonCode} against $socialPrisons for ${visitor.fullName()} on visit ${officialVisit.prisonerNumber} on ${officialVisit.officialVisitId}")

        if (contact.relationshipTypeCode == "S" && !socialPrisons.contains(officialVisit.prisonCode)) {
          logger.info("checkVisitorIssues: Social visitor detected for ${visitor.fullName()} on visit ${officialVisit.prisonerNumber} on ${officialVisit.officialVisitId}")
          add(
            Issue(
              officialVisit.officialVisitId,
              IssueType.VISITOR_NOT_OFFICIAL,
              "Visitor ${visitor.fullName()} has a social relationship with prisoner $prisonerNumber",
            ),
          )
        }

        logger.info("checkVisitorIssues: Checking approval ${contact.isApprovedVisitor} for ${visitor.fullName()} on visit ${officialVisit.prisonerNumber} on ${officialVisit.officialVisitId}")

        if (!contact.isApprovedVisitor) {
          logger.info("checkVisitorIssues: Detected unapproved visitor ${visitor.fullName()} on visit ${officialVisit.prisonerNumber} on ${officialVisit.officialVisitId}")
          add(
            Issue(
              officialVisit.officialVisitId,
              IssueType.VISITOR_NOT_APPROVED,
              "Visitor ${visitor.fullName()} is not approved to visit prisoner $prisonerNumber",
            ),
          )
        }
      }

      if (anyVisitorMissingRelationship) {
        logger.info("checkVisitorIssues: Adding missing relationship issue to visit ${officialVisit.prisonerNumber} on ${officialVisit.officialVisitId}")
        add(
          Issue(
            officialVisit.officialVisitId,
            IssueType.VISITOR_NO_RELATIONSHIP,
            "One or more visitors are not in a relationship with prisoner $prisonerNumber",
          ),
        )
      }
    }

    return issues
  }

  data class Visitor(
    val contactId: Long?,
    val firstName: String?,
    val lastName: String?,
  ) {
    fun fullName() = "${firstName.orEmpty()} ${lastName.orEmpty()}".trim()
  }

  data class Issue(
    val visitId: Long,
    val issueType: IssueType,
    val issueDescription: String,
  )
}
