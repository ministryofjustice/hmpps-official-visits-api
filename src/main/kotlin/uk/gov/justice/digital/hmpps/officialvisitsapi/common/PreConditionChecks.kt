package uk.gov.justice.digital.hmpps.officialvisitsapi.common

fun requireNot(value: Boolean, lazyMessage: () -> Any) = require(!value, lazyMessage)
