package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

fun today(): LocalDate = LocalDate.now()

fun tomorrow(): LocalDate = today().plusDays(1)

fun now(): LocalDateTime = LocalDateTime.now()

fun LocalDate.next(dayOfWeek: DayOfWeek): LocalDate = this.with(TemporalAdjusters.next(dayOfWeek))
