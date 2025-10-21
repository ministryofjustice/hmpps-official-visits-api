package uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.migrate

/**
 * Describes the valid type values for an IdPair object
 */
enum class ElementType(val elementType: String) {
  PRISON_VISIT_SLOT("VisitSlot"),
}
