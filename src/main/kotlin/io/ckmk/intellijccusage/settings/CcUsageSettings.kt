package io.ckmk.intellijccusage.settings

import com.intellij.openapi.components.*

@State(
    name = "CcUsageSettings",
    storages = [Storage("ccusage.xml")]
)
@Service
class CcUsageSettings : PersistentStateComponent<CcUsageSettings.State> {
    
    data class State(
        var refreshIntervalSeconds: Long = 5,
        var enableStatusBarWidget: Boolean = true,
        var showDetailedTooltip: Boolean = true
    )
    
    private var state = State()
    
    override fun getState(): State = state
    
    override fun loadState(state: State) {
        this.state = state
    }
    
    var refreshIntervalSeconds: Long
        get() = state.refreshIntervalSeconds.coerceIn(1, 60) // 1-60 seconds
        set(value) {
            state.refreshIntervalSeconds = value.coerceIn(1, 60)
        }
    
    var enableStatusBarWidget: Boolean
        get() = state.enableStatusBarWidget
        set(value) {
            state.enableStatusBarWidget = value
        }
    
    var showDetailedTooltip: Boolean
        get() = state.showDetailedTooltip
        set(value) {
            state.showDetailedTooltip = value
        }
    
    companion object {
        fun getInstance(): CcUsageSettings = service()
    }
}