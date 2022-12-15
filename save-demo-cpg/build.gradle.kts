import com.saveourtool.save.buildutils.*
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    alias(libs.plugins.kotlin.plugin.serialization)
}

repositories {
    ivy {
        setUrl("https://download.eclipse.org/tools/cdt/releases/10.3/cdt-10.3.2/plugins")
        metadataSources {
            artifact()
        }
        patternLayout {
            artifact("/[organisation].[module]_[revision].[ext]")
        }
    }
    mavenCentral()
}

dependencies {
    implementation(projects.saveCloudCommon)
    implementation("org.neo4j:neo4j-ogm-bolt-driver:3.2.38")
    implementation("org.neo4j:neo4j-ogm-core:3.2.38")
    implementation(libs.spring.data.neo4j)
    api(libs.arrow.kt.core)

    implementation(libs.cpg.core) {
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
    }
    implementation(libs.cpg.python) {
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
    }
}

configureJacoco()
configureSpotless()

// This is a special hack for macOS and JEP, see: https://github.com/Fraunhofer-AISEC/cpg/pull/995/files
val os = System.getProperty("os.name")
run {
    if (os.contains("mac", ignoreCase = true)) {
        tasks.withType<BootRun> {
            environment("CPG_JEP_LIBRARY", "/opt/homebrew/lib/python3.10/site-packages/jep/libjep.jnilib")
        }
    }
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar>().configureEach {
    from("requirements.txt")
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>().configureEach {
    doFirst {
        exec {
            commandLine(
                "docker", "build",
                "-f", "$projectDir/builder/Dockerfile",
                "-t", "ghcr.io/saveourtool/builder:base-plus-gcc",
                "builder"
            )
        }
    }

//    buildpacks = listOf(
//        "paketo-buildpacks/python",
//        "gcr.io/paketo-buildpacks/spring-boot",
//    )
//    builder = "paketobuildpacks/builder:full"
//    builder = "gcr.io/buildpacks/gcp/build:v1"
    pullPolicy = org.springframework.boot.buildpack.platform.build.PullPolicy.IF_NOT_PRESENT
    builder = "ghcr.io/saveourtool/builder:base-plus-gcc"
    buildpacks(
        listOf(
            "paketo-buildpacks/java",
//            "paketo-buildpacks/jvm-application",
//            "paketo-buildpacks/site-packages",
//            "paketo-buildpacks/conda-environment",
//            "paketo-buildpacks/poetry",
//            "paketo-buildpacks/pip-install",
            "paketo-buildpacks/python",
            "paketo-buildpacks/pip",
//            "paketo-buildpacks/spring-boot",
        )
    )
    environment.put(
//    "BPE_CPG_JEP_LIBRARY", "/layers/paketo-buildpacks_pip/pip/lib/python3.10/site-packages/jep/libjep.jnilib"
        "BP_CPYTHON_VERSION", "3.8",
    )
    environment["BP_JVM_TYPE"] = "JDK"
//    environment["BP_CC"] = "gcc -I/usr/include/"
//    environment["BP_C_INCLUDE_PATH"] = "/usr/include:\$C_INCLUDE_PATH"
}
