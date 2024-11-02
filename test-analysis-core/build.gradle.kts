import com.saveourtool.save.buildutils.configureSigning
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    id("com.saveourtool.save.buildutils.publishing-configuration")
}

java {
    withSourcesJar()
}

dependencies {
    /*
     * Necessary, because otherwise Gradle will try to resolve "org.slf4j:slf4j-api:."
     * (the transitive dependency of "projects.saveCloudCommon")
     * and fail to find the undefined version.
     */
    implementation(project.dependencies.platform(libs.spring.boot.dependencies))
    api(projects.common)

    testApi(libs.assertj.core)
    testApi(libs.mockito.kotlin)
    testApi(libs.mockito.junit.jupiter)
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
        includeTestsMatching("com.saveourtool.save.test.analysis.*")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

configureSigning()
