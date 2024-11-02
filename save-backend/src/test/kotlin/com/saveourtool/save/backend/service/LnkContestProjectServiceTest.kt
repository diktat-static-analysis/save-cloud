package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.LnkContestProjectRepository
import com.saveourtool.common.entities.Contest
import com.saveourtool.common.entities.Execution
import com.saveourtool.common.entities.LnkContestProject
import com.saveourtool.common.entities.Project
import com.saveourtool.common.service.ProjectService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.Optional
import kotlin.math.abs

@ExtendWith(SpringExtension::class)
@Import(LnkContestProjectService::class)
@MockBeans(
    MockBean(ProjectService::class),
)
@Suppress("UnsafeCallOnNullableType")
class LnkContestProjectServiceTest {
    @Autowired private lateinit var lnkContestProjectService: LnkContestProjectService
    @MockBean private lateinit var lnkContestProjectRepository: LnkContestProjectRepository
    @MockBean private lateinit var lnkContestExecutionService: LnkContestExecutionService

    @Test
    fun `should update best score if it is empty`() {
        givenOldBestExecution(null)

        lnkContestProjectService.updateBestExecution(Execution.stub(Project.stub(99)).apply { score = 4.0 })

        then(lnkContestProjectRepository)
            .should(times(1))
            .save(argWhere {
                abs(it.bestExecution?.score!! - 4.0) < 1e-4
            })
    }

    @Test
    fun `should update best score if the new one is greater`() {
        givenOldBestExecution(
            Execution.stub(Project.stub(99)).apply {
                id = 99
                score = 3.3
            }
        )

        lnkContestProjectService.updateBestExecution(Execution.stub(Project.stub(99)).apply { score = 4.0 })

        then(lnkContestProjectRepository)
            .should(times(1))
            .save(argWhere {
                abs(it.bestExecution?.score!! - 4.0) < 1e-4
            })
    }

    @Test
    fun `should not update best score if the new one is smaller`() {
        givenOldBestExecution(
            Execution.stub(Project.stub(99)).apply {
                id = 99
                score = 5.0
            }
        )

        lnkContestProjectService.updateBestExecution(Execution.stub(Project.stub(99)).apply { score = 4.4 })

        then(lnkContestProjectRepository)
            .should(never())
            .save(any())
    }

    private fun givenOldBestExecution(oldBestExecution: Execution?) {
        given(lnkContestExecutionService.findContestByExecution(any()))
            .willReturn(Contest.stub(99))
        @Suppress("PARAMETER_NAME_IN_OUTER_LAMBDA")
        given(lnkContestProjectRepository.findByContestAndProject(any(), any()))
            .willAnswer {
                LnkContestProject(it.arguments[1] as Project, it.arguments[0] as Contest, oldBestExecution)
                    .let { Optional.of(it) }
            }
    }
}
