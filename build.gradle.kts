import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val kotlinVersion = "1.3.70"
val logbackVersion = "1.2.3"
val fuelVersion = "2.2.2"
val kotlinLoggingVersion = "1.7.6"
val konfigVersion = "1.6.10.0"

buildscript {
    repositories {
        jcenter()
    }
}

plugins {
    application
    id("com.github.johnrengelman.shadow") version "5.1.0"
    id("org.jetbrains.kotlin.jvm") version "1.3.70"
}

application {
    mainClassName = "ApplicationKt"
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
    jcenter()
    maven("https://kotlin.bintray.com/ktor")
    maven("https://dl.bintray.com/serpro69/maven/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8", kotlinVersion))

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

    implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
    implementation("com.github.kittinunf.fuel:fuel-jackson:$fuelVersion")

    implementation("com.natpryce:konfig:$konfigVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.+")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("tm4j-allure-reporter")
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