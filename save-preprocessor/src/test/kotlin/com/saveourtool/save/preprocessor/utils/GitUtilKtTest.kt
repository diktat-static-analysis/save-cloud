package com.saveourtool.save.preprocessor.utils

import com.saveourtool.common.entities.GitDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.div

internal class GitUtilKtTest {
    @Test
    fun testTag(@TempDir tmpDir: Path) {
        gitDto.cloneTagToDirectory("v0.3.5", tmpDir / "tag")
    }

    @Test
    fun testBranch(@TempDir tmpDir: Path) {
        gitDto.cloneBranchToDirectory("infra/build-logic-includebuild", tmpDir / "branch")
    }

    @Test
    fun testCommit(@TempDir tmpDir: Path) {
        gitDto.cloneCommitToDirectory("8a8f164", tmpDir / "commit")
    }

    companion object {
        private val gitDto = GitDto("https://github.com/saveourtool/save-cli.git")
    }
}
