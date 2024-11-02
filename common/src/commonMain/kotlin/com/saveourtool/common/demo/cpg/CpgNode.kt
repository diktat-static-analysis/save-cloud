/**
 * File that contains CpgNode definition
 */

package com.saveourtool.common.demo.cpg

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.ALWAYS
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * @property key id of a node
 * @property attributes displaying attributes
 */
@Serializable
@ExperimentalSerializationApi
data class CpgNode(
    val key: String,
    @EncodeDefault(ALWAYS) val attributes: CpgNodeAttributes = CpgNodeAttributes(),
)

/**
 * @property label node label
 * @property color node color
 * @property size size of a node
 * @property x x coordinate - will be applied by layout
 * @property y y coordinate - will be applied by layout
 * @property additionalInfo
 */
@Serializable
@ExperimentalSerializationApi
data class CpgNodeAttributes(
    @EncodeDefault(NEVER) val label: String? = null,
    @EncodeDefault(NEVER) val color: String? = null,
    @EncodeDefault(NEVER) val additionalInfo: CpgNodeAdditionalInfo? = null,
    @EncodeDefault(ALWAYS) val size: Long = 5,
    @EncodeDefault(ALWAYS) val x: Long = 0,
    @EncodeDefault(ALWAYS) val y: Long = 0,
)

/**
 * @property code
 * @property comment
 * @property location
 * @property file
 * @property typedefs
 * @property isInferred
 * @property isImplicit
 * @property argumentIndex
 */
@Serializable
@ExperimentalSerializationApi
data class CpgNodeAdditionalInfo(
    @EncodeDefault(NEVER) val code: String? = null,
    @EncodeDefault(NEVER) val comment: String? = null,
    @EncodeDefault(NEVER) val location: String? = null,
    @EncodeDefault(NEVER) val file: String? = null,
    @EncodeDefault(NEVER) val typedefs: Set<Pair<String, String>> = HashSet(),
    @EncodeDefault(NEVER) val isInferred: Boolean = false,
    @EncodeDefault(NEVER) val isImplicit: Boolean = false,
    @EncodeDefault(NEVER) val argumentIndex: Int = 0,
)
