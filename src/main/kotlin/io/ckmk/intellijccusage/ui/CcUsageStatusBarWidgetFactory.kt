package io.ckmk.intellijccusage.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import io.ckmk.intellijccusage.settings.CcUsageSettings

class CcUsageStatusBarWidgetFactory : StatusBarWidgetFactory {
    
    override fun getId(): String = CcUsageStatusBarWidget.ID
    
    override fun getDisplayName(): String = "Claude Code Usage"
    
    override fun isAvailable(project: Project): Boolean {
        return CcUsageSettings.getInstance().enableStatusBarWidget
    }
    
    override fun createWidget(project: Project): StatusBarWidget {
        return CcUsageStatusBarWidget(project)
    }
    
    override fun disposeWidget(widget: StatusBarWidget) {
        widget.dispose()
    }
    
    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}