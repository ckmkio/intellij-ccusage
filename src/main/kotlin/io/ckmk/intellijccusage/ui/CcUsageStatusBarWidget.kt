package io.ckmk.intellijccusage.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.util.Consumer
import io.ckmk.intellijccusage.model.CcUsageResponse
import io.ckmk.intellijccusage.notification.CcUsageNotifications
import io.ckmk.intellijccusage.service.CcUsageService
import io.ckmk.intellijccusage.settings.CcUsageSettings
import kotlinx.coroutines.*
import java.awt.event.MouseEvent
import javax.swing.JLabel

class CcUsageStatusBarWidget(project: Project) : EditorBasedWidget(project), StatusBarWidget.TextPresentation {
    
    private val logger = Logger.getInstance(CcUsageStatusBarWidget::class.java)
    private val ccUsageService = service<CcUsageService>()
    private val settings = CcUsageSettings.getInstance()
    
    private var coroutineScope: CoroutineScope? = null
    private var currentData: CcUsageResponse? = null
    private var isAvailable: Boolean = false
    
    companion object {
        const val ID = "CcUsageStatusBarWidget"
    }
    
    init {
        startDataFetching()
    }
    
    override fun ID(): String = ID
    
    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this
    
    override fun getText(): String {
        return when {
            !isAvailable -> "ccusage: Not Available"
            currentData == null -> "ccusage: Loading..."
            else -> currentData!!.getDisplayText()
        }
    }
    
    override fun getAlignment(): Float = 0.0f
    
    override fun getTooltipText(): String {
        return when {
            !isAvailable -> "ccusage is not installed or not available. Please install it via npm: npm install -g ccusage"
            currentData == null -> "Loading Claude Code usage data..."
            else -> buildTooltipText(currentData!!)
        }
    }
    
    override fun getClickConsumer(): Consumer<MouseEvent>? {
        return Consumer { event ->
            if (event.clickCount == 1) {
                // Refresh data on click
                refreshData()
            }
        }
    }
    
    private fun buildTooltipText(data: CcUsageResponse): String {
        val tooltip = StringBuilder("Claude Code Usage:\n")
        
        val activeBlock = data.blocks.firstOrNull { !it.isGap && it.isActive != false }
        val latestBlock = data.blocks.filterNot { it.isGap }.maxByOrNull { it.startTime ?: "" }
        val currentBlock = activeBlock ?: latestBlock
        
        if (currentBlock != null) {
            currentBlock.startTime?.let { tooltip.append("Session Start: $it\n") }
            currentBlock.endTime?.let { tooltip.append("Session End: $it\n") }
            currentBlock.usageLimitResetTime?.let { tooltip.append("Reset Time: $it\n") }
            
            if (currentBlock.totalTokens > 0) {
                tooltip.append("Tokens: ${currentBlock.totalTokens}\n")
            }
            
            if (currentBlock.costUSD > 0) {
                tooltip.append("Cost: $${String.format("%.4f", currentBlock.costUSD)}\n")
            }
            
            currentBlock.models.firstOrNull()?.let { model ->
                if (model != "<synthetic>") {
                    tooltip.append("Model: $model\n")
                }
            }
            
            tooltip.append("Entries: ${currentBlock.entries}\n")
        }
        
        tooltip.append("\nClick to refresh")
        
        return tooltip.toString().trim()
    }
    
    private fun startDataFetching() {
        coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        
        coroutineScope?.launch {
            // Check availability first
            withContext(Dispatchers.IO) {
                isAvailable = ccUsageService.isCcUsageAvailable()
            }
            
            if (!isAvailable) {
                updateStatusBar()
                CcUsageNotifications.showCcUsageNotAvailable(project)
                return@launch
            }
            
            // Start periodic data fetching
            while (isActive) {
                try {
                    val result = ccUsageService.fetchCcUsageData()
                    result.onSuccess { data ->
                        currentData = data
                        updateStatusBar()
                    }.onFailure { error ->
                        logger.debug("Failed to fetch ccusage data", error)
                        // Keep showing last known data
                    }
                } catch (e: Exception) {
                    logger.debug("Exception during data fetch", e)
                }
                
                delay(settings.refreshIntervalSeconds * 1000L)
            }
        }
    }
    
    private fun refreshData() {
        coroutineScope?.launch {
            try {
                val result = ccUsageService.fetchCcUsageData()
                result.onSuccess { data ->
                    currentData = data
                    updateStatusBar()
                }.onFailure { error ->
                    logger.warn("Failed to refresh ccusage data", error)
                }
            } catch (e: Exception) {
                logger.warn("Exception during manual refresh", e)
            }
        }
    }
    
    private fun updateStatusBar() {
        ApplicationManager.getApplication().invokeLater {
            myStatusBar?.updateWidget(ID)
        }
    }
    
    override fun dispose() {
        coroutineScope?.cancel()
        super.dispose()
    }
}