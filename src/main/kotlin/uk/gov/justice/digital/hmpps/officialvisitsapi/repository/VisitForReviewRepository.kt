package uk.gov.justice.digital.hmpps.officialvisitsapi.repository

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.officialvisitsapi.entity.VisitForReviewEntity

/**
 * This repository is read-only and accessed via the view v_visits_for_review.
 */
@Repository
interface VisitForReviewRepository : ReadOnlyRepository<VisitForReviewEntity, Long>
