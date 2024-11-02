rootProject.name = "save-cloud"

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            content {
                includeGroup("com.saveourtool.sarifutils")
                includeGroup("com.saveourtool.save")
            }
        }
        mavenCentral()
        maven {
            name = "saveourtool/okio-extras"
            url = uri("https://maven.pkg.github.com/saveourtool/okio-extras")
            credentials {
                username = providers.gradleProperty("gprUser").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gprKey").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.enterprise") version "3.15.1"
}

includeBuild("gradle/plugins")
include("api-gateway")
include("cosv-frontend")
include("save-backend")
include("save-orchestrator-common")
include("save-orchestrator")
include("save-frontend")
include("common")
include("save-agent")
include("save-preprocessor")
include("test-utils")
include("save-api")
include("save-api-cli")
include("authentication-service")
include("save-demo")
include("save-demo-cpg")
include("test-analysis-core")
include("save-demo-agent")
include("cosv-backend")
include("frontend-common")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

gradleEnterprise {
    @Suppress("AVOID_NULL_CHECKS")
    if (System.getenv("CI") != null) {
        buildScan {
            publishAlways()
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}
