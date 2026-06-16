package com.example.pixelhunt.ui

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixelhunt.data.StickerEntity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun AlbumScreen(
    albumViewModel: AlbumViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    val stickers by albumViewModel.allStickers.collectAsState()
    val unlockedCount by albumViewModel.unlockedCount.collectAsState()
    val isAlmostFull by albumViewModel.isAlmostFull.collectAsState()
    val totalStickersCount = stickers.size

    var selectedSticker by remember { mutableStateOf<StickerEntity?>(null) }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            Button(
                onClick = onHomeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(65.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("ANA SAYFAYA DÖN", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AlbumHeader(onBackClick)
            
            // Albüm Doluluk Uyarısı (Yeni Geliştirilmiş Mantık)
            if (isAlmostFull) {
                FullAlbumWarning()
            }

            ProgressCard(unlockedCount = unlockedCount, totalCount = totalStickersCount)

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(stickers) { sticker ->
                    StickerCell(
                        sticker = sticker,
                        onClick = {
                            if (sticker.isUnlocked) {
                                selectedSticker = sticker
                            }
                        }
                    )
                }
            }
        }

        selectedSticker?.let { sticker ->
            StickerDetailDialog(
                sticker = sticker,
                onDismiss = { selectedSticker = null }
            )
        }
    }
}

@Composable
fun FullAlbumWarning() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        color = Color(0xFFFFF3CD),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEBAA))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF856404))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Harika gidiyorsun! Albümün neredeyse dolmak üzere!",
                color = Color(0xFF856404),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StickerDetailDialog(
    sticker: StickerEntity,
    onDismiss: () -> Unit
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = sticker.image_path) {
        value = withContext(Dispatchers.IO) {
            val file = File(sticker.image_path)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clickable(enabled = false) { }
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Büyük Çıkartma",
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(50.dp)
                ) {
                    Text("Kapat", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun AlbumHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = "Albüm",
            tint = Color(0xFF8E44AD),
            modifier = Modifier.size(32.dp)
        )
        Text(
            "Sticker Albümü",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8E44AD)
        )
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(45.dp)
                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = "Geri",
                tint = Color(0xFF8E44AD),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProgressCard(unlockedCount: Int, totalCount: Int) {
    val status = when {
        unlockedCount <= 5 -> "Rookie Explorer"
        unlockedCount <= 15 -> "Skilled Hunter"
        else -> "Super Collector"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        color = Color(0xFFF8F4FF),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Koleksiyonun", fontSize = 15.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = Color(0xFFF0E5FF), shape = RoundedCornerShape(14.dp)) {
                    Text(
                        status,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = Color(0xFF8E44AD),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
                Text("$unlockedCount / $totalCount", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF8E44AD))
            }
            Spacer(modifier = Modifier.height(16.dp))
            val progress = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                color = Color(0xFF8E44AD),
                trackColor = Color.White
            )
        }
    }
}

@Composable
fun StickerCell(sticker: StickerEntity, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = sticker.isUnlocked) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(if (sticker.isUnlocked) Color(0xFFFFF4E5) else Color(0xFFF0F0F0))
                .border(width = 1.dp, color = Color.LightGray.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (sticker.isUnlocked) {
                val bitmap by produceState<Bitmap?>(initialValue = null, key1 = sticker.image_path) {
                    value = withContext(Dispatchers.IO) {
                        if (sticker.image_path.isNotEmpty()) {
                            val file = File(sticker.image_path)
                            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                        } else null
                    }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Sticker",
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(Icons.Default.Star, contentDescription = "Açık", tint = Color(0xFFFF9800), modifier = Modifier.size(30.dp))
                }

                Box(modifier = Modifier.fillMaxSize().padding(4.dp), contentAlignment = Alignment.BottomEnd) {
                    Surface(color = Color(0xFFFF9800), shape = CircleShape, modifier = Modifier.size(14.dp).border(1.dp, Color.White, CircleShape)) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(1.dp))
                    }
                }
            } else {
                Icon(Icons.Default.Lock, contentDescription = "Kilitli", tint = Color.LightGray.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
            }
        }
        Text(
            text = if (sticker.isUnlocked && sticker.name.isNotEmpty()) sticker.name else "???",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (sticker.isUnlocked) Color(0xFF8E44AD) else Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
