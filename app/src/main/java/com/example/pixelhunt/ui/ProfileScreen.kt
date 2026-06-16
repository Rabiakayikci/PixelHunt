package com.example.pixelhunt.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.pixelhunt.data.StickerEntity
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userViewModel: UserViewModel = viewModel(),
    gameViewModel: GameViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onNavigateToReport: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as com.example.pixelhunt.PixelHuntApplication
    val userState by userViewModel.userState.collectAsState()
    val unlockedCount by userViewModel.unlockedStickersCount.collectAsState()
    val unlockedStickers by userViewModel.unlockedStickers.collectAsState()

    // Rozetler için veri (çocuk kendi rozetlerini görsün)
    val allLevels by gameViewModel.allLevels.collectAsState()
    val allStickers by gameViewModel.allStickers.collectAsState()
    val completedLevels = allLevels.count { lvl ->
        val ls = allStickers.filter { it.levelId == lvl.levelId }
        ls.isNotEmpty() && ls.count { it.isUnlocked } >= lvl.requiredStickerCount
    }
    val totalWords = allStickers.size
    val childBadges = buildBadges(
        learnedWords = unlockedCount,
        totalWords = totalWords,
        completedLevels = completedLevels,
        totalLevels = allLevels.size,
        quizPlayed = app.getQuizStats().played,
        quizBest = app.getQuizStats().best,
        bestCombo = app.getBestCombo(),
        usageMinutes = (app.getTotalUsageMs() / 60000).toInt()
    )

    val userName = userState?.user_name ?: "Kaşif"
    val totalScore = userState?.total_score ?: 0
    val avatarStickerId = userState?.selected_avatar_sticker_id ?: -1
    val profilePhotoPath = userState?.profile_photo_path

    // Ünvan eşikleri seviye tamamlamalarına göre (L1=4, L2=9, L3=14, L4=19 birikimli)
    // Her seviye geçişinde ünvan görünür şekilde değişir.
    val explorerStatus = explorerTitle(unlockedCount)

    // Debounced name input
    var nameInput by remember(userName) { mutableStateOf(userName) }
    LaunchedEffect(nameInput) {
        delay(500)
        if (nameInput.isNotEmpty() && nameInput != userState?.user_name) {
            userViewModel.updateUser(nameInput)
        }
    }

    // Avatar sheet
    var showAvatarSheet by remember { mutableStateOf(false) }
    val avatarSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Settings sheet
    var showSettingsSheet by remember { mutableStateOf(false) }
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Çocuk kilidi (parental gate)
    var showParentGate by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val dest = copyUriToInternalFile(context, it)
            if (dest != null) userViewModel.updateProfilePhoto(dest.absolutePath)
        }
    }

    val avatarSticker = unlockedStickers.find { it.id == avatarStickerId }

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
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                        text = "Kaşif Profilim",
                        color = Color(0xFFB388FF),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Settings gear
                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier
                        .size(45.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ayarlar",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Avatar circle
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    shape = CircleShape,
                    color = Color(0xFFFDEFD5),
                    border = BorderStroke(4.dp, Color(0xFFFF9800))
                ) {
                    when {
                        !profilePhotoPath.isNullOrEmpty() -> {
                            AsyncImage(
                                model = profilePhotoPath,
                                contentDescription = "Profil Fotoğrafı",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        avatarSticker != null && avatarSticker.image_path.isNotEmpty() -> {
                            val bitmap = remember(avatarSticker.image_path) {
                                val f = File(avatarSticker.image_path)
                                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = avatarSticker.name,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                DefaultAvatarIcon()
                            }
                        }
                        else -> DefaultAvatarIcon()
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .clickable { showAvatarSheet = true },
                    shape = CircleShape,
                    color = Color(0xFFFF9800),
                    border = BorderStroke(2.dp, Color.White)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Fotoğraf Değiştir",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Debounced name field
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFB388FF),
                    textAlign = TextAlign.Center
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF9800),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    cursorColor = Color(0xFFFF9800)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
            )

            Text(
                text = explorerStatus,
                color = Color(0xFFFF9800),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Stats chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                StatChip(emoji = "⭐", value = "$totalScore", label = "Puan")
                StatChip(emoji = "🏷️", value = "$unlockedCount", label = "Çıkartma")
                StatChip(
                    emoji = "🏆",
                    value = explorerRank(unlockedCount),
                    label = "Ünvan"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Çocuğun kazandığı rozetler (yatay kayan şerit)
            Text(
                text = "🏅 Rozetlerin",
                color = Color(0xFFB388FF),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, bottom = 10.dp)
            )
            BadgeStrip(childBadges, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(20.dp))

            // Veli Paneli butonu — çocuk kilidi (parental gate) ile korunur
            OutlinedButton(
                onClick = { showParentGate = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFFB388FF))
            ) {
                Text("🔒", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Veli Paneli",
                    color = Color(0xFFB388FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // Home button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            Button(
                onClick = onHomeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = "ANA SAYFAYA DÖN",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // ── Avatar picker bottom sheet ───────────────────────────────────────
        if (showAvatarSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAvatarSheet = false },
                sheetState = avatarSheetState,
                containerColor = Color(0xFF1A0B2E),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Avatarını Seç",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    Button(
                        onClick = {
                            showAvatarSheet = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Galeriden Fotoğraf Seç",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (unlockedStickers.isEmpty()) {
                        Text(
                            text = "Henüz çıkartma toplamadın! Keşfe çık 🚀",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "Çıkartma Avatarı Seç",
                            color = Color(0xFFB388FF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(unlockedStickers, key = { it.id }) { sticker ->
                                StickerAvatarOption(
                                    sticker = sticker,
                                    isSelected = sticker.id == avatarStickerId,
                                    onClick = {
                                        userViewModel.updateAvatarSticker(sticker.id)
                                        showAvatarSheet = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Settings bottom sheet — delegates to the shared component ────────
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                sheetState = settingsSheetState,
                containerColor = Color(0xFF1A0B2E),
                tonalElevation = 0.dp
            ) {
                SharedSettingsContent(
                    userViewModel = userViewModel,
                    onHomeClick = {
                        showSettingsSheet = false
                        onHomeClick()
                    }
                )
            }
        }

        // ── Çocuk kilidi: Veli Paneli'ne girmeden önce çarpım sorusu ──
        if (showParentGate) {
            ParentalGateDialog(
                onSuccess = {
                    showParentGate = false
                    onNavigateToReport()
                },
                onDismiss = { showParentGate = false }
            )
        }
    }
}

/**
 * Çocuk kilidi — küçük çocuğun cevaplayamayacağı bir çarpım sorusu.
 * Doğru şıkka basınca [onSuccess], yanlışta yeni soru üretilir.
 */
@Composable
private fun ParentalGateDialog(onSuccess: () -> Unit, onDismiss: () -> Unit) {
    var a by remember { mutableStateOf((3..9).random()) }
    var b by remember { mutableStateOf((3..9).random()) }
    var wrongShake by remember { mutableStateOf(false) }

    val correct = a * b
    val options = remember(a, b) {
        (setOf(correct, correct + (1..6).random(), correct - (1..5).random())
            .toMutableList()).also { while (it.size < 3) it.add((10..81).random()) }
            .distinct().shuffled()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A0B2E),
        title = {
            Text("👨‍👩‍👧 Veli Kontrolü", color = Color.White,
                fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Devam etmek için soruyu çöz:",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "$a × $b = ?",
                    color = Color(0xFFFF9800), fontSize = 30.sp, fontWeight = FontWeight.Black
                )
                if (wrongShake) {
                    Spacer(Modifier.height(8.dp))
                    Text("Yanlış, tekrar dene!", color = Color(0xFFE57373), fontSize = 13.sp)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    options.forEach { opt ->
                        Button(
                            onClick = {
                                if (opt == correct) onSuccess()
                                else {
                                    wrongShake = true
                                    a = (3..9).random(); b = (3..9).random()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("$opt", color = Color.White,
                                fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = Color(0xFFB388FF))
            }
        }
    )
}

// ── Ünvan yardımcıları ───────────────────────────────────────────────────────
// Birikimli sticker sayısına göre ünvan. Seviyeler L1=4, L2=9, L3=14, L4=19, L5=24'te biter.
// Eşikler her seviye tamamlandığında ünvanın değişeceği şekilde ayarlandı.

private fun explorerTitle(count: Int): String = when {
    count <= 3  -> "Çaylak Kaşif"      // henüz Level 1 bitmedi
    count <= 8  -> "Yetenekli Avcı"    // Level 1 bitti (4+)
    count <= 13 -> "Usta Kaşif"        // Level 2 bitti (9+)
    count <= 18 -> "Süper Avcı"        // Level 3 bitti (14+)
    else        -> "Efsane Koleksiyoner" // Level 4+ bitti (19+)
}

private fun explorerRank(count: Int): String = when {
    count <= 3  -> "Çaylak"
    count <= 8  -> "Yetenekli"
    count <= 13 -> "Usta"
    count <= 18 -> "Süper"
    else        -> "Efsane"
}

// ── Private composables ──────────────────────────────────────────────────────

@Composable
private fun DefaultAvatarIcon() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = Color(0xFFFF9800).copy(alpha = 0.6f),
            modifier = Modifier.size(80.dp)
        )
    }
}

@Composable
private fun StatChip(emoji: String, value: String, label: String) {
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 22.sp)
            Text(
                text = value,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StickerAvatarOption(
    sticker: StickerEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bitmap = remember(sticker.image_path) {
        val f = File(sticker.image_path)
        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = if (isSelected) Color(0xFFFF9800).copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.05f),
            border = BorderStroke(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFFFF9800) else Color.White.copy(alpha = 0.2f)
            )
        ) {
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
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFF9800))
                }
            }
        }
        Text(
            text = sticker.name,
            color = if (isSelected) Color(0xFFFF9800) else Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

private fun copyUriToInternalFile(context: Context, uri: Uri): File? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val dest = File(context.filesDir, "profile_photo_${System.currentTimeMillis()}.jpg")
        dest.outputStream().use { out -> input.use { it.copyTo(out) } }
        dest
    } catch (e: Exception) {
        null
    }
}
