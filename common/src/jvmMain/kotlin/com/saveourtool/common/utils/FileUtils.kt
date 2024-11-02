@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

@file:JvmName("FileUtilsJVM")

package com.saveourtool.common.utils

import com.akuleshov7.ktoml.file.TomlFileReader
import okio.FileSystem
import org.slf4j.Logger
import org.springframework.core.io.Resource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission
import java.util.*
import java.util.stream.Collectors

import kotlin.io.path.*
import kotlin.jvm.Throws
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
import kotlinx.serialization.serializer

private const val DEFAULT_BUFFER_SIZE = 4096

actual val fs: FileSystem = FileSystem.SYSTEM

/**
 * @return content of file as [Flux] of [DataBuffer]
 */
fun Path.toDataBufferFlux(): Flux<DataBuffer> = if (exists()) {
    DataBufferUtils.read(this, DefaultDataBufferFactory.sharedInstance, DEFAULT_BUFFER_SIZE)
        .cast(DataBuffer::class.java)
} else {
    Flux.empty()
}

/**
 * @receiver a [Resource] that should be read
 * @return content of [Resource] as [Flux] of [ByteBuffer]
 */
fun Resource.toByteBufferFlux(): Flux<ByteBuffer> = if (exists()) {
    DataBufferUtils.read(this, DefaultDataBufferFactory.sharedInstance, DEFAULT_BUFFER_SIZE)
        .cast(DataBuffer::class.java)
} else {
    Flux.empty()
}
    .map { it.asByteBuffer() }

/**
 * @return content of file as [Flux] of [ByteBuffer]
 */
fun Path.toByteBufferFlux(): Flux<ByteBuffer> = this.toDataBufferFlux().map { it.asByteBuffer() }

/**
 * @param target path to file to where a content from receiver will be written
 * @return [Mono] with [target]
 */
fun Flux<DataBuffer>.writeTo(target: Path): Mono<Path> =
        DataBufferUtils.write(this, target.outputStream())
            .map { DataBufferUtils.release(it) }
            .then(Mono.just(target))

/**
 * Creates (if it does not exist) and appends [Flux] of [ByteBuffer] to file by path [target]
 *
 * @param target path to file to where a content from receiver will be written
 * @return [Mono] with number of bytes received
 */
fun Flux<ByteBuffer>.collectToFile(target: Path): Mono<Int> = map { byteBuffer ->
    target.outputStream(StandardOpenOption.CREATE, StandardOpenOption.APPEND).use { os ->
        Channels.newChannel(os).use { it.write(byteBuffer) }
    }
}.collect(Collectors.summingInt { it })

/**
 * Creates (if it does not exist) and appends [Flow] of [ByteBuffer] to file by path [target]
 *
 * @param target path to file to where a content from receiver will be written
 * @return number of bytes received
 */
suspend fun Flow<ByteBuffer>.collectToFile(target: Path): Int = map { byteBuffer ->
    target.outputStream(StandardOpenOption.CREATE, StandardOpenOption.APPEND).use { os ->
        Channels.newChannel(os).use { it.write(byteBuffer) }
    }
}.reduce { result, value -> result + value }

/**
 * @param stop
 * @return count of parts (folders + current file) till [stop]
 */
fun Path.countPartsTill(stop: Path): Int = generateSequence(this, Path::getParent)
    .takeWhile { it != stop }
    .count()

/**
 * @param stop
 * @return list of name of paths (folders + current file) till [stop] (including stop.name)
 */
fun Path.pathNamesTill(stop: Path): List<String> = generateSequence(this, Path::getParent)
    .takeWhile { it != stop }
    .map { it.name }
    .toList()

/**
 * Requires that this path is absolute, throwing an [IllegalArgumentException]
 * if it's not.
 *
 * @return this path.
 * @throws IllegalArgumentException if this path is relative.
 */
@Throws(IllegalArgumentException::class)
fun Path.requireIsAbsolute(): Path = apply {
    require(isAbsolute) {
        "The path is not absolute: $this"
    }
}

actual fun okio.Path.markAsExecutable() {
    val file = this.toFile().toPath()
    Files.setPosixFilePermissions(file, Files.getPosixFilePermissions(file) + EnumSet.of(
        PosixFilePermission.OWNER_EXECUTE,
        PosixFilePermission.GROUP_EXECUTE,
        PosixFilePermission.OTHERS_EXECUTE,
    ))
}

actual fun ByteArray.writeToFile(file: okio.Path, mustCreate: Boolean) {
    fs.write(
        file = file,
        mustCreate = mustCreate,
    ) {
        write(this@writeToFile).flush()
    }
}

/**
 * @param log logger to log debug message with potential [IOException]
 */
@OptIn(ExperimentalPathApi::class)
fun Path.deleteRecursivelySafely(log: Logger): Unit = try {
    deleteRecursively()
} catch (e: IOException) {
    log.debug(e) {
        "Failed to delete recursively ${absolutePathString()}"
    }
}

actual inline fun <reified C : Any> parseConfig(configPath: okio.Path): C = TomlFileReader.decodeFromFile(
    serializer(),
    configPath.toString(),
)

/**
 * Move [source] into [destinationDir], while also copying original file attributes
 *
 * @param source source file
 * @param destinationDir destination directory
 * @throws FileNotFoundException if source doesn't exists
 */
fun moveFileWithAttributes(source: File, destinationDir: File) {
    if (!source.exists()) {
        throw FileNotFoundException("Source file $source doesn't exist!")
    }

    Files.copy(source.toPath(), destinationDir.resolve(source.name).toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
    Files.delete(source.toPath())
}
