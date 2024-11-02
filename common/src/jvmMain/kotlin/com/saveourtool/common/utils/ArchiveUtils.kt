/**
 * This file contains util methods to work with archives
 */

@file:JvmName("ArchiveUtilsJVM")

package com.saveourtool.common.utils

import okio.Path
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.examples.Archiver
import org.apache.commons.compress.archivers.examples.Expander
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path as JPath

import kotlin.io.path.absolutePathString

private val log: Logger = LoggerFactory.getLogger(object {}.javaClass.enclosingClass::class.java)
private val archiver = Archiver()
private val expander = Expander()

/**
 * Extract path as ZIP archive to provided directory
 *
 * @param targetPath
 */
actual fun Path.extractZipTo(targetPath: Path) = toNioPath().extractZipTo(targetPath.toNioPath())

/**
 * Extract path as ZIP archive to provided directory
 *
 * @param targetPath
 */
fun JPath.extractZipTo(targetPath: JPath) {
    log.debug { "Unzip $this into $targetPath" }
    expander.expand(ArchiveStreamFactory.ZIP, toFile(), targetPath.toFile())
}

/**
 * Extract path as ZIP archive to parent
 */
actual fun Path.extractZipHere() = toNioPath().extractZipHere()

/**
 * Extract path as ZIP archive to parent
 */
fun JPath.extractZipHere(): Unit = parent?.let {
    extractZipTo(it)
} ?: throw FileSystemException(this.toFile(), null, "Path to archive is set incorrectly.")

/**
 * Compress path as ZIP archive to provided file
 *
 * @param targetPath
 */
@Suppress("NestedBlockDepth")
fun JPath.compressAsZipTo(targetPath: JPath) {
    log.debug { "Zip ${absolutePathString()} into ${targetPath.absolutePathString()}" }
    archiver.create(ArchiveStreamFactory.ZIP, targetPath, this)
}
