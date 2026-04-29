// ─────────────────────────────────────────────────────────────────────────────
// build.gradle.kts  —  Kotlin DSL Gradle build file
//
// This replaces pom.xml entirely. Every dependency and plugin here is the
// exact Gradle equivalent of what was in the Maven pom.xml.
//
// WHY GRADLE OVER MAVEN?
//   • Shorter, less XML noise
//   • Faster incremental builds (Gradle caches task outputs intelligently)
//   • Kotlin DSL gives you autocomplete in IntelliJ
//   • Same Spring Boot ecosystem — nothing changes in your Java code
//
// KEY GRADLE CONCEPTS:
//   plugins {}      — add build capabilities (Spring Boot, dependency management)
//   dependencies {} — declare your libraries
//   configurations  — scopes: implementation, testImplementation, runtimeOnly
//   tasks {}        — customise or create build steps
//
// COMMON COMMANDS (equivalent Maven commands in comments):
//   ./gradlew bootRun               ← mvn spring-boot:run
//   ./gradlew test                  ← mvn test
//   ./gradlew build                 ← mvn package
//   ./gradlew build -x test         ← mvn package -DskipTests
//   ./gradlew dependencies          ← mvn dependency:tree
//   ./gradlew jacocoTestReport      ← mvn jacoco:report
// ─────────────────────────────────────────────────────────────────────────────

plugins {
    // Spring Boot plugin: adds bootRun, bootJar tasks and manages versions
    id("org.springframework.boot") version "3.2.4"

    // Lets Spring Boot's BOM (Bill of Materials) manage all dependency versions
    // so you never have to specify versions for Spring libraries manually
    id("io.spring.dependency-management") version "1.1.4"

    // Standard Java plugin — compiles .java files, runs tests, builds JARs
    java

    // JaCoCo — generates test coverage reports
    // Run: ./gradlew test jacocoTestReport
    // Report at: build/reports/jacoco/test/html/index.html
    jacoco
}

group   = "com.nightout"
version = "0.0.1-SNAPSHOT"

// ── Java version ──────────────────────────────────────────────────────────────
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// ── Repositories — where Gradle downloads libraries from ─────────────────────
repositories {
    mavenCentral()
}

// ── Dependencies ──────────────────────────────────────────────────────────────
//
// Scopes explained:
//   implementation    — needed to compile AND run your code (on the classpath)
//   runtimeOnly       — only needed at runtime, not at compile time
//                       (e.g. JDBC drivers: you code against javax.sql.DataSource,
//                        not the driver directly)
//   testImplementation — only on the test classpath
//   annotationProcessor — compile-time only (Lombok generates code during compilation)
//
// Note: because we use the Spring Boot dependency-management plugin, most Spring
// libraries don't need explicit versions — the BOM pins them to compatible versions.

dependencies {

    // ── Web ───────────────────────────────────────────────────────────────────
    // Spring MVC + embedded Tomcat. Powers all your @RestController endpoints.
    implementation("org.springframework.boot:spring-boot-starter-web")

    // ── Database ──────────────────────────────────────────────────────────────
    // Spring Data JPA + Hibernate. Turns @Entity classes into database tables.
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // PostgreSQL JDBC driver — runtimeOnly because you code against javax.sql,
    // not the driver class directly
    runtimeOnly("org.postgresql:postgresql")

    // H2 in-memory database — used only in the 'test' Spring profile
    runtimeOnly("com.h2database:h2")

    // ── Security ──────────────────────────────────────────────────────────────
    // Spring Security — protects endpoints, manages authentication/authorisation
    implementation("org.springframework.boot:spring-boot-starter-security")

    // ── Validation ────────────────────────────────────────────────────────────
    // Bean Validation — powers @NotBlank, @Email, @Size, @Valid on your DTOs
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // ── Cache (Redis) ─────────────────────────────────────────────────────────
    // Spring Data Redis — backs the @Cacheable annotation with Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // ── Monitoring ────────────────────────────────────────────────────────────
    // Actuator — exposes /actuator/health, /actuator/metrics, /actuator/prometheus
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Micrometer Prometheus registry — formats metrics for Prometheus scraping
    implementation("io.micrometer:micrometer-registry-prometheus")

    // ── JWT ───────────────────────────────────────────────────────────────────
    // JJWT — create and validate JSON Web Tokens
    // Split into api (compile-time), impl and jackson (runtime only)
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // ── Lombok ────────────────────────────────────────────────────────────────
    // Generates boilerplate (getters, setters, builders, constructors) at compile time.
    // annotationProcessor = runs during javac, not shipped in the final JAR
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // ── Testing ───────────────────────────────────────────────────────────────
    // spring-boot-starter-test bundles: JUnit 5 + Mockito + AssertJ + MockMvc
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Spring Security test utilities — @WithMockUser, SecurityMockMvcRequestPostProcessors
    testImplementation("org.springframework.security:spring-security-test")

    // Lombok also needed in tests (test classes use @Slf4j etc.)
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

// ── Test configuration ────────────────────────────────────────────────────────
tasks.withType<Test> {
    // Tell Gradle to use JUnit 5 (JUnit Platform) as the test runner
    useJUnitPlatform()

    // Always run tests even if nothing changed (useful in CI)
    // Comment this out locally for faster feedback loops
    // outputs.upToDateWhen { false }

    // Show test results in the console while running
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

    // Generate JaCoCo coverage data after tests finish
    finalizedBy(tasks.jacocoTestReport)
}

// ── JaCoCo coverage report ────────────────────────────────────────────────────
tasks.jacocoTestReport {
    // Only run after tests have completed
    dependsOn(tasks.test)

    reports {
        // HTML report — open in browser for a visual coverage breakdown
        html.required.set(true)
        // XML report — used by CI tools (SonarQube, Codecov, etc.)
        xml.required.set(true)
        // CSV — optional, useful for quick scripting
        csv.required.set(false)
    }
}

// ── JaCoCo coverage enforcement ───────────────────────────────────────────────
// Fails the build if coverage drops below the threshold.
// The project requirement is 70% for the service layer.
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            // Apply the rule to all classes under com.nightout.service
            element = "PACKAGE"
            includes = listOf("com.nightout.service.*")
            limit {
                // LINE coverage must be at least 70%
                counter = "LINE"
                value   = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

// ── Spring Boot JAR configuration ─────────────────────────────────────────────
tasks.bootJar {
    // The fat JAR filename: nightout.jar (instead of nightout-0.0.1-SNAPSHOT.jar)
    archiveFileName.set("nightout.jar")
}

// Disable the plain JAR task — Spring Boot projects only need the fat bootJar
tasks.jar {
    enabled = false
}