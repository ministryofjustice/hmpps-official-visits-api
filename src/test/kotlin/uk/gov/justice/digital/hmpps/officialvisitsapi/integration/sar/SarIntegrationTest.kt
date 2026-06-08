package uk.gov.justice.digital.hmpps.officialvisitsapi.integration.sar

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.officialvisitsapi.integration.TestConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import java.time.LocalDate
import javax.sql.DataSource

/**
 * These tests check that the SAR endpoint returns the expected data and that the SAR template
 * is rendered and contains the correct information.
 */

@Import(TestConfiguration::class, SarIntegrationTestHelperConfig::class)
class SarIntegrationTest :
  IntegrationTestBase(),
  SarFlywaySchemaTest,
  SarJpaEntitiesTest,
  SarApiDataTest,
  SarReportTest {

  @Autowired
  lateinit var dataSource: DataSource

  @Autowired
  lateinit var entityManager: EntityManager

  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  override fun getDataSourceInstance(): DataSource = dataSource

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

  override fun getEntityManagerInstance(): EntityManager = entityManager

  override fun setupTestData() {}

  override fun getWebTestClientInstance(): WebTestClient = webTestClient

  override fun getPrn(): String? = "A4567AZ"

  // fix the 'to' date because we currently use it in the template and the test lib
  // defaults to "current date" which means the date in the expected output will keep
  // changing, causing the comparison with the baseline report to fail
  override fun getToDate(): LocalDate? = LocalDate.parse("2026-03-03")

  @Test
  @Sql("classpath:sar/truncate-tables-with-sar-data.sql", "classpath:sar/seed-sar-data.sql")
  override fun `SAR API should return expected data`() {
    super.`SAR API should return expected data`()
  }

  @Test
  @Sql("classpath:sar/truncate-tables-with-sar-data.sql", "classpath:sar/seed-sar-data.sql")
  override fun `SAR report should render as expected`() {
    super.`SAR report should render as expected`()
  }
}
