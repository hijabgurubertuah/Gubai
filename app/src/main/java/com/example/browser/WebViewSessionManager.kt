package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.example.data.model.Tombol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WebViewSessionManager {

    const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"

    private val sessions = mutableMapOf<String, ButtonSession>()
    private val _activeSessionIds = MutableStateFlow<Set<String>>(emptySet())
    val activeSessionIds: StateFlow<Set<String>> = _activeSessionIds.asStateFlow()

    private fun updateActiveState() {
        _activeSessionIds.value = sessions.keys.toSet()
    }

    class ButtonSession(
        val tombolId: String,
        val webView: WebView,
        var mode: String,
        var originalAddress: String
    )

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateSession(
        context: Context,
        tombol: Tombol
    ): ButtonSession {
        val existing = sessions[tombol.id]
        if (existing != null) {
            // Check if mode was changed
            if (!existing.mode.equals(tombol.mode, ignoreCase = true)) {
                existing.mode = tombol.mode
                applyModeSettings(existing.webView, tombol.mode, context)
                existing.webView.reload()
            }
            // Check if address was changed
            if (existing.originalAddress != tombol.alamat) {
                existing.originalAddress = tombol.alamat
                existing.webView.loadUrl(tombol.alamat)
            }
            return existing
        }

        // Create new WebView
        val newWebView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Attach multi-profile isolation
            WebViewProfileManager.attachProfileToWebView(this, tombol.id)

            // Common browser settings
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
            applyModeSettings(this, tombol.mode, context)

            // Load initial target
            val initialTarget = tombol.alamatTerakhir.ifBlank { tombol.alamat }
            loadUrl(initialTarget)
        }

        val session = ButtonSession(
            tombolId = tombol.id,
            webView = newWebView,
            mode = tombol.mode,
            originalAddress = tombol.alamat
        )
        sessions[tombol.id] = session
        updateActiveState()
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

    fun destroySession(tombolId: String) {
        sessions.remove(tombolId)?.let { session ->
            (session.webView.parent as? ViewGroup)?.removeView(session.webView)
            session.webView.stopLoading()
            session.webView.clearHistory()
            session.webView.destroy()
        }
        updateActiveState()
    }

    fun resetSessionData(context: Context, tombol: Tombol) {
        WebViewProfileManager.clearProfileData(context, tombol.id)
        sessions[tombol.id]?.let { session ->
            session.webView.clearCache(true)
            session.webView.clearFormData()
            session.webView.clearHistory()
            session.webView.loadUrl(tombol.alamat)
        }
    }

    fun onAddressOrModeChanged(tombol: Tombol, context: Context) {
        sessions[tombol.id]?.let { session ->
            var needsReload = false
            if (!session.mode.equals(tombol.mode, ignoreCase = true)) {
                session.mode = tombol.mode
                applyModeSettings(session.webView, tombol.mode, context)
                needsReload = true
            }
            if (session.originalAddress != tombol.alamat) {
                session.originalAddress = tombol.alamat
                session.webView.loadUrl(tombol.alamat)
            } else if (needsReload) {
                session.webView.reload()
            }
        }
    }
}
