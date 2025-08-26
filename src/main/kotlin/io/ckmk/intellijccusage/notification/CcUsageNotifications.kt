package io.ckmk.intellijccusage.notification

import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

object CcUsageNotifications {
    
    private val NOTIFICATION_GROUP = NotificationGroupManager.getInstance()
        .getNotificationGroup("ccusage.notifications")
        ?: NotificationGroupManager.getInstance().getNotificationGroup("IDE Internal Errors") // fallback
    
    fun showCcUsageNotAvailable(project: Project?) {
        val notification = NOTIFICATION_GROUP.createNotification(
            "ccusage Not Available",
            "ccusage is not installed or not accessible. Please install it to view Claude Code usage statistics.",
            NotificationType.WARNING
        )
        
        notification.addAction(object : AnAction("Install ccusage") {
            override fun actionPerformed(e: AnActionEvent) {
                notification.hideBalloon()
                showInstallationInstructions(project)
            }
        })
        
        notification.addAction(object : AnAction("Dismiss") {
            override fun actionPerformed(e: AnActionEvent) {
                notification.hideBalloon()
            }
        })
        
        notification.notify(project)
    }
    
    private fun showInstallationInstructions(project: Project?) {
        val notification = NOTIFICATION_GROUP.createNotification(
            "ccusage Installation",
            """
            To install ccusage, run one of these commands in your terminal:
            
            • npm install -g ccusage
            • bunx ccusage
            • npx ccusage
            
            After installation, restart the IDE to enable the status bar widget.
            """.trimIndent(),
            NotificationType.INFORMATION
        )
        
        notification.notify(project)
    }
    
    fun showDataFetchError(project: Project?, error: String) {
        val notification = NOTIFICATION_GROUP.createNotification(
            "ccusage Data Error",
            "Failed to fetch Claude Code usage data: $error",
            NotificationType.ERROR
        )
        
        notification.addAction(object : AnAction("Retry") {
            override fun actionPerformed(e: AnActionEvent) {
                notification.hideBalloon()
                // The widget will automatically retry on next interval
            }
        })
        
        notification.notify(project)
    }
}