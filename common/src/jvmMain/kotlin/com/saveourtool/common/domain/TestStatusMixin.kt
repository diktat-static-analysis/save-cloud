/**
 * Supports hierarchy in Jackson for [TestStatus]
 */

package com.saveourtool.common.domain

import com.saveourtool.save.core.result.*

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

/**
 * MixIn for [TestStatus]
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Pass::class),
    JsonSubTypes.Type(value = Fail::class),
    JsonSubTypes.Type(value = Ignored::class),
    JsonSubTypes.Type(value = Crash::class),
)
internal class TestStatusMixin

/**
 * Registers mixIn for [TestStatus]
 *
 * @return original builder
 */
fun Jackson2ObjectMapperBuilder.supportTestStatus(): Jackson2ObjectMapperBuilder =
        mixIn(TestStatus::class.java, TestStatusMixin::class.java)
