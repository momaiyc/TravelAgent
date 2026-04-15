package com.travelagent.data.repository

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.travelagent.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Claude API 服务
 */
@Singleton
class ClaudeApiService @Inject constructor() {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    private val apiKey: String
        get() = BuildConfig.CLAUDE_API_KEY.ifEmpty { 
            // 如果BuildConfig中没有配置，尝试从系统属性获取
            System.getProperty("CLAUDE_API_KEY", "")
        }
    
    /**
     * 调用Claude API进行推理
     */
    suspend fun chat(
        systemPrompt: String,
        userMessage: String,
        maxTokens: Int = 1500
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isEmpty()) {
                return@withContext Result.failure(Exception("Claude API Key未配置"))
            }
            
            val requestBody = ClaudeRequest(
                model = "claude-sonnet-4-20250514",
                maxTokens = maxTokens,
                system = systemPrompt,
                messages = listOf(
                    ClaudeMessage(role = "user", content = userMessage)
                )
            )
            
            val jsonBody = gson.toJson(requestBody)
            
            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("Content-Type", "application/json")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                return@withContext Result.failure(Exception("API调用失败: ${response.code} - $errorBody"))
            }
            
            val responseBody = response.body?.string() ?: ""
            val claudeResponse = gson.fromJson(responseBody, ClaudeResponse::class.java)
            
            val textContent = claudeResponse.content
                .filterIsInstance<ClaudeContentBlock.Text>()
                .joinToString("\n") { it.text }
            
            Result.success(textContent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 检查API是否可用
     */
    suspend fun checkApiAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isEmpty()) return@withContext false
            
            // 发送一个简单的测试请求
            val result = chat(
                systemPrompt = "You are a helpful assistant.",
                userMessage = "Say 'OK' if you can hear me.",
                maxTokens = 10
            )
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }
}

// API 请求/响应数据类
data class ClaudeRequest(
    val model: String,
    @SerializedName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<ClaudeMessage>
)

data class ClaudeMessage(
    val role: String,
    val content: String
)

data class ClaudeResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeContentBlock>,
    val model: String,
    @SerializedName("stop_reason") val stopReason: String?
)

sealed class ClaudeContentBlock {
    data class Text(val type: String = "text", val text: String) : ClaudeContentBlock()
}

// 自定义反序列化适配器
class ClaudeContentBlockDeserializer : com.google.gson.JsonDeserializer<ClaudeContentBlock> {
    override fun deserialize(
        json: com.google.gson.JsonElement,
        typeOfT: java.lang.reflect.Type,
        context: com.google.gson.JsonDeserializationContext
    ): ClaudeContentBlock {
        val jsonObject = json.asJsonObject
        val type = jsonObject.get("type")?.asString
        
        return when (type) {
            "text" -> ClaudeContentBlock.Text(
                text = jsonObject.get("text")?.asString ?: ""
            )
            else -> ClaudeContentBlock.Text(text = "")
        }
    }
}
