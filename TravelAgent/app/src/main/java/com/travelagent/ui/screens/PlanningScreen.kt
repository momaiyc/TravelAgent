package com.travelagent.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelagent.agents.AgentLog
import com.travelagent.data.models.AgentStatus
import com.travelagent.data.models.AgentType
import com.travelagent.ui.theme.*

@Composable
fun PlanningScreen(
    agentStates: Map<AgentType, AgentStatus>,
    agentLogs: List<AgentLog>,
    isPlanning: Boolean
) {
    val listState = rememberLazyListState()
    
    // 自动滚动到底部
    LaunchedEffect(agentLogs.size) {
        if (agentLogs.isNotEmpty()) {
            listState.animateScrollToItem(agentLogs.size - 1)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // 顶部标题
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Primary, Primary.copy(alpha = 0.9f))
                    )
                )
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "🤖 AI团队工作中",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "5个智能助手正在协同为您规划旅程",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        // Agent状态卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Agent 状态",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AgentType.values().forEach { agentType ->
                        AgentStatusIcon(
                            agentType = agentType,
                            status = agentStates[agentType] ?: AgentStatus.IDLE
                        )
                    }
                }
            }
        }
        
        // 执行日志
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "执行日志",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    
                    if (isPlanning) {
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        val infiniteTransition = rememberInfiniteTransition(label = "loading")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )
                        
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "加载中",
                            tint = Primary,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(rotation)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (agentLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⏳",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "等待Agent开始工作...",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(agentLogs) { log ->
                            LogItem(log = log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgentStatusIcon(
    agentType: AgentType,
    status: AgentStatus
) {
    val agentColor = when (agentType) {
        AgentType.COORDINATOR -> AgentCoordinatorColor
        AgentType.TRANSPORT -> AgentTransportColor
        AgentType.HOTEL -> AgentHotelColor
        AgentType.ATTRACTION -> AgentAttractionColor
        AgentType.FOOD -> AgentFoodColor
    }
    
    val agentLightColor = when (agentType) {
        AgentType.COORDINATOR -> AgentCoordinatorLight
        AgentType.TRANSPORT -> AgentTransportLight
        AgentType.HOTEL -> AgentHotelLight
        AgentType.ATTRACTION -> AgentAttractionLight
        AgentType.FOOD -> AgentFoodLight
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            // 外圈动画（工作中）
            if (status == AgentStatus.WORKING) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                
                Box(
                    modifier = Modifier
                        .size((48 * scale).dp)
                        .clip(CircleShape)
                        .background(agentColor.copy(alpha = 0.2f))
                )
            }
            
            // 主图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (status) {
                            AgentStatus.COMPLETED -> agentLightColor
                            AgentStatus.WORKING -> agentColor
                            AgentStatus.ERROR -> Error.copy(alpha = 0.2f)
                            else -> Color(0xFFF0F0F0)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (status) {
                    AgentStatus.COMPLETED -> {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = agentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    AgentStatus.ERROR -> {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = agentType.emoji,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = agentType.displayName,
            fontSize = 10.sp,
            color = if (status == AgentStatus.WORKING) agentColor else Color.Gray,
            fontWeight = if (status == AgentStatus.WORKING) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun LogItem(log: AgentLog) {
    val agentColor = when (log.agentType) {
        AgentType.COORDINATOR -> AgentCoordinatorColor
        AgentType.TRANSPORT -> AgentTransportColor
        AgentType.HOTEL -> AgentHotelColor
        AgentType.ATTRACTION -> AgentAttractionColor
        AgentType.FOOD -> AgentFoodColor
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = agentColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Agent标识
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(agentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = log.agentType.emoji,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = log.agentType.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = agentColor
            )
            
            Text(
                text = log.message,
                fontSize = 13.sp,
                color = OnBackground.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        
        // 时间戳
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(log.timestamp))
        Text(
            text = time,
            fontSize = 10.sp,
            color = Color.Gray
        )
    }
}
