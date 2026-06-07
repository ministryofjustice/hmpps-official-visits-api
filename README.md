# hmpps-official-visits-api

[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-official-visits-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-official-visits-api "Link to report")
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-official-visits-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://official-visits-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)

HMPPS Official Visits API

# Policies

Our security policy is located [here](https://github.com/ministryofjustice/hmpps-official-visits-api/security/policy).

# Instructions for building the project

Initialise gradle - only required the first time

```bash
./gradlew
```

Clean and build, and run unit and integration tests
```bash
./gradlew clean build test
```

## Running the application locally

The application comes with a `local` spring profile that includes default settings for running locally. 

Setting a profile is not necessary when deploying to kubernetes as these values are included in the helm configuration templates -
e.g. `values-dev.yaml`, `values-preprod.yaml` and `values-prod.yaml`

There is a script `run-local.sh` which sets up environment variables required to run locally, which override the value 
defaults, providing the local equivalents for values provided by Kubernetes secrets. You should not include any sensitive
values in the `run-local.sh`, so it should pull these in from your environment, or a .env file locally.

There is also a `docker-compose.yml` that can be used to start the dependencies of the project as local docker
containers. This will pull (if required), and start a local postgresql container in docker. 

```bash
docker compose pull && docker compose up -d
```

Then run the service with the script:

```bash
./run-local.sh
```
# Changes to SAR template or database schema

If you are making any changes to the SAR .mustache template, you'll need to regenerate the sar-generated-report.html test file which records 
what the result of the SAR report generation process should look like. This allows the integration test to run the SAR generation process and 
compare the output to this file.

To generate the file, run the integration test with the below env variable set to true:
SAR_GENERATE_ACTUAL=true

e.g.
SAR_GENERATE_ACTUAL=true ./gradlew integrationTest --tests "uk.gov.justice.digital.hmpps.officialvisitsapi.integration.sar.SarIntegrationTest"

This will output a new file sar-generated-file.html.log into the src/test/resources folder, generate a new schema file, and generate an API response file.

Rename these files to remove the .log extension and copy them into the src/test/resources/sar directory to act as the new templates for comparison.

If the database schema is updated via Flyway, this will also be detected by the SAR tests. To enable this tests to pass, you will need to 
make sure that the latest Flyway schema version is reflected in the properties within `application-test.yaml`

    hmpps:
      sar:
        tests:
          expected-flyway-schema-version: 2026.05.28
          expected-jpa-entity-schema:
            path: /sar/entity-schema.json
          expected-api-response:
            path: /sar/sar-api-response.json
          expected-render-result:
            path: /sar/sar-generated-report.html

[Further info here](https://github.com/ministryofjustice/hmpps-subject-access-request-lib/blob/main/README.md)