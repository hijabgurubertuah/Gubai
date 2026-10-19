package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.GubAIViewModel
import com.example.ui.Screen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.GubAITheme

class MainActivity : ComponentActivity() {

    private val viewModel: GubAIViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS permission on Android 13+ for Foreground Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Keep screen on during app runtime
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Enable edge-to-edge
        enableEdgeToEdge()

        // Configure Immersive Kiosk Mode (hidden status & navigation bars, transient on swipe)
        hideSystemBars()

        setContent {
            GubAITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GubAIApp(viewModel = viewModel)
                }
            }
        }
    }

    fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
fun GubAIApp(viewModel: GubAIViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val tombolList by viewModel.tombolList.collectAsStateWithLifecycle()
    val activeSessionIds by viewModel.activeSessionIds.collectAsStateWithLifecycle()

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                tombolList = tombolList,
                activeSessionIds = activeSessionIds,
                onOpenBrowser = { tombolId ->
                    viewModel.openBrowser(tombolId)
                },
                onSaveTombol = { tombol ->
                    viewModel.saveTombol(tombol)
                },
                onDeleteTombol = { tombol ->
                    viewModel.deleteTombol(tombol)
                },
                onResetTombolData = { tombol ->
                    viewModel.resetTombolData(tombol)
                },
                onCloseSession = { tombolId ->
                    viewModel.closeSession(tombolId)
                },
                onSaveReorder = { reorderedList ->
                    viewModel.saveReorder(reorderedList)
                }
            )
        }

        is Screen.Browser -> {
            val currentTombol = tombolList.find { it.id == screen.tombolId }
            if (currentTombol != null) {
                val siteTabs by viewModel.getTabsForSite(currentTombol.id)
                    .collectAsStateWithLifecycle(initialValue = emptyList())

                BrowserScreen(
                    tombol = currentTombol,
                    tabs = siteTabs,
                    onAddNewTab = { input ->
                        viewModel.addNewTab(
                            siteId = currentTombol.id,
                            input = input,
                            isDesktop = currentTombol.mode.equals("Desktop", ignoreCase = true)
                        )
                    },
                    onSelectTab = { tabId ->
                        viewModel.switchTab(currentTombol.id, tabId)
                    },
                    onCloseTab = { tabId ->
                        viewModel.closeTab(
                            siteId = currentTombol.id,
                            tabId = tabId,
                            defaultUrl = currentTombol.alamat,
                            defaultTitle = currentTombol.nama,
                            isDesktop = currentTombol.mode.equals("Desktop", ignoreCase = true)
                        )
                    },
                    onUpdateTabTitleAndUrl = { tabId, title, url ->
                        viewModel.updateTabTitleAndUrl(tabId, title, url)
                    },
                    onBackToHome = {
                        viewModel.navigateToHome()
                    },
                    onUpdateLastUrl = { newUrl ->
                        viewModel.updateLastUrl(currentTombol.id, newUrl)
                    }
                )
            } else {
                // If item not found or was deleted, fall back to Home
                LaunchedEffect(Unit) {
                    viewModel.navigateToHome()
                }
            }
        }
    }
}
