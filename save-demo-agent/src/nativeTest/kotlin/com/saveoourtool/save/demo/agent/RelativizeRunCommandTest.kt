package com.saveoourtool.save.demo.agent

import com.saveourtool.common.demo.DemoRunRequest
import com.saveourtool.common.demo.RunConfiguration
import com.saveourtool.save.demo.agent.getRelativeRunCommand
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

class RelativizeRunCommandTest {
    private val pathToTempDir = "path/to/temp/dir".toPath()
    private val runRequest = DemoRunRequest(emptyList(), "test", null)

    @Test
    fun diktatRunCommandTest() {
        val runConfiguration = RunConfiguration(
            "Test.kt",
            null,
            mapOf(
                runRequest.mode to "./ktlint -R diktat-1.2.4.2.jar --disabled_rules=standard --reporter=plain,output=output.txt Test.kt"
            ),
            "output.txt"
        )
        assertEquals(
            runConfiguration.getRelativeRunCommand(runRequest, pathToTempDir),
            "./ktlint -R diktat-1.2.4.2.jar --disabled_rules=standard --reporter=plain,output=$pathToTempDir/output.txt $pathToTempDir/Test.kt"
        )
    }
}
