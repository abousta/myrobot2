plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    application
}

group = "com.dutscher.web.engine"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Spring (juste controllers et juste jdbcTemplate)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    // Json
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    // Playwright
    implementation("com.microsoft.playwright:playwright:1.55.0") // Pour parcourir les sites (comme selenium)
    implementation("com.squareup.okhttp3:okhttp:5.1.0") // Pour lancer des liens http
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1") // Pour charger les vars depuis les fichiers .env
    // Pour lire le CSV
    implementation("com.opencsv:opencsv:5.12.0")
    // Pour SQLite (via JDBC)
    implementation("org.xerial:sqlite-jdbc:3.53.1.0")
    testImplementation(kotlin("test"))

}

application {
    mainClass.set("com.abousta.myrobot2.backupconfluence.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
