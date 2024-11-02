/**
 * Authorization component (Oauth2 elements) for Index View
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.common.entities.NotificationDto
import com.saveourtool.common.utils.toUnixCalendarFormat
import com.saveourtool.frontend.common.components.basic.markdown
import com.saveourtool.frontend.common.externals.fontawesome.faTimes
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.noopLoadingHandler
import com.saveourtool.save.frontend.externals.i18next.useTranslation

import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.StateSetter
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.useState
import web.cssom.*

import kotlinx.browser.window
import kotlinx.datetime.TimeZone

const val INDEX_VIEW_CUSTOM_BG = "rgb(247, 250, 253)"
private const val DEFAULT_MAX_NOTIFICATION_AMOUNT = 5

@Suppress("IDENTIFIER_LENGTH")
val indexViewInfo: FC<UserInfoAwareProps> = FC { props ->
    val (t) = useTranslation("index")

    val (notifications, setNotifications) = useState(emptyList<NotificationDto>())
    val (isAllNotificationsShown, setIsAllNotificationsShown) = useState(false)
    val (notificationForDeletion, setNotificationForDeletion) = useState<NotificationDto?>(null)

    useRequest {
        props.userInfo?.let {
            val newNotifications = get(
                url = "$apiUrl/notifications/get-all-by-user",
                headers = jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
            ).unsafeMap {
                it.decodeFromJsonString<List<NotificationDto>>()
            }

            setNotifications(newNotifications)
        }
    }

    useRequest(arrayOf(notificationForDeletion)) {
        notificationForDeletion?.let { notification ->
            delete(
                url = "$apiUrl/notifications/delete-by-id",
                params = jso<dynamic> {
                    id = notification.id
                },
                headers = jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
            ).run {
                if (ok) {
                    setNotifications { it.minus(notification) }
                    setNotificationForDeletion(null)
                }
            }
        }
    }

    div {
        className = ClassName("row justify-content-center mt-5 text-gray-900")
        h2 {
            +"SaveOurTool!"
        }
    }
    div {
        className = ClassName("row justify-content-center")
        h4 {
            +"Non-profit Opensource Ecosystem with a focus on finding code bugs".t()
        }
    }
    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
    div {
        className = ClassName("row justify-content-center mt-2")
        cardUser { userInfo = props.userInfo }
        cardServiceInfo { }
        cardAboutUs { }
    }
    div {
        className = ClassName("row justify-content-center mt-5 text-gray-900")
        h2 {
            +"Notifications".t()
        }
    }
    div {
        className = ClassName("card mb-4 mr-3 ml-3")
        div {
            className = ClassName("card-body")
            if (notifications.isEmpty()) {
                p {
                    +"Your notifications will be located here.".t()
                }
            } else {
                if (notifications.size > DEFAULT_MAX_NOTIFICATION_AMOUNT && !isAllNotificationsShown) {
                    display(notifications.take(DEFAULT_MAX_NOTIFICATION_AMOUNT), setNotificationForDeletion)
                    div {
                        className = ClassName("col-12 mt-3 px-0")
                        onClick = { setIsAllNotificationsShown(true) }
                        style = jso { cursor = "pointer".unsafeCast<Cursor>() }
                        h4 {
                            className = ClassName("text-center card p-2 shadow")
                            +"Show all ${notifications.size} comments"
                        }
                    }
                } else {
                    display(notifications, setNotificationForDeletion)
                }
            }
        }
    }
}

/**
 * @param img to show it with description on welcome view
 */
@Suppress("MAGIC_NUMBER")
internal fun ChildrenBuilder.cardImage(img: String) {
    img {
        src = img
        style = jso {
            height = 14.rem
            width = 14.rem
        }
    }
}

/**
 * @param notifications list of notification
 * @param setNotificationForDeletion [StateSetter] for delete notifications
 */
internal fun ChildrenBuilder.display(
    notifications: List<NotificationDto>,
    setNotificationForDeletion: StateSetter<NotificationDto?>
) {
    notifications.forEach { notification ->
        div {
            className = ClassName("shadow-none card text-left border-0")
            div {
                className = ClassName("flex-wrap d-flex justify-content-between")
                style = jso { background = "#f1f1f1".unsafeCast<Background>() }
                span {
                    className = ClassName("ml-1")
                    +(notification.createDate?.toUnixCalendarFormat(TimeZone.currentSystemDefault()) ?: "Unknown")
                }
                div {
                    buttonBuilder(faTimes, style = "", classes = "btn-sm") {
                        if (window.confirm("Are you sure you want to delete a notification?")) {
                            setNotificationForDeletion(notification)
                        }
                    }
                }
            }
            div {
                className = ClassName("shadow-none card card-body border-0")
                markdown(notification.message.split("\n").joinToString("\n\n"))
            }
        }
    }
}
