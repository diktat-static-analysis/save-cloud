import com.saveourtool.save.buildutils.configurePublishing
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.plugin.serialization)
    `maven-publish`
    id("com.saveourtool.save.buildutils.detekt-common")
    id("com.saveourtool.save.buildutils.diktat-common")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(Versions.jdk))
    }
}

java {
    withSourcesJar()
}

dependencies {
    implementation(projects.saveCloudCommon)
    implementation(libs.save.common.jvm)
    implementation(libs.log4j)
    implementation(libs.log4j.slf4j.impl)
    implementation(libs.ktor.client.apache)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.serialization)
    implementation(libs.ktor.client.logging)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.saveourtool.save"
            artifactId = "save-cloud-api"
            version = version
            from(components["java"])
        }
    }
}

configurePublishing()
