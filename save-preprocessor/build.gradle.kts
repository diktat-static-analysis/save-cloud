@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    alias(libs.plugins.kotlin.plugin.serialization)
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
}

dependencies {
    implementation(projects.common)
    testImplementation(projects.testUtils)
    implementation(libs.save.common.jvm)
    implementation(libs.save.core.jvm) {
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
        exclude("org.slf4j", "slf4j-api")
    }
    implementation(libs.save.plugins.warn.jvm) {
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
        exclude("org.slf4j", "slf4j-api")
    }
    implementation(libs.save.plugins.fix.jvm) {
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
        exclude("org.slf4j", "slf4j-api")
    }
    implementation(libs.save.plugins.fixAndWarn.jvm) {
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
        exclude("org.slf4j", "slf4j-api")
    }
    implementation(libs.jgit)
    implementation(libs.kotlinx.serialization.properties)
    implementation(libs.ktoml.source)
    implementation(libs.ktoml.file)
    implementation(libs.ktoml.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.commons.compress)
}
