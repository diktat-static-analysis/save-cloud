package com.saveourtool.common.test.analysis.results

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

/**
 * The mix-in for [AnalysisResult].
 */
@JsonTypeInfo(use = CLASS, include = PROPERTY, property = "type")
@JsonSubTypes(
    Type(RegularTest::class),
    Type(FlakyTest::class),
    Type(Regression::class),
    Type(PermanentFailure::class),
)
@Suppress("UnnecessaryAbstractClass")
internal abstract class AnalysisResultMixin

/**
 * Registers the mix-in for [AnalysisResult].
 *
 * @return the original builder.
 */
fun Jackson2ObjectMapperBuilder.supportAnalysisResult(): Jackson2ObjectMapperBuilder =
        mixIn(AnalysisResult::class.java, AnalysisResultMixin::class.java)
