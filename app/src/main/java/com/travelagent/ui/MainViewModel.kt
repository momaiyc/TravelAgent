package com.travelagent.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.travelagent.agents.AgentCoordinator
import com.travelagent.agents.AgentLog
import com.travelagent.data.models.*
import com.travelagent.services.AppAutomationController
import com.travelagent.services.TravelAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val agentCoordinator: AgentCoordinator,
    private val appAutomation: AppAutomationController
) : AndroidViewModel(application) {
    
    // UI状态
    private val _uiState = MutableStateFlow(TravelUiState())
    val uiState: StateFlow<TravelUiState> = _uiState.asStateFlow()
    
    // Agent状态
    val agentStates = agentCoordinator.agentStates
    val agentLogs = agentCoordinator.agentLogs
    val isPlanning = agentCoordinator.isPlanning
    val travelPlan = agentCoordinator.travelPlan
    
    // 无障碍服务状态
    val isAccessibilityEnabled = TravelAccessibilityService.isServiceEnabled
    
    // 已安装App状态
    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()
    
    init {
        checkInstalledApps()
    }
    
    /**
     * 检查已安装的App
     */
    fun checkInstalledApps() {
        _installedApps.value = appAutomation.getInstalledAppsStatus()
    }
    
    /**
     * 检查无障碍服务是否启用
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        val context = getApplication<Application>()
        val serviceName = "${context.packageName}/${TravelAccessibilityService::class.java.canonicalName}"
        
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED, 0
        )
        
        if (accessibilityEnabled != 1) return false
        
        val settingValue = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(settingValue)
        
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(serviceName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
    
    /**
     * 打开无障碍设置
     */
    fun openAccessibilitySettings() {
        val context = getApplication<Application>()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * 打开应用市场安装App
     */
    fun installApp(packageName: String) {
        appAutomation.openAppStore(packageName)
    }
    
    /**
     * 更新表单字段
     */
    fun updateFromCity(city: String) {
        _uiState.update { it.copy(fromCity = city) }
    }
    
    fun updateToCity(city: String) {
        _uiState.update { it.copy(toCity = city) }
    }
    
    fun updateStartDate(date: LocalDate) {
        _uiState.update { it.copy(startDate = date) }
    }
    
    fun updateEndDate(date: LocalDate) {
        _uiState.update { it.copy(endDate = date) }
    }
    
    fun updateBudget(budget: BudgetLevel) {
        _uiState.update { it.copy(budget = budget) }
    }
    
    fun updatePeopleCount(count: Int) {
        _uiState.update { it.copy(peopleCount = count) }
    }
    
    fun togglePreference(preference: TravelPreference) {
        _uiState.update { state ->
            val newPreferences = if (preference in state.preferences) {
                state.preferences - preference
            } else {
                state.preferences + preference
            }
            state.copy(preferences = newPreferences)
        }
    }
    
    /**
     * 开始规划
     */
    fun startPlanning() {
        val state = _uiState.value
        
        // 验证输入
        if (state.fromCity.isBlank() || state.toCity.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请填写出发地和目的地") }
            return
        }
        
        val request = TravelRequest(
            from = state.fromCity,
            to = state.toCity,
            startDate = state.startDate,
            endDate = state.endDate,
            budget = state.budget,
            peopleCount = state.peopleCount,
            preferences = state.preferences.toList()
        )
        
        viewModelScope.launch {
            _uiState.update { it.copy(currentScreen = Screen.PLANNING) }
            
            val result = agentCoordinator.startPlanning(request)
            
            if (result.isSuccess) {
                _uiState.update { it.copy(currentScreen = Screen.RESULT) }
            } else {
                _uiState.update { 
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "规划失败",
                        currentScreen = Screen.INPUT
                    )
                }
            }
        }
    }
    
    /**
     * 重新开始
     */
    fun resetPlanning() {
        _uiState.update { 
            TravelUiState()
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * 设置当前屏幕
     */
    fun setScreen(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }
}

/**
 * UI状态
 */
data class TravelUiState(
    val currentScreen: Screen = Screen.PERMISSION,
    val fromCity: String = "",
    val toCity: String = "",
    val startDate: LocalDate = LocalDate.now().plusDays(1),
    val endDate: LocalDate = LocalDate.now().plusDays(3),
    val budget: BudgetLevel = BudgetLevel.MEDIUM,
    val peopleCount: Int = 2,
    val preferences: Set<TravelPreference> = setOf(TravelPreference.CULTURE, TravelPreference.FOOD),
    val errorMessage: String? = null
)

enum class Screen {
    PERMISSION,  // 权限引导页
    INPUT,       // 输入表单页
    PLANNING,    // Agent执行页
    RESULT       // 结果展示页
}
