package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.webkit.HttpAuthHandler
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.browser.DownloadHandler
import com.example.browser.KioskWebChromeClient
import com.example.browser.KioskWebViewClient
import com.example.browser.WebViewSessionManager
import com.example.data.model.TabEntity
import com.example.data.model.Tombol
import com.example.ui.components.BrowserTabBar
import com.example.ui.components.BrowserTopBar
import com.example.ui.theme.rememberBrowserThemeColors

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    tombol: Tombol,
    tabs: List<TabEntity>,
    onAddNewTab: (String) -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onUpdateTabTitleAndUrl: (tabId: String, title: String, url: String) -> Unit,
    onBackToHome: () -> Unit,
    onUpdateLastUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val browserColors = rememberBrowserThemeColors(tombol.warna)
    var sessionVersion by remember { mutableIntStateOf(0) }

    // Active tab in this browser
    val activeTab = remember(tabs) {
        tabs.find { it.isActive } ?: tabs.firstOrNull()
    }

    if (activeTab == null) {
        // While initial tab is created
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(browserColors.topBarBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = browserColors.accent)
        }
        return
    }

    // Retrieve or create persistent background session for this (siteId, tabId)
    val session = remember(tombol.id, activeTab.tabId, sessionVersion) {
        WebViewSessionManager.getOrCreateTabSession(
            context = context,
            siteId = tombol.id,
            tabId = activeTab.tabId,
            initialUrl = activeTab.url.ifBlank { tombol.alamat },
            mode = if (activeTab.isDesktopMode) "Desktop" else tombol.mode
        )
    }

    var currentProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Active tab navigation states for Undo/Redo button status
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isAddTabOpen by remember { mutableStateOf(false) }

    val updateNavigationState = {
        val activeSession = WebViewSessionManager.getSession(tombol.id, activeTab.tabId) ?: session
        canGoBack = activeSession.webView.canGoBack()
        canGoForward = activeSession.webView.canGoForward()
    }

    LaunchedEffect(activeTab.tabId, sessionVersion) {
        updateNavigationState()
    }

    // Fullscreen video
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Popup window from window.open (OAuth login, payments, verification)
    var popupWebView by remember { mutableStateOf<WebView?>(null) }

    // File upload callback
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    // Hardware permission requests
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingGeoCallback by remember {
        mutableStateOf<Pair<String, android.webkit.GeolocationPermissions.Callback>?>(null)
    }

    // Dialog state for JS Alert, Confirm, Prompt
    var jsAlertData by remember { mutableStateOf<Pair<String, JsResult>?>(null) }
    var jsConfirmData by remember { mutableStateOf<Pair<String, JsResult>?>(null) }
    var jsPromptData by remember { mutableStateOf<Triple<String, String, JsPromptResult>?>(null) }

    // HTTP Auth Dialog state
    var httpAuthData by remember { mutableStateOf<Triple<String, String, HttpAuthHandler>?>(null) }

    // SSL Error Dialog state
    var sslErrorData by remember { mutableStateOf<Pair<SslErrorHandler, SslError>?>(null) }

    // Download handler
    val downloadHandler = remember { DownloadHandler(context) }

    DisposableEffect(activeTab.tabId) {
        downloadHandler.register()
        onDispose {
            downloadHandler.unregister()
            // Detach webView from its parent container so it stays alive in memory without being destroyed
            (session.webView.parent as? ViewGroup)?.removeView(session.webView)
        }
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intentData = result.data
            val results = when {
                intentData?.clipData != null -> {
                    val count = intentData.clipData!!.itemCount
                    Array(count) { i -> intentData.clipData!!.getItemAt(i).uri }
                }
                intentData?.data != null -> arrayOf(intentData.data!!)
                else -> null
            }
            filePathCallback?.onReceiveValue(results)
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    // Permission launcher for Camera & Audio
    val cameraAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val grantedList = mutableListOf<String>()
        val request = pendingPermissionRequest
        if (request != null) {
            for (resource in request.resources) {
                if (resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE &&
                    permissions[Manifest.permission.CAMERA] == true
                ) {
                    grantedList.add(resource)
                } else if (resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE &&
                    permissions[Manifest.permission.RECORD_AUDIO] == true
                ) {
                    grantedList.add(resource)
                }
            }
            if (grantedList.isNotEmpty()) {
                request.grant(grantedList.toTypedArray())
            } else {
                request.deny()
            }
        }
        pendingPermissionRequest = null
    }

    // Geolocation permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        pendingGeoCallback?.let { (origin, callback) ->
            callback.invoke(origin, isGranted, false)
        }
        pendingGeoCallback = null
    }

    // Configure clients on the active tab session's webView
    val isDesktop = activeTab.isDesktopMode || tombol.mode.equals("Desktop", ignoreCase = true)

    session.webView.apply {
        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            downloadHandler.startDownload(url, userAgent, contentDisposition, mimetype)
        }

        webChromeClient = KioskWebChromeClient(
            tombolId = tombol.id,
            onProgressChanged = { progress ->
                currentProgress = progress
                isLoading = progress < 100
            },
            onReceivedTitleCallback = { newTitle ->
                if (newTitle.isNotBlank()) {
                    onUpdateTabTitleAndUrl(activeTab.tabId, newTitle, session.webView.url ?: activeTab.url)
                }
            },
            onShowFileChooser = { callback, params ->
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                try {
                    val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                    filePickerLauncher.launch(intent)
                    true
                } catch (e: Exception) {
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = null
                    false
                }
            },
            onPermissionRequest = { request ->
                val needed = mutableListOf<String>()
                for (res in request.resources) {
                    if (res == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            needed.add(Manifest.permission.CAMERA)
                        }
                    }
                    if (res == PermissionRequest.RESOURCE_AUDIO_CAPTURE) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            needed.add(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
                if (needed.isEmpty()) {
                    request.grant(request.resources)
                } else {
                    pendingPermissionRequest = request
                    cameraAudioPermissionLauncher.launch(needed.toTypedArray())
                }
            },
            onGeolocationPrompt = { origin, callback ->
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false)
                } else {
                    pendingGeoCallback = Pair(origin, callback)
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            onShowCustomView = { view, callback ->
                customVideoView = view
                customViewCallback = callback
            },
            onHideCustomView = {
                customVideoView = null
                customViewCallback = null
            },
            onCreatePopup = { newPopupWebView ->
                popupWebView = newPopupWebView
            },
            onClosePopup = {
                popupWebView = null
            },
            onJsAlertRequest = { msg, res ->
                jsAlertData = Pair(msg, res)
            },
            onJsConfirmRequest = { msg, res ->
                jsConfirmData = Pair(msg, res)
            },
            onJsPromptRequest = { msg, def, res ->
                jsPromptData = Triple(msg, def, res)
            }
        )

        webViewClient = KioskWebViewClient(
            context = context,
            isDesktop = isDesktop,
            onPageStartedCallback = { url ->
                hasError = false
                onUpdateLastUrl(url)
                updateNavigationState()
            },
            onPageFinishedCallback = { url ->
                val currentTitle = title?.takeIf { it.isNotBlank() } ?: activeTab.title
                onUpdateTabTitleAndUrl(activeTab.tabId, currentTitle, url)
                onUpdateLastUrl(url)
                updateNavigationState()
            },
            onErrorCallback = { desc ->
                hasError = true
                errorMessage = desc
                updateNavigationState()
            },
            onHttpAuthRequest = { host, realm, handler ->
                httpAuthData = Triple(host, realm, handler)
            },
            onSslErrorRequest = { handler, error ->
                sslErrorData = Pair(handler, error)
            },
            onRenderProcessGoneCallback = {
                WebViewSessionManager.destroyTabSession(context, tombol.id, activeTab.tabId)
                sessionVersion++
                updateNavigationState()
            }
        )
    }

    // Android Back button handling (only applies to active tab)
    BackHandler {
        if (popupWebView != null) {
            popupWebView = null
        } else if (customVideoView != null) {
            customViewCallback?.onCustomViewHidden()
            customVideoView = null
            customViewCallback = null
        } else {
            val activeSession = WebViewSessionManager.getSession(tombol.id, activeTab.tabId) ?: session
            if (activeSession.webView.canGoBack()) {
                activeSession.webView.goBack()
                activeSession.webView.post { updateNavigationState() }
            } else {
                onBackToHome()
            }
        }
    }

    // Layout utama: Kolom dengan BrowserTopBar di atas, BrowserTabBar di bawahnya, WebView di tengah
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(browserColors.topBarBackground)
    ) {
        // 1. BrowserTopBar di bagian paling atas (hanya beroperasi pada tab aktif)
        BrowserTopBar(
            browserColors = browserColors,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            isAddTabOpen = isAddTabOpen,
            onToggleAddTab = { isAddTabOpen = !isAddTabOpen },
            onUndo = {
                val activeSession = WebViewSessionManager.getSession(tombol.id, activeTab.tabId) ?: session
                if (activeSession.webView.canGoBack()) {
                    hasError = false
                    activeSession.webView.goBack()
                    activeSession.webView.post { updateNavigationState() }
                } else {
                    Toast.makeText(context, "Tidak ada riwayat sebelumnya", Toast.LENGTH_SHORT).show()
                }
            },
            onRedo = {
                val activeSession = WebViewSessionManager.getSession(tombol.id, activeTab.tabId) ?: session
                if (activeSession.webView.canGoForward()) {
                    hasError = false
                    activeSession.webView.goForward()
                    activeSession.webView.post { updateNavigationState() }
                } else {
                    Toast.makeText(context, "Tidak ada riwayat berikutnya", Toast.LENGTH_SHORT).show()
                }
            },
            onReload = {
                val activeSession = WebViewSessionManager.getSession(tombol.id, activeTab.tabId) ?: session
                hasError = false
                activeSession.webView.reload()
                activeSession.webView.post { updateNavigationState() }
            },
            onGoHome = {
                onBackToHome()
            },
            onGoToHomepage = {
                val activeSession = WebViewSessionManager.getSession(tombol.id, activeTab.tabId) ?: session
                hasError = false
                activeSession.webView.loadUrl(tombol.alamat)
            },
            onOpenUrl = { newUrl ->
                val activeSession = WebViewSessionManager.getSession(tombol.id, activeTab.tabId) ?: session
                hasError = false
                activeSession.webView.loadUrl(newUrl)
            },
            onAddNewTab = { input ->
                onAddNewTab(input)
            }
        )

        // 2. Bar Tab yang menampilkan tab-tab di dalam browser ini
        BrowserTabBar(
            tabs = tabs,
            activeTabId = activeTab.tabId,
            browserColors = browserColors,
            onSelectTab = { selectedId ->
                if (selectedId != activeTab.tabId) {
                    onSelectTab(selectedId)
                }
            },
            onCloseTab = { closeId ->
                onCloseTab(closeId)
            },
            onAddNewTabClick = {
                isAddTabOpen = true
            }
        )

        // 3. Area WebView di tengah
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            // Main Persistent WebView for Active Tab
            AndroidView(
                factory = { _ ->
                    (session.webView.parent as? ViewGroup)?.removeView(session.webView)
                    session.webView
                },
                update = { webView ->
                    (webView.parent as? ViewGroup)?.let { parent ->
                        if (parent != webView.parent) {
                            parent.removeView(webView)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Custom Fullscreen Video Container
            customVideoView?.let { videoView ->
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            addView(videoView)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Popup Window from window.open
            popupWebView?.let { popup ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(browserColors.topBarBackground)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { popupWebView = null },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Tutup Jendela",
                                        tint = Color.White
                                    )
                                }
                            }
                            AndroidView(
                                factory = { _ ->
                                    (popup.parent as? ViewGroup)?.removeView(popup)
                                    popup
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Progress Bar saat loading
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { currentProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter),
                    color = browserColors.accent,
                    trackColor = Color.Transparent
                )
            }

            // Error Screen jika terjadi kendala jaringan
            if (hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Gagal Memuat Halaman",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage.ifEmpty { "Periksa koneksi internet Anda lalu coba lagi." },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = onBackToHome,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Beranda")
                                }
                                Button(
                                    onClick = {
                                        hasError = false
                                        session.webView.reload()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Bar Kosong di Bagian Bawah
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(browserColors.bottomBarBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "By : Guber Smart",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }

    // JS Alert Dialog
    jsAlertData?.let { (message, result) ->
        AlertDialog(
            onDismissRequest = {
                result.cancel()
                jsAlertData = null
            },
            title = { Text(text = "Pemberitahuan Situs", fontWeight = FontWeight.Bold) },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(onClick = {
                    result.confirm()
                    jsAlertData = null
                }) {
                    Text("OK")
                }
            }
        )
    }

    // JS Confirm Dialog
    jsConfirmData?.let { (message, result) ->
        AlertDialog(
            onDismissRequest = {
                result.cancel()
                jsConfirmData = null
            },
            title = { Text(text = "Konfirmasi Situs", fontWeight = FontWeight.Bold) },
            text = { Text(text = message) },
            confirmButton = {
                Button(onClick = {
                    result.confirm()
                    jsConfirmData = null
                }) {
                    Text("Ya")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    result.cancel()
                    jsConfirmData = null
                }) {
                    Text("Batal")
                }
            }
        )
    }

    // JS Prompt Dialog
    jsPromptData?.let { (message, defaultValue, result) ->
        var promptInput by remember { mutableStateOf(defaultValue) }
        AlertDialog(
            onDismissRequest = {
                result.cancel()
                jsPromptData = null
            },
            title = { Text(text = "Masukan Situs", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = message)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    result.confirm(promptInput)
                    jsPromptData = null
                }) {
                    Text("Kirim")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    result.cancel()
                    jsPromptData = null
                }) {
                    Text("Batal")
                }
            }
        )
    }

    // HTTP Auth Dialog
    httpAuthData?.let { (host, realm, handler) ->
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                handler.cancel()
                httpAuthData = null
            },
            title = { Text(text = "Autentikasi Diperlukan", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = "Host: $host\nRealm: $realm", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Nama Pengguna") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Kata Sandi") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    handler.proceed(username, password)
                    httpAuthData = null
                }) {
                    Text("Masuk")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    handler.cancel()
                    httpAuthData = null
                }) {
                    Text("Batal")
                }
            }
        )
    }

    // SSL Error Dialog
    sslErrorData?.let { (handler, error) ->
        val sslMessage = when (error.primaryError) {
            SslError.SSL_UNTRUSTED -> "Sertifikat keamanan situs tidak tepercaya."
            SslError.SSL_EXPIRED -> "Sertifikat keamanan situs telah kedaluwarsa."
            SslError.SSL_IDMISMATCH -> "Nama host tidak sesuai dengan sertifikat."
            SslError.SSL_NOTYETVALID -> "Sertifikat keamanan belum valid."
            else -> "Terjadi kesalahan sertifikat keamanan SSL (${error.primaryError})."
        }

        AlertDialog(
            onDismissRequest = {
                handler.cancel()
                sslErrorData = null
            },
            title = { Text(text = "Peringatan Keamanan SSL", fontWeight = FontWeight.Bold) },
            text = { Text(text = "$sslMessage\n\nApakah Anda yakin ingin tetap melanjutkan?") },
            confirmButton = {
                Button(
                    onClick = {
                        handler.proceed()
                        sslErrorData = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Tetap Lanjutkan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    handler.cancel()
                    sslErrorData = null
                }) {
                    Text("Kembali")
                }
            }
        )
    }
}
