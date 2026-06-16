# System Patterns

## Technical Architecture
* Language: Android Kotlin and Python Server API.
* ML Flow: Raw images sent from the device are processed in the Python API and returned as segmented sticker data.
* Communication: Internet connection is mandatory and API responses are managed asynchronously.

## Database Plan
Data will be stored offline first across 3 primary tables:
1. USER: Stores user information and total score.
2. MISSION: Contains the target object list and predefined clue texts for TTS.
3. STICKER: Stores the successfully extracted object file path, earned points, timestamp, and `level_id`.

### Sticker Table Schema Update
* **level_id (Integer):** Indicates which level (1-5) the sticker belongs to. Each level has 5 unique stickers.

### User Profile & Persistence
* **User_Progress Tablo Güncellemesi:** Bu tablo artık `user_name` (String) ve `selected_avatar_sticker_id` (Integer, default = -1) alanlarını içermelidir.
* **Avatar Logic:** Eğer `selected_avatar_sticker_id` değeri -1 ise UI varsayılan `Icons.Default.Person` ikonunu gösterir; pozitif bir ID varsa Stickers tablosundan ilgili görseli çeker.
* **Persistence:** Ayarlar ekranında yapılan her değişiklik (isim veya avatar) anında Room Database üzerinden güncellenmeli ve uygulama yeniden başlatıldığında bu verilerle açılmalıdır.

## Database & Gamification Logic
* Sticker Model: Each sticker in the database will have an ID, an image reference (drawable ID), `level_id`, and an `isUnlocked` (Boolean) state.
* Level/Status System: User status is calculated dynamically based on the `unlocked_sticker_count`:
    * 0-5 stickers: 'Rookie Explorer' (Çaylak Kaşif)
    * 6-15 stickers: 'Skilled Hunter' (Yetenekli Avcı)
    * 16-25 stickers: 'Super Collector' (Süper Koleksiyoner)
* UI Reflection: Status texts and progress bar fill ratios in screens like AlbumScreen and MainScreen will be rendered directly from these database statistics instead of hardcoded strings.

## Progressive Level System
* Structure: The app features 5 main levels (Planets).
* Mission Flow: Each level consists of 5 sub-missions.
* Progression: When a user reaches 5/5 progress, the next level's `isUnlocked` status becomes true, and the lock icon is replaced by a rocket.

## Level Detail Filtering
* **Logic:** `LevelDetailScreen` queries the database for stickers where `level_id` matches the selected level.
* **Display:** Shows 5 sticker slots. Unlocked stickers show their color image, locked ones show a placeholder/lock.

## Software Rules
* Visual Feedback: SAM-r must display a waiting notification or animation while awaiting the API response.
* Data Model: Timestamp and score are mandatory fields for sticker records.
* Error Management: SAM-r provides both vocal and visual speech bubble feedback in case of API errors or incorrect objects.