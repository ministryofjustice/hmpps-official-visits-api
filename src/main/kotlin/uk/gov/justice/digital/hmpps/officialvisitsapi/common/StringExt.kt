package uk.gov.justice.digital.hmpps.officialvisitsapi.common

import java.util.Locale

fun String.toTitleCase() = lowercase().split(" ").joinToString(separator = " ") { word -> word.replaceFirstChar { it.titlecase(Locale.getDefault()) } }
