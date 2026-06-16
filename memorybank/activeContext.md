# Active Context - PixelHUNT

## Current Focus
Implemented "Test/Verification Mode" to ensure easy testing of the SAM-r object detection and database persistence. Added "Mouse" as the primary target for all levels.

## Recent Changes
- **Camera Logic:**
    - Modified `CameraViewModel.kt` to include "Mouse" in all theme object lists.
    - Forced `selectRandomTarget` to always select the first item ("Mouse") for consistent testing.
- **Database Seeding:**
    - Updated `UserRepository.kt`'s `seedInitialData` to include "Mouse" as the first sticker for every level (requires DB reset/reinstall to see name changes in UI).
- **Navigation Fix:**
    - Modified `LevelDetailScreen.kt` to disable camera navigation from locked sticker slots.
- **Previous:** Integrated Room DB for dynamic sticker display and implemented SAM-r randomized target logic.

## Status
Verification mode active: SAM-r will now always ask for a "Mouse" across all levels. This allows for consistent testing of the API and database persistence.
