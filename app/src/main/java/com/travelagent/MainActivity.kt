package com.travelagent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.travelagent.ui.MainViewModel
import com.travelagent.ui.Screen
import com.travelagent.ui.screens.*
import com.travelagent.ui.theme.Background
import com.travelagent.ui.theme.TravelAgentTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            TravelAgentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background
                ) {
                    TravelAgentApp()
                }
            }
        }
    }
}

@Composable
fun TravelAgentApp(
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val agentStates by viewModel.agentStates.collectAsStateWithLifecycle()
    val agentLogs by viewModel.agentLogs.collectAsStateWithLifecycle()
    val isPlanning by viewModel.isPlanning.collectAsStateWithLifecycle()
    val travelPlan by viewModel.travelPlan.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    
    // 检查无障碍服务状态
    val isAccessibilityEnabled = viewModel.isAccessibilityServiceEnabled()
    
    // 自动跳转到输入页面（如果权限已满足）
    LaunchedEffect(isAccessibilityEnabled, installedApps) {
        if (uiState.currentScreen == Screen.PERMISSION && 
            isAccessibilityEnabled && 
            installedApps.any { it.isInstalled }) {
            viewModel.setScreen(Screen.INPUT)
        }
    }
    
    AnimatedContent(
        targetState = uiState.currentScreen,
        transitionSpec = {
            when {
                targetState.ordinal > initialState.ordinal -> {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> -width } + fadeOut()
                }
                else -> {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> width } + fadeOut()
                }
            }
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            Screen.PERMISSION -> {
                PermissionScreen(
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    installedApps = installedApps,
                    onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() },
                    onInstallApp = { packageName -> viewModel.installApp(packageName) },
                    onContinue = { viewModel.setScreen(Screen.INPUT) },
                    onRefresh = { viewModel.checkInstalledApps() }
                )
            }
            
            Screen.INPUT -> {
                InputScreen(
                    fromCity = uiState.fromCity,
                    toCity = uiState.toCity,
                    startDate = uiState.startDate,
                    endDate = uiState.endDate,
                    budget = uiState.budget,
                    peopleCount = uiState.peopleCount,
                    preferences = uiState.preferences,
                    onFromCityChange = { viewModel.updateFromCity(it) },
                    onToCityChange = { viewModel.updateToCity(it) },
                    onStartDateChange = { viewModel.updateStartDate(it) },
                    onEndDateChange = { viewModel.updateEndDate(it) },
                    onBudgetChange = { viewModel.updateBudget(it) },
                    onPeopleCountChange = { viewModel.updatePeopleCount(it) },
                    onPreferenceToggle = { viewModel.togglePreference(it) },
                    onStartPlanning = { viewModel.startPlanning() },
                    errorMessage = uiState.errorMessage,
                    onClearError = { viewModel.clearError() }
                )
            }
            
            Screen.PLANNING -> {
                PlanningScreen(
                    agentStates = agentStates,
                    agentLogs = agentLogs,
                    isPlanning = isPlanning
                )
            }
            
            Screen.RESULT -> {
                ResultScreen(
                    travelPlan = travelPlan,
                    onRestart = { viewModel.resetPlanning() }
                )
            }
        }
    }
}
