package com.example.pixelhunt.data

object GameData {
    // Level 1 kasıtlı KÜÇÜK ve KOLAY (4 masa/mutfak eşyası) → çocuk hızlıca geçip Level 2'yi açar.
    // Diğer seviyeler 5'er nesne. Hepsi ImageNet ile güvenilir tanınan, evde kolay bulunan objeler.
    // requiredStickerCount otomatik olarak listenin boyutuna eşitlenir (UserRepository).
    val levelThemes: Map<String, List<String>> = mapOf(
        "Oyuncak Dünyası" to listOf("Kalem", "Telefon", "Bardak", "Kaşık"),
        "Mutfak Macerası" to listOf("Kupa", "Tabak", "Tava", "Muz", "Şişe"),
        "Okul Eşyaları"   to listOf("Çanta", "Cetvel", "Gözlük", "Şemsiye", "Kalem"),
        "Bahçe Keşfi"     to listOf("Saksı", "Çiçek", "Kova", "Süpürge", "Top"),
        "Oturma Odası"    to listOf("Televizyon", "Saat", "Yastık", "Lamba", "Vazo")
    )

    // Türkçe UI adı → Vision API'ye gönderilen İngilizce ifade.
    // Backend top-10 ImageNet tahmininde substring eşleşmesi (`p in target`) yaptığı için,
    // her ifade gerçek ImageNet sınıf kelimelerini "or" ile içerir.
    val turkishToEnglish: Map<String, String> = mapOf(
        // Seviye 1 — kolay başlangıç
        "Kalem"       to "ballpoint or pencil or pen",
        "Telefon"     to "cell phone or cellular telephone or smartphone or mobile phone",
        "Bardak"      to "cup or glass or beaker",
        "Kaşık"       to "wooden spoon or spoon or ladle",
        // Seviye 2 — Mutfak Macerası
        "Kupa"        to "coffee mug or cup or mug",
        "Tabak"       to "plate or dish",
        "Tava"        to "frying pan or skillet or wok",
        "Muz"         to "banana",
        "Şişe"        to "water bottle or pop bottle or bottle",
        // Seviye 3 — Okul Eşyaları
        "Çanta"       to "backpack or purse or bag",
        "Cetvel"      to "ruler or rule",
        "Gözlük"      to "sunglasses or dark glasses or glasses",
        "Şemsiye"     to "umbrella",
        // Seviye 4 — Bahçe Keşfi
        "Saksı"       to "flowerpot or pot",
        "Çiçek"       to "daisy or flower",
        "Kova"        to "bucket or pail",
        "Süpürge"     to "broom",
        "Top"         to "ball or soccer ball or tennis ball",
        // Seviye 5 — Oturma Odası
        "Televizyon"  to "television or screen or monitor",
        "Saat"        to "analog clock or wall clock or clock",
        "Yastık"      to "pillow or cushion",
        "Lamba"       to "lampshade or table lamp or lamp",
        "Vazo"        to "vase"
    )

    fun toEnglish(turkishName: String): String =
        turkishToEnglish[turkishName] ?: turkishName

    // ── EĞİTSEL İÇERİK (iki dilli erken öğrenme — SDG 4 Nitelikli Eğitim) ──────
    // Çocuğa öğretilecek TEMİZ İngilizce kelime (API açıklamasından farklı).
    val englishWord: Map<String, String> = mapOf(
        "Kalem" to "pencil", "Telefon" to "phone", "Bardak" to "glass", "Kaşık" to "spoon",
        "Kupa" to "mug", "Tabak" to "plate", "Tava" to "pan", "Muz" to "banana", "Şişe" to "bottle",
        "Çanta" to "bag", "Cetvel" to "ruler", "Gözlük" to "glasses", "Şemsiye" to "umbrella",
        "Saksı" to "flowerpot", "Çiçek" to "flower", "Kova" to "bucket", "Süpürge" to "broom", "Top" to "ball",
        "Televizyon" to "television", "Saat" to "clock", "Yastık" to "pillow", "Lamba" to "lamp", "Vazo" to "vase"
    )

    // Her nesne için çocuk dostu eğlenceli bilgi
    val funFact: Map<String, String> = mapOf(
        "Kalem" to "Kalemlerin içi grafit denen özel bir taştandır!",
        "Telefon" to "Telefonla dünyanın öbür ucundaki insanlarla bile konuşabilirsin!",
        "Bardak" to "Cam bardaklar kumdan yapılır, inanılmaz değil mi!",
        "Kaşık" to "İlk kaşıklar deniz kabuğundan yapılmış!",
        "Kupa" to "Sıcak içecekler kupada daha geç soğur!",
        "Tabak" to "Tabaklar yuvarlaktır çünkü kolayca temizlenir!",
        "Tava" to "Tavalar ısıyı çok hızlı yayar!",
        "Muz" to "Muzlar potasyum doludur ve maymunlar bayılır!",
        "Şişe" to "Bir cam şişe sonsuza kadar geri dönüştürülebilir!",
        "Çanta" to "İlk çantalar hayvan derisinden yapılmış!",
        "Cetvel" to "Cetvel düz çizgi çizmemize yardım eder!",
        "Gözlük" to "Gözlükler yaklaşık 700 yıl önce icat edildi!",
        "Şemsiye" to "Şemsiye ilk olarak güneşten korunmak için yapılmış!",
        "Saksı" to "Bitkiler saksıda büyür ve bize oksijen verir!",
        "Çiçek" to "Çiçekler arılara yardım eder, arılar da bal yapar!",
        "Kova" to "Kovayla su taşımak çok kolaydır!",
        "Süpürge" to "Süpürge tozları toplayıp evimizi temizler!",
        "Top" to "Toplar yuvarlaktır, bu yüzden çok güzel zıplar!",
        "Televizyon" to "İlk televizyonlar siyah-beyazdı!",
        "Saat" to "Saat bir günde tam iki kez döner!",
        "Yastık" to "Yastık başımızı yumuşacık tutar!",
        "Lamba" to "Lamba karanlığı aydınlatır, ilk ampulü Edison yaptı!",
        "Vazo" to "Vazolar çiçekleri suda taze ve mutlu tutar!"
    )

    fun toEnglishWord(turkishName: String): String = englishWord[turkishName] ?: ""
    fun toFunFact(turkishName: String): String = funFact[turkishName] ?: ""

    // Bilgi verirken kullanılan giriş kalıpları — nesneye göre SABİT seçilir (cache uyumu için).
    private val factIntros = listOf(
        "Biliyor musun?",
        "Bunu biliyor muydun?",
        "Sana bir sır vereyim,",
        "Bak şimdi öğreneceksin,",
        "Şunu da bil bakalım,"
    )

    fun factIntro(turkishName: String): String {
        if (turkishName.isEmpty()) return factIntros[0]
        val idx = (turkishName.hashCode() % factIntros.size + factIntros.size) % factIntros.size
        return factIntros[idx]
    }
}
