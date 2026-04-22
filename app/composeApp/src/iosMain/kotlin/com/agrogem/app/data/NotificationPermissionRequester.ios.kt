package com.agrogem.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit,
): NotificationPermissionRequester {
    return remember(onResult) {
        object : NotificationPermissionRequester {
            override fun request() {
                val center = UNUserNotificationCenter.currentNotificationCenter()
                center.requestAuthorizationWithOptions(
                    options = UNAuthorizationOptionAlert or
                        UNAuthorizationOptionBadge or
                        UNAuthorizationOptionSound,
                    completionHandler = { granted, _ ->
                        dispatch_async(dispatch_get_main_queue()) {
                            onResult(granted)
                        }
                    },
                )
            }
        }
    }
}
