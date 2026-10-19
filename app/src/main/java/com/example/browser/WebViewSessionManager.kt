package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.example.service.SessionKeepAliveService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object WebViewSessionManager {

    const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"

    // Sessions map keyed by "$siteId:$tabId"
    private val sessions = mutableMapOf<String, TabSession>()

    // Set of active site IDs (for Home Screen card active indicator)
    private val _activeSiteIds = MutableStateFlow<Set<String>>(emptySet())
    val activeSiteIds: StateFlow<Set<String>> = _activeSiteIds.asStateFlow()
    val activeSessionIds: StateFlow<Set<String>> = _activeSiteIds.asStateFlow()

    // Tracks which site was last visible
    var lastActiveTombolId: String? = null

    private fun sessionKey(siteId: String, tabId: String): String = "$siteId:$tabId"

    private fun updateActiveState(context: Context? = null) {
        val uniqueSites = sessions.values.map { it.siteId }.toSet()
        _activeSiteIds.value = uniqueSites

        // Manage Foreground Service based on active session count
        context?.let { ctx ->
            if (sessions.isNotEmpty()) {
                SessionKeepAliveService.startOrUpdate(ctx, sessions.size)
            } else {
                SessionKeepAliveService.stop(ctx)
            }
        }
    }

    class TabSession(
        val siteId: String,
        val tabId: String,
        val webView: WebView,
        var mode: String,
        var currentUrl: String
    )

    fun getSession(siteId: String, tabId: String): TabSession? {
        return sessions[sessionKey(siteId, tabId)]
    }

    fun hasAnyActiveTab(siteId: String): Boolean {
        return sessions.values.any { it.siteId == siteId }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateTabSession(
        context: Context,
        siteId: String,
        tabId: String,
        initialUrl: String,
        mode: String
    ): TabSession {
        val key = sessionKey(siteId, tabId)
        val existing = sessions[key]
        if (existing != null) {
            if (!existing.mode.equals(mode, ignoreCase = true)) {
                existing.mode = mode
                applyModeSettings(existing.webView, mode, context)
                existing.webView.reload()
            }
            return existing
        }

        // Create new WebView for this tab
        val newWebView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // CRITICAL: Attach profile of the parent site!
            // All tabs belonging to the same siteId share cookies, login, localStorage, and Google accounts!
            WebViewProfileManager.attachProfileToWebView(this, siteId)

            // Kiosk web settings
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                allowFileAccess = true
                allowContentAccess = true
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            // Enable 1st & 3rd party cookies
            try {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)
            } catch (e: Exception) {
                // ignore
            }

            // Apply Mobile / Desktop specific configuration
            applyModeSettings(this, mode, context)

            // Try restoring low-memory backup state first, or fallback to initial URL
            val restored = restoreSessionState(context, siteId, tabId, this)
            if (!restored) {
                val targetUrl = initialUrl.ifBlank { "https://www.google.com" }
                loadUrl(targetUrl)
            }
        }

        val session = TabSession(
            siteId = siteId,
            tabId = tabId,
            webView = newWebView,
            mode = mode,
            currentUrl = initialUrl
        )
        sessions[key] = session
        updateActiveState(context)
        return session
    }

    fun applyModeSettings(webView: WebView, mode: String, context: Context) {
        val isDesktop = mode.equals("Desktop", ignoreCase = true)
        val settings = webView.settings
        if (isDesktop) {
            settings.userAgentString = DESKTOP_USER_AGENT
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            webView.setInitialScale(100)
        } else {
            val defaultUa = WebSettings.getDefaultUserAgent(context)
            settings.userAgentString = "$defaultUa GubAI/1.0"
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
        }
    }

    fun injectDesktopIdentity(webView: WebView) {
        val js = """
            (function() {
                try {
                    Object.defineProperty(navigator, 'platform', { get: () => 'Win32', configurable: true });
                    Object.defineProperty(navigator, 'maxTouchPoints', { get: () => 1, configurable: true });
                    if (navigator.userAgentData) {
                        Object.defineProperty(navigator.userAgentData, 'platform', { get: () => 'Windows', configurable: true });
                        Object.defineProperty(navigator.userAgentData, 'mobile', { get: () => false, configurable: true });
                    }
                } catch(e) {}
                try {
                    var meta = document.querySelector('meta[name="viewport"]');
                    if (meta) {
                        meta.setAttribute('content', 'width=1280, initial-scale=0.8');
                    } else {
                        meta = document.createElement('meta');
                        meta.name = 'viewport';
                        meta.content = 'width=1280, initial-scale=0.8';
                        document.head.appendChild(meta);
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    fun destroyTabSession(context: Context, siteId: String, tabId: String) {
        val key = sessionKey(siteId, tabId)
        clearSavedState(context, siteId, tabId)
        sessions.remove(key)?.let { session ->
            (session.webView.parent as? ViewGroup)?.removeView(session.webView)
            session.webView.stopLoading()
            session.webView.clearHistory()
            session.webView.destroy()
        }
        updateActiveState(context)
    }

    fun destroyAllSessionsForSite(context: Context, siteId: String) {
        val keysToRemove = sessions.filter { it.value.siteId == siteId }.keys.toList()
        keysToRemove.forEach { key ->
            val session = sessions.remove(key)
            if (session != null) {
                clearSavedState(context, session.siteId, session.tabId)
                (session.webView.parent as? ViewGroup)?.removeView(session.webView)
                session.webView.stopLoading()
                session.webView.clearHistory()
                session.webView.destroy()
            }
        }
        if (lastActiveTombolId == siteId) {
            lastActiveTombolId = null
        }
        updateActiveState(context)
    }

    fun closeAllSessions() {
        val sessionList = sessions.values.toList()
        sessions.clear()
        sessionList.forEach { session ->
            try {
                (session.webView.parent as? ViewGroup)?.removeView(session.webView)
                session.webView.stopLoading()
                session.webView.clearHistory()
                session.webView.destroy()
            } catch (e: Exception) {
                // ignore
            }
        }
        lastActiveTombolId = null
        _activeSiteIds.value = emptySet()
    }

    fun resetSiteData(context: Context, siteId: String) {
        destroyAllSessionsForSite(context, siteId)
        WebViewProfileManager.clearProfileData(context, siteId)
    }

    // Called when the application is backgrounded/minimized
    fun onAppBackgrounded(context: Context) {
        // Flush cookies to ensure storage persistence across restarts
        try {
            CookieManager.getInstance().flush()
        } catch (e: Exception) {
            // ignore
        }

        // Save state backup for each running tab session in case of system low-memory kill
        sessions.forEach { (_, session) ->
            saveSessionState(context, session.siteId, session.tabId, session.webView)
        }
        // CRITICAL: DO NOT call webView.pauseTimers() or webView.onPause()
        // This keeps audio, video, WebSockets, background tabs running seamlessly.
    }

    // Called when the application returns to foreground
    fun onResumeAllSessions() {
        sessions.values.forEach { session ->
            try {
                session.webView.onResume()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun getStateFile(context: Context, siteId: String, tabId: String): File {
        val dir = File(context.filesDir, "webview_states")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "state_${siteId}_${tabId}.bin")
    }

    fun saveSessionState(context: Context, siteId: String, tabId: String, webView: WebView) {
        try {
            val bundle = Bundle()
            val stateSaved = webView.saveState(bundle)
            if (stateSaved != null) {
                val parcel = Parcel.obtain()
                bundle.writeToParcel(parcel, 0)
                val bytes = parcel.marshall()
                parcel.recycle()

                val file = getStateFile(context, siteId, tabId)
                FileOutputStream(file).use { fos ->
                    fos.write(bytes)
                }
            }
        } catch (e: Exception) {
            // ignore save state failure
        }
    }

    fun restoreSessionState(context: Context, siteId: String, tabId: String, webView: WebView): Boolean {
        try {
            val file = getStateFile(context, siteId, tabId)
            if (!file.exists()) return false

            val bytes = FileInputStream(file).use { fis ->
                fis.readBytes()
            }
            if (bytes.isEmpty()) return false

            val parcel = Parcel.obtain()
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            val bundle = Bundle.CREATOR.createFromParcel(parcel)
            parcel.recycle()

            val restored = webView.restoreState(bundle)
            return restored != null
        } catch (e: Exception) {
            return false
        }
    }

    fun clearSavedState(context: Context, siteId: String, tabId: String) {
        try {
            val file = getStateFile(context, siteId, tabId)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            // ignore
        }
    }
}
