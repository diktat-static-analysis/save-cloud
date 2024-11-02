package com.saveourtool.common.storage.impl

/**
 * @property name
 * @property version
 * @property fileName
 */
data class InternalFileKey(
    val name: String,
    val version: String,
    val fileName: String,
) {
    /**
     * @return true if version is latest, otherwise -- false
     */
    fun isLatest(): Boolean = version == LATEST_VERSION

    companion object {
        private const val LATEST_VERSION = "latest"

        /**
         * [InternalFileKey] for *save-agent*
         */
        val saveAgentKey: InternalFileKey = latest("save-agent", "save-agent.kexe")

        /**
         * @param name
         * @param fileName
         * @return [InternalFileKey] with latest version
         */
        fun latest(name: String, fileName: String): InternalFileKey = InternalFileKey(name, LATEST_VERSION, fileName)

        /**
         * @param version
         * @return [InternalFileKey] for *save-cli* with version [version]
         */
        fun saveCliKey(version: String): InternalFileKey = InternalFileKey(
            name = "save-cli",
            version = version,
            fileName = "save-$version-linuxX64.kexe",
        )
    }
}
