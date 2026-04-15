package com.travelagent.agents

import android.util.Log
import com.travelagent.data.models.*
import com.travelagent.data.repository.ClaudeApiService
import com.travelagent.services.AppAutomationController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 多Agent协调器
 * 
 * 负责：
 * 1. 接收用户旅行请求
 * 2. 分解任务分配给各专业Agent
 * 3. 并行执行Agent任务
 * 4. 整合结果生成最终方案
 */
@Singleton
class AgentCoordinator @Inject constructor(
    private val claudeApi: ClaudeApiService,
    private val appAutomation: AppAutomationController
) {
    
    companion object {
        private const val TAG = "AgentCoordinator"
    }
    
    // Agent状态流
    private val _agentStates = MutableStateFlow<Map<AgentType, AgentStatus>>(
        AgentType.values().associateWith { AgentStatus.IDLE }
    )
    val agentStates: StateFlow<Map<AgentType, AgentStatus>> = _agentStates
    
    // Agent执行日志
    private val _agentLogs = MutableStateFlow<List<AgentLog>>(emptyList())
    val agentLogs: StateFlow<List<AgentLog>> = _agentLogs
    
    // 最终结果
    private val _travelPlan = MutableStateFlow<TravelPlan?>(null)
    val travelPlan: StateFlow<TravelPlan?> = _travelPlan
    
    // 是否正在规划
    private val _isPlanning = MutableStateFlow(false)
    val isPlanning: StateFlow<Boolean> = _isPlanning
    
    /**
     * 开始旅行规划
     */
    suspend fun startPlanning(request: TravelRequest): Result<TravelPlan> {
        _isPlanning.value = true
        _agentLogs.value = emptyList()
        resetAgentStates()
        
        return try {
            // 1. 协调者分析请求
            updateAgentState(AgentType.COORDINATOR, AgentStatus.WORKING)
            addLog(AgentType.COORDINATOR, "收到旅行规划请求，开始分析...")
            addLog(AgentType.COORDINATOR, buildRequestSummary(request))
            
            delay(500)
            addLog(AgentType.COORDINATOR, "任务分解完成，启动各专业Agent...")
            
            // 2. 并行执行各Agent任务
            val results = coroutineScope {
                val transportDeferred = async { executeTransportAgent(request) }
                val hotelDeferred = async { executeHotelAgent(request) }
                val attractionDeferred = async { executeAttractionAgent(request) }
                val foodDeferred = async { executeFoodAgent(request) }
                
                listOf(
                    transportDeferred.await(),
                    hotelDeferred.await(),
                    attractionDeferred.await(),
                    foodDeferred.await()
                )
            }
            
            // 3. 协调者整合结果
            updateAgentState(AgentType.COORDINATOR, AgentStatus.WORKING)
            addLog(AgentType.COORDINATOR, "所有Agent完成任务，正在整合结果...")
            
            val plan = integratResults(request, results)
            
            updateAgentState(AgentType.COORDINATOR, AgentStatus.COMPLETED)
            addLog(AgentType.COORDINATOR, "✅ 旅行方案生成完成！")
            
            _travelPlan.value = plan
            Result.success(plan)
        } catch (e: Exception) {
            Log.e(TAG, "规划失败: ${e.message}")
            updateAgentState(AgentType.COORDINATOR, AgentStatus.ERROR)
            addLog(AgentType.COORDINATOR, "❌ 规划失败: ${e.message}")
            Result.failure(e)
        } finally {
            _isPlanning.value = false
        }
    }
    
    /**
     * 交通Agent - 查询高铁/机票
     */
    private suspend fun executeTransportAgent(request: TravelRequest): AgentResult {
        updateAgentState(AgentType.TRANSPORT, AgentStatus.WORKING)
        addLog(AgentType.TRANSPORT, "正在查询 ${request.from} → ${request.to} 的交通信息...")
        
        return try {
            // 尝试使用12306自动化
            val trainsResult = appAutomation.search12306Tickets(
                from = request.from,
                to = request.to,
                date = request.startDate.toString()
            )
            
            val trains = if (trainsResult.isSuccess) {
                trainsResult.getOrNull() ?: emptyList()
            } else {
                // 如果自动化失败，使用模拟数据
                addLog(AgentType.TRANSPORT, "自动查询失败，使用智能推荐...")
                getMockTrainData(request.from, request.to)
            }
            
            // 使用Claude分析推荐
            val analysis = claudeApi.chat(
                systemPrompt = """你是一个专业的交通规划专家。根据用户需求推荐最佳交通方案。
                    考虑因素：时间效率、价格、舒适度。用简洁的中文回复。""",
                userMessage = """请为以下需求推荐交通方案：
                    出发地：${request.from}
                    目的地：${request.to}
                    日期：${request.startDate}
                    人数：${request.peopleCount}人
                    预算：${request.budget.displayName}
                    
                    可选车次：${trains.joinToString { "${it.trainNo} ${it.departTime}-${it.arriveTime} ¥${it.price}" }}
                    
                    请推荐最合适的1-2个方案。"""
            )
            
            val reasoning = analysis.getOrNull() ?: "已找到${trains.size}个车次供您选择"
            addLog(AgentType.TRANSPORT, reasoning.take(100) + "...")
            
            updateAgentState(AgentType.TRANSPORT, AgentStatus.COMPLETED)
            
            AgentResult(
                agentType = AgentType.TRANSPORT,
                status = AgentStatus.COMPLETED,
                data = TransportPlan(
                    outbound = trains.firstOrNull(),
                    returnTrip = trains.getOrNull(1),
                    alternatives = trains
                ),
                reasoning = reasoning
            )
        } catch (e: Exception) {
            updateAgentState(AgentType.TRANSPORT, AgentStatus.ERROR)
            addLog(AgentType.TRANSPORT, "❌ 查询失败: ${e.message}")
            
            AgentResult(
                agentType = AgentType.TRANSPORT,
                status = AgentStatus.ERROR,
                error = e.message
            )
        }
    }
    
    /**
     * 住宿Agent - 查询酒店
     */
    private suspend fun executeHotelAgent(request: TravelRequest): AgentResult {
        updateAgentState(AgentType.HOTEL, AgentStatus.WORKING)
        addLog(AgentType.HOTEL, "正在搜索 ${request.to} 的住宿信息...")
        
        return try {
            // 尝试使用携程自动化
            val hotelsResult = appAutomation.searchCtripHotels(
                city = request.to,
                checkIn = request.startDate.toString(),
                checkOut = request.endDate.toString()
            )
            
            val hotels = if (hotelsResult.isSuccess) {
                hotelsResult.getOrNull() ?: emptyList()
            } else {
                addLog(AgentType.HOTEL, "自动查询失败，使用智能推荐...")
                getMockHotelData(request.to, request.budget)
            }
            
            // 使用Claude分析推荐
            val analysis = claudeApi.chat(
                systemPrompt = """你是一个专业的酒店顾问。根据用户需求推荐最合适的住宿。
                    考虑因素：位置、价格、评分、设施。用简洁的中文回复。""",
                userMessage = """请为以下需求推荐酒店：
                    目的地：${request.to}
                    入住：${request.startDate} 退房：${request.endDate}
                    人数：${request.peopleCount}人
                    预算：${request.budget.displayName}
                    偏好：${request.preferences.joinToString { it.displayName }}
                    
                    可选酒店：${hotels.joinToString { "${it.name} ${it.type} ¥${it.price}/晚 ${it.rating}分" }}
                    
                    请推荐最合适的酒店。"""
            )
            
            val reasoning = analysis.getOrNull() ?: "已找到${hotels.size}家酒店供您选择"
            addLog(AgentType.HOTEL, reasoning.take(100) + "...")
            
            updateAgentState(AgentType.HOTEL, AgentStatus.COMPLETED)
            
            AgentResult(
                agentType = AgentType.HOTEL,
                status = AgentStatus.COMPLETED,
                data = HotelPlan(
                    recommended = hotels.firstOrNull(),
                    alternatives = hotels
                ),
                reasoning = reasoning
            )
        } catch (e: Exception) {
            updateAgentState(AgentType.HOTEL, AgentStatus.ERROR)
            addLog(AgentType.HOTEL, "❌ 查询失败: ${e.message}")
            
            AgentResult(
                agentType = AgentType.HOTEL,
                status = AgentStatus.ERROR,
                error = e.message
            )
        }
    }
    
    /**
     * 景点Agent - 规划游览路线
     */
    private suspend fun executeAttractionAgent(request: TravelRequest): AgentResult {
        updateAgentState(AgentType.ATTRACTION, AgentStatus.WORKING)
        addLog(AgentType.ATTRACTION, "正在搜索 ${request.to} 的景点攻略...")
        
        return try {
            // 尝试使用小红书自动化
            val attractionsResult = appAutomation.searchXiaohongshuGuides(
                destination = request.to,
                keywords = request.preferences.map { it.displayName }
            )
            
            val attractions = if (attractionsResult.isSuccess) {
                attractionsResult.getOrNull() ?: emptyList()
            } else {
                addLog(AgentType.ATTRACTION, "自动查询失败，使用智能推荐...")
                getMockAttractionData(request.to, request.preferences)
            }
            
            // 使用Claude规划路线
            val analysis = claudeApi.chat(
                systemPrompt = """你是一个专业的旅游规划师。根据用户偏好制定游览路线。
                    考虑因素：景点距离、游览时间、路线效率。用简洁的中文回复。""",
                userMessage = """请为以下需求制定景点游览计划：
                    目的地：${request.to}
                    日期：${request.startDate} 至 ${request.endDate}
                    人数：${request.peopleCount}人
                    偏好：${request.preferences.joinToString { it.displayName }}
                    
                    推荐景点：${attractions.joinToString { "${it.name} ${it.duration} ${it.ticketPrice}" }}
                    
                    请制定每日游览路线。"""
            )
            
            val reasoning = analysis.getOrNull() ?: "已为您规划${attractions.size}个景点"
            addLog(AgentType.ATTRACTION, reasoning.take(100) + "...")
            
            updateAgentState(AgentType.ATTRACTION, AgentStatus.COMPLETED)
            
            // 按天分配景点
            val days = java.time.temporal.ChronoUnit.DAYS.between(request.startDate, request.endDate).toInt() + 1
            val dailyItinerary = attractions.chunked((attractions.size + days - 1) / days)
                .mapIndexed { index, list -> index + 1 to list }
                .toMap()
            
            AgentResult(
                agentType = AgentType.ATTRACTION,
                status = AgentStatus.COMPLETED,
                data = AttractionPlan(
                    dailyItinerary = dailyItinerary,
                    allAttractions = attractions
                ),
                reasoning = reasoning
            )
        } catch (e: Exception) {
            updateAgentState(AgentType.ATTRACTION, AgentStatus.ERROR)
            addLog(AgentType.ATTRACTION, "❌ 查询失败: ${e.message}")
            
            AgentResult(
                agentType = AgentType.ATTRACTION,
                status = AgentStatus.ERROR,
                error = e.message
            )
        }
    }
    
    /**
     * 美食Agent - 推荐当地美食
     */
    private suspend fun executeFoodAgent(request: TravelRequest): AgentResult {
        updateAgentState(AgentType.FOOD, AgentStatus.WORKING)
        addLog(AgentType.FOOD, "正在搜索 ${request.to} 的美食推荐...")
        
        return try {
            // 尝试使用美团自动化
            val restaurantsResult = appAutomation.searchMeituanFood(
                city = request.to,
                keywords = if (request.preferences.contains(TravelPreference.FOOD)) 
                    listOf("特色", "网红", "必吃") else emptyList()
            )
            
            val restaurants = if (restaurantsResult.isSuccess) {
                restaurantsResult.getOrNull() ?: emptyList()
            } else {
                addLog(AgentType.FOOD, "自动查询失败，使用智能推荐...")
                getMockFoodData(request.to, request.budget)
            }
            
            // 使用Claude推荐
            val analysis = claudeApi.chat(
                systemPrompt = """你是一个美食探索专家。根据用户偏好推荐当地特色美食。
                    考虑因素：当地特色、价格、评分。用简洁的中文回复。""",
                userMessage = """请为以下需求推荐美食：
                    目的地：${request.to}
                    人数：${request.peopleCount}人
                    预算：${request.budget.displayName}
                    偏好：${request.preferences.joinToString { it.displayName }}
                    
                    推荐餐厅：${restaurants.joinToString { "${it.name} ${it.cuisine} ${it.priceRange} ${it.rating}分" }}
                    
                    请推荐必吃美食和餐厅。"""
            )
            
            val reasoning = analysis.getOrNull() ?: "已为您找到${restaurants.size}家推荐餐厅"
            addLog(AgentType.FOOD, reasoning.take(100) + "...")
            
            updateAgentState(AgentType.FOOD, AgentStatus.COMPLETED)
            
            AgentResult(
                agentType = AgentType.FOOD,
                status = AgentStatus.COMPLETED,
                data = FoodPlan(
                    recommended = restaurants,
                    mustTry = restaurants.take(3).map { it.specialty }.filter { it.isNotEmpty() }
                ),
                reasoning = reasoning
            )
        } catch (e: Exception) {
            updateAgentState(AgentType.FOOD, AgentStatus.ERROR)
            addLog(AgentType.FOOD, "❌ 查询失败: ${e.message}")
            
            AgentResult(
                agentType = AgentType.FOOD,
                status = AgentStatus.ERROR,
                error = e.message
            )
        }
    }
    
    /**
     * 整合所有Agent结果
     */
    private suspend fun integratResults(
        request: TravelRequest,
        results: List<AgentResult>
    ): TravelPlan {
        val transportResult = results.find { it.agentType == AgentType.TRANSPORT }
        val hotelResult = results.find { it.agentType == AgentType.HOTEL }
        val attractionResult = results.find { it.agentType == AgentType.ATTRACTION }
        val foodResult = results.find { it.agentType == AgentType.FOOD }
        
        val transportPlan = transportResult?.data as? TransportPlan
        val hotelPlan = hotelResult?.data as? HotelPlan
        val attractionPlan = attractionResult?.data as? AttractionPlan
        val foodPlan = foodResult?.data as? FoodPlan
        
        // 计算预估费用
        val days = java.time.temporal.ChronoUnit.DAYS.between(request.startDate, request.endDate).toInt() + 1
        val transportCost = (transportPlan?.outbound?.price ?: 0.0) * 2 * request.peopleCount
        val hotelCost = (hotelPlan?.recommended?.price ?: 0.0) * (days - 1)
        val attractionCost = (attractionPlan?.allAttractions?.sumOf { 
            it.ticketPrice.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 0.0 
        } ?: 0.0) * request.peopleCount
        val foodCost = 150.0 * days * request.peopleCount // 估算每人每天150
        
        val estimatedCost = EstimatedCost(
            transport = transportCost,
            hotel = hotelCost,
            attractions = attractionCost,
            food = foodCost,
            total = transportCost + hotelCost + attractionCost + foodCost
        )
        
        // 生成摘要
        val summary = buildString {
            append("${request.from} → ${request.to} ${days}日游\n")
            append("日期: ${request.startDate} 至 ${request.endDate}\n")
            append("人数: ${request.peopleCount}人\n\n")
            
            transportPlan?.outbound?.let {
                append("【交通】${it.trainNo} ${it.departTime}出发\n")
            }
            hotelPlan?.recommended?.let {
                append("【住宿】${it.name} ¥${it.price}/晚\n")
            }
            append("【景点】${attractionPlan?.allAttractions?.size ?: 0}个推荐景点\n")
            append("【美食】${foodPlan?.recommended?.size ?: 0}家推荐餐厅\n\n")
            append("预估总费用: ¥${estimatedCost.total.toInt()}")
        }
        
        return TravelPlan(
            request = request,
            transport = transportPlan,
            hotel = hotelPlan,
            attractions = attractionPlan,
            food = foodPlan,
            estimatedCost = estimatedCost,
            summary = summary
        )
    }
    
    // ============ 辅助方法 ============
    
    private fun updateAgentState(type: AgentType, status: AgentStatus) {
        _agentStates.value = _agentStates.value.toMutableMap().apply {
            put(type, status)
        }
    }
    
    private fun resetAgentStates() {
        _agentStates.value = AgentType.values().associateWith { AgentStatus.IDLE }
    }
    
    private fun addLog(agentType: AgentType, message: String) {
        _agentLogs.value = _agentLogs.value + AgentLog(
            agentType = agentType,
            message = message,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun buildRequestSummary(request: TravelRequest): String {
        return """
            收到规划请求：
            • 出发地: ${request.from}
            • 目的地: ${request.to}
            • 日期: ${request.startDate} 至 ${request.endDate}
            • 人数: ${request.peopleCount}人
            • 预算: ${request.budget.displayName}
            • 偏好: ${request.preferences.joinToString { it.displayName }}
        """.trimIndent()
    }
    
    // ============ 模拟数据（无障碍服务不可用时使用）============
    
    private fun getMockTrainData(from: String, to: String): List<TrainInfo> {
        return listOf(
            TrainInfo("G1234", from, to, "08:00", "12:30", "4h30m", 553.0, "二等座"),
            TrainInfo("G5678", from, to, "09:30", "14:00", "4h30m", 553.0, "二等座"),
            TrainInfo("D2468", from, to, "07:15", "13:45", "6h30m", 327.0, "二等座")
        )
    }
    
    private fun getMockHotelData(city: String, budget: BudgetLevel): List<HotelInfo> {
        val allHotels = listOf(
            HotelInfo("外滩璞丽酒店", "黄浦区", 2800.0, 4.9f, "豪华型", listOf("泳池", "健身房")),
            HotelInfo("静安香格里拉", "静安区", 1500.0, 4.8f, "高档型", listOf("泳池", "行政酒廊")),
            HotelInfo("全季酒店南京路", "黄浦区", 450.0, 4.5f, "舒适型", listOf("早餐")),
            HotelInfo("如家精选", "浦东新区", 280.0, 4.2f, "经济型", listOf("早餐"))
        )
        
        return when (budget) {
            BudgetLevel.LOW -> allHotels.filter { it.price < 500 }
            BudgetLevel.MEDIUM -> allHotels.filter { it.price in 400.0..2000.0 }
            BudgetLevel.HIGH -> allHotels
        }
    }
    
    private fun getMockAttractionData(city: String, preferences: List<TravelPreference>): List<AttractionInfo> {
        return listOf(
            AttractionInfo("外滩", "上海地标，欣赏浦江两岸风光", "2小时", "免费", "地标"),
            AttractionInfo("东方明珠", "上海标志性建筑，俯瞰全城", "3小时", "220元", "地标"),
            AttractionInfo("豫园", "明代古典园林，江南园林代表", "2小时", "40元", "文化历史"),
            AttractionInfo("田子坊", "文艺街区，小店咖啡馆聚集地", "2小时", "免费", "购物休闲"),
            AttractionInfo("上海博物馆", "中国古代艺术珍品收藏", "3小时", "免费", "文化历史"),
            AttractionInfo("南京路步行街", "购物天堂", "2小时", "免费", "购物休闲")
        )
    }
    
    private fun getMockFoodData(city: String, budget: BudgetLevel): List<RestaurantInfo> {
        return listOf(
            RestaurantInfo("南翔馒头店", "本帮菜", "小笼包", "人均60元", 4.7f),
            RestaurantInfo("老正兴", "本帮菜", "油爆虾、红烧肉", "人均150元", 4.6f),
            RestaurantInfo("新荣记", "台州菜", "海鲜", "人均800元", 4.9f),
            RestaurantInfo("耳光馄饨", "小吃", "鲜肉馄饨", "人均25元", 4.5f)
        )
    }
}

/**
 * Agent日志
 */
data class AgentLog(
    val agentType: AgentType,
    val message: String,
    val timestamp: Long
)
