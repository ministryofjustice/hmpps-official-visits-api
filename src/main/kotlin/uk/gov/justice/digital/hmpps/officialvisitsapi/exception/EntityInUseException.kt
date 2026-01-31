package uk.gov.justice.digital.hmpps.officialvisitsapi.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.CONFLICT)
class EntityInUseException(message: String) : RuntimeException(message)
