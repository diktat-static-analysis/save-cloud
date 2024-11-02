package com.saveourtool.save.frontend.components.basic.codeeditor

import com.saveourtool.common.utils.Languages

/**
 * @property fileName displayed name
 * @property urlPart part of url for uploading\downloading text
 * @property editorMode highlight mode that should be enabled, if null, mode can be chosen using selector
 */
enum class FileType(
    val fileName: String,
    val urlPart: String,
    val editorMode: Languages?,
) {
    SAVE_TOML("save.toml", "test", Languages.TOML),
    SETUP_SH("setup.sh", "file", Languages.SHELL),
    TEST("test", "test", null),
    ;
    companion object {
        /**
         * @param fileType [FileType]
         * @param optionForCode will be returned if [fileType] is CODE
         * @param optionForSaveToml will be returned if [fileType] is SAVE_TOML
         * @param optionForSetupSh will be returned if [fileType] is SETUP_SH
         * @return option corresponding to [fileType]
         */
        fun <T> getTypedOption(
            fileType: FileType,
            optionForCode: T,
            optionForSaveToml: T,
            optionForSetupSh: T,
        ) = when (fileType) {
            TEST -> optionForCode
            SAVE_TOML -> optionForSaveToml
            SETUP_SH -> optionForSetupSh
        }

        /**
         * @param fileType [FileType]
         * @param optionForCode will be returned if [fileType] is CODE
         * @param optionForSaveToml will be returned if [fileType] is SAVE_TOML
         * @param optionForSetupSh will be returned if [fileType] is SETUP_SH
         * @return option corresponding to [fileType] or null if [fileType] is null
         */
        fun <T> getTypedOption(
            fileType: FileType?,
            optionForCode: T,
            optionForSaveToml: T,
            optionForSetupSh: T,
        ) = fileType?.let { getTypedOption(it, optionForCode, optionForSaveToml, optionForSetupSh) }
    }
}
