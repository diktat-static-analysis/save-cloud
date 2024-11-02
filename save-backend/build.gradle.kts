import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.spring-data-configuration")
    id("com.saveourtool.save.buildutils.save-cli-configuration")
    id("com.saveourtool.save.buildutils.save-agent-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    // this plugin will generate generateOpenApiDocs task
    // running this task, it will write the OpenAPI spec into a backend-api-docs.json file in save-backend dir.
    id("org.springdoc.openapi-gradle-plugin") version "1.8.0"
}

openApi {
    apiDocsUrl.set("http://localhost:5800/internal/v3/api-docs/latest")
    outputDir.set(file(projectDir))
    outputFileName.set("backend-api-docs.json")
    waitTimeInSeconds.set(120)

    customBootRun {
        jvmArgs.add("-Dbackend.test-analysis-settings.replay-on-startup=false")
        jvmArgs.add("-Dbackend.s3-storage.createBucketIfNotExists=false")
        args.add("--debug")
    }
}

// a workaround for https://github.com/springdoc/springdoc-openapi-gradle-plugin/issues/102
project.afterEvaluate {
    tasks.findByName("inspectClassesForKotlinIC")
        ?.let { incrementalTask ->
            tasks.named("forkedSpringBootRun") {
                mustRunAfter("jar", incrementalTask)
            }
        }
}

tasks.named("processTestResources") {
    dependsOn("copyLiquibase")
}

tasks.register<Copy>("copyLiquibase") {
    from("$rootDir/db")
    into("$buildDir/resources/test/db")
}

dependencies {
    implementation(projects.common)
    implementation(projects.authenticationService)
    implementation(projects.testAnalysisCore)
    implementation(libs.save.common.jvm)
    implementation(libs.spring.boot.starter.quartz)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.core)
    implementation(libs.hibernate.micrometer)
    implementation(libs.spring.cloud.starter.kubernetes.client.config) {
        because("needed for kubernetes configs and secrets to be added as additional configurations")
    }
    implementation(libs.reactor.extra)
    implementation(libs.arrow.kt.core)
    implementation(project.dependencies.platform(libs.aws.sdk.bom))
    implementation(libs.aws.sdk.s3)
    implementation(libs.aws.sdk.netty.nio)
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.apache)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(projects.testUtils)
}

tasks.withType<Test> {
    extensions.configure(JacocoTaskExtension::class) {
        // this file is only used in dev profile for debugging, no need to calculate test coverage
        excludes = listOf("**/CorsFilter.kt")
    }
}
