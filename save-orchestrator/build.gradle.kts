import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

dependencies {
    api(projects.common)
    implementation(projects.saveOrchestratorCommon)
    implementation(libs.save.common.jvm)
    implementation(libs.dockerJava.core)
    implementation(libs.dockerJava.transport.httpclient5)
    implementation(libs.kotlinx.serialization.json.jvm)
    implementation(libs.commons.compress)
    implementation(libs.kotlinx.datetime)
    implementation(libs.zip4j)
    implementation(libs.spring.cloud.starter.kubernetes.client.config) {
        because("needed for kubernetes configs and secrets to be added as additional configurations")
    }
    implementation(libs.fabric8.kubernetes.client) {
        exclude("org.slf4j", "slf4j-api")
    }
    implementation(libs.spring.kafka)
    testImplementation(projects.testUtils)
    testImplementation(libs.fabric8.kubernetes.server.mock)
}
