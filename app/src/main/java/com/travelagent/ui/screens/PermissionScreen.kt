package com.travelagent.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelagent.data.models.InstalledAppInfo
import com.travelagent.ui.theme.*

@Composable
fun PermissionScreen(
    isAccessibilityEnabled: Boolean,
    installedApps: List<InstalledAppInfo>,
    onOpenAccessibilitySettings: () -> Unit,
    onInstallApp: (String) -> Unit,
    onContinue: () -> Unit,
    onRefresh: () -> Unit
) {
    val canContinue = isAccessibilityEnabled && installedApps.any { it.isInstalled }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Primary, Primary.copy(alpha = 0.8f))
                )
            )
    ) {
        // 顶部区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🧭",
                        fontSize = 40.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "旅行智能助手",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "5个AI助手协同工作，为您定制完美旅程",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        // 底部卡片区域
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.65f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 无障碍服务
                item {
                    Text(
                        text = "准备工作",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBackground
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    PermissionCard(
                        icon = Icons.Default.Accessibility,
                        title = "开启无障碍服务",
                        description = "需要无障碍权限来自动查询其他App信息",
                        isEnabled = isAccessibilityEnabled,
                        buttonText = if (isAccessibilityEnabled) "已开启" else "前往设置",
                        onClick = onOpenAccessibilitySettings
                    )
                }
                
                // 已安装App
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "支持的应用",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnBackground
                        )
                        
                        TextButton(onClick = onRefresh) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "刷新",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("刷新")
                        }
                    }
                }
                
                items(installedApps) { app ->
                    AppStatusCard(
                        appName = app.appName,
                        isInstalled = app.isInstalled,
                        onInstall = { onInstallApp(app.packageName) }
                    )
                }
                
                // 继续按钮
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onContinue,
                        enabled = canContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = if (canContinue) "开始规划旅程" else "请完成上述准备",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (canContinue) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                    
                    if (!canContinue) {
                        Text(
                            text = "提示：至少需要开启无障碍服务并安装一个支持的应用",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isEnabled: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) SecondaryLight else Color(0xFFF5F5F5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isEnabled) Secondary else Color.Gray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isEnabled) Color.White else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnBackground
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            if (isEnabled) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "已启用",
                    tint = Secondary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(buttonText, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AppStatusCard(
    appName: String,
    isInstalled: Boolean,
    onInstall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isInstalled) SecondaryLight else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = if (isInstalled) SecondaryLight.copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App图标占位
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appName.first().toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
        
        Text(
            text = appName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )
        
        if (isInstalled) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "已安装",
                    fontSize = 12.sp,
                    color = Secondary
                )
            }
        } else {
            TextButton(
                onClick = onInstall,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "去安装",
                    fontSize = 12.sp,
                    color = Primary
                )
            }
        }
    }
}
