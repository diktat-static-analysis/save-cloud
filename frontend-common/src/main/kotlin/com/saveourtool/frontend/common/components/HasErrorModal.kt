@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.frontend.common.components

import com.saveourtool.frontend.common.externals.animations.ringLoader

import js.core.jso
import org.w3c.fetch.Response
import react.*

/**
 * Loader animation
 */
@Suppress("MAGIC_NUMBER", "MagicNumber")
val ringLoader = ringLoader(jso {
    this.size = 80
    this.loading = true
    this.color = "#3a00c2"
})

/**
 * Context to store data about current request such as errors and isLoading flag.
 */
@Suppress("TYPE_ALIAS")
val requestStatusContext: Context<RequestStatusContext?> = createContext()

/**
 * @property setResponse [StateSetter] for response error handler
 * @property setLoadingCounter [StateSetter] for active request counter
 * @property setRedirectToFallbackView
 */
data class RequestStatusContext(
    val setResponse: StateSetter<Response?>,
    val setRedirectToFallbackView: StateSetter<Boolean>,
    val setLoadingCounter: StateSetter<Int>,
)

/**
 * @property isErrorModalOpen
 * @property errorMessage
 * @property errorLabel
 * @property confirmationText text that will be displayed on modal dismiss button
 * @property status
 * @property redirectToFallbackView
 */
data class ErrorModalState(
    val isErrorModalOpen: Boolean,
    val errorMessage: String,
    val errorLabel: String,
    val confirmationText: String = "Close",
    val status: Short?,
    val redirectToFallbackView: Boolean = false,
)

/**
 * @property isLoadingModalOpen
 */
data class LoadingModalState(
    val isLoadingModalOpen: Boolean,
)
