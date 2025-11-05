package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

import java.time.LocalDate
import java.time.LocalDateTime

fun today(): LocalDate = LocalDate.now()

fun now(): LocalDateTime = LocalDateTime.now()
