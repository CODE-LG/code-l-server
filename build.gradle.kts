plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.25"
}

group = "codel"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // db
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")

    // test
    testImplementation("io.rest-assured:rest-assured:5.3.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // jwt
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.4")

    // s3
    implementation("software.amazon.awssdk:s3:2.20.148")

    // monitoring
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // fcm
    implementation("com.google.firebase:firebase-admin:9.4.3")

    // logging
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.1")
    implementation("com.github.loki4j:loki-logback-appender:1.4.0")

    // web
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // web socket
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // redisson (분산 락)
    implementation("org.redisson:redisson-spring-boot-starter:3.27.2")

    // shedlock (스케줄러 중복 실행 방지)
    implementation("net.javacrumbs.shedlock:shedlock-spring:5.12.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.12.0")

    // caffeine cache (메시지 중복 방지)
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
