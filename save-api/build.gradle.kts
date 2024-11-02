import com.saveourtool.save.buildutils.configureSigning
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    id("com.saveourtool.save.buildutils.publishing-configuration")
    alias(libs.plugins.kotlin.plugin.serialization)
}

java {
    withSourcesJar()
}

dependencies {
    api(projects.common)
    implementation(libs.save.common.jvm)
    implementation(libs.log4j)
    implementation(libs.log4j.slf4j.impl)
    implementation(libs.ktor.client.apache)
    api(libs.ktor.client.auth)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.serialization)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    api(libs.arrow.kt.core)

    testApi(libs.assertj.core)
    testApi(libs.junit.jupiter.api)
    testApi(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        showStandardStreams = true
        showCauses = true
        showExceptions = true
        showStackTraces = true
        exceptionFormat = FULL
        events("passed", "skipped")
    }

    filter {
        includeTestsMatching("com.saveourtool.save.*")
    }
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

configureSigning()
