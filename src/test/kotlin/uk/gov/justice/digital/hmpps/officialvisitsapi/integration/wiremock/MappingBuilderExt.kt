package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.equalTo

fun MappingBuilder.maybeQueryParam(key: String, value: Any?) = apply { if (value != null) withQueryParam(key, equalTo(value.toString())) }
