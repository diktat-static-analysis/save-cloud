@file:Suppress(
    "FILE_NAME_INCORRECT",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION"
)

package com.saveourtool.common.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

inline fun Logger.trace(msg: () -> String) {
    if (this.isTraceEnabled) {
        trace(msg())
    }
}

inline fun Logger.debug(msg: () -> String) {
    if (this.isDebugEnabled) {
        debug(msg())
    }
}

inline fun Logger.debug(exception: Throwable, msg: () -> String) {
    if (this.isDebugEnabled) {
        debug(msg(), exception)
    }
}

inline fun Logger.info(msg: () -> String) {
    if (this.isInfoEnabled) {
        info(msg())
    }
}

inline fun Logger.warn(msg: () -> String) {
    if (this.isWarnEnabled) {
        warn(msg())
    }
}

inline fun Logger.warn(exception: Throwable, msg: () -> String) {
    if (this.isWarnEnabled) {
        warn(msg(), exception)
    }
}

inline fun Logger.error(msg: () -> String) {
    if (this.isErrorEnabled) {
        error(msg())
    }
}

inline fun Logger.error(exception: Throwable, msg: () -> String) {
    if (this.isErrorEnabled) {
        error(msg(), exception)
    }
}

inline fun <reified T> getLogger(): Logger = getLogger(T::class)

/**
 * Copied from https://github.com/oshai/kotlin-logging/blob/a7caeef24ecbd392907d377a3120f4515d4ac5a9/src/javaMain/kotlin/io/github/oshai/kotlinlogging/internal/KLoggerNameResolver.kt
 *
 * @param func should be {}
 * @return the logger for util class\file
 */
fun getLogger(func: () -> Unit): Logger {
    val name = func.javaClass.name

    val slicedName =
            when {
                name.contains("Kt$") -> name.substringBefore("Kt$")
                name.contains("$") -> name.substringBefore("$")
                else -> name
            }
    return LoggerFactory.getLogger(
        slicedName
    )
}

/**
 * @param clazz the class to return the logger for.
 * @return the logger for the specified Java [class][clazz].
 */
fun getLogger(clazz: Class<*>): Logger = LoggerFactory.getLogger(clazz)

/**
 * @param clazz the class to return the logger for.
 * @return the logger for the specified Kotlin [class][clazz].
 */
fun getLogger(clazz: KClass<*>): Logger = getLogger(clazz.java)
