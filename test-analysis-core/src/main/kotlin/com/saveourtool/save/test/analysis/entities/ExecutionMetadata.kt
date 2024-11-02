package com.saveourtool.save.test.analysis.entities

import com.saveourtool.common.entities.Execution
import com.saveourtool.common.entities.TestExecution

/**
 * Metadata of an execution.
 *
 * @property organizationName the name of the organization.
 * @property projectName the name of the project.
 * @property testSuiteSourceName the name of the test suite source.
 * @property testSuiteVersion the version of the test suite.
 */
open class ExecutionMetadata(
    val organizationName: OrganizationName,
    val projectName: ProjectName,
    val testSuiteSourceName: TestSuiteSourceName?,
    val testSuiteVersion: TestSuiteVersion?,
) {
    /**
     * Extends this execution metadata with [testExecution] metadata.
     *
     * @param testExecution the test execution within this execution.
     * @return the newly-created test execution metadata instance.
     */
    fun extendWith(testExecution: TestExecution): TestExecutionMetadata =
            testExecution.test.run {
                TestExecutionMetadata(
                    organizationName,
                    projectName,
                    testSuiteSourceName,
                    testSuiteVersion,
                    testSuite.name(),
                    pluginName(),
                    filePath(),
                )
            }
}

/**
 * @return the metadata of this execution.
 */
fun Execution.metadata(): ExecutionMetadata {
    val project = project

    return ExecutionMetadata(
        project.organization.name(),
        project.name(),
        testSuiteSourceName(),
        version(),
    )
}
