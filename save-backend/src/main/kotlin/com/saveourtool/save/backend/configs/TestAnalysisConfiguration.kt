package com.saveourtool.save.backend.configs

import com.saveourtool.common.utils.getLogger
import com.saveourtool.common.utils.info
import com.saveourtool.save.test.analysis.api.TestIdGenerator
import com.saveourtool.save.test.analysis.api.TestStatisticsStorage
import com.saveourtool.save.test.analysis.internal.MemoryBacked
import com.saveourtool.save.test.analysis.internal.MutableTestStatisticsStorage

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for Test Analysis components.
 */
@Configuration
class TestAnalysisConfiguration {
    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    private val logger = getLogger<TestAnalysisConfiguration>()

    /**
     * @param config the configuration.
     * @return the storage of statistical data about test runs.
     * @see TestStatisticsStorage
     */
    @Bean
    fun statisticsStorage(config: ConfigProperties): MutableTestStatisticsStorage {
        val slidingWindowSize = config.testAnalysisSettings.slidingWindowSize
        val statisticsStorage = MemoryBacked(slidingWindowSize = slidingWindowSize)
        logger.info {
            "Started a ${statisticsStorage.javaClass.simpleName} test statistics storage with a sliding window size of $slidingWindowSize"
        }
        return statisticsStorage
    }

    /**
     * @return the test id generator.
     * @see TestIdGenerator
     */
    @Bean
    fun testIdGenerator(): TestIdGenerator =
            TestIdGenerator()
}
