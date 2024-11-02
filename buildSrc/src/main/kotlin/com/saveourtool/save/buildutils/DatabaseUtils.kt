@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.buildutils

import org.gradle.api.Project
import java.io.File

/**
 * @property databaseUrl database URL in the same format as used by jdbc
 * @property username username to connect
 * @property password password to connect
 */
data class DatabaseCredentials(
    val databaseUrl: String,
    val username: String,
    val password: String
) {
    /**
     * @return adjusted [databaseUrl] for liquibase runner in docker
     */
    fun getDatabaseUrlForLiquibaseInDocker(): String =
            "${databaseUrl.replace("localhost", "mysql")}?createDatabaseIfNotExist=true"
}

/**
 * @param projectName save-backend or save-demo
 * @param profile a profile to get credentials for
 * @return an instance of [DatabaseCredentials] for [profile] in [projectName]
 */
fun Project.getDatabaseCredentials(projectName: String, profile: String): DatabaseCredentials {
    val props = java.util.Properties()
    // Branch for other environments, e.g. local deployment or server deployment
    file("$projectName/src/main/resources/application-$profile.properties").inputStream().use(props::load)
    if (File("${System.getenv("HOME")}/secrets").exists()) {
        file("${System.getenv("HOME")}/secrets").inputStream().use(props::load)
    }
    val databaseUrl: String = props.getProperty("spring.datasource.url")

    System.getenv("DATABASE_SECRETS_PATH")?.let { secretsPath ->
        // Branch for environment with explicit file with database credentials, e.g. Kubernetes Secrets
        val username = file("$secretsPath/spring.datasource.username").readText()
        val password = file("$secretsPath/spring.datasource.password").readText()
        return DatabaseCredentials(databaseUrl, username, password)
    }

    val username: String
    val password: String

    if (profile == "prod") {
        username = props.getProperty("username")
        password = props.getProperty("password")
    } else {
        username = props.getProperty("spring.datasource.username")
        password = props.getProperty("spring.datasource.password")
    }

    return DatabaseCredentials(databaseUrl, username, password)
}
