package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ReferenceCode
import uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.OfficialVisitorEntity
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.RelationshipType
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.VisitorType
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.PersonalRelationshipsReferenceDataService
import uk.gov.justice.digital.hmpps.officialvisitsapi.service.ReferenceDataService
import java.time.LocalDateTime

class OfficialVisitorDetailsMappingTest {
  private val officialVisit: OfficialVisitEntity = mock()
  private val referenceDataService: ReferenceDataService = mock()
  private val personalRelationshipsReferenceDataService: PersonalRelationshipsReferenceDataService = mock()
  private val referenceCode: ReferenceCode = mock()

  @Test
  fun `should be default relationship description when no relationship code`() {
    val contactInformation = object : VisitorContactInformation {
      override fun phoneNumber(visitorContactId: Long): String = "123456"
      override fun emailAddress(visitorContactId: Long): String = "email_address"
    }

    val visitor = OfficialVisitorEntity(
      officialVisitorId = 1,
      visitorTypeCode = VisitorType.CONTACT,
      contactId = 1,
      prisonerContactId = 1,
      relationshipCode = null,
      leadVisitor = true,
      assistedVisit = true,
      officialVisit = officialVisit,
      firstName = "Fred",
      lastName = "Bloggs",
      relationshipTypeCode = null,
      visitorNotes = "Notes",
      createdBy = "test",
      createdTime = LocalDateTime.now(),
      offenderVisitVisitorId = 1,
    )

    val mapping = visitor.toModel(referenceDataService, personalRelationshipsReferenceDataService, contactInformation)

    mapping.relationshipDescription isEqualTo "No relationship"
    verifyNoInteractions(personalRelationshipsReferenceDataService)
  }

  @Test
  fun `should be populate relationship description from reference data when relationship code present`() {
    val contactInformation = object : VisitorContactInformation {
      override fun phoneNumber(visitorContactId: Long): String = "123456"
      override fun emailAddress(visitorContactId: Long): String = "email_address"
    }

    val visitor = OfficialVisitorEntity(
      officialVisitorId = 1,
      visitorTypeCode = VisitorType.CONTACT,
      contactId = 1,
      prisonerContactId = 1,
      relationshipCode = "RELATED",
      leadVisitor = true,
      assistedVisit = true,
      officialVisit = officialVisit,
      firstName = "Fred",
      lastName = "Bloggs",
      relationshipTypeCode = RelationshipType.OFFICIAL,
      visitorNotes = "Notes",
      createdBy = "test",
      createdTime = LocalDateTime.now(),
      offenderVisitVisitorId = 1,
    )

    whenever { personalRelationshipsReferenceDataService.getReferenceDataByCode(ReferenceCodeGroup.OFFICIAL_RELATIONSHIP.toString(), "RELATED") } doReturn referenceCode
    whenever { referenceCode.description } doReturn "related description"

    val mapping = visitor.toModel(referenceDataService, personalRelationshipsReferenceDataService, contactInformation)

    mapping.relationshipDescription isEqualTo "related description"
    verify(personalRelationshipsReferenceDataService).getReferenceDataByCode(ReferenceCodeGroup.OFFICIAL_RELATIONSHIP.toString(), "RELATED")
  }
}
