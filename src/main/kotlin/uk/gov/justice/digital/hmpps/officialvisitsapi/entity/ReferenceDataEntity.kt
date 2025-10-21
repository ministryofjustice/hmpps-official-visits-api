package uk.gov.justice.digital.hmpps.officialvisitsapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.ReferenceDataGroup

@Entity
@Table(name = "reference_data")
data class ReferenceDataEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val referenceDataId: Long = 0,

  @Enumerated(EnumType.STRING)
  val groupCode: ReferenceDataGroup,

  val code: String,

  val description: String,

  val displaySequence: Int,

  val enabled: Boolean,
)
