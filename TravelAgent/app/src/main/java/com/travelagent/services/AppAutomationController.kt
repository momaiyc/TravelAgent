package com.travelagent.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.travelagent.data.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App自动化控制器
 * 
 * 负责通过无障碍服务或Intent调用其他App获取数据
 */
@Singleton
class AppAutomationController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "AppAutomation"
    }
    
    private val accessibilityService: TravelAccessibilityService?
        get() = TravelAccessibilityService.instance
    
    /**
     * 检查App是否已安装
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取所有相关App的安装状态
     */
    fun getInstalledAppsStatus(): List<InstalledAppInfo> {
        return AppPackages.ALL_PACKAGES.map { (packageName, appName) ->
            InstalledAppInfo(
                packageName = packageName,
                appName = appName,
                isInstalled = isAppInstalled(packageName),
                canAutomate = accessibilityService != null
            )
        }
    }
    
    /**
     * 打开App应用市场安装指定App
     */
    fun openAppStore(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // 如果没有应用市场，打开网页版
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
    
    // ============ 12306 自动化 ============
    
    /**
     * 在12306中搜索车票
     */
    suspend fun search12306Tickets(
        from: String,
        to: String,
        date: String
    ): Result<List<TrainInfo>> {
        val service = accessibilityService ?: return Result.failure(
            Exception("无障碍服务未启用")
        )
        
        return try {
            // 启动12306
            if (!service.launchApp(AppPackages.RAILWAY_12306)) {
                return Result.failure(Exception("无法启动12306"))
            }
            
            // 等待App启动
            delay(2000)
            
            // 点击首页的"车票"或搜索框
            service.clickByText("车票") || service.clickByText("查询")
            delay(1000)
            
            // 设置出发地
            service.findNodeByText("出发地")?.let { node ->
                service.clickNode(node)
                delay(500)
                service.findNodeByText(from)?.let { service.clickNode(it) }
                    ?: run {
                        // 如果没找到，尝试输入搜索
                        service.findNodeById("search_input")?.let { input ->
                            service.inputText(input, from)
                            delay(500)
                            service.findNodeByText(from)?.let { service.clickNode(it) }
                        }
                    }
            }
            delay(500)
            
            // 设置目的地
            service.findNodeByText("目的地")?.let { node ->
                service.clickNode(node)
                delay(500)
                service.findNodeByText(to)?.let { service.clickNode(it) }
            }
            delay(500)
            
            // 设置日期
            service.findNodeByText("出发日期")?.let { node ->
                service.clickNode(node)
                delay(500)
                // 选择日期（简化处理）
                service.clickByText(date.takeLast(2)) // 点击日期数字
            }
            delay(500)
            
            // 点击查询按钮
            service.clickByText("查询") || service.clickByText("搜索")
            delay(3000) // 等待结果加载
            
            // 读取搜索结果
            val screenContent = service.readScreenContent()
            val trainInfos = parseTrainResults(screenContent)
            
            // 返回原App
            service.goBack()
            delay(500)
            service.goBack()
            
            Result.success(trainInfos)
        } catch (e: Exception) {
            Log.e(TAG, "12306搜索失败: ${e.message}")
            Result.failure(e)
        }
    }
    
    private fun parseTrainResults(content: List<String>): List<TrainInfo> {
        // 简化的解析逻辑，实际需要根据12306的UI结构调整
        val trainInfos = mutableListOf<TrainInfo>()
        
        // 查找车次模式 (G/D/C/K/T/Z + 数字)
        val trainPattern = Regex("([GDCKTZ]\\d+)")
        val timePattern = Regex("(\\d{2}:\\d{2})")
        val pricePattern = Regex("([¥￥]?\\d+\\.?\\d*)")
        
        var i = 0
        while (i < content.size) {
            val text = content[i]
            val trainMatch = trainPattern.find(text)
            
            if (trainMatch != null) {
                val trainNo = trainMatch.value
                
                // 尝试提取后续信息
                val times = mutableListOf<String>()
                val prices = mutableListOf<String>()
                
                for (j in i until minOf(i + 10, content.size)) {
                    val item = content[j]
                    timePattern.findAll(item).forEach { times.add(it.value) }
                    pricePattern.findAll(item).forEach { prices.add(it.value) }
                }
                
                if (times.size >= 2) {
                    trainInfos.add(
                        TrainInfo(
                            trainNo = trainNo,
                            from = "", // 需要从上下文获取
                            to = "",
                            departTime = times.getOrElse(0) { "00:00" },
                            arriveTime = times.getOrElse(1) { "00:00" },
                            duration = calculateDuration(times.getOrElse(0) { "00:00" }, times.getOrElse(1) { "00:00" }),
                            price = prices.firstOrNull()?.replace("[¥￥]".toRegex(), "")?.toDoubleOrNull() ?: 0.0,
                            seatType = "二等座"
                        )
                    )
                }
            }
            i++
        }
        
        return trainInfos.take(5) // 只返回前5个结果
    }
    
    private fun calculateDuration(depart: String, arrive: String): String {
        try {
            val (dh, dm) = depart.split(":").map { it.toInt() }
            val (ah, am) = arrive.split(":").map { it.toInt() }
            
            var hours = ah - dh
            var minutes = am - dm
            
            if (minutes < 0) {
                hours -= 1
                minutes += 60
            }
            if (hours < 0) {
                hours += 24
            }
            
            return "${hours}h${minutes}m"
        } catch (e: Exception) {
            return "N/A"
        }
    }
    
    // ============ 携程自动化 ============
    
    /**
     * 在携程中搜索酒店
     */
    suspend fun searchCtripHotels(
        city: String,
        checkIn: String,
        checkOut: String
    ): Result<List<HotelInfo>> {
        val service = accessibilityService ?: return Result.failure(
            Exception("无障碍服务未启用")
        )
        
        return try {
            // 启动携程
            if (!service.launchApp(AppPackages.CTRIP)) {
                return Result.failure(Exception("无法启动携程"))
            }
            
            delay(2500)
            
            // 点击酒店tab
            service.clickByText("酒店") 
            delay(1000)
            
            // 输入城市
            service.clickByText("目的地") || service.clickByText("城市")
            delay(500)
            
            // 搜索城市
            val searchInput = service.findNodeByText("搜索") 
                ?: service.findNodeById("search_input")
            searchInput?.let { 
                service.inputText(it, city)
                delay(800)
                service.clickByText(city)
            }
            delay(500)
            
            // 点击搜索
            service.clickByText("搜索酒店") || service.clickByText("查询")
            delay(3000)
            
            // 读取结果
            val screenContent = service.readScreenContent()
            val hotels = parseHotelResults(screenContent)
            
            // 返回
            service.goBack()
            delay(500)
            service.goBack()
            
            Result.success(hotels)
        } catch (e: Exception) {
            Log.e(TAG, "携程酒店搜索失败: ${e.message}")
            Result.failure(e)
        }
    }
    
    private fun parseHotelResults(content: List<String>): List<HotelInfo> {
        val hotels = mutableListOf<HotelInfo>()
        val pricePattern = Regex("([¥￥]\\d+)")
        val ratingPattern = Regex("(\\d\\.\\d)分?")
        
        var currentHotel: String? = null
        
        for (text in content) {
            // 检测酒店名称（通常较长且包含"酒店"、"宾馆"等关键词）
            if (text.length > 4 && (text.contains("酒店") || text.contains("宾馆") || 
                text.contains("民宿") || text.contains("公寓"))) {
                currentHotel = text
            }
            
            // 如果有当前酒店名，尝试提取价格
            if (currentHotel != null) {
                val priceMatch = pricePattern.find(text)
                val ratingMatch = ratingPattern.find(text)
                
                if (priceMatch != null) {
                    hotels.add(
                        HotelInfo(
                            name = currentHotel,
                            location = "", // 需要从上下文获取
                            price = priceMatch.value.replace("[¥￥]".toRegex(), "").toDoubleOrNull() ?: 0.0,
                            rating = ratingMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 4.5f,
                            type = when {
                                currentHotel.contains("豪华") || currentHotel.contains("五星") -> "豪华型"
                                currentHotel.contains("精品") || currentHotel.contains("四星") -> "高档型"
                                currentHotel.contains("快捷") || currentHotel.contains("如家") -> "经济型"
                                else -> "舒适型"
                            },
                            amenities = listOf()
                        )
                    )
                    currentHotel = null
                }
            }
        }
        
        return hotels.take(5)
    }
    
    // ============ 小红书自动化 ============
    
    /**
     * 在小红书中搜索旅游攻略
     */
    suspend fun searchXiaohongshuGuides(
        destination: String,
        keywords: List<String> = listOf("攻略", "旅游", "景点")
    ): Result<List<AttractionInfo>> {
        val service = accessibilityService ?: return Result.failure(
            Exception("无障碍服务未启用")
        )
        
        return try {
            // 启动小红书
            if (!service.launchApp(AppPackages.XIAOHONGSHU)) {
                return Result.failure(Exception("无法启动小红书"))
            }
            
            delay(2500)
            
            // 点击搜索
            service.clickByText("搜索") || service.findNodeById("search_bar")?.let { service.clickNode(it) }
            delay(800)
            
            // 输入搜索词
            val searchQuery = "$destination ${keywords.first()}"
            val searchInput = service.findNodeByText("搜索") 
                ?: service.findNodeById("search_input")
            searchInput?.let { 
                service.inputText(it, searchQuery)
                delay(500)
                // 点击搜索按钮或回车
                service.clickByText("搜索")
            }
            delay(3000)
            
            // 读取搜索结果
            val screenContent = service.readScreenContent()
            val attractions = parseXiaohongshuResults(screenContent, destination)
            
            // 返回
            service.goBack()
            delay(500)
            service.goBack()
            
            Result.success(attractions)
        } catch (e: Exception) {
            Log.e(TAG, "小红书搜索失败: ${e.message}")
            Result.failure(e)
        }
    }
    
    private fun parseXiaohongshuResults(content: List<String>, destination: String): List<AttractionInfo> {
        val attractions = mutableListOf<AttractionInfo>()
        
        // 小红书内容通常是标题 + 描述的形式
        // 提取包含景点关键词的内容
        val attractionKeywords = listOf("景点", "打卡", "必去", "推荐", "网红", "拍照")
        
        for (text in content) {
            if (text.length > 10 && attractionKeywords.any { text.contains(it) }) {
                // 尝试提取景点名称
                val potentialNames = extractAttractionNames(text, destination)
                potentialNames.forEach { name ->
                    if (attractions.none { it.name == name }) {
                        attractions.add(
                            AttractionInfo(
                                name = name,
                                description = text.take(50),
                                duration = "2-3小时",
                                ticketPrice = "待查询",
                                category = "小红书推荐",
                                tips = "来自小红书用户推荐"
                            )
                        )
                    }
                }
            }
        }
        
        return attractions.take(6)
    }
    
    private fun extractAttractionNames(text: String, destination: String): List<String> {
        val names = mutableListOf<String>()
        
        // 常见景点后缀
        val suffixes = listOf("公园", "博物馆", "广场", "塔", "寺", "庙", "湖", "山", "园", "街", "路", "桥")
        
        for (suffix in suffixes) {
            val pattern = Regex("([\\u4e00-\\u9fa5]{2,6}$suffix)")
            pattern.findAll(text).forEach { match ->
                names.add(match.value)
            }
        }
        
        return names.distinct()
    }
    
    // ============ 美团自动化 ============
    
    /**
     * 在美团中搜索美食
     */
    suspend fun searchMeituanFood(
        city: String,
        keywords: List<String> = listOf()
    ): Result<List<RestaurantInfo>> {
        val service = accessibilityService ?: return Result.failure(
            Exception("无障碍服务未启用")
        )
        
        return try {
            // 启动美团
            if (!service.launchApp(AppPackages.MEITUAN)) {
                return Result.failure(Exception("无法启动美团"))
            }
            
            delay(2500)
            
            // 点击美食
            service.clickByText("美食") || service.clickByText("餐饮")
            delay(1500)
            
            // 如果有关键词，进行搜索
            if (keywords.isNotEmpty()) {
                service.clickByText("搜索")
                delay(500)
                val searchInput = service.findNodeByText("搜索")
                searchInput?.let { 
                    service.inputText(it, keywords.first())
                    delay(500)
                    service.clickByText("搜索")
                }
                delay(2000)
            }
            
            // 读取结果
            val screenContent = service.readScreenContent()
            val restaurants = parseMeituanResults(screenContent)
            
            // 返回
            service.goBack()
            delay(500)
            service.goBack()
            
            Result.success(restaurants)
        } catch (e: Exception) {
            Log.e(TAG, "美团搜索失败: ${e.message}")
            Result.failure(e)
        }
    }
    
    private fun parseMeituanResults(content: List<String>): List<RestaurantInfo> {
        val restaurants = mutableListOf<RestaurantInfo>()
        val pricePattern = Regex("人均[¥￥]?(\\d+)")
        val ratingPattern = Regex("(\\d\\.\\d)")
        
        var currentRestaurant: String? = null
        
        for (text in content) {
            // 检测餐厅名称
            if (text.length in 3..20 && !text.contains("人均") && !text.contains("¥")) {
                currentRestaurant = text
            }
            
            if (currentRestaurant != null) {
                val priceMatch = pricePattern.find(text)
                if (priceMatch != null) {
                    val ratingMatch = content.take(content.indexOf(text) + 5)
                        .mapNotNull { ratingPattern.find(it)?.value }
                        .firstOrNull()
                    
                    restaurants.add(
                        RestaurantInfo(
                            name = currentRestaurant,
                            cuisine = detectCuisine(currentRestaurant),
                            specialty = "",
                            priceRange = "人均${priceMatch.groupValues[1]}元",
                            rating = ratingMatch?.toFloatOrNull() ?: 4.5f
                        )
                    )
                    currentRestaurant = null
                }
            }
        }
        
        return restaurants.take(5)
    }
    
    private fun detectCuisine(name: String): String {
        return when {
            name.contains("火锅") -> "火锅"
            name.contains("烧烤") || name.contains("烤肉") -> "烧烤"
            name.contains("日料") || name.contains("寿司") -> "日料"
            name.contains("西餐") || name.contains("牛排") -> "西餐"
            name.contains("川菜") || name.contains("麻辣") -> "川菜"
            name.contains("粤菜") || name.contains("茶餐厅") -> "粤菜"
            name.contains("湘菜") -> "湘菜"
            else -> "中餐"
        }
    }
}
