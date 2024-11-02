/**
 * Logic for configuration for http client and evaluated tool
 */

package com.saveourtool.save.api.config

import com.saveourtool.common.domain.Jdk
import com.saveourtool.common.domain.Python
import com.saveourtool.common.domain.Sdk
import com.saveourtool.common.utils.getLogger

import kotlinx.serialization.Serializable

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
private val log = getLogger<PropertiesConfiguration>()

/**
 * Available types of configurations
 */
enum class PropertiesConfigurationType {
    EVALUATED_TOOL,
    WEB_CLIENT,
    ;
}

/**
 * Base class for configuration
 */
@Serializable
sealed class PropertiesConfiguration

/**
 * @property backendUrl
 */
@Serializable
data class WebClientProperties(
    val backendUrl: String,
) : PropertiesConfiguration()

/**
 * @property organizationName
 * @property projectName
 * @property sdk
 * @property additionalFiles
 * @property testSuites
 * @property execCmd
 * @property batchSize
 */
@Serializable
data class EvaluatedToolProperties(
    val organizationName: String,
    val projectName: String,
    val sdk: String? = null,
    val additionalFiles: String? = null,
    val testSuites: String,
    val execCmd: String? = null,
    val batchSize: String? = null,
) : PropertiesConfiguration()

/**
 * @return sdk instance converted from string representation
 * @throws IllegalArgumentException in case of invalid configuration
 */
internal fun String?.toSdk(): Sdk {
    this ?: run {
        log.info("Setting SDK to default value: Java 11")
        return Jdk("11")
    }
    val sdk = this.split(" ").map { it.trim() }
    require(sdk.size == 2) {
        "SDK should have the environment and version separated by whitespace, e.g.: `Java 11`, but found ${this}."
    }
    return if (sdk.first().lowercase() == "java" && sdk.last() in Jdk.versions) {
        Jdk(sdk.last())
    } else if (sdk.first().lowercase() == "python" && sdk.last() in Python.versions) {
        Python(sdk.last())
    } else {
        throw IllegalArgumentException(
            """
            Provided SDK $sdk have incorrect value!
            Available list of SDK:
            Java: ${Jdk.versions.map { "Java $it" }}
            Python: ${Python.versions.map { "Python $it" }}
            """.trimMargin()
        )
    }
}
