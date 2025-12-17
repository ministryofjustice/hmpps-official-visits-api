package uk.gov.justice.digital.hmpps.officialvisitsapi.mapping.sync

import org.springframework.data.domain.Page
import uk.gov.justice.digital.hmpps.officialvisitsapi.model.response.sync.SyncOfficialVisitId

fun Long.toModelIds(): SyncOfficialVisitId = SyncOfficialVisitId(officialVisitId = this)
fun Page<Long>.toModelIds(): Page<SyncOfficialVisitId> = map { it.toModelIds() }
