plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.spring") version "2.4.0"
    id("org.springframework.boot") version "4.1.0"
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
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Playwright
    implementation("com.microsoft.playwright:playwright:1.55.0") // Pour parcourir les sites (comme selenium)
    implementation("com.squareup.okhttp3:okhttp:5.1.0") // Pour lancer des liens http
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1") // Pour charger les vars depuis les fichiers .env
    // Pour lire le CSV
    implementation("com.opencsv:opencsv:5.12.0")
    // Pour SQLite (via JDBC)
    implementation("org.xerial:sqlite-jdbc:3.53.1.0")

    // Jsoup
    implementation("org.jsoup:jsoup:1.22.2")

    // JXL (jusqu'à disparition du excel igam et gestion tva à part)
    implementation("net.sourceforge.jexcelapi:jxl:2.6.12")

    // Test
    testImplementation(kotlin("test"))

}

application {
    mainClass.set("com.abousta.myrobot2.backupconfluence.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(25)
}
