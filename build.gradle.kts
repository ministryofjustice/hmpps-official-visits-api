import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.5.7"
  id("org.openapi.generator") version "7.23.0"
  kotlin("plugin.spring") version "2.4.0"
  kotlin("plugin.jpa") version "2.4.0"
}

dependencies {
  // HMPPS dependencies
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.5.0")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.4.0")

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.springframework.boot:spring-boot-starter-validation")

  // OpenAPI dependencies
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:3.0.3")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
  implementation("org.springdoc:springdoc-openapi-starter-common:3.0.3")
  constraints {
    implementation("org.webjars:swagger-ui:5.32.2")
  }

  // Postgresql dependencies
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.12")

  // Open telemetry dependencies
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.29.0")

  // Gov Notify client
  implementation("uk.gov.service.notify:notifications-java-client:6.0.1-RELEASE")

  // Test dependencies
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:2.5.0")
  testImplementation("org.springframework.security:spring-security-test:7.0.2")
  testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
  testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")

  // SAR test library
  testImplementation("uk.gov.justice.service.hmpps:hmpps-subject-access-request-test-support:2.4.3")

  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")

  // JSON web token
  testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.13.0")

  // JUnit
  testImplementation("net.javacrumbs.json-unit:json-unit:6.0.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:5.1.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-json-path:5.1.1")

  // Mockito
  testImplementation("org.mockito:mockito-inline:5.2.0")

  // Test containers
  testImplementation("org.testcontainers:postgresql:1.21.4")

  // Wiremock
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")

  // Swagger
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.43") {
    exclude(group = "io.swagger.core.v3")
  }
}

kotlin {
  jvmToolchain(25)
}

tasks {
  withType<KotlinCompile> {
    dependsOn("buildLocationsInsidePrisonApiModel", "buildManageUsersApiModel", "buildPersonalRelationshipsApiModel")
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
    compilerOptions.freeCompilerArgs.add("-Xannotation-default-target=param-property")
  }
}

val configValues = mapOf(
  "dateLibrary" to "java8-localdatetime",
  "serializationLibrary" to "jackson",
  "enumPropertyNaming" to "original",
)

val buildDirectory: Directory = layout.buildDirectory.get()

tasks.register("buildLocationsInsidePrisonApiModel", GenerateTask::class) {
  generatorName.set("kotlin")
  inputSpec.set("openapi-specs/locations-inside-prison-api.json")
  outputDir.set("$buildDirectory/generated/locationsinsideprisonapi")
  modelPackage.set("uk.gov.justice.digital.hmpps.officialvisitsapi.client.locationsinsideprison.model")
  configOptions.set(configValues)
  globalProperties.set(mapOf("models" to ""))
}

tasks.register("buildManageUsersApiModel", GenerateTask::class) {
  generatorName.set("kotlin")
  inputSpec.set("openapi-specs/manage-users-api.json")
  outputDir.set("$buildDirectory/generated/manageusersapi")
  modelPackage.set("uk.gov.justice.digital.hmpps.officialvisitsapi.client.manageusers.model")
  configOptions.set(configValues)
  globalProperties.set(mapOf("models" to ""))
}

tasks.register("buildPersonalRelationshipsApiModel", GenerateTask::class) {
  generatorName.set("kotlin")
  inputSpec.set("openapi-specs/personal-relationships-api.json")
  outputDir.set("$buildDirectory/generated/personalrelationships")
  modelPackage.set("uk.gov.justice.digital.hmpps.officialvisitsapi.client.personalrelationships.model")
  configOptions.set(configValues)
  globalProperties.set(mapOf("models" to ""))
}

val generatedProjectDirs = listOf("locationsinsideprisonapi", "manageusersapi", "personalrelationships")

tasks.register("integrationTest", Test::class) {
  description = "Runs integration tests"
  group = "verification"
  testClassesDirs = sourceSets["test"].output.classesDirs
  classpath = sourceSets["test"].runtimeClasspath

  useJUnitPlatform {
    filter {
      includeTestsMatching("*.integration.*")
    }
  }

  shouldRunAfter("test")
  maxHeapSize = "2048m"
}
tasks.named<Test>("test") {
  filter {
    excludeTestsMatching("*.integration.*")
  }
}

kotlin {
  generatedProjectDirs.forEach { generatedProject ->
    sourceSets["main"].apply {
      kotlin.srcDir("$buildDirectory/generated/$generatedProject/src/main/kotlin")
    }
  }
}

tasks.named("runKtlintCheckOverMainSourceSet") {
  dependsOn("buildLocationsInsidePrisonApiModel", "buildManageUsersApiModel", "buildPersonalRelationshipsApiModel")
}

configure<KtlintExtension> {
  filter {
    generatedProjectDirs.forEach { generatedProject ->
      exclude { element ->
        element.file.path.contains("build/generated/$generatedProject/src/main/")
      }
    }
  }
}
