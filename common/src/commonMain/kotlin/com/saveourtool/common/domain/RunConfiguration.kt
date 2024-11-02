package com.saveourtool.common.domain

import kotlinx.serialization.Serializable

/**
 * Describes the configuration to run tests on SAVE backend.
 *
 * @property startCommand shell command to run the file and its arguments
 * @property fileName name of the supplied file. fixme: is it needed?
 */
@Serializable
data class RunConfiguration(val startCommand: List<String>, val fileName: String)
