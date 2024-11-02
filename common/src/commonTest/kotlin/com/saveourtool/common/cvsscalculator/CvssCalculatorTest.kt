package com.saveourtool.common.cvsscalculator

import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

class CvssCalculatorTest {

    @Test
    @JsName("parsingVector")
    fun `parsing vector`() {

        val vector = "CVSS:3.1/AV:A/AC:H/PR:N/UI:R/S:C/C:H/I:N/A:H"
        val score = calculateBaseScore(vector);

        assertEquals(score, 7.5f)
    }

}