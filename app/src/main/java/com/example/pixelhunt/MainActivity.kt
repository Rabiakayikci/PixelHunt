package com.example.pixelhunt

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pixelhunt.data.HunterPixelDatabase
import com.example.pixelhunt.ui.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Uygulamada geçirilen süreyi ölçmek için oturum başlangıcı
    private var sessionStart = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            testDatabase()
        }

        setContent {
            PixelHuntNavigation()
        }
    }

    override fun onResume() {
        super.onResume()
        sessionStart = SystemClock.elapsedRealtime()
    }

    override fun onPause() {
        super.onPause()
        if (sessionStart > 0) {
            (application as PixelHuntApplication).addUsageTime(SystemClock.elapsedRealtime() - sessionStart)
            sessionStart = 0
        }
    }

    private fun testDatabase() {
        val db = HunterPixelDatabase.getDatabase(this)
        lifecycleScope.launch {
            try {
                val user = db.userDao().getUser()
                if (user != null) {
                    Log.d("DB_TEST", "Veritabanı Hazır! Kullanıcı: ${user.user_name}, Skor: ${user.total_score}")
                }
            } catch (e: Exception) {
                Log.e("DB_TEST", "Veritabanı Hatası: ${e.message}")
            }
        }
    }
}

@Composable
fun PixelHuntNavigation() {
    val navController = rememberNavController()
    // ViewModel'lar merkezi olarak tanımlandı
    val userViewModel: UserViewModel = viewModel()
    val gameViewModel: GameViewModel = viewModel()
    val albumViewModel: AlbumViewModel = viewModel()
    val levels by gameViewModel.allLevels.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "main_screen"
    ) {
        // 1. Ana Sayfa (MainScreen)
        composable("main_screen") {
            MainScreen(
                onAlbumClick = {
                    navController.navigate("album_screen")
                },
                onExploreClick = {
                    navController.navigate("map_screen")
                },
                onNavigateToMap = {
                    navController.navigate("map_screen")
                },
                onNavigateToSettings = {
                    navController.navigate("settings_screen")
                },
                onNavigateToProfile = {
                    navController.navigate("profile_screen")
                },
                onNavigateToCamera = {
                    val targetLevelId = levels
                        .filter { it.isUnlocked }
                        .maxByOrNull { it.levelId }
                        ?.levelId ?: 1
                    navController.navigate("camera_screen/$targetLevelId")
                },
                onNavigateToQuiz = {
                    navController.navigate("quiz_screen")
                }
            )
        }

        // Mini Quiz (QuizScreen)
        composable("quiz_screen") {
            QuizScreen(
                gameViewModel = gameViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 2. Albüm Sayfası (AlbumScreen)
        composable("album_screen") {
            AlbumScreen(
                albumViewModel = albumViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onHomeClick = {
                    navController.navigate("main_screen") {
                        popUpTo("main_screen") { inclusive = true }
                    }
                }
            )
        }

        // 3. Harita Sayfası (MapScreen)
        composable("map_screen") {
            MapScreen(
                gameViewModel = gameViewModel,
                userViewModel = userViewModel,
                onNavigateToSettings = {
                    navController.navigate("settings_screen")
                },
                onNavigateToProfile = {
                    navController.navigate("profile_screen")
                },
                onHomeClick = {
                    navController.navigate("main_screen") {
                        popUpTo("main_screen") { inclusive = true }
                    }
                },
                onLevelClick = { levelId ->
                    navController.navigate("level_detail/$levelId")
                }
            )
        }

        // 4. Ayarlar Sayfası (SettingsScreen)
        composable("settings_screen") {
            SettingsScreen(
                userViewModel = userViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onHomeClick = {
                    navController.navigate("main_screen") {
                        popUpTo("main_screen") { inclusive = true }
                    }
                }
            )
        }

        // 5. Profil Sayfası (ProfileScreen)
        composable("profile_screen") {
            ProfileScreen(
                userViewModel = userViewModel,
                gameViewModel = gameViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onHomeClick = {
                    navController.navigate("main_screen") {
                        popUpTo("main_screen") { inclusive = true }
                    }
                },
                onNavigateToReport = {
                    navController.navigate("report_screen")
                }
            )
        }

        // 5b. Veli & Öğretmen Raporu (ReportScreen)
        composable("report_screen") {
            ReportScreen(
                gameViewModel = gameViewModel,
                userViewModel = userViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 6. Seviye Detay Sayfası (LevelDetailScreen)
        composable(
            route = "level_detail/{levelId}",
            arguments = listOf(navArgument("levelId") { type = NavType.IntType })
        ) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getInt("levelId") ?: 1
            LevelDetailScreen(
                levelId = levelId,
                gameViewModel = gameViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToCamera = {
                    navController.navigate("camera_screen/$levelId")
                },
                onNavigateToRetake = { retakeTarget, retakeStickerDbId ->
                    navController.navigate(
                        "camera_screen/$levelId?retakeTarget=$retakeTarget&retakeStickerDbId=$retakeStickerDbId"
                    )
                }
            )
        }

        // 7. Kamera Ekranı (Keşif) — optional retake params for "Tekrar Oluştur" flow
        composable(
            route = "camera_screen/{levelId}?retakeTarget={retakeTarget}&retakeStickerDbId={retakeStickerDbId}",
            arguments = listOf(
                navArgument("levelId") { type = NavType.IntType },
                navArgument("retakeTarget") { type = NavType.StringType; defaultValue = "" },
                navArgument("retakeStickerDbId") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { backStackEntry ->
            val levelId           = backStackEntry.arguments?.getInt("levelId") ?: 1
            val retakeTarget      = backStackEntry.arguments?.getString("retakeTarget") ?: ""
            val retakeStickerDbId = backStackEntry.arguments?.getInt("retakeStickerDbId") ?: -1
            CameraScreen(
                currentLevelId = levelId,
                retakeTarget = retakeTarget,
                retakeStickerDbId = retakeStickerDbId,
                onNavigateToMap = {
                    navController.navigate("map_screen") {
                        popUpTo("camera_screen/$levelId") { inclusive = true }
                    }
                },
                onNavigateToAlbum = {
                    navController.navigate("album_screen") {
                        popUpTo("camera_screen/$levelId") { inclusive = true }
                    }
                }
            )
        }
    }
}
