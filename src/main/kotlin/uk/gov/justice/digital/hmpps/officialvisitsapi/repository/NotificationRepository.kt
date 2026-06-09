package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.NotificationEntity
import java.util.UUID

@Repository
interface NotificationRepository : JpaRepository<NotificationEntity, Long> {
  fun findByGovNotifyNotificationId(govNotifyNotificationId: UUID): NotificationEntity?

  fun findTopByOfficialVisitIdOrderByCreatedTimeDesc(officialVisitId: Long): NotificationEntity?

  fun findByOfficialVisitId(officialVisitId: Long, sort: Sort): List<NotificationEntity>
}
