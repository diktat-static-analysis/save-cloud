@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.inputform

import com.saveourtool.common.entities.OrganizationDto
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.utils.DEFAULT_DEBOUNCE_PERIOD
import com.saveourtool.frontend.common.components.basic.renderAvatar
import com.saveourtool.frontend.common.components.basic.renderFilterAvatar
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import org.w3c.fetch.Response
import react.*
import react.dom.html.AutoComplete
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.option
import web.cssom.*
import web.html.InputType
import web.timers.setTimeout

import kotlin.time.Duration.Companion.milliseconds

private const val DROPDOWN_ID = "option-dropdown"

/**
 * Component that encapsulates debounced prefix autocompletion over [UserInfo.name]
 */
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val inputWithDebounceForUserInfo = inputWithDebounce(
    asOption = { UserInfo(name = this) },
    asString = { name },
    // for Vulnerability Collection View this index should be bigger than for Organizations
    zindexShift = 1,
    decodeListFromJsonString = { decodeFromJsonString() },
)

/**
 * Component that encapsulates debounced prefix autocompletion over [OrganizationDto.name]
 */
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val inputWithDebounceForOrganizationDto = inputWithDebounce(
    asOption = { OrganizationDto(name = this) },
    asString = { name },
    decodeListFromJsonString = { decodeFromJsonString() },
)

/**
 * Component that encapsulates debounced prefix autocompletion over [String]
 */
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val inputWithDebounceForString = inputWithDebounce(
    asOption = { this },
    asString = { this },
    decodeListFromJsonString = { decodeFromJsonString() },
)

/**
 * [Props] for [inputWithDebounce] component
 */
external interface InputWithDebounceProps<T> : PropsWithChildren {
    /**
     * Callback to get url for options fetch
     */
    var getUrlForOptionsFetch: (prefix: String) -> String

    /**
     * Currently selected option
     */
    var selectedOption: T

    /**
     * Callback to set selected option
     */
    var setSelectedOption: (option: T) -> Unit

    /**
     * Callback to create [option] tag from [T]
     */
    @Suppress("VARIABLE_NAME_INCORRECT_FORMAT", "TYPE_ALIAS")
    var renderOption: (ChildrenBuilder, option: T) -> Unit

    /**
     * Debounce period, equals to [DEFAULT_DEBOUNCE_PERIOD] by default
     */
    var debouncePeriod: Int?

    /**
     * Placeholder for form
     */
    var placeholder: String

    /**
     * Callback invoked on option click
     */
    var onOptionClick: (option: T) -> Unit

    /**
     * Maximum amount of options to display
     */
    var maxOptions: Int?

    /**
     * Flag that defines if input form is disabled or not
     */
    var isDisabled: Boolean?
}

/**
 * Default renderer for [inputWithDebounceForUserInfo]
 *
 * @param childrenBuilder [ChildrenBuilder] instance
 * @param userInfo user's [UserInfo]
 */
fun renderUserWithAvatar(childrenBuilder: ChildrenBuilder, userInfo: UserInfo) {
    with(childrenBuilder) {
        div {
            className = ClassName("row d-flex align-items-center")
            renderAvatar(userInfo) {
                width = 2.rem
                height = 2.rem
            }
            h6 {
                className = ClassName("col-auto mb-0")
                +userInfo.name
            }
        }
    }
}

/**
 * Default renderer for [inputWithDebounceForOrganizationDto]
 *
 * @param childrenBuilder [ChildrenBuilder] instance
 * @param organizationDto [OrganizationDto] to display
 */
fun renderOrganizationWithAvatar(childrenBuilder: ChildrenBuilder, organizationDto: OrganizationDto) {
    with(childrenBuilder) {
        div {
            className = ClassName("row d-flex align-items-center")
            style = jso {
                fontSize = 1.2.rem
            }
            renderFilterAvatar(organizationDto) {
                width = 2.rem
                height = 2.rem
            }
            h6 {
                className = ClassName("col-auto mb-0")
                +organizationDto.name
            }
        }
    }
}

/**
 * Default renderer for [inputWithDebounceForString]
 *
 * @param childrenBuilder [ChildrenBuilder] instance
 * @param stringOption option to display as [String]
 */
fun renderString(childrenBuilder: ChildrenBuilder, stringOption: String) {
    with(childrenBuilder) {
        h6 {
            className = ClassName("text-sm align-middle m-0")
            +stringOption
        }
    }
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun <T> inputWithDebounce(
    zindexShift: Int = 0,
    asOption: String.() -> T,
    asString: T.() -> String,
    decodeListFromJsonString: suspend Response.() -> List<T>,
) = FC<InputWithDebounceProps<T>> { props ->
    val (options, setOptions) = useState<List<T>?>(null)
    val getOptions = useDebouncedDeferredRequest(props.debouncePeriod ?: DEFAULT_DEBOUNCE_PERIOD) {
        if (props.selectedOption.asString().isNotBlank()) {
            val optionsFromBackend: List<T> = get(
                url = props.getUrlForOptionsFetch(props.selectedOption.asString()),
                headers = jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )
                .unsafeMap { it.decodeListFromJsonString() }
            setOptions(optionsFromBackend)
        } else {
            setOptions(emptyList())
        }
    }

    useEffect(props.selectedOption) { getOptions() }

    div {
        className = ClassName("")
        id = "input-with-$DROPDOWN_ID"
        style = jso {
            width = "100%".unsafeCast<Width>()
            position = "relative".unsafeCast<Position>()
            zIndex = (1 + zindexShift).unsafeCast<ZIndex>()
        }
        onBlur = { setTimeout(ON_BLUR_TIMEOUT_MILLIS.milliseconds) { setOptions(null) } }
        div {
            className = ClassName("input-group")
            input {
                className = ClassName("form-control")
                id = "input-with-autocompletion"
                type = InputType.text
                placeholder = props.placeholder
                autoComplete = "off".unsafeCast<AutoComplete>()
                value = props.selectedOption.asString()
                disabled = props.isDisabled
                onChange = { props.setSelectedOption(it.target.value.asOption()) }
            }
            props.children?.let { +it }
        }
        if (props.isDisabled != true) {
            div {
                className = ClassName("list-group")
                id = DROPDOWN_ID
                style = jso {
                    position = "absolute".unsafeCast<Position>()
                    top = "100%".unsafeCast<Top>()
                    width = "max(100%, 10rem)".unsafeCast<Width>()
                    zIndex = "3".unsafeCast<ZIndex>()
                    overflowY = "scroll".unsafeCast<Overflow>()
                }
                if (props.selectedOption.asString().isNotEmpty() && options?.isEmpty() == true) {
                    div {
                        className = ClassName("list-group-item")
                        style = jso {
                            zIndex = "4".unsafeCast<ZIndex>()
                        }
                        +"Could not find anything that starts with ${props.selectedOption.asString()}..."
                    }
                } else {
                    options.let { optionList -> props.maxOptions?.let { optionList?.take(it) } ?: options }
                        ?.forEachIndexed { idx, option ->
                            div {
                                className = ClassName("list-group-item list-group-item-action")
                                style = jso {
                                    zIndex = "4".unsafeCast<ZIndex>()
                                }
                                id = "$DROPDOWN_ID-$idx"
                                onClick = { props.onOptionClick(option) }
                                props.renderOption(this, option)
                            }
                        }
                }
            }
        }
    }
}
