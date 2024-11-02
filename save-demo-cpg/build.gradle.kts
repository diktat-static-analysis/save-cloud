import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.springframework.boot.gradle.tasks.run.BootRun

@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    alias(libs.plugins.kotlin.plugin.serialization)
}

repositories {
    ivy {
        setUrl("https://download.eclipse.org/tools/cdt/releases/11.0/cdt-11.0.0/plugins")
        metadataSources {
            artifact()
        }
        patternLayout {
            artifact("/[organisation].[module]_[revision].[ext]")
        }
    }
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        content {
            includeGroup("com.saveourtool.save")
        }
    }
    mavenCentral()
    maven {
        name = "0x6675636b796f75676974687562/kotlintree"
        url = uri("https://maven.pkg.github.com/0x6675636b796f75676974687562/kotlintree")
        credentials {
            username = project.findProperty("gprUser") as String?
                ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gprKey") as String?
                ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
val jepArchive by configurations.creating

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val resolveJep: TaskProvider<Copy> = tasks.register<Copy>("resolveJep") {
    destinationDir = file("$buildDir/distros/jep-distro")
    from(tarTree(jepArchive.singleFile))
}

dependencies {
    implementation(projects.common)
    api(libs.arrow.kt.core)

    val excludeLogging: Action<ExternalModuleDependency> = Action {
        // we use logback
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
        exclude("org.apache.logging.log4j", "log4j-core")
        // we don't migrate to slf4j 2.x yet
        exclude("org.slf4j", "slf4j-api")
    }
    implementation(libs.neo4j.ogm.core, excludeLogging)
    implementation(libs.neo4j.ogm.bolt.driver, excludeLogging)
    implementation(libs.neo4j.java.driver, excludeLogging)

    implementation(libs.cpg.core, excludeLogging)
    implementation(libs.cpg.cxx, excludeLogging)
    implementation(libs.cpg.java, excludeLogging)
    implementation(libs.cpg.python, excludeLogging)
    implementation(libs.cpg.typescript, excludeLogging)

    jepArchive("com.icemachined:jep-distro:4.1.1@tgz")
    runtimeOnly(fileTree("$buildDir/distros/jep-distro").apply {
        builtBy(resolveJep)
    })
    implementation("io.github.oxisto:kotlin-tree-jna:0.0.2")
}

// This is a special hack for macOS and JEP, see: https://github.com/Fraunhofer-AISEC/cpg/pull/995/files
run {
    val jepLibraryFile =
            with(DefaultNativePlatform.getCurrentOperatingSystem()) {
                when {
                    isMacOsX -> "libjep.jnilib"
                    isWindows -> "jep.dll"
                    isLinux -> "libjep.so"
                    else -> throw Exception("Unsupported operating system: ${System.getProperty("os.name")}")
                }
            }

    tasks.withType<BootRun> {
        environment("CPG_JEP_LIBRARY", "$buildDir/distros/jep-distro/jep/$jepLibraryFile")
    }
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar>().configureEach {
    from("requirements.txt")
    from("$buildDir/distros")
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>().configureEach {
    builder = "ghcr.io/saveourtool/builder:base-plus-gcc"
    buildpacks(
        listOf(
            "paketo-buildpacks/java",
            "paketo-buildpacks/python",
            "paketo-buildpacks/pip",
        )
    )
    environment["BPE_CPG_JEP_LIBRARY"] = "jep-distro/jep/libjep.so"
    environment["BP_CPYTHON_VERSION"] = "3.10"
    environment["BP_JVM_TYPE"] = "JDK"
}
