package com.travelagent.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import com.travelagent.data.models.*
import com.travelagent.ui.theme.*
import java.text.NumberFormat
import java.util.*

@Composable
fun ResultScreen(
    travelPlan: TravelPlan?,
    onRestart: () -> Unit
) {
    if (travelPlan == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }
    
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // 顶部卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Primary, Primary.copy(alpha = 0.8f))
                    )
                )
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🎉 行程规划完成",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${travelPlan.request.from} → ${travelPlan.request.to}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    
                    IconButton(
                        onClick = onRestart,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "重新规划",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 费用概览
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "预估总费用",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "¥${travelPlan.estimatedCost.total.toInt()}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            val days = java.time.temporal.ChronoUnit.DAYS.between(
                                travelPlan.request.startDate,
                                travelPlan.request.endDate
                            ) + 1
                            Text(
                                text = "${travelPlan.request.peopleCount}人 · ${days}天",
                                fontSize = 14.sp,
                                color = OnBackground
                            )
                            Text(
                                text = "人均 ¥${(travelPlan.estimatedCost.total / travelPlan.request.peopleCount).toInt()}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
        
        // 详细内容
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 费用明细
            item {
                CostBreakdownCard(cost = travelPlan.estimatedCost)
            }
            
            // 交通安排
            item {
                travelPlan.transport?.let { transport ->
                    SectionCard(
                        icon = Icons.Default.Train,
                        title = "交通安排",
                        color = AgentTransportColor
                    ) {
                        transport.outbound?.let { train ->
                            TrainCard(train = train, label = "去程")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        transport.returnTrip?.let { train ->
                            TrainCard(train = train, label = "返程")
                        }
                    }
                }
            }
            
            // 住宿推荐
            item {
                travelPlan.hotel?.let { hotel ->
                    SectionCard(
                        icon = Icons.Default.Hotel,
                        title = "住宿推荐",
                        color = AgentHotelColor
                    ) {
                        hotel.recommended?.let { hotelInfo ->
                            HotelCard(hotel = hotelInfo)
                        }
                        
                        if (hotel.alternatives.size > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "还有 ${hotel.alternatives.size - 1} 家备选酒店",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
            
            // 景点路线
            item {
                travelPlan.attractions?.let { attractions ->
                    SectionCard(
                        icon = Icons.Default.Place,
                        title = "景点路线",
                        color = AgentAttractionColor
                    ) {
                        attractions.dailyItinerary.forEach { (day, spots) ->
                            Text(
                                text = "第${day}天",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = AgentAttractionColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            spots.forEach { attraction ->
                                AttractionItem(attraction = attraction)
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
            
            // 美食推荐
            item {
                travelPlan.food?.let { food ->
                    SectionCard(
                        icon = Icons.Default.Restaurant,
                        title = "美食推荐",
                        color = AgentFoodColor
                    ) {
                        food.recommended.forEach { restaurant ->
                            RestaurantItem(restaurant = restaurant)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            
            // 重新规划按钮
            item {
                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "规划新旅程",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CostBreakdownCard(cost: EstimatedCost) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "费用明细",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CostRow(icon = "🚄", label = "交通", amount = cost.transport)
            CostRow(icon = "🏨", label = "住宿", amount = cost.hotel)
            CostRow(icon = "🎫", label = "门票", amount = cost.attractions)
            CostRow(icon = "🍜", label = "餐饮", amount = cost.food)
        }
    }
}

@Composable
fun CostRow(icon: String, label: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontSize = 14.sp)
        }
        Text(
            text = "¥${amount.toInt()}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SectionCard(
    icon: ImageVector,
    title: String,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

@Composable
fun TrainCard(train: TrainInfo, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AgentTransportLight,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.Gray
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = train.trainNo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgentTransportColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = train.seatType,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = train.departTime,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = train.duration,
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
        
        Icon(
            Icons.Default.ArrowForward,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = train.arriveTime,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = "¥${train.price.toInt()}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
    }
}

@Composable
fun HotelCard(hotel: HotelInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AgentHotelLight,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 酒店图片占位
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text("🏨", fontSize = 24.sp)
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = hotel.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = hotel.type,
                    fontSize = 11.sp,
                    color = AgentHotelColor,
                    modifier = Modifier
                        .background(
                            AgentHotelColor.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Warning,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${hotel.rating}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "¥${hotel.price.toInt()}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            Text(
                text = "/晚",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun AttractionItem(attraction: AttractionInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(AgentAttractionColor)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = attraction.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${attraction.duration} · ${attraction.ticketPrice}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun RestaurantItem(restaurant: RestaurantInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AgentFoodLight,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🍽️", fontSize = 24.sp)
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = restaurant.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = restaurant.cuisine,
                    fontSize = 11.sp,
                    color = AgentFoodColor
                )
                if (restaurant.specialty.isNotEmpty()) {
                    Text(
                        text = " · ${restaurant.specialty}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Warning,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${restaurant.rating}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = restaurant.priceRange,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}
