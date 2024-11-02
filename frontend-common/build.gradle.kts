import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest

@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    kotlin("js")
    id("com.saveourtool.save.buildutils.build-frontend-image-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    id("com.saveourtool.save.buildutils.save-cloud-version-file-configuration")
    alias(libs.plugins.kotlin.plugin.serialization)
}

rootProject.plugins.withType<NodeJsRootPlugin> {
    rootProject.the<NodeJsRootExtension>().nodeVersion = "16.13.1"
}

dependencies {
    implementation(projects.common)

    implementation(enforcedPlatform(libs.kotlin.wrappers.bom))
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-extensions")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-router-dom")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-tanstack-react-table")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-mui-icons")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-mui")
    implementation("io.github.petertrr:kotlin-multiplatform-diff-js:0.4.0")

    implementation(libs.save.common)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktor.http)
}

val distributionsDirName = "distributions"

kotlin {
    js(IR) {
        // as for `-pre.148-kotlin-1.4.21`, react-table gives errors with IR
        browser {
            distribution(
                Action {
                    // TODO: need to remove this overriding
                    outputDirectory = layout.buildDirectory.dir(distributionsDirName)
                }
            )
            testTask {
                useKarma {
                    when (properties["save.profile"]) {
                        "dev" -> {
                            useChrome()
                            // useFirefox()
                        }
                        null -> useChromeHeadless()
                    }
                }
            }
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        // kotlin-wrapper migrates to commonjs and missed @JsNonModule annotations
        // https://github.com/JetBrains/kotlin-wrappers/issues/1935
        useCommonJs()
        binaries.executable()  // already default for LEGACY, but explicitly needed for IR
        sourceSets.all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
                optIn("kotlin.js.ExperimentalJsExport")
            }
        }
        sourceSets["main"].dependencies {
            compileOnly(devNpm("sass", "^1.43.0"))
            compileOnly(devNpm("sass-loader", "^12.0.0"))
            compileOnly(devNpm("style-loader", "^3.3.1"))
            compileOnly(devNpm("css-loader", "^6.5.0"))
            compileOnly(devNpm("file-loader", "^6.2.0"))
            // https://getbootstrap.com/docs/4.0/getting-started/webpack/#importing-precompiled-sass
            compileOnly(devNpm("postcss-loader", "^6.2.1"))
            compileOnly(devNpm("postcss", "^8.2.13"))
            // See https://stackoverflow.com/a/72828500; newer versions are supported only for Bootstrap 5.2+
            compileOnly(devNpm("autoprefixer", "10.4.5"))
            compileOnly(devNpm("webpack-bundle-analyzer", "^4.5.0"))
            compileOnly(devNpm("mini-css-extract-plugin", "^2.6.0"))
            compileOnly(devNpm("html-webpack-plugin", "^5.5.0"))

            // web-specific dependencies
            implementation(npm("@fortawesome/fontawesome-svg-core", "^1.2.36"))
            implementation(npm("@fortawesome/free-solid-svg-icons", "5.15.3"))
            implementation(npm("@fortawesome/free-brands-svg-icons", "5.15.3"))
            implementation(npm("@fortawesome/react-fontawesome", "^0.1.16"))
            implementation(npm("devicon", "^2.15.1"))
            implementation(npm("animate.css", "^4.1.1"))
            implementation(npm("react-scroll-motion", "^0.3.0"))
            implementation(npm("react-spinners", "0.13.0"))
            implementation(npm("react-tsparticles", "1.42.1"))
            implementation(npm("tsparticles", "2.1.3"))
            implementation(npm("jquery", "3.6.0"))
            // BS5: implementation(npm("@popperjs/core", "2.11.0"))
            implementation(npm("popper.js", "1.16.1"))
            // BS5: implementation(npm("bootstrap", "5.0.1"))
            implementation(npm("react-calendar", "^3.8.0"))
            implementation(npm("bootstrap", "^4.6.0"))
            implementation(npm("react", "^18.0.0"))
            implementation(npm("react-dom", "^18.0.0"))
            implementation(npm("react-modal", "^3.0.0"))
            implementation(npm("os-browserify", "^0.3.0"))
            implementation(npm("path-browserify", "^1.0.1"))
            implementation(npm("react-minimal-pie-chart", "^8.2.0"))
            implementation(npm("lodash.debounce", "^4.0.8"))
            implementation(npm("react-markdown", "^8.0.3"))
            implementation(npm("rehype-highlight", "^5.0.2"))
            implementation(npm("react-ace", "^10.1.0"))
            implementation(npm("react-avatar-image-cropper", "^1.4.2"))
            implementation(npm("react-circle", "^1.1.1"))
            implementation(npm("react-diff-viewer-continued", "^3.2.6"))
            implementation(npm("react-json-view", "^1.21.3"))
            implementation(npm("multi-range-slider-react", "^2.0.5"))
            // react-sigma
            implementation(npm("@react-sigma/core", "^3.1.0"))
            implementation(npm("sigma", "^2.4.0"))
            implementation(npm("graphology", "^0.25.1"))
            implementation(npm("graphology-layout", "^0.6.1"))
            implementation(npm("graphology-layout-forceatlas2", "^0.10.1"))
            implementation(npm("@react-sigma/layout-core", "^3.1.0"))
            implementation(npm("@react-sigma/layout-random", "^3.1.0"))
            implementation(npm("@react-sigma/layout-circular", "^3.1.0"))
            implementation(npm("@react-sigma/layout-forceatlas2", "^3.1.0"))
            implementation(npm("react-graph-viz-engine", "^0.1.0"))
            implementation(npm("cytoscape", "^3.25.0"))
            // translation
            implementation(npm("i18next", "^23.4.5"))
            implementation(npm("react-i18next", "^13.2.0"))
            implementation(npm("i18next-http-backend", "^2.2.2"))
            implementation(npm("js-cookie", "^3.0.5"))
            // transitive dependencies with explicit version ranges required for security reasons
            compileOnly(devNpm("minimist", "^1.2.6"))
            compileOnly(devNpm("async", "^2.6.4"))
            compileOnly(devNpm("follow-redirects", "^1.14.8"))
        }
        sourceSets["test"].dependencies {
            implementation(kotlin("test-js"))
            implementation(devNpm("jsdom", "^19.0.0"))
            implementation(devNpm("global-jsdom", "^8.4.0"))
            implementation(devNpm("@testing-library/react", "^13.2.0"))
            implementation(devNpm("@testing-library/user-event", "^14.0.0"))
            implementation(devNpm("karma-mocha-reporter", "^2.0.0"))
            implementation(devNpm("istanbul-instrumenter-loader", "^3.0.1"))
            implementation(devNpm("karma-coverage-istanbul-reporter", "^3.0.3"))
            implementation(devNpm("msw", "^0.40.0"))
        }
    }
}

rootProject.plugins.withType(NodeJsRootPlugin::class.java) {
    rootProject.the<NodeJsRootExtension>().versions.apply {
        // workaround for continuous work of WebPack: (https://github.com/webpack/webpack-cli/issues/2990)
        webpackCli.version = "4.9.0"
        webpackDevServer.version = "^4.9.0"
        // override default version from KGP for security reasons
        karma.version = "^6.3.14"
        mocha.version = "^9.2.0"
    }
}
// store yarn.lock in the root directory
rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension> {
    lockFileDirectory = rootProject.projectDir
}

val mswScriptTargetPath = file("${rootProject.buildDir}/js/packages/${rootProject.name}-${project.name}-test/node_modules").absolutePath
val mswScriptTargetFile = "$mswScriptTargetPath/mockServiceWorker.js"
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val installMwsScriptTaskProvider = tasks.register<Exec>("installMswScript") {
    dependsOn(":kotlinNodeJsSetup", ":kotlinNpmInstall", "packageJson")
    inputs.dir(mswScriptTargetPath)
    outputs.file(mswScriptTargetFile)
    // cd to directory where the generated package.json is located. This is required for correct operation of npm/npx
    workingDir("$rootDir/build/js")

    val isWindows = DefaultNativePlatform.getCurrentOperatingSystem().isWindows
    val nodeJsEnv = NodeJsRootPlugin.apply(project.rootProject).requireConfigured()
    val nodeDir = nodeJsEnv.nodeDir
    val nodeBinDir = nodeJsEnv.nodeBinDir
    listOf(
        System.getenv("PATH"),
        nodeBinDir.absolutePath,
    )
        .filterNot { it.isNullOrEmpty() }
        .joinToString(separator = File.pathSeparator)
        .let { environment("PATH", it) }

    if (!isWindows) {
        doFirst {
            // workaround, because `npx` is a symlink but symlinks are lost when Gradle unpacks archive
            exec {
                commandLine("ln", "-sf", "$nodeDir/lib/node_modules/npm/bin/npx-cli.js", "$nodeBinDir/npx")
            }
            exec {
                commandLine("ln", "-sf", "$nodeDir/lib/node_modules/npm/bin/npm-cli.js", "$nodeBinDir/npm")
            }
            exec {
                commandLine("ln", "-sf", "$nodeDir/lib/node_modules/corepack/dist/corepack.js", "$nodeBinDir/corepack")
            }
        }
    }

    commandLine(
        nodeBinDir.resolve(if (isWindows) "npx.cmd" else "npx").canonicalPath,
        "msw",
        "init",
        mswScriptTargetPath,
        "--no-save",
    )
}
tasks.named<KotlinJsTest>("browserTest").configure {
    dependsOn(installMwsScriptTaskProvider)
    inputs.file(mswScriptTargetFile)
}

kotlin.sourceSets.getByName("main") {
    kotlin.srcDir(
        tasks.named("generateSaveCloudVersionFile").map {
            it.outputs.files.singleFile
        }
    )
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack> {
    // Since we inject timestamp into HTML file, we would like this task to always be re-run.
    inputs.property("Build timestamp", System.currentTimeMillis())
    doFirst {
        val additionalWebpackResources = fileTree("$buildDir/processedResources/js/main/") {
            include("scss/**")
            include("index.html")
        }
        copy {
            from(additionalWebpackResources)
            into("${rootProject.buildDir}/js/packages/${rootProject.name}-${project.name}")
        }
    }
}

val distribution: Configuration by configurations.creating
val distributionJarTask by tasks.registering(Jar::class) {
    dependsOn(":frontend-common:browserDistribution")
    archiveClassifier.set("distribution")
    from("$buildDir/$distributionsDirName") {
        into("static")
        exclude("scss")
    }
    from("$projectDir/nginx.conf") {
        into("")
    }
}
artifacts.add(distribution.name, distributionJarTask.get().archiveFile) {
    builtBy(distributionJarTask)
}

detekt {
    config.setFrom(config.plus(file("detekt.yml")))
}
