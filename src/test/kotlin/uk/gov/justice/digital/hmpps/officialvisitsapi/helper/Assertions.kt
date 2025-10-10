package uk.gov.justice.digital.hmpps.officialvisitsapi.helper

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal inline infix fun <reified T> Collection<T>.containsExactlyInAnyOrder(value: Collection<T>) {
  assertThat(this).containsExactlyInAnyOrder(*value.toTypedArray())
}

internal inline infix fun <reified T> Collection<T>.containsExactly(value: Collection<T>) {
  assertThat(this).containsExactly(*value.toTypedArray())
}

internal inline infix fun <reified K, V> Map<K, V>.containsEntriesExactlyInAnyOrder(value: Map<K, V>) {
  assertThat(this).containsExactlyInAnyOrderEntriesOf(value)
}

internal inline infix fun <reified K, V> Map<K, V>.containsEntry(entry: Pair<K, V>) {
  assertThat(this).containsEntry(entry.first, entry.second)
}

internal infix fun <T> Collection<T>.hasSize(size: Int) {
  assertThat(this).hasSize(size)
}

internal infix fun Boolean.isBool(value: Boolean) {
  assertThat(this).isEqualTo(value)
}

internal infix fun LocalDateTime?.isCloseTo(dateTime: LocalDateTime) {
  assertThat(this).isCloseTo(dateTime, Assertions.within(2, ChronoUnit.SECONDS))
}

internal infix fun <T> T.isEqualTo(value: T) {
  assertThat(this).isEqualTo(value)
}

internal infix fun <T> T.isNotEqualTo(value: T) {
  assertThat(this).isNotEqualTo(value)
}

internal infix fun String.contains(value: String) {
  assertThat(this).contains(value)
}

internal infix fun <T> T.isInstanceOf(value: Class<*>) {
  assertThat(this).isInstanceOf(value)
}
