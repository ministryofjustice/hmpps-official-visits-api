import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.3.0"
  id("org.openapi.generator") version "7.16.0"
  kotlin("plugin.spring") version "2.2.20"
  kotlin("plugin.jpa") version "2.2.20"
}

allOpen {
  annotations(
    "javax.persistence.Entity",
    "javax.persistence.MappedSuperclass",
    "javax.persistence.Embeddable",
  )
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.7.0")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.5.0")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.openapitools:jackson-databind-nullable:0.2.7")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.8")

  // OpenAPI dependencies
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")

  // Open telemetry dependencies
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.20.1")

  // Test dependencies
  testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
  testImplementation("net.javacrumbs.json-unit:json-unit:4.1.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-json-path:4.1.1")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:postgresql:1.21.3")
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.5.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.32") {
    exclude(group = "io.swagger.core.v3")
  }
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<KotlinCompile> {
    dependsOn("buildLocationsInsidePrisonApiModel", "buildManageUsersApiModel", "buildPersonalRelationshipsApiModel")
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    compilerOptions.freeCompilerArgs.add("-Xannotation-default-target=param-property")
    compilerOptions.freeCompilerArgs.add("-Xwarning-level=IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE:disabled")
  }
}

val configValues = mapOf(
  "dateLibrary" to "java8-localdatetime",
  "serializationLibrary" to "jackson",
  "enumPropertyNaming" to "original",
  "useSpringBoot3" to "true",
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
