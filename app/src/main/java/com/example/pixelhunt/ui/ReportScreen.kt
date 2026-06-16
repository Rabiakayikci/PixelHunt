package com.example.pixelhunt.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixelhunt.PixelHuntApplication
import com.example.pixelhunt.data.GameData
import com.example.pixelhunt.data.LevelEntity
import com.example.pixelhunt.data.StickerEntity

/**
 * Veli & Öğretmen Raporu — çocuğun öğrenme ilerlemesini analitik olarak sunar.
 * İki dilli öğrenme çıktısını (TR + EN kelimeler) ve seviye ilerlemesini gösterir.
 */
@Composable
fun ReportScreen(
    gameViewModel: GameViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as PixelHuntApplication
    val quiz = remember { app.getQuizStats() }
    val usageMinutes = remember { (app.getTotalUsageMs() / 60000).toInt() }
    val bestCombo = remember { app.getBestCombo() }
    val levels by gameViewModel.allLevels.collectAsState()
    val stickers by gameViewModel.allStickers.collectAsState()
    val userState by userViewModel.userState.collectAsState()

    val unlockedStickers = stickers.filter { it.isUnlocked }
    val learnedWords = unlockedStickers.size
    val totalWords = stickers.size.coerceAtLeast(1)
    val completedLevels = levels.count { lvl ->
        val lvlStickers = stickers.filter { it.levelId == lvl.levelId }
        lvlStickers.isNotEmpty() && lvlStickers.count { it.isUnlocked } >= lvl.requiredStickerCount
    }
    val totalScore = userState?.total_score ?: 0
    val childName = userState?.user_name ?: "Kaşif"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF1A0B2E)))
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Üst bar
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
                    Icon(Icons.Default.ArrowBackIosNew, "Geri", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Surface(
                    color = Color(0xFF673AB7).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFB388FF).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Veli Paneli",
                        color = Color(0xFFB388FF),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.size(45.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "$childName'in Öğrenme Karnesi",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Özet kartları
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReportStat("📚", "$learnedWords", "Öğrenilen\nKelime", Modifier.weight(1f))
                        ReportStat("🏆", "$completedLevels", "Tamamlanan\nSeviye", Modifier.weight(1f))
                        ReportStat("⭐", "$totalScore", "Toplam\nPuan", Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReportStat(
                            "⏱️",
                            if (usageMinutes >= 60) "${usageMinutes / 60}s ${usageMinutes % 60}dk" else "$usageMinutes dk",
                            "Geçirilen\nSüre", Modifier.weight(1f)
                        )
                        ReportStat("🔥", "$bestCombo", "En İyi\nKombo", Modifier.weight(1f))
                    }
                }

                // ── Rozetler ──
                item {
                    Text("🏅 Rozetler", color = Color(0xFFB388FF),
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                item {
                    BadgeGrid(
                        buildBadges(
                            learnedWords = learnedWords,
                            totalWords = totalWords,
                            completedLevels = completedLevels,
                            totalLevels = levels.size,
                            quizPlayed = quiz.played,
                            quizBest = quiz.best,
                            bestCombo = bestCombo,
                            usageMinutes = usageMinutes
                        )
                    )
                }

                // Genel ilerleme
                item {
                    val ratio = learnedWords.toFloat() / totalWords
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Genel İlerleme  •  %${(ratio * 100).toInt()}",
                                color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = Color(0xFFFF9800),
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Text(
                                "$learnedWords / $totalWords kelime öğrenildi",
                                color = Color.Gray, fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }

                // Quiz performansı
                item {
                    Text("🧩 Quiz Performansı", color = Color(0xFFB388FF),
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                item {
                    if (quiz.played == 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Henüz quiz oynanmadı. Ana ekrandan 'Quiz Zamanı' ile başlayın!",
                                color = Color.Gray, fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        val accuracy = if (quiz.questions > 0) quiz.correct * 100 / quiz.questions else 0
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ReportStat("🎮", "${quiz.played}", "Oynanan\nQuiz", Modifier.weight(1f))
                                ReportStat("🎯", "%$accuracy", "Doğruluk\nOranı", Modifier.weight(1f))
                                ReportStat("⭐", "${quiz.best}", "En İyi\nSkor", Modifier.weight(1f))
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Toplam ${quiz.questions} soruda ${quiz.correct} doğru cevap.",
                                    color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }

                // Seviye bazlı ilerleme
                item {
                    Text("Seviye Bazlı İlerleme", color = Color(0xFFB388FF),
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                items(levels) { level ->
                    LevelProgressRow(level, stickers.filter { it.levelId == level.levelId })
                }

                // Veliye öneri — sırada öğrenilecek kelimeler
                val nextWords = stickers.filter { !it.isUnlocked }.take(4)
                if (nextWords.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF2A1F4A),
                            border = BorderStroke(1.dp, Color(0xFFB388FF).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("💡 Veliye Öneri", color = Color(0xFFFFD54F),
                                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Çocuğunuzla birlikte sırada şu nesneleri arayın: " +
                                        nextWords.joinToString(", ") {
                                            "${it.name} (${GameData.toEnglishWord(it.name)})"
                                        },
                                    color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }

                // Öğrenilen kelimeler (iki dilli — eğitsel çıktı)
                item {
                    Text("Öğrenilen Kelimeler (Türkçe – İngilizce)", color = Color(0xFFB388FF),
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                items(stickers.sortedByDescending { it.isUnlocked }) { sticker ->
                    WordRow(sticker)
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ReportStat(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 22.sp)
            Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Color.Gray, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun LevelProgressRow(level: LevelEntity, levelStickers: List<StickerEntity>) {
    val unlocked = levelStickers.count { it.isUnlocked }
    val total = levelStickers.size.coerceAtLeast(1)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.05f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(level.levelTheme, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { unlocked.toFloat() / total },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text("$unlocked/$total", color = Color(0xFFFF9800), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun WordRow(sticker: StickerEntity) {
    val en = GameData.toEnglishWord(sticker.name)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (sticker.isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
            contentDescription = null,
            tint = if (sticker.isUnlocked) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.25f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = sticker.name,
            color = if (sticker.isUnlocked) Color.White else Color.Gray,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (en.isNotEmpty()) en else "—",
            color = if (sticker.isUnlocked) Color(0xFFFF9800) else Color.Gray.copy(alpha = 0.5f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}
