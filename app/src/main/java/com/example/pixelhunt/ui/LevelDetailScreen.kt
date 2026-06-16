package com.example.pixelhunt.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixelhunt.PixelHuntApplication
import com.example.pixelhunt.R
import com.example.pixelhunt.TtsSegment
import com.example.pixelhunt.data.GameData
import com.example.pixelhunt.data.LevelEntity
import com.example.pixelhunt.data.StickerEntity
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelDetailScreen(
    levelId: Int,
    gameViewModel: GameViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToCamera: () -> Unit = {},
    onNavigateToRetake: (retakeTarget: String, retakeStickerDbId: Int) -> Unit = { _, _ -> }
) {
    val app = LocalContext.current.applicationContext as PixelHuntApplication

    val stickers by gameViewModel.getStickersForLevel(levelId).collectAsState(initial = emptyList())
    val levelData by produceState<LevelEntity?>(initialValue = null, key1 = levelId) {
        value = gameViewModel.getLevel(levelId)
    }

    val unlockedCount = stickers.count { it.isUnlocked }
    val totalCount    = stickers.size.coerceAtLeast(1)
    val progress      = unlockedCount.toFloat() / totalCount

    // ── Celebration banner ───────────────────────────────────────────────────
    var showCelebration by remember { mutableStateOf(false) }
    var celebrationText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        app.stickerCapturedEvent.collect { count ->
            celebrationText = "Bu dünyada $count hazine buldun! 🌟"
            showCelebration = true
            delay(3000)
            showCelebration = false
        }
    }

    // ── Tapped sticker pulse ─────────────────────────────────────────────────
    var tappedStickerId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(tappedStickerId) {
        if (tappedStickerId != null) {
            delay(3000)
            tappedStickerId = null
        }
    }

    // ── Retake bottom sheet ──────────────────────────────────────────────────
    var retakeSheetSticker by remember { mutableStateOf<StickerEntity?>(null) }
    val retakeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF000000), Color(0xFF1A0B2E))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(45.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Geri",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    color = Color(0xFF673AB7).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFB388FF).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "İlerleme: $unlockedCount / $totalCount",
                        color = Color(0xFFB388FF),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFFF9800),
                trackColor = Color.White.copy(alpha = 0.1f),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = levelData?.levelName ?: "Yükleniyor...",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                text = "Tema: ${levelData?.levelTheme ?: "..."}",
                color = Color(0xFFFF9800),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            Text(
                text = "Bu bölgedeki hedefleri bul ve koleksiyonunu tamamla!",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(stickers) { sticker ->
                    LevelStickerCell(
                        sticker = sticker,
                        isHighlighted = tappedStickerId == sticker.id,
                        onClick = {
                            tappedStickerId = sticker.id
                            // Türkçe adı + gerçek İngilizce telaffuz
                            val en = GameData.toEnglishWord(sticker.name)
                            app.speakSegments(buildList {
                                add(TtsSegment(sticker.name, "tr"))
                                if (en.isNotEmpty()) {
                                    add(TtsSegment("İngilizcesi", "tr"))
                                    add(TtsSegment(en, "en"))
                                }
                            })
                            retakeSheetSticker = sticker
                        }
                    )
                }
            }
        }

        // KEŞFE KOŞ button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            Button(
                onClick = onNavigateToCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "KEŞFE KOŞ",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Animated celebration banner
        AnimatedVisibility(
            visible = showCelebration,
            enter = scaleIn(
                initialScale = 0.3f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMediumLow
                )
            ) + fadeIn(),
            exit  = scaleOut(targetScale = 0.8f) + fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
        ) {
            Surface(
                color    = Color(0xFFFF9800),
                shape    = RoundedCornerShape(28.dp),
                shadowElevation = 20.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🌟", fontSize = 44.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = celebrationText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp
                    )
                }
            }
        }

        // ── Retake bottom sheet ──────────────────────────────────────────────
        retakeSheetSticker?.let { sticker ->
            ModalBottomSheet(
                onDismissRequest = { retakeSheetSticker = null },
                sheetState = retakeSheetState,
                containerColor = Color(0xFF1A0B2E),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = sticker.name,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Bu çıkartmayı beğenmedin mi?",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    // Big sticker preview
                    val bitmap = remember(sticker.image_path) {
                        if (sticker.image_path.isNotEmpty()) {
                            val f = File(sticker.image_path)
                            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                        } else null
                    }
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(
                                2.dp,
                                Color(0xFFFF9800).copy(alpha = 0.5f),
                                RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = sticker.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_album_badge),
                                contentDescription = sticker.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            retakeSheetSticker = null
                            onNavigateToRetake(sticker.name, sticker.id)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(18.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tekrar Oluştur",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LevelStickerCell(
    sticker: StickerEntity,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "pulse_${sticker.id}")
    val pulseScale by pulseTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.07f,
        animationSpec = infiniteRepeatable(
            animation  = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ps"
    )
    val ringAlpha by pulseTransition.animateFloat(
        initialValue  = 0.5f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "ra"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = sticker.isUnlocked) { onClick() }
            .scale(if (isHighlighted) pulseScale else 1f)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (sticker.isUnlocked) Color(0xFFFF9800).copy(alpha = 0.1f)
                    else Color.White.copy(alpha = 0.05f)
                )
                .border(
                    width = if (isHighlighted) 3.dp else 2.dp,
                    color = when {
                        isHighlighted      -> Color(0xFF4CAF50).copy(alpha = ringAlpha)
                        sticker.isUnlocked -> Color(0xFFFF9800)
                        else               -> Color.White.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (sticker.isUnlocked) {
                val bitmap = remember(sticker.image_path) {
                    if (sticker.image_path.isNotEmpty()) {
                        val file = File(sticker.image_path)
                        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                    } else null
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = sticker.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_album_badge),
                        contentDescription = sticker.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Kilitli",
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (sticker.isUnlocked) sticker.name else "???",
            color = if (sticker.isUnlocked) Color(0xFFFF9800) else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
