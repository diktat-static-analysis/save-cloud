import org.gradle.api.tasks.TaskProvider
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.plugin.serialization)
    id("com.saveourtool.save.buildutils.code-quality-convention")
    id("com.saveourtool.save.buildutils.save-cloud-version-file-configuration")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = Versions.jdk
            }
        }
    }

    // Create a target for the host platform.
    val linuxTarget = linuxX64 {
        binaries.executable {
            entryPoint = "com.saveourtool.save.agent.main"
            baseName = "save-agent"
        }
        binaries.all {
            binaryOptions["memoryModel"] = "experimental"
            freeCompilerArgs = freeCompilerArgs + "-Xruntime-logs=gc=info"
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }

        commonMain {
            kotlin {
                srcDir(
                    tasks.named("generateSaveCloudVersionFile").map {
                        it.outputs.files.singleFile
                    }
                )
            }
            dependencies {
                implementation(libs.save.common)
                implementation(projects.common)
                implementation(libs.save.core)
                implementation(libs.save.plugins.fix)
                implementation(libs.save.reporters)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.kotlinx.serialization.properties)
                implementation(libs.okio)
                implementation(libs.kotlinx.datetime)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.ktor.client.mock)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.apache)
                implementation(libs.commons.compress)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            tasks.withType<Test> {
                useJUnitPlatform()
            }
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit.jupiter.engine)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val linuxX64Main by getting {
            dependencies {
                implementation(libs.ktor.client.curl)
                implementation(libs.kotlinx.coroutines.core.linuxx64)
            }
        }
        @Suppress("UNUSED_VARIABLE")
        val linuxX64Test by getting
    }

    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    val linkTask: TaskProvider<KotlinNativeLink> = tasks.named<KotlinNativeLink>("linkReleaseExecutableLinuxX64")

    val copyAgentDistribution by tasks.registering(Jar::class) {
        dependsOn(linkTask)
        archiveClassifier.set("distribution")
        from(linkTask.flatMap { it.outputFile })
        from(file("$projectDir/src/linuxX64Main/resources/agent.toml"))
    }
    val distribution by configurations.creating
    artifacts.add(distribution.name, copyAgentDistribution.flatMap { it.archiveFile }) {
        builtBy(copyAgentDistribution)
    }

    // code coverage: https://github.com/JetBrains/kotlin/blob/master/kotlin-native/CODE_COVERAGE.md,
    // https://github.com/JetBrains/kotlin/blob/master/kotlin-native/samples/coverage/build.gradle.kts
    if (false) {
        // this doesn't work for 1.4.31, maybe will be fixed later
        linuxTarget.binaries.getTest("DEBUG").apply {
            freeCompilerArgs = freeCompilerArgs + listOf("-Xlibrary-to-cover=${linuxTarget.compilations["main"].output.classesDirs.singleFile.absolutePath}")
        }
        val createCoverageReportTask by tasks.creating {
            dependsOn("${linuxTarget.name}Test")
            description = "Create coverage report"

            doLast {
                val testDebugBinary = linuxTarget.binaries.getTest("DEBUG").outputFile
                val llvmPath = "${System.getenv()["HOME"]}/.konan/dependencies/clang-llvm-8.0.0-linux-x86-64/bin"
                exec {
                    commandLine("$llvmPath/llvm-profdata", "merge", "$testDebugBinary.profraw", "-o", "$testDebugBinary.profdata")
                }
                exec {
                    commandLine("$llvmPath/llvm-cov", "show", testDebugBinary, "-instr-profile", "$testDebugBinary.profdata")
                }
            }
        }
        tasks.named("${linuxTarget.name}Test") {
            finalizedBy(createCoverageReportTask)
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinTest> {
    testLogging.showStandardStreams = true
}

/*
 * On Windows, it's impossible to link a Linux executable against
 * `io.ktor:ktor-client-curl` because `-lcurl` is not found by `ld`.
 *
 * Also, additionally disable debug artifacts for CI to speed-up builds
 */
tasks.named("linkDebugExecutableLinuxX64") {
    onlyIf {
        !DefaultNativePlatform.getCurrentOperatingSystem().isWindows && (System.getenv("CI") == null)
    }
}

tasks.named("linkReleaseExecutableLinuxX64") {
    onlyIf {
        !DefaultNativePlatform.getCurrentOperatingSystem().isWindows
    }
}

tasks.named("linkDebugTestLinuxX64") {
    onlyIf {
        !DefaultNativePlatform.getCurrentOperatingSystem().isWindows && (System.getenv("CI") == null)
    }
}
