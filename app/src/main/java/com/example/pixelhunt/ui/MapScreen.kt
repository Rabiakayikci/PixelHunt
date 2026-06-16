package com.example.pixelhunt.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixelhunt.R
import com.example.pixelhunt.data.LevelEntity

@Composable
fun MapScreen(
    gameViewModel: GameViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onLevelClick: (Int) -> Unit = {}
) {
    val levels by gameViewModel.allLevels.collectAsState()
    val userState by userViewModel.userState.collectAsState()
    val totalScore = userState?.total_score ?: 0

    val offsets = listOf(0, 50, -50, 60, -30)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_space_map),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(45.dp, Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            levels.reversed().forEach { level ->
                val index = (level.levelId - 1) % 5
                LevelNode(
                    level = level,
                    alignmentOffset = offsets[index],
                    onLevelClick = onLevelClick,
                    gameViewModel = gameViewModel
                )
            }
            Spacer(modifier = Modifier.height(120.dp))
        }

        SamRFloatingGuidance()
        MapTopBar(totalScore, onNavigateToSettings)

        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            MapBottomBar(onHomeClick, onNavigateToProfile)
        }
    }
}

@Composable
fun MapTopBar(score: Int, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color(0xFF673AB7).copy(alpha = 0.92f),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.45f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_coin),
                    contentDescription = "Coin",
                    modifier = Modifier.size(32.dp) 
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "$score", 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp 
                )
            }
        }

        IconButton(onClick = onSettingsClick) {
            Icon(
                Icons.Default.Settings, 
                contentDescription = "Ayarlar", 
                tint = Color.White, 
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
fun SamRFloatingGuidance() {
    val infiniteTransition = rememberInfiniteTransition(label = "speech_float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 70.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .offset(x = 80.dp, y = offsetY.dp)
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier
                    .widthIn(max = 130.dp)
                    .alpha(0.95f),
                border = BorderStroke(1.dp, Color(0xFF673AB7).copy(alpha = 0.2f))
            ) {
                Text(
                    "Hangi gezegeni keşfetmek istersin?",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 10.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 13.sp
                )
            }
            
            Spacer(modifier = Modifier.width(6.dp)) 
            
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF).copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                Image(
                    painter = painterResource(id = R.drawable.img_samr_floating),
                    contentDescription = "SAM-r Guidance",
                    modifier = Modifier.size(110.dp)
                )
            }
        }
    }
}

@Composable
fun LevelNode(
    level: LevelEntity, 
    alignmentOffset: Int, 
    onLevelClick: (Int) -> Unit,
    gameViewModel: GameViewModel
) {
    val stickers by gameViewModel.getStickersForLevel(level.levelId).collectAsState(initial = emptyList())
    val unlockedCount = stickers.count { it.isUnlocked }
    val totalCount = stickers.size.coerceAtLeast(level.requiredStickerCount)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(x = alignmentOffset.dp)
            .clickable(enabled = level.isUnlocked) { onLevelClick(level.levelId) }
            .alpha(if (level.isUnlocked) 1f else 0.5f)
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "$unlockedCount/$totalCount",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        Surface(
            modifier = Modifier
                .size(85.dp),
            border = BorderStroke(
                width = 3.dp, 
                color = if (level.isUnlocked) Color.White else Color.White.copy(alpha = 0.4f)
            ),
            color = if (level.isUnlocked) Color(0xFFFFD54F) else Color(0xFF4A4A4A).copy(alpha = 0.8f),
            shape = CircleShape,
            shadowElevation = if (level.isUnlocked) 12.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (level.isUnlocked) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_rocket),
                        contentDescription = "Explore",
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = level.levelName.uppercase(),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.7f),
                    blurRadius = 10f
                )
            )
        )
    }
}

@Composable
fun MapBottomBar(onHomeClick: () -> Unit, onProfileClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .height(80.dp),
        color = Color(0xFF1A0B2E).copy(alpha = 0.95f),
        shape = RoundedCornerShape(40.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star, 
                    contentDescription = "Harita", 
                    tint = Color(0xFFFFD54F), 
                    modifier = Modifier.size(34.dp)
                )
            }

            IconButton(
                onClick = onHomeClick,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Home, 
                    contentDescription = "Anasayfa", 
                    tint = Color.White.copy(alpha = 0.8f), 
                    modifier = Modifier.size(34.dp)
                )
            }

            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Person, 
                    contentDescription = "Profil", 
                    tint = Color.White.copy(alpha = 0.8f), 
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}
