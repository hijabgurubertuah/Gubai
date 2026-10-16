package com.example.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.browser.WebViewProfileManager
import com.example.browser.WebViewSessionManager
import com.example.data.local.AppDatabase
import com.example.data.model.Tombol
import com.example.data.repository.TombolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data class Browser(val tombolId: String) : Screen
}

class GubAIViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TombolRepository
    val tombolList: StateFlow<List<Tombol>>
    val activeSessionIds: StateFlow<Set<String>> = WebViewSessionManager.activeSessionIds

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TombolRepository(database.tombolDao())
        tombolList = repository.allTombol.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed default item if database is fresh
        viewModelScope.launch {
            repository.ensureDefaultSeedIfEmpty()
        }

        // Check multi-profile capability and show single notice if unsupported
        if (WebViewProfileManager.shouldShowUnsupportedNotice()) {
            Toast.makeText(
                application,
                "Perangkat ini belum mendukung pemisahan data antar tombol",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun openBrowser(tombolId: String) {
        val tombol = tombolList.value.find { it.id == tombolId }
        if (tombol != null) {
            WebViewSessionManager.getOrCreateSession(getApplication<Application>(), tombol)
        }
        _currentScreen.value = Screen.Browser(tombolId)
    }

    fun navigateToHome() {
        _currentScreen.value = Screen.Home
    }

    fun saveTombol(tombol: Tombol) {
        viewModelScope.launch {
            repository.insert(tombol)
            WebViewSessionManager.onAddressOrModeChanged(tombol, getApplication<Application>())
            Toast.makeText(getApplication<Application>(), "Tombol disimpan", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteTombol(tombol: Tombol) {
        viewModelScope.launch {
            WebViewSessionManager.destroySession(tombol.id)
            // Delete profile data (cookies, storage, history)
            WebViewProfileManager.deleteProfile(getApplication<Application>(), tombol.id)
            repository.delete(tombol)
            Toast.makeText(getApplication<Application>(), "Tombol dihapus", Toast.LENGTH_SHORT).show()
        }
    }

    fun resetTombolData(tombol: Tombol) {
        viewModelScope.launch {
            // Reset profile data only and reload session
            WebViewSessionManager.resetSessionData(getApplication<Application>(), tombol)
            Toast.makeText(getApplication<Application>(), "Data tombol dibersihkan", Toast.LENGTH_SHORT).show()
        }
    }

    fun closeSession(tombolId: String) {
        WebViewSessionManager.destroySession(tombolId)
        Toast.makeText(getApplication<Application>(), "Sesi ditutup", Toast.LENGTH_SHORT).show()
    }

    fun saveReorder(items: List<Tombol>) {
        viewModelScope.launch {
            repository.reorder(items)
        }
    }

    fun updateLastUrl(tombolId: String, url: String) {
        viewModelScope.launch {
            repository.updateLastUrl(tombolId, url)
        }
    }

    fun updateButtonPosition(tombolId: String, x: Float, y: Float) {
        viewModelScope.launch {
            repository.updateButtonPosition(tombolId, x, y)
        }
    }
}
