package com.saveourtool.save.demo.cpg.controller

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.demo.cpg.*
import com.saveourtool.common.utils.blockingToMono
import com.saveourtool.common.utils.getLogger
import com.saveourtool.save.demo.cpg.config.ConfigProperties
import com.saveourtool.save.demo.cpg.repository.CpgRepository
import com.saveourtool.save.demo.cpg.service.CpgService
import com.saveourtool.save.demo.cpg.service.TreeSitterService
import com.saveourtool.save.demo.cpg.utils.*

import arrow.core.getOrElse
import de.fraunhofer.aisec.cpg.*
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import java.util.*

import kotlin.io.path.*
import kotlinx.serialization.ExperimentalSerializationApi

const val FILE_NAME_SEPARATOR = "==="

/**
 * A simple controller
 * @property configProperties
 * @property cpgService
 * @property cpgRepository
 * @property treeSitterService
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "cpg-demo"),
)
@RestController
@RequestMapping("/api/cpg")
@ExperimentalSerializationApi
class CpgController(
    val configProperties: ConfigProperties,
    val cpgService: CpgService,
    val cpgRepository: CpgRepository,
    val treeSitterService: TreeSitterService,
) {
    /**
     * @param request
     * @return result of uploading, it contains ID to request the result further
     */
    @PostMapping("/upload-code")
    fun uploadCode(
        @RequestBody request: CpgRunRequest,
    ): Mono<CpgResult> = blockingToMono {
        when (request.params.engine) {
            CpgEngine.CPG -> doUploadCode(
                request,
                cpgService::translate,
                cpgRepository::save
            ) {
                cpgRepository.getGraph(it)
            }
            CpgEngine.TREE_SITTER -> doUploadCode(
                request,
                treeSitterService::translate,
                cpgRepository::save
            ) {
                cpgRepository.getGraphForTreeSitter(it)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun <T> doUploadCode(
        @RequestBody request: CpgRunRequest,
        translateFunction: (Path) -> ResultWithLogs<T>,
        saveFunction: (T) -> Long,
        graphFunction: (Long) -> CpgGraph,
    ): CpgResult {
        val tmpFolder = createTempDirectory(request.params.language.modeName)
        val logs: MutableList<String> = mutableListOf()
        return try {
            createFiles(request, tmpFolder)
            val (result, logsFromLogback) = translateFunction(tmpFolder)
            logs.addAll(logsFromLogback)

            result
                .map {
                    saveFunction(it)
                }
                .map { queryId ->
                    CpgResult(
                        graphFunction(queryId),
                        CpgRepository.getQueryForNodes(queryId),
                        logs,
                    )
                }
                .getOrElse {
                    logs += "Exception: ${it.message} ${it.stackTraceToString()}"
                    logs.stubCpgResult(ERROR_PARSING)
                }
        } catch (e: Exception) {
            // this is a very generic exception, but unfortunately we cannot let users get any stacktrace on the FE
            logs += "Exception: ${e.message} ${e.stackTraceToString()}"
            logs.stubCpgResult(ERROR_DB)
        } finally {
            FileUtils.deleteDirectory(tmpFolder.toFile())
        }
    }

    private fun List<String>.stubCpgResult(error: String) =
            CpgResult(
                CpgGraph.placeholder,
                error,
                this,
            )

    private fun createFiles(request: CpgRunRequest, tmpFolder: Path) {
        val files: MutableList<SourceCodeFile> = mutableListOf()
        request.codeLines.filterNot { it.isBlank() }.forEachIndexed { index, line ->
            if (line.startsWith(FILE_NAME_SEPARATOR) && line.endsWith(FILE_NAME_SEPARATOR)) {
                files.add(SourceCodeFile(line.getFileName(), mutableListOf()))
            } else {
                if (index == 0) {
                    files.add(SourceCodeFile("demo${request.params.language.extension}", mutableListOf()))
                }
                files.last().lines.add(line)
            }
        }
        files.forEach {
            it.createSourceFile(tmpFolder)
        }
    }

    private fun String.getFileName() =
            this.trim()
                .drop(FILE_NAME_SEPARATOR.length)
                .dropLast(FILE_NAME_SEPARATOR.length)
                .trim()

    /**
     * @property name
     * @property lines
     */
    private data class SourceCodeFile(
        val name: String,
        val lines: MutableList<String>
    ) {
        /**
         * @param tmpFolder
         */
        fun createSourceFile(tmpFolder: Path) {
            val file = (tmpFolder / name)
            file.writeLines(lines)
            log.info("Created a file with sources: ${file.fileName}")
        }
    }

    companion object {
        private val log: Logger = getLogger<CpgController>()
        private const val ERROR_DB = "Error happened on read/write from/to a graph database, check logs for more details"
        private const val ERROR_PARSING = "Error happened during the parsing of code to CPG, check logs for more details"
    }
}
