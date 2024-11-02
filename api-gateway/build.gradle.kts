plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
}

dependencies {
    api(projects.common)
    implementation(libs.spring.cloud.starter.gateway)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.cloud.starter.kubernetes.client.config) {
        because("needed for kubernetes configs and secrets to be added as additional configurations")
    }
    implementation(libs.spring.security.core)
    implementation(projects.authenticationService)

    // FixMe: we officially bring serialization CVE to our project
    // easy way to hack us during the deserialization:
    // https://github.com/advisories/GHSA-mjmj-j48q-9wg2
    implementation("org.yaml:snakeyaml") {
        version {
            strictly("1.33")
        }
    }
}
