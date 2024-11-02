package com.saveourtool.frontend.common.components.views

import com.saveourtool.frontend.common.utils.ComponentWithScope
import com.saveourtool.frontend.common.utils.Style

import react.*

import kotlinx.browser.document

/**
 * Abstract view class that should be used in all functional views
 */
abstract class AbstractView<P : Props, S : State>(private val style: Style = Style.SAVE_DARK) : ComponentWithScope<P, S>() {
    // A small hack to avoid duplication of main content-wrapper from App.kt
    // We will have custom background only for sign-up and sign-in views
    override fun componentDidMount() {
        document.getElementById("main-body")?.apply {
            className = when (style) {
                Style.SAVE_DARK, Style.SAVE_LIGHT -> className.replace("vuln", "save")
                Style.VULN_DARK, Style.VULN_LIGHT -> className.replace("save", "vuln")
                Style.INDEX -> className.replace("vuln", "save")
            }
        }

        document.getElementById("content-wrapper")?.setAttribute(
            "style",
            "background: ${style.globalBackground}"
        )

        configureTopBar(style)
    }

    private fun configureTopBar(style: Style) {
        val topBar = document.getElementById("navigation-top-bar")
        topBar?.setAttribute(
            "class",
            "navbar navbar-expand ${style.topBarBgColor} navbar-dark topbar ${style.marginBottomForTopBar} " +
                    "static-top shadow mr-1 ml-1 rounded"
        )

        topBar?.setAttribute(
            "style",
            "background: ${style.topBarTransparency}"
        )

        val container = document.getElementById("common-save-container")
        container?.setAttribute(
            "class",
            "container-fluid ${style.borderForContainer}"
        )
    }
}
