package com.saveourtool.save.test.analysis.api

import com.saveourtool.common.entities.Test

/**
 * A test executed within a particular project (the same test executed in a
 * different project even within the same organization will have a different
 * [hash]).
 *
 * This is similar to [Test.hash] in a sense that both identify a series of test
 * runs (so that these runs can be statistically analyzed).
 * The difference is that the same [Test] executed for different projects
 * (e.g.: _KtLint_ vs _Diktat_, or _Diktat 1.2.3_ vs Diktat _1.2.4_) or within
 * different organizations will have different [TestId]'s.
 *
 * In terms of CI and unit testing, [TestId] is like a _JUnit_ or _TestNG_ test
 * name (`ClassName.testMethodName`), partitioned by organization name and
 * project name.
 *
 * To create a [TestId] instance, use [TestIdGenerator.testId].
 *
 * @param hash the SHA hash that uniquely identifies this test.
 */
@JvmInline
value class TestId internal constructor(@Suppress("UnusedPrivateProperty") private val hash: String)
