package com.saveourtool.save.storage

import com.saveourtool.common.storage.concatS3Key
import com.saveourtool.common.storage.s3KeyToParts
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class S3StorageUtilsKtTest {
    @Test
    fun testConcatS3Key() {
        doTestConcatS3Key("prefix", "suffix", "prefix/suffix")
        doTestConcatS3Key("prefix/", "suffix", "prefix/suffix")
        doTestConcatS3Key("prefix", "/suffix", "prefix/suffix")
        doTestConcatS3Key("prefix/", "/suffix", "prefix/suffix")
        doTestConcatS3Key("prefix", "", "prefix")
        doTestConcatS3Key("prefix/", "", "prefix")
        doTestConcatS3Key("", "suffix", "suffix")
        doTestConcatS3Key("", "/suffix", "suffix")

        Assertions.assertThrows(IllegalArgumentException::class.java) {
            concatS3Key("", "")
        }

        Assertions.assertEquals(
            "prefix/middle/suffix",
            concatS3Key("prefix", "/middle", "/suffix")
        )
    }

    private fun doTestConcatS3Key(prefix: String, suffix: String, expectedValue: String) {
        Assertions.assertEquals(
            expectedValue,
            concatS3Key(prefix, suffix)
        )
    }

    @Test
    fun testS3KeyToParts() {
        doTestS3KeyToParts("only-prefix/", listOf("only-prefix"))
        doTestS3KeyToParts("prefix/suffix", listOf("prefix", "suffix"))
        doTestS3KeyToParts("prefix/middle/suffix", listOf("prefix", "middle", "suffix"))
        doTestS3KeyToParts("/prefix/middle/suffix", listOf("prefix", "middle", "suffix"))
        doTestS3KeyToParts("prefix/middle/suffix/", listOf("prefix", "middle", "suffix"))
    }

    private fun doTestS3KeyToParts(s3Key: String, expectedValue: List<String>) {
        Assertions.assertEquals(
            expectedValue,
            s3Key.s3KeyToParts()
        )
    }
}
