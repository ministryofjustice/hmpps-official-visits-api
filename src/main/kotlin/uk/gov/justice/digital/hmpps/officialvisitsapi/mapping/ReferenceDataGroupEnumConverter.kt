package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping

import org.springframework.core.convert.converter.Converter
import uk.gov.justice.digital.hmpps.officialvisitsapi.exception.InvalidReferenceDataGroupException
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.ReferenceDataGroup

class ReferenceDataGroupEnumConverter : Converter<String, ReferenceDataGroup> {
  override fun convert(source: String): ReferenceDataGroup = ReferenceDataGroup.entries.find { it.name === source } ?: throw InvalidReferenceDataGroupException(source)
}
