package com.saveourtool.common.utils

/**
 * Highlight modes for AceEditor
 *
 * @property modeName substring of file with required highlight mode
 * @property prettyName
 * @property extension
 */
@Suppress("IDENTIFIER_LENGTH")
enum class Languages(val modeName: String, val prettyName: String, val extension: String) {
    /**
     * C highlight mode
     */
    C("c_cpp", "C", ".c"),

    /**
     * C++ highlight mode
     */
    CPP("c_cpp", "C++", ".cpp"),

    /**
     * Golang highlight mode
     */
    GO("golang", "GO", ".go"),

    /**
     * Java highlight mode
     */
    JAVA("java", "JAVA", ".java"),

    /**
     * Javascript highlight mode
     */
    JAVASCRIPT("jsx", "JS", ".js"),

    /**
     * Kotlin highlight mode
     */
    KOTLIN("kotlin", "KOTLIN", ".kt"),

    /**
     * Python highlight mode
     */
    PYTHON("python", "PYTHON", ".py"),

    /**
     * Shell highlight mode
     */
    SHELL("sh", "SHELL", ".sh"),

    /**
     * Toml highlight mode
     */
    TOML("toml", "TOML", ".toml"),

    /**
     * Typescript highlight mode
     */
    TYPESCRIPT("typescript", "TYPESCRIPT", ".ts"),
    ;
}
