package com.saveourtool.save.test.analysis.algorithms

import com.saveourtool.common.test.analysis.metrics.RegularTestMetrics
import com.saveourtool.common.test.analysis.metrics.TestMetrics
import com.saveourtool.common.test.analysis.results.IrregularTest
import com.saveourtool.save.test.analysis.api.TestRuns

/**
 * A heuristic algorithm which accepts test history ([TestRuns]), pre-calculated
 * metrics ([TestMetrics]) and returns either a result ([IrregularTest]), or
 * `null` if this algorithm hasn't detected anything special about the test.
 */
fun interface Algorithm : Function2<TestRuns, RegularTestMetrics, IrregularTest?>
