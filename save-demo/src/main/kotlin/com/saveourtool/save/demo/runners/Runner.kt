package com.saveourtool.save.demo.runners

import com.saveourtool.common.demo.DemoResult
import com.saveourtool.common.demo.DemoRunRequest
import reactor.core.publisher.Mono

/**
 * Interface should be implemented by all the runners.
 */
interface Runner {
    /**
     * @param demoRunRequest params of type [DemoRunRequest]
     * @return tool's report as [DemoResult]
     */
    fun run(demoRunRequest: DemoRunRequest): Mono<DemoResult>
}
