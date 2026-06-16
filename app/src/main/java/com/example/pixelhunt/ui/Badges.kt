package com.example.pixelhunt.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tek bir rozet — hem Veli Paneli hem Çocuk Profili tarafından kullanılır. */
data class Badge(val emoji: String, val title: String, val earned: Boolean)

/** Mevcut istatistiklerden rozet listesini üretir (kalıcı state yok, hep hesaplanır). */
fun buildBadges(
    learnedWords: Int,
    totalWords: Int,
    completedLevels: Int,
    totalLevels: Int,
    quizPlayed: Int,
    quizBest: Int,
    bestCombo: Int,
    usageMinutes: Int
): List<Badge> = listOf(
    Badge("🌱", "İlk Adım", learnedWords >= 1),
    Badge("📚", "Koleksiyoncu", learnedWords >= 10),
    Badge("🎓", "Kelime Ustası", totalWords > 0 && learnedWords >= totalWords),
    Badge("🗺️", "Seviye Avcısı", completedLevels >= 1),
    Badge("🌍", "Dünya Kaşifi", totalLevels > 0 && completedLevels >= totalLevels),
    Badge("🧩", "Quiz Meraklısı", quizPlayed >= 1),
    Badge("🏆", "Quiz Şampiyonu", quizBest >= 5),
    Badge("🔥", "Kombo Kralı", bestCombo >= 3),
    Badge("⏰", "Çalışkan Kaşif", usageMinutes >= 15)
)

/** Rozetleri 3'erli satırlar hâlinde ızgara olarak gösterir. */
@Composable
fun BadgeGrid(badges: List<Badge>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        badges.chunked(3).forEach { rowBadges ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                rowBadges.forEach { b ->
                    BadgeItem(b, Modifier.weight(1f))
                }
                repeat(3 - rowBadges.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Yatay kayan rozet şeridi — çocuk profili gibi dar alanlar için (taşma olmaz). */
@Composable
fun BadgeStrip(badges: List<Badge>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        items(badges) { b ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(72.dp)
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = if (b.earned) Color(0xFF2A1F4A) else Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(2.dp, if (b.earned) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.1f))
                ) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.weight(1f))
                        Text(if (b.earned) b.emoji else "🔒", fontSize = 28.sp)
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    b.title,
                    color = if (b.earned) Color.White else Color.Gray,
                    fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, lineHeight = 11.sp
                )
            }
        }
    }
}

@Composable
fun BadgeItem(badge: Badge, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.aspectRatio(0.85f),
        shape = RoundedCornerShape(16.dp),
        color = if (badge.earned) Color(0xFF2A1F4A) else Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, if (badge.earned) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (badge.earned) badge.emoji else "🔒", fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                badge.title,
                color = if (badge.earned) Color.White else Color.Gray,
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, lineHeight = 12.sp
            )
        }
    }
}
