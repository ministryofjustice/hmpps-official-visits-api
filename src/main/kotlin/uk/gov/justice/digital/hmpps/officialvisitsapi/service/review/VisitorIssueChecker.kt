package uk.gov.justice.digital.hmpps.officialvisitsapi.service.review

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.officialvisitsapi.config.StringFeature
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.IssueType
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ContactsService

@Component
class VisitorIssueChecker(
  private val contactsService: ContactsService,
  private val featureSwitches: FeatureSwitches,
) {
  private val socialPrisons = featureSwitches.getValue(StringFeature.FEATURE_ALLOW_SOCIAL_VISITORS_PRISONS, null)?.split(',')?.toSet() ?: emptySet()

  fun checkVisitorIssues(officialVisit: OfficialVisitEntity): Set<Issue> {
    val prisonerNumber = officialVisit.prisonerNumber
    val contactsById = contactsService.getAllPrisonerContacts(prisonerNumber, approved = null, currentTerm = true)
      .associateBy { it.contactId }

    val visitors = officialVisit.officialVisitors().map { Visitor(it.contactId, it.firstName, it.lastName) }

    val issues = buildSet {
      var anyVisitorMissingRelationship = false
      visitors.forEach { visitor ->
        val contact = contactsById[visitor.contactId]
        if (contact == null) {
          anyVisitorMissingRelationship = true
          return@forEach
        }

        if (contact.relationshipTypeCode == "S" && !socialPrisons.contains(officialVisit.prisonCode)) {
          add(
            Issue(
              officialVisit.officialVisitId,
              IssueType.VISITOR_NOT_OFFICIAL,
              "Visitor ${visitor.fullName()} has a social relationship with prisoner $prisonerNumber",
            ),
          )
        }

        if (!contact.isApprovedVisitor) {
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
