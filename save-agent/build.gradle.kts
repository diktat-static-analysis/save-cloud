import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version Versions.kotlin
}

kotlin {
    val os = getCurrentOperatingSystem()
    // Create a target for the host platform.
    val hostTarget = when {
        os.isLinux -> linuxX64("agent")
        os.isWindows -> mingwX64("agent")  // you'll need to install msys2 and run `pacman -S mingw-w64-x86_64-curl` to have libcurl for ktor-client
        os.isMacOsX -> macosX64("agent")
        else -> throw GradleException("Host OS '${os.name}' is not supported in Kotlin/Native $project.")
    }

    configure(listOf(hostTarget)) {
        binaries.executable {
            entryPoint = "org.cqfn.save.agent.main"
            baseName = "save-agent"
        }
    }

    tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadSaveCli") {
        src("https://docs.gradle.org/current/userguide/custom_tasks.html")
        dest("$buildDir/classes/kotlin/agent/test")
    }

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
            languageSettings.useExperimentalAnnotation("okio.ExperimentalFileSystem")
        }
        val nativeMain by creating {
            dependencies {
                implementation(project(":save-common"))
                implementation("io.ktor:ktor-client-core:${Versions.ktor}")
                implementation("io.ktor:ktor-client-curl:${Versions.ktor}")
                implementation("io.ktor:ktor-client-serialization:${Versions.ktor}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:${Versions.serialization}")
                implementation("com.squareup.okio:okio-multiplatform:3.0.0-alpha.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Versions.kotlinxDatetime}")
                if (os.isWindows) {
                    implementation("org.cqfn.save:save-core-mingwx64:${Versions.saveCore}")
                } else if (os.isLinux) {
                    implementation("org.cqfn.save:save-core-linuxx64:${Versions.saveCore}")
                }
                // as for 2.0.4, kotlin-logging doesn't have mingw version and it'll be PITA to use it
//                implementation("io.github.microutils:kotlin-logging:2.0.4")
            }
        }
        getByName("${hostTarget.name}Main").dependsOn(nativeMain)
        val nativeTest by creating {
            dependencies {
                implementation("io.ktor:ktor-client-mock:${Versions.ktor}")
            }
        }
        getByName("${hostTarget.name}Test").dependsOn(nativeTest)
        tasks.getByName("${hostTarget.name}Test").dependsOn("downloadSaveCli")
    }

    val distribution by configurations.creating
    val copyAgentDistribution by tasks.registering(Jar::class) {
        dependsOn("linkReleaseExecutableAgent")
        archiveClassifier.set("distribution")
        from(file("$buildDir/bin/agent/releaseExecutable")) {
            include("*")
        }
        from(file("$projectDir/src/nativeMain/resources/agent.properties"))
    }
    artifacts.add(distribution.name, file("$buildDir/libs/${project.name}-${project.version}-distribution.jar")) {
        builtBy(copyAgentDistribution)
    }

    // code coverage: https://github.com/JetBrains/kotlin/blob/master/kotlin-native/CODE_COVERAGE.md, https://github.com/JetBrains/kotlin/blob/master/kotlin-native/samples/coverage/build.gradle.kts
    if (false /*os.isLinux*/) {  // this doesn't work for 1.4.31, maybe will be fixed later
        hostTarget.binaries.getTest("DEBUG").apply {
            freeCompilerArgs = freeCompilerArgs + listOf("-Xlibrary-to-cover=${hostTarget.compilations["main"].output.classesDirs.singleFile.absolutePath}")
        }
        val createCoverageReportTask by tasks.creating {
            dependsOn("${hostTarget.name}Test")
            description = "Create coverage report"

            doLast {
                val testDebugBinary = hostTarget.binaries.getTest("DEBUG").outputFile
                val llvmPath = "${System.getenv()["HOME"]}/.konan/dependencies/clang-llvm-8.0.0-linux-x86-64/bin"
                exec {
                    commandLine("$llvmPath/llvm-profdata", "merge", "$testDebugBinary.profraw", "-o", "$testDebugBinary.profdata")
                }
                exec {
                    commandLine("$llvmPath/llvm-cov", "show", "$testDebugBinary", "-instr-profile", "$testDebugBinary.profdata")
                }
            }
        }
        tasks.getByName("${hostTarget.name}Test").dependsOn("downloadSaveCli")
        tasks.getByName("${hostTarget.name}Test").finalizedBy(createCoverageReportTask)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinTest> {
    testLogging.showStandardStreams = true
}