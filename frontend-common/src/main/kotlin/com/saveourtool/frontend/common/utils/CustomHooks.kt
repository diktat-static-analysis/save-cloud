/**
 * Contains custom react hooks
 *
 * Keep in mind that hooks could only be used from functional components!
 */

package com.saveourtool.frontend.common.utils

import com.saveourtool.common.info.UserStatus
import com.saveourtool.common.utils.DEFAULT_DEBOUNCE_PERIOD
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.components.requestStatusContext
import com.saveourtool.frontend.common.externals.lodash.debounce

import js.core.jso
import org.w3c.dom.EventSource
import org.w3c.dom.EventSource.Companion.CONNECTING
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.*
import react.router.useNavigate

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion

/**
 * Hook that redirects to special pages depending on [status]
 *
 * @param status [UserStatus] of current user
 */
fun useUserStatusRedirects(status: UserStatus?) {
    val navigate = useNavigate()
    useEffect(status, window.location.pathname) {
        if (status == UserStatus.CREATED && window.location.pathname != "/${FrontendRoutes.TERMS_OF_USE}") {
            navigate("/${FrontendRoutes.REGISTRATION}", jso { replace = false })
        } else if (status == UserStatus.BANNED) {
            navigate("/${FrontendRoutes.BAN}", jso { replace = false })
        } else if (status == UserStatus.NOT_APPROVED) {
            navigate(to = "/${FrontendRoutes.THANKS_FOR_REGISTRATION}", jso { replace = false })
        }
    }
}

/**
 * Hook that redirects to index view when [predicate] is true
 *
 * @param dependencies dependencies to be put to underlying [useEffect]
 * @param predicate callback that defines whether redirect should be performed or not
 */
fun useRedirectToIndexIf(vararg dependencies: Any?, predicate: () -> Boolean) {
    val navigate = useNavigate()
    useEffect(dependencies) {
        if (predicate()) {
            navigate("/", jso { replace = true })
        }
    }
}

/**
 * Runs the provided [action] only once of first render
 *
 * @param action
 */
fun useOnce(action: () -> Unit) {
    val useOnceAction = useOnceAction()
    useOnceAction {
        action()
    }
}

/**
 * @return action which will be run once per function component
 */
fun useOnceAction(): (() -> Unit) -> Unit {
    val (isFirstRender, setFirstRender) = useState(true)
    return { action ->
        if (isFirstRender) {
            action()
            setFirstRender(false)
        }
    }
}

/**
 * Custom hook to enable tooltips.
 *
 * Requires element to have "data-toggle=tooltip" attribute set
 * Show timeout can be set by setting "data-show-timeout=100"
 * Hide timeout can be set by setting "data-hide-timeout=100"
 * In order to update the tooltip content dynamically, you need to change "data-original-title" attribute
 *
 * @see [enableTooltip]
 */
fun useTooltip() {
    useEffect { enableTooltip() }
}

/**
 * Custom hook to enable tooltips and popovers.
 */
fun useTooltipAndPopover() {
    useEffect {
        enableTooltipAndPopover()
        return@useEffect
    }
}

/**
 * Hook to get callbacks to perform requests in functional components.
 *
 * @param request
 * @return a function to trigger request execution.
 */
fun <R> useDeferredRequest(
    request: suspend WithRequestStatusContext.() -> R,
): () -> Unit {
    val scope = CoroutineScope(Dispatchers.Default)
    val context = useRequestStatusContext()
    val (isSending, setIsSending) = useState(false)
    useEffect(isSending) {
        if (!isSending) {
            return@useEffect
        }
        scope.launch {
            request(context)
            setIsSending(false)
        }.invokeOnCompletion {
            if (it != null && it !is CancellationException) {
                setIsSending(false)
            }
        }
        cleanup {
            if (scope.isActive) {
                scope.cancel()
            }
        }
    }
    val initiateSending: () -> Unit = {
        if (!isSending) {
            setIsSending(true)
        }
    }
    return initiateSending
}

/**
 * Hook to perform requests in functional components.
 *
 * @param dependencies
 * @param request
 */
fun <R> useRequest(
    dependencies: Array<dynamic> = emptyArray(),
    request: suspend WithRequestStatusContext.() -> R,
) {
    val scope = CoroutineScope(Dispatchers.Default)
    val context = useRequestStatusContext()

    useEffect(*dependencies) {
        scope.launch {
            request(context)
        }
        cleanup {
            if (scope.isActive) {
                scope.cancel()
            }
        }
    }
}

/**
 * @return [WithRequestStatusContext] implementation
 */
@Suppress("TOO_LONG_FUNCTION", "MAGIC_NUMBER")
fun useRequestStatusContext(): WithRequestStatusContext {
    val statusContext = useContext(requestStatusContext)
    val context = object : WithRequestStatusContext {
        override val coroutineScope = CoroutineScope(Dispatchers.Default)
        override fun setResponse(response: Response) {
            statusContext?.run {
                setResponse(response)
            }
        }
        override fun setRedirectToFallbackView(isNeedRedirect: Boolean, response: Response) {
            statusContext?.run {
                setRedirectToFallbackView(
                    isNeedRedirect && response.status == 404.toShort()
                )
            }
        }
        override fun setLoadingCounter(transform: (oldValue: Int) -> Int) {
            statusContext?.run { setLoadingCounter(transform) }
        }
    }
    return context
}

/**
 * @param colorStyle page style
 */
fun useBackground(colorStyle: Style) {
    useOnce {
        document.getElementById("main-body")?.apply {
            className = when (colorStyle) {
                Style.SAVE_DARK, Style.SAVE_LIGHT -> className.replace("vuln", "save")
                Style.VULN_DARK, Style.VULN_LIGHT -> className.replace("save", "vuln")
                Style.INDEX -> className.replace("vuln", "save")
            }
        }
    }
    useEffect {
        document.getElementById("content-wrapper")?.apply {
            setAttribute(
                "style",
                "background: ${colorStyle.globalBackground}",
            )
        }
        configureTopBar(colorStyle)
    }
}

/**
 * Creates a callback that is run synchronously.
 *
 * Only works inside functional components.
 *
 * @param effect the callback body (executed under [useEffect]).
 * @return a lambda that triggers the callback.
 * @see useEffect
 */
fun useDeferredEffect(
    effect: EffectBuilder.() -> Unit,
): () -> Unit {
    var isRunning by useState(initialValue = false)

    useEffect(isRunning) {
        if (!isRunning) {
            return@useEffect
        }

        effect()

        isRunning = false
    }

    return {
        if (!isRunning) {
            isRunning = true
        }
    }
}

/**
 * Reads the response of `text/event-stream` `Content-Type`.
 * _Server-Sent Events_ (SSE) are limited to `HTTP GET` method.
 *
 * Only works inside functional components.
 *
 * @param url the URL that accepts an `HTTP GET` and can respond with a
 *   `text/event-stream` `Content-Type`.
 * @param withCredentials whether
 *   [CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
 *   should be allowed, the default is `false`.
 * @param eventType the event type selector.
 *   The same HTTP endpoint may return events of different types, and this
 *   selector allows to receive only a certain subset of the whole event volume.
 *   The default is `message`.
 *   On the server side, use `org.springframework.http.codec.ServerSentEvent` to
 *   create an event with a custom type.
 * @param init invoked before an event stream is requested.
 *   Allowed to change the state of components.
 * @param onCompletion invoked if the response is `HTTP 200 OK`, and when the
 *   server closes the connection.
 *   Allowed to change the state of components.
 * @param onError invoked when the event source reports an error.
 *   Allowed to change the state of components.
 * @param onEvent invoked when a new event arrives.
 *   Allowed to change the state of components.
 * @return a lambda that triggers the callback.
 * @see useNdjson
 */
@Suppress(
    "LongParameterList",
    "TOO_MANY_PARAMETERS",
    "TYPE_ALIAS",
)
fun useEventStream(
    url: String,
    withCredentials: Boolean = false,
    eventType: String = "message",
    init: EffectBuilder.() -> Unit = {},
    onCompletion: EffectBuilder.() -> Unit = {},
    onError: EffectBuilder.(error: Event, readyState: Short) -> Unit = { _, _ -> },
    onEvent: EffectBuilder.(message: MessageEvent) -> Unit,
): () -> Unit =
        useDeferredEffect {
            init()

            val source = EventSource(
                url = url,
                eventSourceInitDict = jso {
                    this.withCredentials = withCredentials
                },
            )

            source.addEventListener(eventType, { event ->
                onEvent(event as MessageEvent)
            })

            source.onerror = { error ->
                if (source.readyState == CONNECTING) {
                    source.close()
                    onCompletion()
                } else {
                    onError(error, source.readyState)
                }
            }
        }

/**
 * [useState] modification in order to support default state value from [Props].
 * Usually data passed with [Props] is not up-to-date yet and expected to be updated (e.g. when the response is received)
 * In this case, [useStateFromProps] should update the state once on first [valueFromProps] change.
 *
 * @param valueFromProps value that somehow depends on variable from [Props]
 * @param postProcess callback that should be applied to [valueFromProps] when [Props] are loaded
 * @return [StateInstance] of [valueFromProps] changing its value on first [valueFromProps] change
 * @see [useState]
 */
fun <T : Any> useStateFromProps(valueFromProps: T, postProcess: (T) -> T = { it }): StateInstance<T> {
    val state = useState(valueFromProps)
    val onceWrapper = useOnceAction()
    val reference = useRef(valueFromProps)
    useEffect(valueFromProps) {
        if (reference.current != valueFromProps) {
            onceWrapper {
                state.component2().invoke(valueFromProps.let(postProcess))
            }
        }
    }
    return state
}

/**
 * Hook to get callbacks to perform requests in functional components with [debounce].
 *
 * @param debouncePeriodMillis debounce period milliseconds
 * @param request request that should be sent
 * @return a function to trigger request execution.
 */
fun useDebouncedDeferredRequest(
    debouncePeriodMillis: Int = DEFAULT_DEBOUNCE_PERIOD,
    request: suspend WithRequestStatusContext.() -> Unit,
) = debounce(useDeferredRequest(request), debouncePeriodMillis)

/**
 * Reads the response of `application/x-ndjson` `Content-Type`.
 *
 * Only works inside functional components.
 *
 * @param url the URL that accepts an `HTTP GET` and can respond with a
 *   `application/x-ndjson` `Content-Type`.
 * @param init invoked before an event stream is requested.
 *   Allowed to change the state of components.
 * @param onCompletion invoked if the response is `HTTP 200 OK`, and when the
 *   server closes the connection.
 *   Allowed to change the state of components.
 * @param onError invoked when the event source reports an error.
 *   Allowed to change the state of components.
 * @param onEvent invoked when a new event arrives.
 *   Allowed to change the state of components.
 * @return a lambda that triggers the callback.
 * @see useEventStream
 */
internal fun useNdjson(
    url: String,
    init: () -> Unit = {},
    onCompletion: () -> Unit = {},
    onError: suspend (response: Response) -> Unit = { _ -> },
    onEvent: (message: String) -> Unit,
): () -> Unit =
        useDeferredRequest {
            init()

            val response = get(
                url = url,
                params = jso(),
                headers = Headers(jso {
                    Accept = "application/x-ndjson"
                }),
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )

            when {
                response.ok -> response
                    .readLines()
                    .filter(String::isNotEmpty)
                    .onCompletion {
                        onCompletion()
                    }
                    .collect(onEvent)

                else -> onError(response)
            }
        }
