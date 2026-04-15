package com.travelagent.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 无障碍服务 - 用于自动化操作其他App
 * 
 * 功能：
 * 1. 打开指定App
 * 2. 查找并点击UI元素
 * 3. 输入文本
 * 4. 滚动页面
 * 5. 读取屏幕内容
 */
class TravelAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "TravelAccessibility"
        
        // 单例引用
        var instance: TravelAccessibilityService? = null
            private set
        
        // 服务状态
        private val _isServiceEnabled = MutableStateFlow(false)
        val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled
        
        // 当前屏幕内容
        private val _screenContent = MutableStateFlow<List<String>>(emptyList())
        val screenContent: StateFlow<List<String>> = _screenContent
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentPackage: String = ""
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceEnabled.value = true
        Log.i(TAG, "无障碍服务已连接")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceEnabled.value = false
        serviceScope.cancel()
        Log.i(TAG, "无障碍服务已断开")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                currentPackage = event.packageName?.toString() ?: ""
                Log.d(TAG, "窗口切换: $currentPackage")
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // 页面内容变化时，可以触发内容读取
            }
        }
    }
    
    override fun onInterrupt() {
        Log.w(TAG, "无障碍服务被中断")
    }
    
    // ============ 公开的自动化方法 ============
    
    /**
     * 打开指定App
     */
    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Log.i(TAG, "启动App: $packageName")
                true
            } else {
                Log.e(TAG, "找不到App: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动App失败: ${e.message}")
            false
        }
    }
    
    /**
     * 等待指定包名的窗口出现
     */
    suspend fun waitForPackage(packageName: String, timeoutMs: Long = 5000): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (currentPackage == packageName) {
                return true
            }
            delay(200)
        }
        return false
    }
    
    /**
     * 查找包含指定文本的节点
     */
    fun findNodeByText(text: String, exact: Boolean = false): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return findNodeByTextRecursive(rootNode, text, exact)
    }
    
    private fun findNodeByTextRecursive(
        node: AccessibilityNodeInfo,
        text: String,
        exact: Boolean
    ): AccessibilityNodeInfo? {
        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        
        val matches = if (exact) {
            nodeText == text || contentDesc == text
        } else {
            nodeText.contains(text, ignoreCase = true) || 
            contentDesc.contains(text, ignoreCase = true)
        }
        
        if (matches) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByTextRecursive(child, text, exact)
            if (result != null) {
                return result
            }
        }
        
        return null
    }
    
    /**
     * 查找指定ID的节点
     */
    fun findNodeById(viewId: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        return nodes.firstOrNull()
    }
    
    /**
     * 点击节点
     */
    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        // 首先尝试直接点击
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        
        // 如果节点不可点击，尝试点击父节点
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            parent = parent.parent
        }
        
        // 如果都不行，使用手势点击
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return performClick(bounds.centerX().toFloat(), bounds.centerY().toFloat())
    }
    
    /**
     * 在指定坐标执行点击
     */
    fun performClick(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }
        
        val path = Path().apply {
            moveTo(x, y)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        
        return dispatchGesture(gesture, null, null)
    }
    
    /**
     * 输入文本
     */
    fun inputText(node: AccessibilityNodeInfo, text: String): Boolean {
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }
    
    /**
     * 点击包含指定文本的元素
     */
    fun clickByText(text: String, exact: Boolean = false): Boolean {
        val node = findNodeByText(text, exact)
        return if (node != null) {
            clickNode(node)
        } else {
            Log.w(TAG, "找不到包含文本的元素: $text")
            false
        }
    }
    
    /**
     * 滚动页面
     */
    fun scroll(direction: ScrollDirection): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val scrollableNode = findScrollableNode(rootNode)
        
        return if (scrollableNode != null) {
            val action = when (direction) {
                ScrollDirection.UP -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                ScrollDirection.DOWN -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            }
            scrollableNode.performAction(action)
        } else {
            // 使用手势滚动
            performSwipe(direction)
        }
    }
    
    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findScrollableNode(child)
            if (result != null) {
                return result
            }
        }
        
        return null
    }
    
    private fun performSwipe(direction: ScrollDirection): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }
        
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        val centerX = screenWidth / 2f
        val (startY, endY) = when (direction) {
            ScrollDirection.DOWN -> screenHeight * 0.7f to screenHeight * 0.3f
            ScrollDirection.UP -> screenHeight * 0.3f to screenHeight * 0.7f
        }
        
        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        
        return dispatchGesture(gesture, null, null)
    }
    
    /**
     * 读取当前屏幕上的所有文本
     */
    fun readScreenContent(): List<String> {
        val rootNode = rootInActiveWindow ?: return emptyList()
        val texts = mutableListOf<String>()
        collectTexts(rootNode, texts)
        _screenContent.value = texts
        return texts
    }
    
    private fun collectTexts(node: AccessibilityNodeInfo, texts: MutableList<String>) {
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTexts(child, texts)
        }
    }
    
    /**
     * 返回上一页
     */
    fun goBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }
    
    /**
     * 返回主屏幕
     */
    fun goHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }
    
    /**
     * 打开最近任务
     */
    fun openRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }
    
    enum class ScrollDirection {
        UP, DOWN
    }
}
