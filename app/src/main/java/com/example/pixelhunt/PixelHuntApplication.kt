package com.example.pixelhunt

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import com.example.pixelhunt.BuildConfig
import com.example.pixelhunt.data.HunterPixelDatabase
import com.example.pixelhunt.data.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

private const val TAG = "PixelHuntApp"

class PixelHuntApplication : Application() {

    val database: HunterPixelDatabase by lazy { HunterPixelDatabase.getDatabase(this) }
    val repository: UserRepository by lazy {
        UserRepository(database.userDao(), database.stickerDao(), database.levelDao())
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ttsJob: Job? = null
    private var player: MediaPlayer? = null

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _isTtsReady = MutableStateFlow(true)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady

    private val _stickerCapturedEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val stickerCapturedEvent: SharedFlow<Int> = _stickerCapturedEvent

    override fun onCreate() {
        super.onCreate()
        // Uygulama açılır açılmaz hoş geldin konuşması
        appScope.launch {
            kotlinx.coroutines.delay(1500)
            launch("Hoş geldin! Ben SAM-r! Seninle birlikte harika nesneler keşfedeceğiz! Hazır mısın?")
        }
    }

    fun speak(text: String)                                     = launch(text)
    fun speakWithVoice(text: String, pitch: Float, rate: Float) = launch(text)
    fun stopSpeech()                                            { ttsJob?.cancel(); release() }
    fun notifyStickerCaptured(n: Int)                           { _stickerCapturedEvent.tryEmit(n) }

    // ── Kombo zinciri (navigasyon arası korunması için singleton'da tutulur) ──
    var correctStreak: Int = 0
        private set
    fun incrementStreak(): Int { correctStreak++; reportBestCombo(correctStreak); return correctStreak }
    fun resetStreak() { correctStreak = 0 }

    // ── Genel istatistikler: geçirilen süre + en iyi kombo (SharedPreferences) ──
    private fun statsPrefs() = getSharedPreferences("samr_stats", MODE_PRIVATE)

    fun addUsageTime(ms: Long) {
        if (ms <= 0) return
        val p = statsPrefs()
        p.edit().putLong("usage_ms", p.getLong("usage_ms", 0L) + ms).apply()
    }
    fun getTotalUsageMs(): Long = statsPrefs().getLong("usage_ms", 0L)

    fun reportBestCombo(combo: Int) {
        val p = statsPrefs()
        if (combo > p.getInt("best_combo", 0)) p.edit().putInt("best_combo", combo).apply()
    }
    fun getBestCombo(): Int = statsPrefs().getInt("best_combo", 0)

    fun resetExtraStats() { statsPrefs().edit().clear().apply() }

    // ── Quiz istatistikleri (Veli Paneli'nde gösterilir, SharedPreferences'ta kalıcı) ──
    private fun quizPrefs() = getSharedPreferences("samr_quiz", MODE_PRIVATE)

    fun recordQuizResult(correct: Int, total: Int) {
        val p = quizPrefs()
        p.edit()
            .putInt("played", p.getInt("played", 0) + 1)
            .putInt("questions", p.getInt("questions", 0) + total)
            .putInt("correct", p.getInt("correct", 0) + correct)
            .putInt("best", maxOf(p.getInt("best", 0), correct))
            .putLong("last", System.currentTimeMillis())
            .apply()
    }

    fun getQuizStats(): QuizStats {
        val p = quizPrefs()
        return QuizStats(
            played    = p.getInt("played", 0),
            questions = p.getInt("questions", 0),
            correct   = p.getInt("correct", 0),
            best      = p.getInt("best", 0),
            lastPlayed = p.getLong("last", 0L)
        )
    }

    fun resetQuizStats() { quizPrefs().edit().clear().apply() }

    // ── Ses efektleri (AudioTrack ile sentez — asset gerektirmez, yüksek & net) ──

    /** Doğru yakalama: yükselen neşeli akor (Do-Mi-Sol-Do) */
    fun playCorrectSound() =
        playChime(listOf(523.25, 659.25, 783.99, 1046.50), noteMs = 130)

    /** Kombo: daha uzun, coşkulu yükselen dizi (Do-Mi-Sol-Do-Mi-Sol) */
    fun playComboSound() =
        playChime(listOf(523.25, 659.25, 783.99, 1046.50, 1318.51, 1567.98), noteMs = 110)

    /** Yanlış yakalama: nazik inen iki nota */
    fun playWrongSound() =
        playChime(listOf(392.00, 293.66), noteMs = 200)

    /**
     * Verilen frekansları sırayla, kesintisiz tek bir tampona sentezleyip çalar.
     * Her notada yumuşak attack/release zarfı uygulanır (tıkırtı olmaz).
     */
    private fun playChime(freqs: List<Double>, noteMs: Int) {
        if (!isSoundEnabled()) return
        appScope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 44100
                val perNote = sampleRate * noteMs / 1000
                val total = perNote * freqs.size
                val buffer = ShortArray(total)
                freqs.forEachIndexed { n, freq ->
                    val attack = perNote * 0.08
                    val release = perNote * 0.35
                    for (i in 0 until perNote) {
                        val env = when {
                            i < attack                -> i / attack
                            i > perNote - release     -> (perNote - i) / release
                            else                      -> 1.0
                        }
                        val sample = kotlin.math.sin(2.0 * Math.PI * i * freq / sampleRate) * env * 0.7
                        buffer[n * perNote + i] = (sample * Short.MAX_VALUE).toInt().toShort()
                    }
                }
                @Suppress("DEPRECATION")
                val track = AudioTrack(
                    AudioManager.STREAM_MUSIC, sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    total * 2, AudioTrack.MODE_STATIC
                )
                track.write(buffer, 0, total)
                track.setVolume(1.0f)
                track.play()
                kotlinx.coroutines.delay((noteMs.toLong() * freqs.size) + 150)
                track.release()
            } catch (e: Exception) { Log.w(TAG, "Chime: ${e.message}") }
        }
    }

    // ── Ses açık/kapalı tercihi (Ayarlar'daki "Ses ve Müzik" anahtarı) ──────
    fun isSoundEnabled(): Boolean =
        getSharedPreferences("samr_prefs", MODE_PRIVATE).getBoolean("sound_on", true)

    fun setSoundEnabled(enabled: Boolean) {
        getSharedPreferences("samr_prefs", MODE_PRIVATE).edit().putBoolean("sound_on", enabled).apply()
        if (!enabled) stopSpeech()
    }

    // ── TTS ses cache — tekrarlanan cümleler ağ beklemeden anında çalsın ──────
    private val audioCacheDir by lazy { File(cacheDir, "tts_cache").apply { mkdirs() } }
    private val audioCache = object : LinkedHashMap<String, File>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, File>): Boolean {
            if (size > 60) { eldest.value.delete(); return true }
            return false
        }
    }

    /** Metnin ses dosyasını önceden indirip cache'ler (oynatmaz). [lang]: "tr" veya "en". */
    fun prewarm(text: String, lang: String = "tr") {
        if (text.isBlank() || !isSoundEnabled()) return
        appScope.launch { runCatching { getOrFetchAudioFile(text, lang) } }
    }

    private suspend fun getOrFetchAudioFile(text: String, lang: String = "tr"): File? = withContext(Dispatchers.IO) {
        val key = "$lang:$text"
        synchronized(audioCache) { audioCache[key] }?.takeIf { it.exists() }?.let { return@withContext it }
        val bytes = fetchAudio(text, lang) ?: return@withContext null
        val f = File(audioCacheDir, "tts_${key.hashCode()}.mp3")
        runCatching { f.writeBytes(bytes) }
        synchronized(audioCache) { audioCache[key] = f }
        f
    }

    private fun launch(text: String) {
        if (text.isBlank() || !isSoundEnabled()) return
        ttsJob?.cancel()
        ttsJob = appScope.launch {
            release()
            try {
                val file = getOrFetchAudioFile(text, "tr") ?: return@launch
                playFile(file)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { Log.e(TAG, "TTS: ${e.message}") }
        }
    }

    /**
     * Birden çok parçayı SIRAYLA seslendirir — her parça kendi dilinde (TR/EN).
     * İngilizce kelime gerçek İngilizce telaffuzla okunur. Parçalar ayrı ayrı çalınır
     * (birleştirilmiş MP3 değil), böylece MediaPlayer'da sorunsuz çalar.
     */
    fun speakSegments(segments: List<TtsSegment>) {
        if (!isSoundEnabled()) return
        val clean = segments.filter { it.text.isNotBlank() }
        if (clean.isEmpty()) return
        ttsJob?.cancel()
        ttsJob = appScope.launch {
            release()
            try {
                for (seg in clean) {
                    val file = getOrFetchAudioFile(seg.text, seg.lang) ?: continue
                    playFile(file)   // tamamlanana kadar bekler, sonra sıradaki parça
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { Log.e(TAG, "TTS seq: ${e.message}") }
        }
    }

    private fun fetchAudio(text: String, lang: String = "tr"): ByteArray? {
        val vrHl   = if (lang == "en") "en-us" else "tr-tr"
        val gTl    = if (lang == "en") "en" else "tr"

        // ── 1. VoiceRSS — ANA MOTOR (en güzel ses), key tanımlı ──
        val vrKey = BuildConfig.VOICERSS_KEY
        if (vrKey.isNotEmpty()) {
            try {
                val enc = URLEncoder.encode(text.take(490), "UTF-8")
                val resp = http.newCall(
                    Request.Builder()
                        .url("https://api.voicerss.org/?key=$vrKey&hl=$vrHl&src=$enc&c=MP3&f=44khz_16bit_stereo&r=0")
                        .build()
                ).execute()
                val bytes = resp.body?.bytes()
                if (bytes != null && bytes.size > 800 && !String(bytes.copyOf(10)).startsWith("ERR")) {
                    Log.d(TAG, "VoiceRSS OK ($lang) — ${bytes.size} bayt"); return bytes
                } else {
                    Log.w(TAG, "VoiceRSS hata/kota: ${bytes?.let { String(it.copyOf(120)) }}")
                }
            } catch (e: Exception) { Log.w(TAG, "VoiceRSS: ${e.message}") }
        }

        // ── 2. Google Translate TTS — TEK çağrı, dile göre tl ──
        return try {
            val enc = URLEncoder.encode(text.take(190), "UTF-8")
            http.newCall(
                Request.Builder()
                    .url("https://translate.googleapis.com/translate_tts?ie=UTF-8&q=$enc&tl=$gTl&client=gtx&ttsspeed=0.95")
                    .addHeader("User-Agent", "Mozilla/5.0").build()
            ).execute().use { r ->
                val bytes = if (r.isSuccessful) r.body?.bytes() else null
                bytes?.takeIf { it.size > 800 }?.also { Log.d(TAG, "Google TTS OK ($lang) — ${it.size} bayt") }
            }
        } catch (e: Exception) { Log.w(TAG, "GTrans: ${e.message}"); null }
    }

    // Cache'lenmiş yerel dosyayı çalar — dosyayı SİLMEZ (yeniden kullanılacak).
    private suspend fun playFile(file: File) {
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(file.absolutePath)
                    setOnPreparedListener  { it.start() }
                    setOnCompletionListener { if (cont.isActive) cont.resume(Unit) }
                    setOnErrorListener     { _, _, _ -> if (cont.isActive) cont.resume(Unit); true }
                    prepareAsync()
                }
                player = mp
                cont.invokeOnCancellation { mp.runCatching { stop(); release() } }
            }
            player = null
        }
    }

    private fun release() {
        player?.runCatching { if (isPlaying) stop(); release() }
        player = null
    }

    override fun onTerminate() { super.onTerminate(); appScope.cancel(); release() }
}

/** Çok dilli seslendirme parçası — [lang] "tr" veya "en". */
data class TtsSegment(val text: String, val lang: String = "tr")

/** Veli Paneli için quiz performans özeti. */
data class QuizStats(
    val played: Int,
    val questions: Int,
    val correct: Int,
    val best: Int,
    val lastPlayed: Long
)
