package com.saveourtool.save.frontend.utils

import com.saveourtool.save.entities.benchmarks.BenchmarkCategoryEnum
import com.saveourtool.save.validation.FrontendRoutes

/**
 * The class for analyzing url address and creating links in topBar
 * @property href
 */
class TopBarUrl(val href: String) {
    /**
     * CurrentPath is the link that we put in buttons
     */
    var currentPath = "#"
    private var circumstance: SituationUrlClassification = SituationUrlClassification.KEYWORD_PROCESS
    private var processLastSegments = 0
    private val sizeUrlSegments: Int = href.split("/").size

    init {
        findExclude(href)
    }

    /**
     * The function is called to specify the link address in the button before creating the button itself
     *
     * @param pathPart is an appended suffix to an already existing [currentPath]
     */
    fun changeUrlBeforeButton(pathPart: String) {
        currentPath = when (circumstance) {
            SituationUrlClassification.PROJECT, SituationUrlClassification.ORGANIZATION -> "#/${FrontendRoutes.PROJECTS.path}"
            SituationUrlClassification.ARCHIVE -> "#/${FrontendRoutes.AWESOME_BENCHMARKS.path}"
            SituationUrlClassification.DETAILS, SituationUrlClassification.EXECUTION -> if (pathPart == "execution") currentPath else mergeUrls(pathPart)
            else -> mergeUrls(pathPart)
        }
    }

    /**
     * The function is called to specify the link address in the button after creating the button itself
     *
     * @param pathPart is an appended suffix to an already existing [currentPath]
     */
    fun changeUrlAfterButton(pathPart: String) {
        fixCurrentPathAfter(pathPart)
        fixExcludeAfter("\\d+".toRegex().matches(pathPart))
    }

    /**
     * The function set a flag whether to create this button or not
     *
     * @param index
     */
    fun shouldDisplayPathFragment(index: Int) = when (circumstance) {
        SituationUrlClassification.KEYWORD_PROCESS_LAST_SEGMENTS -> index >= sizeUrlSegments - 1 - processLastSegments
        SituationUrlClassification.KEYWORD_NOT_PROCESS -> false
        else -> true
    }

    /**
     * The function classification url address
     *
     * @param href
     */
    private fun findExclude(href: String) {
        circumstance = SituationUrlClassification.values
            .filter { it.regex != null }
            .firstOrNull { href.contains(Regex(it.regex ?: "")) }
            ?: SituationUrlClassification.KEYWORD_PROCESS
    }

    /**
     * Function check exclude and generate currentPath after the buttons creating
     *
     * @param pathPart
     */
    private fun fixCurrentPathAfter(pathPart: String) {
        currentPath = when (circumstance) {
            SituationUrlClassification.PROJECT, SituationUrlClassification.ORGANIZATION, SituationUrlClassification.ARCHIVE -> "#"
            SituationUrlClassification.DETAILS, SituationUrlClassification.EXECUTION -> if (pathPart == "execution") mergeUrls(pathPart) else currentPath
            else -> currentPath
        }
    }

    /**
     * The function changes the exclude after the button is created
     *
     * @param isNumber
     */
    private fun fixExcludeAfter(isNumber: Boolean) {
        circumstance = when (circumstance) {
            SituationUrlClassification.PROJECT, SituationUrlClassification.ORGANIZATION, SituationUrlClassification.ARCHIVE -> SituationUrlClassification.KEYWORD_PROCESS
            SituationUrlClassification.DETAILS -> if (isNumber) setProcessLastSegments(1) else SituationUrlClassification.DETAILS
            else -> circumstance
        }
    }

    private fun mergeUrls(secondPath: String) = "$currentPath/$secondPath"

    private fun setProcessLastSegments(number: Int): SituationUrlClassification {
        processLastSegments = number
        return SituationUrlClassification.KEYWORD_PROCESS_LAST_SEGMENTS
    }

    /**
     * This Enum class classifies work with the url address segment
     * @property regex
     */
    sealed class SituationUrlClassification(val regex: String? = null) {
        init {
            lazyValues += lazy { this }
        }

        /**
         * Situation with the processing of the "archive" in the url address - need for tabs in AwesomeBenchmarksView
         */
        object ARCHIVE : SituationUrlClassification(BenchmarkCategoryEnum.regexForUrlClassification)

        /**
         * Situation with the processing of the "details" in the url address - need for deleted multi-segment urls, starting with the word "details"
         */
        object DETAILS : SituationUrlClassification("/[^/]+/[^/]+/history/execution/\\d+/details")

        /**
         * Situation with the processing of the "execution" in the url address - need for redirect to the page with the executions history
         */
        object EXECUTION : SituationUrlClassification("/[^/]+/[^/]+/history/execution")

        /**
         * The button with this url segment is not created
         */
        object KEYWORD_NOT_PROCESS : SituationUrlClassification()

        /**
         * The button with this url segment is created without changes
         */
        object KEYWORD_PROCESS : SituationUrlClassification()

        /**
         * The button is created if this segment is one of the last
         */
        object KEYWORD_PROCESS_LAST_SEGMENTS : SituationUrlClassification()

        /**
         * Situation with the processing of the "organization" in the url address - need for tabs in OrganizationView
         */
        object ORGANIZATION : SituationUrlClassification(OrganizationMenuBar.regexForUrlClassification)

        /**
         * Situation with the processing of the "project" in the url address - need for tabs in ProjectView
         */
        object PROJECT : SituationUrlClassification(ProjectMenuBar.regexForUrlClassification)

        companion object {
            private val lazyValues = mutableListOf<Lazy<SituationUrlClassification>>()
            val values: List<SituationUrlClassification> by lazy {
                lazyValues.map { it.value }
            }
        }
    }
}
