package com.example.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.browser.WebViewProfileManager
import com.example.browser.WebViewSessionManager
import com.example.data.local.AppDatabase
import com.example.data.local.TabDao
import com.example.data.model.TabEntity
import com.example.data.model.Tombol
import com.example.data.repository.TombolRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.UUID

sealed interface Screen {
    data object Home : Screen
    data class Browser(val tombolId: String) : Screen
}

class GubAIViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val MAX_TABS_PER_BROWSER = 10

        fun resolveInput(text: String): String {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return "https://www.google.com"

            val hasScheme = trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true)
            if (hasScheme) return trimmed

            val domainRegex = Regex("^[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+.*$")
            return if (!trimmed.contains(" ") && domainRegex.matches(trimmed)) {
                "https://$trimmed"
            } else {
                val encoded = try {
                    URLEncoder.encode(trimmed, "UTF-8")
                } catch (e: Exception) {
                    trimmed
                }
                "https://www.google.com/search?q=$encoded"
            }
        }
    }

    private val repository: TombolRepository
    private val tabDao: TabDao

    val tombolList: StateFlow<List<Tombol>>
    val activeSessionIds: StateFlow<Set<String>> = WebViewSessionManager.activeSiteIds

    private val _currentScreen = MutableStateFlow<Screen>(
        WebViewSessionManager.lastActiveTombolId?.let { Screen.Browser(it) } ?: Screen.Home
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TombolRepository(database.tombolDao())
        tabDao = database.tabDao()

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
            viewModelScope.launch {
                ensureInitialTab(
                    siteId = tombol.id,
                    defaultUrl = tombol.alamatTerakhir.ifBlank { tombol.alamat },
                    defaultTitle = tombol.nama,
                    isDesktop = tombol.mode.equals("Desktop", ignoreCase = true)
                )
            }
        }
        WebViewSessionManager.lastActiveTombolId = tombolId
        _currentScreen.value = Screen.Browser(tombolId)
    }

    fun navigateToHome() {
        WebViewSessionManager.lastActiveTombolId = null
        _currentScreen.value = Screen.Home
    }

    fun getTabsForSite(siteId: String): Flow<List<TabEntity>> {
        return tabDao.getTabsForSite(siteId)
    }

    fun ensureInitialTab(
        siteId: String,
        defaultUrl: String,
        defaultTitle: String,
        isDesktop: Boolean
    ) {
        viewModelScope.launch {
            val count = tabDao.getTabCount(siteId)
            if (count == 0) {
                val initialTab = TabEntity(
                    tabId = UUID.randomUUID().toString(),
                    siteId = siteId,
                    url = defaultUrl.ifBlank { "https://www.google.com" },
                    title = defaultTitle.ifBlank { "Beranda" },
                    isDesktopMode = isDesktop,
                    urutan = 0,
                    isActive = true
                )
                tabDao.insert(initialTab)
            }
        }
    }

    fun addNewTab(
        siteId: String,
        input: String,
        isDesktop: Boolean,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val currentCount = tabDao.getTabCount(siteId)
            if (currentCount >= MAX_TABS_PER_BROWSER) {
                Toast.makeText(
                    getApplication(),
                    "Maksimal $MAX_TABS_PER_BROWSER tab per browser",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val targetUrl = resolveInput(input)
            tabDao.deactivateAllForSite(siteId)

            val newTab = TabEntity(
                tabId = UUID.randomUUID().toString(),
                siteId = siteId,
                url = targetUrl,
                title = if (input.startsWith("http://") || input.startsWith("https://")) input else "Tab Baru",
                isDesktopMode = isDesktop,
                urutan = currentCount,
                isActive = true
            )
            tabDao.insert(newTab)
            onSuccess?.invoke()
        }
    }

    fun switchTab(siteId: String, tabId: String) {
        viewModelScope.launch {
            tabDao.deactivateAllForSite(siteId)
            tabDao.activateTab(tabId)
        }
    }

    fun closeTab(
        siteId: String,
        tabId: String,
        defaultUrl: String,
        defaultTitle: String,
        isDesktop: Boolean
    ) {
        viewModelScope.launch {
            WebViewSessionManager.destroyTabSession(getApplication(), siteId, tabId)
            tabDao.deleteById(tabId)

            val remaining = tabDao.getTabsForSiteList(siteId)
            if (remaining.isEmpty()) {
                // Rule 4: If last tab closed, auto create a new tab with homepage, do NOT exit browser
                val fallbackTab = TabEntity(
                    tabId = UUID.randomUUID().toString(),
                    siteId = siteId,
                    url = defaultUrl.ifBlank { "https://www.google.com" },
                    title = defaultTitle.ifBlank { "Beranda" },
                    isDesktopMode = isDesktop,
                    urutan = 0,
                    isActive = true
                )
                tabDao.insert(fallbackTab)
            } else if (remaining.none { it.isActive }) {
                // Activate the last remaining tab
                val lastTab = remaining.last()
                tabDao.activateTab(lastTab.tabId)
            }
        }
    }

    fun updateTabTitleAndUrl(tabId: String, title: String, url: String) {
        viewModelScope.launch {
            tabDao.updateTitleAndUrl(tabId, title, url)
        }
    }

    fun saveTombol(tombol: Tombol) {
        viewModelScope.launch {
            repository.insert(tombol)
            Toast.makeText(getApplication(), "Tombol disimpan", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteTombol(tombol: Tombol) {
        viewModelScope.launch {
            WebViewSessionManager.destroyAllSessionsForSite(getApplication(), tombol.id)
            WebViewProfileManager.deleteProfile(getApplication(), tombol.id)
            tabDao.deleteBySiteId(tombol.id)
            repository.delete(tombol)
            Toast.makeText(getApplication(), "Tombol dihapus", Toast.LENGTH_SHORT).show()
        }
    }

    fun resetTombolData(tombol: Tombol) {
        viewModelScope.launch {
            WebViewSessionManager.resetSiteData(getApplication(), tombol.id)
            tabDao.deleteBySiteId(tombol.id)
            ensureInitialTab(
                siteId = tombol.id,
                defaultUrl = tombol.alamat,
                defaultTitle = tombol.nama,
                isDesktop = tombol.mode.equals("Desktop", ignoreCase = true)
            )
            Toast.makeText(getApplication(), "Cache dan data dibersihkan", Toast.LENGTH_SHORT).show()
        }
    }

    fun closeSession(tombolId: String) {
        WebViewSessionManager.destroyAllSessionsForSite(getApplication(), tombolId)
        Toast.makeText(getApplication(), "Sesi diakhiri", Toast.LENGTH_SHORT).show()
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
