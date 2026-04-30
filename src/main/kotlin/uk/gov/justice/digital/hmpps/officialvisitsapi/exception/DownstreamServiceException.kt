package uk.gov.justice.digital.hmpps.officialvisitsapi.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.REQUEST_TIMEOUT)
class DownstreamServiceException(message: String) : RuntimeException(message)
