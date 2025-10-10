package uk.gov.justice.digital.hmpps.officialvisitsapi.common

/**
 * Conversion is not guaranteed, only use when you know (or at least expect) all types to be the same type.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> List<*>.asListOfType() = this as List<T>
