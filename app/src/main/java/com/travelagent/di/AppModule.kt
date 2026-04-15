package com.travelagent.di

import android.content.Context
import com.travelagent.agents.AgentCoordinator
import com.travelagent.data.repository.ClaudeApiService
import com.travelagent.services.AppAutomationController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideClaudeApiService(): ClaudeApiService {
        return ClaudeApiService()
    }
    
    @Provides
    @Singleton
    fun provideAppAutomationController(
        @ApplicationContext context: Context
    ): AppAutomationController {
        return AppAutomationController(context)
    }
    
    @Provides
    @Singleton
    fun provideAgentCoordinator(
        claudeApiService: ClaudeApiService,
        appAutomationController: AppAutomationController
    ): AgentCoordinator {
        return AgentCoordinator(claudeApiService, appAutomationController)
    }
}
