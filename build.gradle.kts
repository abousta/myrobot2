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
    implementation("com.microsoft.playwright:playwright:1.55.0") // Pour parcourir les sites
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
