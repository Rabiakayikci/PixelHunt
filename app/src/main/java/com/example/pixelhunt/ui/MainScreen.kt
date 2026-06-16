package com.example.pixelhunt.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pixelhunt.R

@Composable
fun MainScreen(
    onExploreClick: () -> Unit = {},
    onAlbumClick: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCamera: () -> Unit = {},
    onNavigateToQuiz: () -> Unit = {}
) {
    val samrTransition = rememberInfiniteTransition(label = "samr_breathe")
    val samrScale by samrTransition.animateFloat(
        initialValue = 0.96f,
        targetValue  = 1.04f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "samr_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF8E44AD))
    ) {
        TopBarSection(onNavigateToSettings)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_samr_center),
                    contentDescription = "SAM-r",
                    modifier = Modifier
                        .fillMaxHeight(0.9f)
                        .scale(samrScale),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = onAlbumClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (5).dp, y = (-30).dp)
                        .size(75.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_album_badge),
                        contentDescription = "Albüm",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Merhaba!",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                
                Text(
                    text = "Hazine avına başlamaya hazır mısın?",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = onNavigateToCamera,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(70.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "KEŞFE ÇIK",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quiz Zamanı butonu
                OutlinedButton(
                    onClick = onNavigateToQuiz,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(2.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = 0.12f))
                ) {
                    Text("🧩", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "QUİZ ZAMANI",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                BottomNavigationBar(
                    onMapClick = onNavigateToMap,
                    onProfileClick = onNavigateToProfile
                )
            }
        }
    }
}

@Composable
fun TopBarSection(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "PixelHunt",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Ayarlar",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun BottomNavigationBar(onMapClick: () -> Unit, onProfileClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(40.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(Icons.Default.Home, "ANA SAYFA", isSelected = true)
            NavItem(
                icon = Icons.Default.Map, 
                label = "HARİTA", 
                isSelected = false,
                onClick = onMapClick
            )
            NavItem(
                icon = Icons.Default.Person, 
                label = "PROFİL", 
                isSelected = false,
                onClick = onProfileClick
            )
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector, 
    label: String, 
    isSelected: Boolean,
    onClick: () -> Unit = {}
) {
    val tint = if (isSelected) Color(0xFF8E44AD) else Color.Gray.copy(alpha = 0.7f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
