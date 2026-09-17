package uk.gov.justice.digital.hmpps.officialvisitsapi.client.prisonersearch.model

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.officialvisitsapi.helper.isEqualTo
import kotlin.reflect.full.declaredMemberProperties

class PrisonerTest {
  /**
   * This test has been added to cover an issue with a bug in the Prisoner Search API specification which we have manually fixed.
   *
   * In this case the smoker field on Prisoner was an enum which was causing deserialization issues. We have manually fixed this by changing the field in the spec to a string.
   *
   * You see the (temporary) change in the PR: https://github.com/ministryofjustice/hmpps-official-visits-api/pull/303
   */
  @Test
  fun `should be of type String for the smoker field`() {
    val smokerField = Prisoner::class.declaredMemberProperties.single { it.name == "smoker" }

    smokerField.returnType.classifier isEqualTo String::class
  }
}
