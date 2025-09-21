plugins {
    kotlin("jvm") version "2.2.0"
    application
}

group = "com.dutscher.web.engine"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.microsoft.playwright:playwright:1.55.0") // Pour parcourir les sites (comme selenium)
    implementation("com.squareup.okhttp3:okhttp:5.1.0") // Pour lancer des liens http
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1") // Pour charger les vars depuis les fichiers .env
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
