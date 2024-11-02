/**
 * Git utilities that are used in preprocessor for download/clone/update git repo
 */

package com.saveourtool.save.preprocessor.utils

import com.saveourtool.common.entities.GitDto
import com.saveourtool.common.utils.debug
import org.eclipse.jgit.api.*
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.TagOpt
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.jetbrains.annotations.Blocking
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Instant

private val log = LoggerFactory.getLogger(object {}.javaClass.enclosingClass::class.java)

/**
 * @return default branch name
 * @throws IllegalStateException when failed to detect default branch name
 */
fun GitDto.detectDefaultBranchName() = Git.lsRemoteRepository()
    .setCredentialsProvider(credentialsProvider())
    .setRemote(url)
    .gitCallWithRethrow {
        it.callAsMap()
    }
    ?.findDefaultBranchName()
    ?.also { defaultBranch ->
        log.debug { "Getting default branch name $defaultBranch for httpUrl $url" }
    }
    ?: throw IllegalStateException("Couldn't detect default branch name for $url")

/**
 * @param branch
 * @param pathToDirectory
 * @return commit id and timestamp as [Instant]
 * @throws IllegalStateException
 */
@Blocking
fun GitDto.cloneBranchToDirectory(branch: String, pathToDirectory: Path): GitCommitInfo = doCloneToDirectory(pathToDirectory, branchWithPrefix = branch to Constants.R_HEADS)

/**
 * @param tagName
 * @param pathToDirectory
 * @return commit id and timestamp as [Instant]
 * @throws IllegalStateException
 */
@Blocking
fun GitDto.cloneTagToDirectory(tagName: String, pathToDirectory: Path): GitCommitInfo = doCloneToDirectory(pathToDirectory, branchWithPrefix = tagName to Constants.R_TAGS)

/**
 * @param commitId
 * @param pathToDirectory
 * @return commit id and timestamp as [Instant]
 * @throws IllegalStateException
 */
@Blocking
fun GitDto.cloneCommitToDirectory(commitId: String, pathToDirectory: Path): GitCommitInfo = doCloneToDirectory(pathToDirectory, commitToCheckout = commitId)

/**
 * Sorted set of tags for [GitDto]
 *
 * @return list of tags
 * @throws IllegalStateException
 */
fun GitDto.detectTagList(): Collection<String> = Git.lsRemoteRepository()
    .setCredentialsProvider(credentialsProvider())
    .setRemote(url)
    .setHeads(false)
    .setTags(true)
    .gitCallWithRethrow { it.callAsMap() }
    .keys
    .filter { it.startsWith(Constants.R_TAGS) }
    .map { it.removePrefix(Constants.R_TAGS) }
    .toSortedSet()

/**
 * Sorted set of branches for [GitDto], where default branch comes at first position
 *
 * @return list of branches
 * @throws IllegalStateException
 */
fun GitDto.detectBranchList(): Collection<String> = Git.lsRemoteRepository()
    .setCredentialsProvider(credentialsProvider())
    .setRemote(url)
    .gitCallWithRethrow { it.callAsMap() }
    .let { map ->
        val defaultBranch =
                map.findDefaultBranchName() ?: throw IllegalStateException("Couldn't detect default branch name for $url")
        val branches = map
            .keys
            .filter { it.startsWith(Constants.R_HEADS) }
            .map { it.removePrefix(Constants.R_HEADS) }
            .filterNot { it == defaultBranch }
            .toSortedSet()
        sortedSetOf(defaultBranch) + branches
    }

private fun Map<String, Ref>.findDefaultBranchName(): String? = this[Constants.HEAD]
    ?.takeIf { it.isSymbolic }
    ?.target
    ?.name
    ?.replace(Constants.R_HEADS, "")

@Blocking
private fun GitDto.doCloneToDirectory(
    pathToDirectory: Path,
    branchWithPrefix: Pair<String, String>? = null,
    commitToCheckout: String? = null,
): GitCommitInfo = Git.cloneRepository()
    .setCredentialsProvider(credentialsProvider())
    .setURI(url)
    .setDirectory(pathToDirectory.toFile())
    .setRemote(Constants.DEFAULT_REMOTE_NAME)
    .let { command ->
        branchWithPrefix?.let { (branch, branchToClonePrefix) ->
            command.setBranch(branch)
                .setCloneAllBranches(false)
                .setTagOption(TagOpt.AUTO_FOLLOW)
                .setBranchesToClone(listOf("$branchToClonePrefix$branch"))
        } ?: command.setNoTags()
            .setBranch(Constants.HEAD)
    }
    .callWithRethrow()
    .use { git ->
        commitToCheckout?.let { sha1 ->
            git.checkout()
                .setName(sha1)
                .callWithRethrow()
        }
        withRethrow {
            val objectId = git.repository.resolve(Constants.HEAD)
            GitCommitInfo(
                id = objectId.name,
                time = RevWalk(git.repository).use {
                    it.parseCommit(objectId)
                        .authorIdent
                        .whenAsInstant
                }
            )
        }
    }

private fun GitDto.credentialsProvider(): CredentialsProvider? = when {
    username != null && password != null -> UsernamePasswordCredentialsProvider(username, password)
    // https://stackoverflow.com/questions/28073266/how-to-use-jgit-to-push-changes-to-remote-with-oauth-access-token
    username == null && password != null -> UsernamePasswordCredentialsProvider(password, "")
    username == null && password == null -> CredentialsProvider.getDefault()
    else -> throw IllegalArgumentException("Unexpected git credentials")
}

private fun <R, T : GitCommand<*>> T.gitCallWithRethrow(call: (T) -> R): R = withRethrow {
    call(this)
}

private fun <R, T : GitCommand<R>> T.callWithRethrow(): R = gitCallWithRethrow {
    call()
}

private fun <R> withRethrow(action: () -> R): R {
    try {
        return action()
    } catch (ex: GitAPIException) {
        throw IllegalStateException("Error in JGit API", ex)
    }
}
