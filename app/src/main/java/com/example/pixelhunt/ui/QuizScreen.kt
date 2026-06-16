package com.example.pixelhunt.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixelhunt.PixelHuntApplication
import com.example.pixelhunt.TtsSegment
import com.example.pixelhunt.data.GameData
import com.example.pixelhunt.data.StickerEntity
import java.io.File

/**
 * Mini Quiz — çocuğun TOPLADIĞI nesneler arasından seçim yaptırır.
 * "Hangisi {nesne}?" sorusu seslendirilir (TR + İngilizce), çocuk doğru görsele dokunur.
 */
@Composable
fun QuizScreen(
    gameViewModel: GameViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as PixelHuntApplication
    val allStickers by gameViewModel.allStickers.collectAsState()
    val unlocked = allStickers.filter { it.isUnlocked && it.image_path.isNotEmpty() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF1A0B2E))))
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
                    modifier = Modifier.size(45.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBackIosNew, "Geri", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Surface(
                    color = Color(0xFF673AB7).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFB388FF).copy(alpha = 0.5f))
                ) {
                    Text(
                        "🧩 Quiz Zamanı", color = Color(0xFFB388FF),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.size(45.dp))
            }

            if (unlocked.size < 3) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Quiz oynamak için en az 3 nesne topla! 🚀\nKeşfe çıkıp çıkartma kazan.",
                        color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium, lineHeight = 26.sp
                    )
                }
            } else {
                QuizGame(unlocked = unlocked, app = app)
            }
        }
    }
}

@Composable
private fun QuizGame(unlocked: List<StickerEntity>, app: PixelHuntApplication) {
    val maxRounds = minOf(5, unlocked.size)
    var round by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var answeredId by remember { mutableStateOf<Int?>(null) }
    var finished by remember { mutableStateOf(false) }

    // Her tur için soru + 3 şık (doğru + 2 çeldirici), tur değişince yeniden üretilir
    val question = remember(round) {
        val answer = unlocked.random()
        val distractors = (unlocked.filter { it.id != answer.id }).shuffled().take(2)
        QuizQuestion(answer, (distractors + answer).shuffled())
    }

    // Soruyu seslendir
    LaunchedEffect(round) {
        if (!finished) {
            val en = GameData.toEnglishWord(question.answer.name)
            app.speakSegments(buildList {
                add(TtsSegment("Hangisi ${question.answer.name}?", "tr"))
                if (en.isNotEmpty()) add(TtsSegment(en, "en"))
            })
        }
    }

    if (finished) {
        QuizResult(score = score, total = maxRounds, onRestart = {
            round = 0; score = 0; answeredId = null; finished = false
        })
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Soru ${round + 1} / $maxRounds", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Hangisi bu?", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "${question.answer.name}  •  ${GameData.toEnglishWord(question.answer.name)}",
            color = Color(0xFFFF9800), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(28.dp))

        // Şıklar (görseller) — tek satır
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            question.options.forEach { opt ->
                QuizOption(
                    sticker = opt,
                    answered = answeredId != null,
                    isCorrect = opt.id == question.answer.id,
                    isPicked = opt.id == answeredId,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (answeredId == null) {
                            answeredId = opt.id
                            if (opt.id == question.answer.id) {
                                score++; app.playCorrectSound()
                            } else app.playWrongSound()
                        }
                    }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Sıradaki / Bitir
        if (answeredId != null) {
            Button(
                onClick = {
                    if (round + 1 >= maxRounds) {
                        app.recordQuizResult(score, maxRounds)   // Veli Paneli'ne aktar
                        finished = true
                    } else { round++; answeredId = null }
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    if (round + 1 >= maxRounds) "SONUCU GÖR" else "SIRADAKİ SORU",
                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun QuizOption(
    sticker: StickerEntity,
    answered: Boolean,
    isCorrect: Boolean,
    isPicked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bitmap = remember(sticker.image_path) {
        val f = File(sticker.image_path)
        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }
    val borderColor = when {
        answered && isCorrect -> Color(0xFF4CAF50)
        answered && isPicked  -> Color(0xFFE53935)
        else                  -> Color.White.copy(alpha = 0.15f)
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(3.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(enabled = !answered) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = sticker.name,
                modifier = Modifier.fillMaxSize().padding(10.dp),
                contentScale = ContentScale.Fit
            )
        }
        if (answered && isCorrect) {
            Text("✓", color = Color(0xFF4CAF50), fontSize = 40.sp, fontWeight = FontWeight.Black)
        } else if (answered && isPicked) {
            Text("✗", color = Color(0xFFE53935), fontSize = 40.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun QuizResult(score: Int, total: Int, onRestart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (score == total) "🏆" else "🌟", fontSize = 72.sp)
        Spacer(Modifier.height(16.dp))
        Text("Quiz Bitti!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "$total soruda $score doğru!",
            color = Color(0xFFFF9800), fontSize = 22.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                score == total   -> "Mükemmel! Hepsini bildin! 🎉"
                score >= total / 2 -> "Harika gidiyorsun! 👏"
                else             -> "Biraz daha pratik yapalım! 💪"
            },
            color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("TEKRAR OYNA", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private data class QuizQuestion(val answer: StickerEntity, val options: List<StickerEntity>)
