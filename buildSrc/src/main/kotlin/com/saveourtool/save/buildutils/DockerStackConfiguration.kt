/**
 * Configuration for docker swarm deployment
 */

package com.saveourtool.save.buildutils

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

const val MYSQL_STARTUP_DELAY_MILLIS = 30_000L
@Suppress("CONSTANT_UPPERCASE")
const val NEO4J_STARTUP_DELAY_MILLIS = 30_000L
const val MINIO_STARTUP_DELAY_MILLIS = 5_000L
const val KAFKA_STARTUP_DELAY_MILLIS = 5_000L

fun Project.registerLiquibaseTask(profile: String) {
    val registerLiquibaseTaskBackend = registerLiquibaseTask(
        projectName = "save-backend",
        relativeChangeLogFile = "db/db.changelog-master.xml",
        profile = profile,
        mustRunAfterTask = null,
    )

    val registerLiquibaseTaskDemo = registerLiquibaseTask(
        projectName = "save-demo",
        relativeChangeLogFile = "save-demo/db/db.changelog-demo.xml",
        profile = profile,
        mustRunAfterTask = "save-backend",
    )

    val registerLiquibaseTaskCosv = registerLiquibaseTask(
        projectName = "cosv-backend",
        relativeChangeLogFile = "cosv-backend/db/db.changelog-cosv.xml",
        profile = profile,
        mustRunAfterTask = "save-backend",
    )
    tasks.register("liquibaseUpdate") {
        dependsOn(
            registerLiquibaseTaskBackend,
            registerLiquibaseTaskDemo,
            registerLiquibaseTaskCosv,
        )
    }
}

private fun Project.registerLiquibaseTask(projectName: String, relativeChangeLogFile: String, profile: String, mustRunAfterTask: String?): TaskProvider<Exec> {
    val taskName = getTaskName(projectName)
    val credentials = getDatabaseCredentials(projectName, profile)

    return tasks.register<Exec>(taskName) {

        mustRunAfterTask?.let {
            mustRunAfter(getTaskName(it))
        }

        val contexts = when (profile) {
            "prod" -> "prod"
            "dev" -> "dev"
            else -> throw GradleException("Profile $profile not configured to map on a particular liquibase context")
        }

        val changeLogFile = rootDir.resolve(relativeChangeLogFile)
        val changeLogFileSource = "${changeLogFile.parent}"
        val changeLogFileTarget = relativeChangeLogFile.substringBeforeLast("/")
        commandLine(
            "docker", "run",
            "-v", "$changeLogFileSource:/liquibase/changelog/$changeLogFileTarget",
            "--rm",
            "--env", "INSTALL_MYSQL=true",
            "--network", "build_default",
            "liquibase/liquibase:4.20",
            "--url=${credentials.getDatabaseUrlForLiquibaseInDocker()}",
            "--changeLogFile=$relativeChangeLogFile",
            "--username=${credentials.username}",
            "--password=${credentials.password}",
            "--log-level=info",
            "--contexts=$contexts",
            "update"
        )
    }
}

private fun getTaskName(projectName: String) = "liquibaseUpdate" + projectName.split("-").map { it.capitalized() }.joinToString("")

/**
 * @param profile deployment profile, used, for example, to start SQL database in dev profile only
 */
@OptIn(ExperimentalStdlibApi::class)
@Suppress(
    "TOO_LONG_FUNCTION",
    "TOO_MANY_LINES_IN_LAMBDA",
    "AVOID_NULL_CHECKS",
    "GENERIC_VARIABLE_WRONG_DECLARATION"
)
fun Project.createStackDeployTask(profile: String) {
    tasks.register("generateComposeFile") {
        description = "Set project version in docker-compose file"
        val templateFile = "$rootDir/docker-compose.yaml"
        val composeFile = file("$buildDir/docker-compose.yaml")
        val envFile = "$buildDir/.env"
        inputs.file(templateFile)
        inputs.property("project version", version.toString())
        inputs.property("profile", profile)
        outputs.file(composeFile)
        doFirst {
            val newText = file(templateFile).readLines()
                .joinToString(System.lineSeparator()) {
                    if (profile == "dev" && it.startsWith("services:")) {
                        // `docker stack deploy` doesn't recognise `profiles` option in compose file for some reason, with docker 20.10.5, compose file 3.9
                        // so we create it here only in dev profile
                        """|$it
                           |  mysql:
                           |    image: mysql:8.0.28-oracle
                           |    container_name: mysql
                           |    ports:
                           |      - "3306:3306"
                           |    environment:
                           |      - "MYSQL_ROOT_PASSWORD=123"
                           |    command: ["--log_bin_trust_function_creators=1"]
                           |
                           |  neo4j:
                           |    image: neo4j:5.1.0-community
                           |    container_name: neo4j
                           |    ports:
                           |        - "7474:7474"
                           |        - "7687:7687"
                           |    environment:
                           |        - "NEO4J_AUTH=neo4j/123"
                           |
                           |  zookeeper:
                           |    image: confluentinc/cp-zookeeper:latest
                           |    environment:
                           |      ZOOKEEPER_CLIENT_PORT: 2181
                           |      ZOOKEEPER_TICK_TIME: 2000
                           |    ports:
                           |      - 22181:2181
                           |
                           |  kafka:
                           |    image: confluentinc/cp-kafka:latest
                           |    depends_on:
                           |      - zookeeper
                           |    ports:
                           |      - 29092:29092
                           |    environment:
                           |      KAFKA_BROKER_ID: 1
                           |      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
                           |      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092
                           |      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
                           |      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
                           |      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
                           |
                           |  minio:
                           |    image: minio/minio:latest
                           |    container_name: minio
                           |    command: server /data --console-address ":9090"
                           |    ports:
                           |      - 9000:9000
                           |      - 9090:9090
                           |    environment:
                           |      MINIO_ROOT_USER: admin
                           |      MINIO_ROOT_PASSWORD: adminadmin
                           |
                           |  minio-startup:
                           |    image: minio/mc:latest
                           |    depends_on:
                           |      - minio
                           |    entrypoint:
                           |      - /bin/sh
                           |      - -c
                           |      - |
                           |        /usr/bin/mc alias set minio http://minio:9000 admin adminadmin
                           |        /usr/bin/mc mb --ignore-existing minio/cnb
                           |        /usr/bin/mc policy set public minio/cnb
                           |        /usr/bin/mc cp --recursive /data/ minio/cnb/cnb/files
                           |    volumes:
                           |      - type: bind
                           |        source: ${project.rootProject.layout.projectDirectory.asFile}/save-backend/src/test/resources/minio
                           |        target: /data
                           |
                           |${declareDexService().prependIndent("  ")}
                           """.trimMargin()
                    } else if (profile == "dev" && it.trim().startsWith("logging:")) {
                        ""
                    } else {
                        it
                    }
                }
            composeFile
                .apply { createNewFile() }
                .writeText(newText)
        }

        doLast {
            val defaultVersionOrProperty: (propertyName: String) -> String = { propertyName ->
                // Image tag can be specified explicitly for a particular service,
                // or specified explicitly for the whole app,
                // or be inferred based on the project.version
                findProperty(propertyName) as String?
                    ?: findProperty("dockerTag") as String?
                    ?: versionForDockerImages()
            }
            // https://docs.docker.com/compose/environment-variables/#the-env-file
            file(envFile).writeText(
                """
                    BACKEND_TAG=${defaultVersionOrProperty("backend.dockerTag")}
                    FRONTEND_TAG=${defaultVersionOrProperty("frontend.dockerTag")}
                    GATEWAY_TAG=${defaultVersionOrProperty("gateway.dockerTag")}
                    ORCHESTRATOR_TAG=${defaultVersionOrProperty("orchestrator.dockerTag")}
                    PREPROCESSOR_TAG=${defaultVersionOrProperty("preprocessor.dockerTag")}
                    DEMO_TAG=${defaultVersionOrProperty("demo.dockerTag")}
                    PROFILE=$profile
                """.trimIndent()
            )
        }
    }

    tasks.register<Exec>("deployDockerStack") {
        dependsOn("liquibaseUpdate")
        dependsOn("generateComposeFile")
        subprojects {
            // in case bootBuildImage tasks are also requested, they should run before deployment task
            tasks.withType<BootBuildImage>().configureEach {
                this@register.shouldRunAfter(this)
            }
        }

        val configsDir = Paths.get("${System.getProperty("user.home")}/configs")
        val useOverride = (properties.getOrDefault("useOverride", "true") as String).toBoolean()
        val composeOverride = File("$configsDir/docker-compose.override.yaml")
        if (useOverride && !composeOverride.exists()) {
            logger.warn("`useOverride` option is set to true, but can't use override configuration, because ${composeOverride.canonicalPath} doesn't exist")
        } else {
            logger.info("Using override configuration from ${composeOverride.canonicalPath}")
        }
        doFirst {
            copy {
                description = "Copy configuration files from repo to actual locations"
                from("save-deploy")
                into(configsDir)
            }
            // create directories for optional property files
            Files.createDirectories(configsDir.resolve("backend"))
            Files.createDirectories(configsDir.resolve("gateway"))
            Files.createDirectories(configsDir.resolve("orchestrator"))
            Files.createDirectories(configsDir.resolve("preprocessor"))
            Files.createDirectories(configsDir.resolve("demo"))
        }
        description =
                "Deploy to docker swarm. If swarm contains more than one node, some registry for built images is required."
        // this command puts env variables into compose file
        val composeCmd =
                "docker compose -f ${rootProject.buildDir}/docker-compose.yaml --env-file ${rootProject.buildDir}/.env config"
        val stackCmd = "docker stack deploy --compose-file -" +
                if (useOverride && composeOverride.exists()) {
                    " --compose-file ${composeOverride.canonicalPath}"
                } else {
                    ""
                } +
                " save"
        commandLine("bash", "-c", "$composeCmd | $stackCmd")
    }

    tasks.register("buildAndDeployDockerStack") {
        dependsOn(subprojects.flatMap { it.tasks.withType<BootBuildImage>() })
        dependsOn("deployDockerStack")
    }

    tasks.register<Exec>("stopDockerStack") {
        description =
                "Completely stop all services in docker swarm. NOT NEEDED FOR REDEPLOYING! Use only to explicitly stop everything."
        commandLine("docker", "stack", "rm", "save")
    }

    // in case you are running it on MAC, first do the following: docker pull --platform linux/x86_64 mysql
    val mysqlTaskName = registerService("mysql", MYSQL_STARTUP_DELAY_MILLIS)
    tasks.named("liquibaseUpdate") {
        mustRunAfter(mysqlTaskName)
    }
    tasks.register("startMysqlDb") {
        dependsOn("liquibaseUpdate")
        dependsOn(mysqlTaskName)
    }

    registerService("neo4j", NEO4J_STARTUP_DELAY_MILLIS)

    val kafkaTaskName = registerService("kafka", KAFKA_STARTUP_DELAY_MILLIS)
    tasks.register("startKafka") {
        dependsOn(kafkaTaskName)
    }

    val minioTaskName = registerService("minio-startup", MINIO_STARTUP_DELAY_MILLIS, "startMinioService")
    tasks.register("startMinio") {
        dependsOn(minioTaskName)
    }

    tasks.register<Exec>("restartMysqlDb") {
        dependsOn("generateComposeFile")
        commandLine("docker", "compose", "--file", "$buildDir/docker-compose.yaml", "rm", "--force", "mysql")
        finalizedBy("startMysqlDb")
    }

    tasks.register<Exec>("restartKafka") {
        dependsOn("generateComposeFile")
        commandLine("docker", "compose", "--file", "$buildDir/docker-compose.yaml", "rm", "--force", "kafka")
        commandLine("docker", "compose", "--file", "$buildDir/docker-compose.yaml", "rm", "--force", "zookeeper")
        finalizedBy("startKafka")
    }

    tasks.register<Exec>("restartMinio") {
        dependsOn("generateComposeFile")
        commandLine("docker", "compose", "--file", "$buildDir/docker-compose.yaml", "rm", "--force", "minio")
        finalizedBy("startMinio")
    }

    tasks.register<Exec>("deployLocal") {
        dependsOn(subprojects.flatMap { it.tasks.withType<BootBuildImage>() })
        dependsOn("startMysqlDb")
        dependsOn("startMinio")
        commandLine(
            "docker",
            "compose",
            "--file",
            "$buildDir/docker-compose.yaml",
            "up",
            "-d",
            "orchestrator",
            "backend",
            "frontend",
            "preprocessor",
            "demo"
        )
    }

    val componentName = findProperty("save.component") as String?
    if (componentName != null) {
        tasks.register<Exec>("buildAndDeployComponent") {
            description =
                    "Build and deploy a single component of save-cloud. Component name should be provided via `-Psave.component=<name> " +
                            "and it should be a name of one of gradle subprojects. If component name is `save-backend`, then `save-frontend` will be built too" +
                            " and bundled into save-backend image."
            require(componentName in allprojects.map { it.name }) { "Component name should be one of gradle subproject names, but was [$componentName]" }
            val buildTask: TaskProvider<BootBuildImage> =
                    project(componentName).tasks.named<BootBuildImage>("bootBuildImage")
            dependsOn(buildTask)
            val serviceName = when (componentName) {
                "save-backend", "save-frontend", "save-orchestrator", "save-preprocessor" -> "save_${componentName.substringAfter("save-")}"
                "api-gateway" -> "save_gateway"
                else -> error("Wrong component name $componentName")
            }
            commandLine("docker", "service", "update", "--image", buildTask.get().imageName, serviceName)
        }
    }
}

private fun Project.declareDexService() =
        """
            |dex:
            |  image: ghcr.io/dexidp/dex:latest-distroless
            |  ports:
            |    - "5556:5556"
            |  volumes:
            |    - $rootDir/save-deploy/dex.dev.yaml:/etc/dex/config.docker.yaml
        """.trimMargin()

/**
 * Image reference must be in the form '[domainHost:port/][path/]name[:tag][@digest]', with 'path' and 'name' containing
 * only [a-z0-9][.][_][-].
 * FixMe: temporarily copy-pasted in here and in gradle/plugins
 *
 * @return correctly formatted version
 */
fun Project.versionForDockerImages(): String =
    (project.findProperty("build.dockerTag") as String? ?: version.toString())
        .replace(Regex("[^._\\-a-zA-Z0-9]"), "-")

private fun Project.registerService(serviceName: String, startupDelayInMillis: Long, overrideTaskName: String? = null): String {
    val taskName = overrideTaskName ?: "start${serviceName.capitalized()}Service"
    tasks.register<Exec>(taskName) {
        dependsOn("generateComposeFile")
        doFirst {
            logger.lifecycle("Running the following command: [docker compose --file $buildDir/docker-compose.yaml up -d $serviceName]")
        }
        standardOutput = ByteArrayOutputStream()
        errorOutput = ByteArrayOutputStream()
        commandLine("docker", "compose", "--file", "$buildDir/docker-compose.yaml", "up", "-d", serviceName)
        isIgnoreExitValue = true
        doLast {
            val execResult = executionResult.get()
            if (execResult.exitValue != 0) {
                logger.lifecycle("$taskName failed with following output:")
                logger.info(standardOutput.toString())
                logger.error(errorOutput.toString())
                execResult.assertNormalExitValue()
                execResult.rethrowFailure()
            }
            logger.lifecycle("Waiting $startupDelayInMillis millis for $serviceName to start")
            Thread.sleep(startupDelayInMillis)
        }
    }
    return taskName
}