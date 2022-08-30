import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion = "1.7.10"
val logbackVersion = "1.2.11"
val fuelVersion = "2.3.1"
val kotlinLoggingVersion = "2.1.23"
val arkenvVersion = "3.3.3"

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    application
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
}

application {
    mainClassName = "ApplicationKt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

sourceSets {
    val main by getting
    main.java.srcDirs("src")
    main.resources.srcDirs("resources")

    val test by getting
    test.java.srcDirs("test")
    test.resources.srcDirs("testresources")
}

repositories {
    mavenCentral()
    maven("https://kotlin.bintray.com/ktor")
    maven("https://dl.bintray.com/serpro69/maven/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8", kotlinVersion))

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

    implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
    implementation("com.github.kittinunf.fuel:fuel-jackson:$fuelVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.13.3")
    implementation("com.apurebase:arkenv:$arkenvVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("zs-reporter")
        archiveVersion.set("")
        archiveClassifier.set("")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to application.mainClassName))
        }
    }

    build {
        dependsOn(shadowJar)
    }

    val stage by registering {
        dependsOn("build")
    }
}