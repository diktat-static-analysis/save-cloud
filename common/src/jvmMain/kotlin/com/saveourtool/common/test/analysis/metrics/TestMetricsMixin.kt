package com.saveourtool.common.test.analysis.metrics

import com.saveourtool.common.utils.supportKDuration
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

/**
 * The mix-in for [TestMetrics].
 */
@JsonTypeInfo(use = CLASS, include = PROPERTY, property = "type")
@JsonSubTypes(
    Type(RegularTestMetrics::class),
    Type(NoDataAvailable::class),
)
@Suppress(
    "unused",
    "UnnecessaryAbstractClass",
    "MISSING_KDOC_CLASS_ELEMENTS",
)
internal abstract class TestMetricsMixin {
    /**
     * @see RegularTestMetrics.runCount
     */
    @get:JsonIgnore
    abstract val runCount: Int

    /**
     * @see RegularTestMetrics.failureRatePercentage
     */
    @get:JsonIgnore
    abstract val failureRatePercentage: Int

    /**
     * @see RegularTestMetrics.failureRate
     */
    @get:JsonIgnore
    abstract val failureRate: Double

    /**
     * @see RegularTestMetrics.flipRatePercentage
     */
    @get:JsonIgnore
    abstract val flipRatePercentage: Int

    /**
     * @see RegularTestMetrics.flipRate
     */
    @get:JsonIgnore
    abstract val flipRate: Double
}

/**
 * Registers the mix-in for [TestMetrics].
 *
 * @return the original builder.
 */
fun Jackson2ObjectMapperBuilder.supportTestMetrics(): Jackson2ObjectMapperBuilder =
        supportKDuration()
            .mixIn(TestMetrics::class.java, TestMetricsMixin::class.java)
