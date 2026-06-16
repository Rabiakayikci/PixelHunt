package com.example.pixelhunt.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pixelhunt.BuildConfig
import com.example.pixelhunt.PixelHuntApplication
import com.example.pixelhunt.TtsSegment
import com.example.pixelhunt.data.*
import com.example.pixelhunt.network.HunterPixelApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG = "CameraVM"

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PixelHuntApplication
    private val repository: UserRepository = app.repository

    val isTtsReady: StateFlow<Boolean> = app.isTtsReady

    private val api: HunterPixelApi

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Idle)
    val uiState: StateFlow<CameraUiState> = _uiState

    private val _currentTarget = MutableStateFlow("")
    val currentTarget: StateFlow<String> = _currentTarget

    // When > 0 the current capture is a retake for that specific sticker DB row.
    private val _retakeStickerDbId = MutableStateFlow(-1)

    init {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HunterPixelApi::class.java)
    }

    // ── Target Selection ────────────────────────────────────────────────────

    // Kilitli (henüz toplanmamış) çıkartmalardan rastgele birini hedef seçer.
    // Toplanan çıkartma kilitli listede olmadığı için bir daha hedeflenmez.
    fun selectRandomTarget(levelId: Int) {
        viewModelScope.launch {
            val lockedStickers = repository.getLockedStickersByLevel(levelId)
            val target = lockedStickers.randomOrNull()?.name ?: ""
            _currentTarget.value = target
            _retakeStickerDbId.value = -1
            // Prompt + başarı parçalarını (TR cümle, EN kelime, bilgi) önceden indir → anında çalsın
            if (target.isNotEmpty()) {
                app.prewarm("Hoş geldin! Hadi önce bana bir $target bulalım.", "tr")
                val en = GameData.toEnglishWord(target)
                val fact = GameData.toFunFact(target)
                if (en.isNotEmpty()) {
                    app.prewarm("Aferin! Bu bir $target. İngilizcesi", "tr")
                    app.prewarm(en, "en")
                } else {
                    app.prewarm("Aferin! Bu bir $target.", "tr")
                }
                if (fact.isNotEmpty()) {
                    app.prewarm(GameData.factIntro(target), "tr")
                    app.prewarm(fact, "tr")
                }
            }
        }
    }

    /** Called from LevelDetailScreen when the user taps "Tekrar Oluştur". */
    fun setRetakeTarget(name: String, stickerDbId: Int) {
        _currentTarget.value = name
        _retakeStickerDbId.value = stickerDbId
    }

    /** Called by CameraScreen LaunchedEffect (auto) and SAM-r tap (manual). */
    fun repeatSpeech() {
        val target = _currentTarget.value
        if (target.isNotEmpty()) app.speak("Hoş geldin! Hadi önce bana bir $target bulalım.")
    }

    /** Called from CameraScreen's DisposableEffect.onDispose — cuts audio instantly on navigation. */
    fun stopSpeech() = app.stopSpeech()

    // ── Image Capture & Processing ──────────────────────────────────────────

    fun processCapturedImage(imageFile: File, currentLevelId: Int) {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Loading("Düşünüyorum... Bekle!")
            try {
                val uploadFile = createResizedImageFile(imageFile)

                val dimOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(uploadFile.absolutePath, dimOpts)
                val targetX = dimOpts.outWidth / 2f
                val targetY = dimOpts.outHeight / 2f
                val targetObject   = _currentTarget.value
                val targetObjectEn = GameData.toEnglish(targetObject)

                Log.d(TAG, "── UPLOAD ──────────────────────────────────────────")
                Log.d(TAG, "File size : ${uploadFile.length() / 1024} KB")
                Log.d(TAG, "Dimensions: ${dimOpts.outWidth} x ${dimOpts.outHeight}")
                Log.d(TAG, "Target TR : '$targetObject'  →  EN: '$targetObjectEn'")
                Log.d(TAG, "Retake ID : ${_retakeStickerDbId.value}")

                val imagePart = MultipartBody.Part.createFormData(
                    "image", uploadFile.name,
                    uploadFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                val xPart      = targetX.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val yPart      = targetY.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val targetPart = targetObjectEn.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = api.processImage(imagePart, xPart, yPart, targetPart)

                Log.d(TAG, "── API RESPONSE ────────────────────────────────────")
                Log.d(TAG, "status       : ${response.status}")
                Log.d(TAG, "is_correct   : ${response.is_correct}")
                Log.d(TAG, "detected_obj : '${response.detected_object}'")

                if (response.status == "success") {
                    val imageData = response.image ?: run {
                        _uiState.value = CameraUiState.Error("API yanıtı geçersiz: görüntü verisi eksik.")
                        return@launch
                    }

                    val bitmap = decodeBase64(imageData)
                    if (bitmap != null) {
                        val isCorrect = response.is_correct == true
                        val detected  = response.detected_object?.trim() ?: ""

                        if (isCorrect) {
                            val savedFile = saveBitmapToInternalStorage(bitmap)
                            val retakeId  = _retakeStickerDbId.value

                            val wasSaved = if (retakeId > 0) {
                                retakeExistingSticker(savedFile.absolutePath, retakeId)
                                _retakeStickerDbId.value = -1
                                true
                            } else {
                                // Görseli TAM olarak istenen hedef nesnenin satırına kaydet
                                saveStickerForTarget(savedFile.absolutePath, currentLevelId, targetObject)
                            }

                            // ── KOMBO: üst üste doğru → bonus puan ──
                            var combo = 0
                            var bonus = 0
                            if (wasSaved) {
                                combo = app.incrementStreak()
                                bonus = if (combo >= 2) (combo - 1) * 5 else 0
                                if (bonus > 0) {
                                    repository.getUser()?.let { u ->
                                        repository.updateUser(u.copy(total_score = u.total_score + bonus))
                                    }
                                }
                            }

                            // Başarı ses efekti — kombo varsa daha coşkulu
                            if (combo >= 2) app.playComboSound() else app.playCorrectSound()

                            // ── Eğitsel içerik (iki dilli öğrenme) ──
                            val enWord  = GameData.toEnglishWord(targetObject)
                            val funFact = GameData.toFunFact(targetObject)

                            // Seviye tamamlandı mı? (toplanan >= gerekli)
                            var levelDone = false
                            if (wasSaved) {
                                val newCount = repository.getUnlockedCountForLevel(currentLevelId)
                                val required = repository.getLevelById(currentLevelId)?.requiredStickerCount ?: Int.MAX_VALUE
                                levelDone = newCount >= required
                                // Çok dilli seslendirme: TR cümle + İngilizce kelime + "Biliyor musun?" + TR bilgi
                                val segments = buildList {
                                    if (enWord.isNotEmpty()) {
                                        add(TtsSegment("Aferin! Bu bir $targetObject. İngilizcesi", "tr"))
                                        add(TtsSegment(enWord, "en"))
                                    } else {
                                        add(TtsSegment("Aferin! Bu bir $targetObject.", "tr"))
                                    }
                                    if (funFact.isNotEmpty()) {
                                        add(TtsSegment(GameData.factIntro(targetObject), "tr"))
                                        add(TtsSegment(funFact, "tr"))
                                    }
                                    if (levelDone) add(TtsSegment("Tebrikler, yeni dünya açıldı!", "tr"))
                                }
                                app.speakSegments(segments)
                                app.notifyStickerCaptured(newCount)
                            } else {
                                app.speak("Doğru! Ama bu seviye tamamlandı.")
                            }
                            _uiState.value = CameraUiState.Success(
                                message        = if (wasSaved) "Harika! Koleksiyonuna ekledim."
                                                 else "Doğru! Ama bu seviye için tüm çıkartmalar toplandı.",
                                stickerBitmap  = bitmap,
                                isCorrect      = true,
                                detectedObject = detected,
                                targetObject   = targetObject,
                                levelCompleted = levelDone,
                                englishWord    = enWord,
                                funFact        = funFact,
                                comboCount     = combo,
                                bonusPoints    = bonus
                            )
                        } else {
                            val isBlind = detected.isEmpty() ||
                                          detected.equals("undefined", ignoreCase = true)

                            val toastMsg = if (isBlind)
                                "Üzgünüm, fotoğrafta ne olduğunu tam anlayamadım. Biraz daha yakından çekebilir misin?"
                            else
                                "Yanlış! Sistem bunu \"$detected\" sandı."

                            val ttsMsg = if (isBlind)
                                "Fotoğrafı biraz daha yakından çekebilir misin?"
                            else
                                "Bu bir $targetObject değil gibi, tekrar deneyelim mi?"

                            app.resetStreak()   // kombo zinciri kırıldı
                            app.playWrongSound()
                            app.speak(ttsMsg)
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(app, toastMsg, Toast.LENGTH_LONG).show()
                            }
                            _uiState.value = CameraUiState.Success(
                                message        = if (isBlind)
                                                     "Fotoğrafı biraz daha yakın çek, anlayamadım!"
                                                 else
                                                     "Dostum, bu bir $targetObject değil. Sanırım başka bir şey buldun!",
                                stickerBitmap  = bitmap,
                                isCorrect      = false,
                                detectedObject = if (isBlind) "?" else detected,
                                targetObject   = targetObject
                            )
                        }
                    } else {
                        _uiState.value = CameraUiState.Error("Görüntü işlenirken hata oluştu.")
                    }
                } else {
                    _uiState.value = CameraUiState.Error(response.message ?: "API Hatası")
                }
            } catch (e: Exception) {
                Log.e(TAG, "processCapturedImage error: ${e.message}", e)
                _uiState.value = CameraUiState.Error("Bağlantı Hatası: ${e.localizedMessage}")
            } finally {
                imageFile.delete()
            }
        }
    }

    // ── Image Resize ────────────────────────────────────────────────────────

    private suspend fun createResizedImageFile(source: File, maxDimension: Int = 800): File =
        withContext(Dispatchers.IO) {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.absolutePath, boundsOpts)

            boundsOpts.inSampleSize   = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxDimension)
            boundsOpts.inJustDecodeBounds = false
            val sampled = BitmapFactory.decodeFile(source.absolutePath, boundsOpts)

            val finalBitmap = if (sampled.width > maxDimension || sampled.height > maxDimension) {
                val ratio = maxDimension.toFloat() / maxOf(sampled.width, sampled.height)
                Bitmap.createScaledBitmap(
                    sampled,
                    (sampled.width  * ratio).toInt(),
                    (sampled.height * ratio).toInt(),
                    true
                ).also { sampled.recycle() }
            } else sampled

            val dest = File(source.parent, "resized_capture.jpg")
            FileOutputStream(dest).use { out -> finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out) }
            finalBitmap.recycle()
            dest
        }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        if (width > maxDimension || height > maxDimension) {
            val halfW = width / 2
            val halfH = height / 2
            while (halfW / inSampleSize > maxDimension || halfH / inSampleSize > maxDimension) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Çekilen görseli, kameranın istediği TAM hedef nesnenin çıkartma satırına kaydeder.
     * (Eski hatalı davranış: id sırasına göre ilk kilitli satıra kaydediyordu → yanlış isim.)
     * Zaten toplanmışsa veya bulunamazsa false döner.
     */
    private suspend fun saveStickerForTarget(filePath: String, levelId: Int, targetName: String): Boolean {
        val sticker = repository.getStickerByLevelAndName(levelId, targetName) ?: return false
        if (sticker.isUnlocked) return false   // güvenlik: aynı nesne ikinci kez kaydedilmesin

        repository.updateSticker(
            sticker.copy(
                image_path = filePath,
                isUnlocked = true,
                timestamp  = System.currentTimeMillis()
            )
        )
        repository.getUser()?.let { user ->
            repository.updateUser(user.copy(total_score = user.total_score + sticker.points))
        }
        return true
    }

    // Replaces the image of an already-unlocked sticker row without touching score or unlock status.
    private suspend fun retakeExistingSticker(newFilePath: String, stickerId: Int) {
        val existing = repository.getStickerById(stickerId) ?: return
        if (existing.image_path.isNotEmpty()) {
            File(existing.image_path).takeIf { it.exists() }?.delete()
        }
        repository.updateSticker(
            existing.copy(
                image_path = newFilePath,
                timestamp  = System.currentTimeMillis()
            )
        )
    }

    private fun decodeBase64(base64String: String): Bitmap? {
        val bytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap): File {
        val file = File(app.filesDir, "sticker_${UUID.randomUUID()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return file
    }

    fun resetState() {
        _uiState.value = CameraUiState.Idle
    }
}

sealed class CameraUiState {
    object Idle : CameraUiState()
    data class Loading(val message: String) : CameraUiState()
    data class Success(
        val message: String,
        val stickerBitmap: Bitmap,
        val isCorrect: Boolean,
        val detectedObject: String,
        val targetObject: String,
        val levelCompleted: Boolean = false,
        val englishWord: String = "",
        val funFact: String = "",
        val comboCount: Int = 0,
        val bonusPoints: Int = 0
    ) : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}
