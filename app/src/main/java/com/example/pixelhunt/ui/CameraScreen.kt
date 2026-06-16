package com.example.pixelhunt.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.animation.core.*
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixelhunt.R
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    cameraViewModel: CameraViewModel = viewModel(),
    currentLevelId: Int = 1,
    retakeTarget: String = "",
    retakeStickerDbId: Int = -1,
    onNavigateToMap: () -> Unit = {},
    onNavigateToAlbum: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by cameraViewModel.uiState.collectAsState()
    val currentTarget by cameraViewModel.currentTarget.collectAsState()
    val isTtsReady by cameraViewModel.isTtsReady.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            cameraViewModel.stopSpeech()   // cut audio the instant user leaves this screen
        }
    }

    // Ask for permission on first composition if not already granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(currentLevelId, retakeTarget, retakeStickerDbId) {
        if (retakeTarget.isNotEmpty() && retakeStickerDbId > 0) {
            cameraViewModel.setRetakeTarget(retakeTarget, retakeStickerDbId)
        } else {
            cameraViewModel.selectRandomTarget(currentLevelId)
        }
    }

    // Auto-speak: fires whenever EITHER the target OR TTS readiness changes.
    // This covers both orderings of the race:
    //   - TTS ready first  → target arrives  → this fires and speaks
    //   - Target set first → TTS becomes ready → this fires and speaks
    LaunchedEffect(currentTarget, isTtsReady) {
        if (isTtsReady && currentTarget.isNotEmpty()) {
            cameraViewModel.repeatSpeech()
        }
    }

    // Show a friendly gate screen while permission is missing
    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Kamera İzni Gerekli",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SAM-r ile keşfe çıkmak için kamera iznine ihtiyaç var!",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("İzin Ver", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Arka Plan: Canlı Kamera Ön İzleme
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            imageCapture = imageCapture
        )

        // 2. Ön Plan: Odak Vizörü
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 180.dp),
            contentAlignment = Alignment.Center
        ) {
            FocusViewfinder(
                modifier = Modifier.size(280.dp)
            )
        }

        // 3. Karakter ve Yönlendirme (SAM-r Konuşma Balonu)
        val samrTransition = rememberInfiniteTransition(label = "samr_float")
        val samrOffsetY by samrTransition.animateFloat(
            initialValue = -6f,
            targetValue  =  6f,
            animationSpec = infiniteRepeatable(
                animation  = tween(2500, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "samr_y"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 180.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.offset(y = samrOffsetY.dp)
            ) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp),
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .alpha(0.9f)
                ) {
                    Text(
                        text = when (val state = uiState) {
                            is CameraUiState.Loading -> state.message
                            is CameraUiState.Error   -> state.message
                            is CameraUiState.Success -> state.message
                            else -> if (currentTarget.isNotEmpty())
                                        "Hadi, bana bir $currentTarget bulup tarayalım!"
                                    else "Tüm çıkartmaları topladın! 🎉"
                        },
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                val mascotMood = when (val s = uiState) {
                    is CameraUiState.Loading -> SamrMood.Thinking
                    is CameraUiState.Success -> if (s.isCorrect) SamrMood.Happy else SamrMood.Sad
                    is CameraUiState.Error   -> SamrMood.Sad
                    else -> if (currentTarget.isNotEmpty()) SamrMood.Talking else SamrMood.Idle
                }
                SamrMascot(
                    mood = mascotMood,
                    size = 110.dp,
                    modifier = Modifier.clickable { cameraViewModel.repeatSpeech() }
                )
            }
        }

        // 4. ÇIKART Butonu
        if (uiState !is CameraUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 110.dp)
                    .align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_btn_cikart),
                    contentDescription = "Çıkart Butonu",
                    modifier = Modifier
                        .width(220.dp)
                        .height(80.dp)
                        .clickable {
                            captureAndProcess(
                                context,
                                imageCapture,
                                cameraExecutor,
                                cameraViewModel,
                                currentLevelId
                            )
                        }
                )
            }
        } else {
            // Loading Indicator
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp),
                color = Color(0xFFFF9800)
            )
        }

        // 5. Alt Navigasyon
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            DiscoveryBottomBar(
                onNavigateToMap = onNavigateToMap,
                onNavigateToAlbum = onNavigateToAlbum
            )
        }
        
        // Success / Validation Dialog
        (uiState as? CameraUiState.Success)?.let { successState ->
            SuccessStickerDialog(
                stickerBitmap = successState.stickerBitmap,
                isCorrect = successState.isCorrect,
                targetObject = successState.targetObject,
                detectedObject = successState.detectedObject,
                statusMessage = successState.message,
                levelCompleted = successState.levelCompleted,
                englishWord = successState.englishWord,
                funFact = successState.funFact,
                comboCount = successState.comboCount,
                bonusPoints = successState.bonusPoints,
                onDismiss = {
                    val wasCorrect = successState.isCorrect
                    cameraViewModel.resetState()
                    if (wasCorrect) {
                        cameraViewModel.selectRandomTarget(currentLevelId)
                        onNavigateToAlbum()
                    }
                }
            )
        }
    }
}

@Composable
fun SuccessStickerDialog(
    stickerBitmap: Bitmap,
    isCorrect: Boolean,
    targetObject: String,
    detectedObject: String,
    statusMessage: String,
    levelCompleted: Boolean = false,
    englishWord: String = "",
    funFact: String = "",
    comboCount: Int = 0,
    bonusPoints: Int = 0,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(12.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isCorrect) {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "TEBRİKLER!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF9800)
                    )

                    // ── Kombo rozeti ──
                    if (comboCount >= 2) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFE91E63)
                        ) {
                            Text(
                                text = "🔥 ${comboCount}x KOMBO!  +$bonusPoints puan",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                } else {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "YANLIŞ NESNE!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Red
                    )
                }

                Text(
                    text = if (isCorrect) "Harika! Yeni bir çıkartma kazandın." else "Aradığımız şey bu değil, tekrar dene!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFF1F3F5))
                        .border(
                            width = 3.dp,
                            color = if (isCorrect) Color(0xFF4CAF50) else Color.Red.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = stickerBitmap.asImageBitmap(),
                        contentDescription = "Yeni Çıkartma",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // ── İki dilli öğrenme kartı (eğitsel değer) ──
                if (isCorrect && englishWord.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF3EDFF),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "🇹🇷 $targetObject",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF673AB7)
                                )
                                Text(
                                    text = "  =  ",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "🇬🇧 $englishWord",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFF9800)
                                )
                            }
                            if (funFact.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "💡 $funFact",
                                    fontSize = 13.sp,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (!isCorrect) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Bu bir $targetObject değil, sanırım başka bir şey buldun.",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                } else if (statusMessage.contains("dolu")) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Bu seviye için tüm çıkartmalar toplandı veya albüm dolu!",
                        fontSize = 14.sp,
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCorrect) Color(0xFF673AB7) else Color.Gray
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        if (isCorrect) "DEVAM ET" else "TEKRAR DENE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }

            // Konfeti — doğru cevapta kartın üstünde, tam ekran yağar (seviye bitince daha yoğun)
            if (isCorrect) {
                ConfettiOverlay(
                    visible = true,
                    particleCount = if (levelCompleted) 150 else 70
                )
            }
        }
    }
}

private fun captureAndProcess(
    context: Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    viewModel: CameraViewModel,
    levelId: Int
) {
    val photoFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                viewModel.processCapturedImage(photoFile, levelId)
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e("CameraScreen", "Photo capture failed: ${exc.message}", exc)
            }
        }
    )
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    imageCapture: ImageCapture
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Use case binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

@Composable
fun FocusViewfinder(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = 4.dp.toPx()
        val cornerLength = 40.dp.toPx()
        val dashWidth = 10.dp.toPx()
        val dashGap = 10.dp.toPx()

        drawRect(
            color = Color.White,
            size = Size(width, height),
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
            )
        )

        // Köşeler
        drawLine(Color.White, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
        drawLine(Color.White, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)
        drawLine(Color.White, Offset(width, 0f), Offset(width - cornerLength, 0f), strokeWidth)
        drawLine(Color.White, Offset(width, 0f), Offset(width, cornerLength), strokeWidth)
        drawLine(Color.White, Offset(0f, height), Offset(cornerLength, height), strokeWidth)
        drawLine(Color.White, Offset(0f, height), Offset(0f, height - cornerLength), strokeWidth)
        drawLine(Color.White, Offset(width, height), Offset(width - cornerLength, height), strokeWidth)
        drawLine(Color.White, Offset(width, height), Offset(width, height - cornerLength), strokeWidth)

        // Hedef Nişangah (Target Dot)
        drawCircle(
            color = Color(0xFFFF9800), // Turuncu renk
            radius = 5.dp.toPx(),
            center = Offset(width / 2, height / 2)
        )
    }
}

@Composable
fun DiscoveryBottomBar(
    onNavigateToMap: () -> Unit,
    onNavigateToAlbum: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(80.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onNavigateToMap() }
            ) {
                Icon(Icons.Default.Map, contentDescription = "Harita", tint = Color.Gray)
                Text("Harita", fontSize = 12.sp, color = Color.Gray)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Explore, contentDescription = "Keşif", tint = Color(0xFF673AB7))
                Text("Keşif", fontSize = 12.sp, color = Color(0xFF673AB7), fontWeight = FontWeight.Bold)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onNavigateToAlbum() }
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Koleksiyon", tint = Color.Gray)
                Text("Koleksiyon", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}
