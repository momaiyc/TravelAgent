package com.travelagent.data.models

import java.time.LocalDate

/**
 * 旅行请求数据模型
 */
data class TravelRequest(
    val from: String,
    val to: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val budget: BudgetLevel,
    val peopleCount: Int,
    val preferences: List<TravelPreference>
)

enum class BudgetLevel(val displayName: String) {
    LOW("经济型"),
    MEDIUM("舒适型"),
    HIGH("豪华型")
}

enum class TravelPreference(val displayName: String, val emoji: String) {
    CULTURE("文化历史", "🏛️"),
    FOOD("美食探索", "🍜"),
    NATURE("自然风光", "🏞️"),
    SHOPPING("购物休闲", "🛍️"),
    FAMILY("亲子游", "👨‍👩‍👧"),
    COUPLE("情侣游", "💑"),
    ADVENTURE("探险刺激", "🎢"),
    RELAX("休闲放松", "🧘")
}

/**
 * Agent类型
 */
enum class AgentType(
    val displayName: String,
    val emoji: String,
    val colorName: String
) {
    COORDINATOR("协调者", "🎯", "coordinator"),
    TRANSPORT("交通专家", "🚄", "transport"),
    HOTEL("住宿顾问", "🏨", "hotel"),
    ATTRACTION("景点规划", "🗺️", "attraction"),
    FOOD("美食探索", "🍜", "food")
}

/**
 * Agent状态
 */
enum class AgentStatus {
    IDLE,       // 待命
    WORKING,    // 工作中
    COMPLETED,  // 已完成
    ERROR       // 出错
}

/**
 * Agent执行结果
 */
data class AgentResult(
    val agentType: AgentType,
    val status: AgentStatus,
    val data: Any? = null,
    val reasoning: String? = null,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 交通信息
 */
data class TrainInfo(
    val trainNo: String,
    val from: String,
    val to: String,
    val departTime: String,
    val arriveTime: String,
    val duration: String,
    val price: Double,
    val seatType: String
)

/**
 * 酒店信息
 */
data class HotelInfo(
    val name: String,
    val location: String,
    val price: Double,
    val rating: Float,
    val type: String,
    val amenities: List<String>,
    val imageUrl: String? = null
)

/**
 * 景点信息
 */
data class AttractionInfo(
    val name: String,
    val description: String,
    val duration: String,
    val ticketPrice: String,
    val category: String,
    val tips: String? = null,
    val imageUrl: String? = null
)

/**
 * 餐厅信息
 */
data class RestaurantInfo(
    val name: String,
    val cuisine: String,
    val specialty: String,
    val priceRange: String,
    val rating: Float,
    val address: String? = null,
    val imageUrl: String? = null
)

/**
 * 完整旅行方案
 */
data class TravelPlan(
    val request: TravelRequest,
    val transport: TransportPlan?,
    val hotel: HotelPlan?,
    val attractions: AttractionPlan?,
    val food: FoodPlan?,
    val estimatedCost: EstimatedCost,
    val summary: String
)

data class TransportPlan(
    val outbound: TrainInfo?,
    val returnTrip: TrainInfo?,
    val alternatives: List<TrainInfo> = emptyList()
)

data class HotelPlan(
    val recommended: HotelInfo?,
    val alternatives: List<HotelInfo> = emptyList()
)

data class AttractionPlan(
    val dailyItinerary: Map<Int, List<AttractionInfo>>,
    val allAttractions: List<AttractionInfo> = emptyList()
)

data class FoodPlan(
    val recommended: List<RestaurantInfo>,
    val mustTry: List<String> = emptyList()
)

data class EstimatedCost(
    val transport: Double,
    val hotel: Double,
    val attractions: Double,
    val food: Double,
    val total: Double
)

/**
 * 已安装App信息
 */
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isInstalled: Boolean,
    val canAutomate: Boolean = true
)

/**
 * App包名常量
 */
object AppPackages {
    const val XIAOHONGSHU = "com.xingin.xhs"
    const val CTRIP = "ctrip.android.view"
    const val RAILWAY_12306 = "com.MobileTicket"
    const val MEITUAN = "com.sankuai.meituan"
    const val DIANPING = "com.dianping.v1"
    const val FLIGGY = "com.taobao.trip"
    const val ALIPAY = "com.eg.android.AlipayGphone"
    
    val ALL_PACKAGES = listOf(
        XIAOHONGSHU to "小红书",
        CTRIP to "携程",
        RAILWAY_12306 to "铁路12306",
        MEITUAN to "美团",
        DIANPING to "大众点评"
    )
}
