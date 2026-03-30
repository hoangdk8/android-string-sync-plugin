package com.atu.tools.stringsync.util

import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project

object NotificationUtils {
    private const val GROUP_ID = "Đồng bộ String"

    fun info(project: Project, title: String, content: String) {
        Notifications.Bus.notify(
            com.intellij.notification.Notification(GROUP_ID, title, content, NotificationType.INFORMATION),
            project
        )
    }

    fun warn(project: Project, title: String, content: String) {
        Notifications.Bus.notify(
            com.intellij.notification.Notification(GROUP_ID, title, content, NotificationType.WARNING),
            project
        )
    }

    fun error(project: Project, title: String, content: String) {
        Notifications.Bus.notify(
            com.intellij.notification.Notification(GROUP_ID, title, content, NotificationType.ERROR),
            project
        )
    }
}
