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
import com.example.data.model.Tombol
import com.example.ui.components.BrowserTopBar

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    tombol: Tombol,
    onBackToHome: () -> Unit,
    onUpdateLastUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Retrieve or create persistent background session
    val session = remember(tombol.id) {
        WebViewSessionManager.getOrCreateSession(context, tombol)
    }

    var currentProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Fullscreen video
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Popup window from window.open (OAuth login, payments, verification)
    var popupWebView by remember { mutableStateOf<WebView?>(null) }

    // File upload callback
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    // Hardware permission requests
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingGeoCallback by remember { mutableStateOf<Pair<String, android.webkit.GeolocationPermissions.Callback>?>(null) }

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

    DisposableEffect(Unit) {
        downloadHandler.register()
        onDispose {
            downloadHandler.unregister()
            // Detach webView from its parent container so it stays in memory without being destroyed
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

    // Configure clients on the session's webView
    val isDesktop = tombol.mode.equals("Desktop", ignoreCase = true)

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
            },
            onPageFinishedCallback = { url ->
                onUpdateLastUrl(url)
            },
            onErrorCallback = { desc ->
                hasError = true
                errorMessage = desc
            },
            onHttpAuthRequest = { host, realm, handler ->
                httpAuthData = Triple(host, realm, handler)
            },
            onSslErrorRequest = { handler, error ->
                sslErrorData = Pair(handler, error)
            }
        )
    }

    // Android Back button handling
    BackHandler {
        if (popupWebView != null) {
            popupWebView = null
        } else if (customVideoView != null) {
            customViewCallback?.onCustomViewHidden()
            customVideoView = null
            customViewCallback = null
        } else if (session.webView.canGoBack()) {
            session.webView.goBack()
        } else {
            onBackToHome()
        }
    }

    // Layout utama: Kolom dengan BrowserTopBar di atas, WebView di tengah, dan bar kosong di bawah
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF042F2E))
    ) {
        // 1. BrowserTopBar di bagian paling atas
        BrowserTopBar(
            onUndo = {
                if (session.webView.canGoBack()) {
                    hasError = false
                    session.webView.goBack()
                } else {
                    Toast.makeText(context, "Tidak ada riwayat sebelumnya", Toast.LENGTH_SHORT).show()
                }
            },
            onRedo = {
                if (session.webView.canGoForward()) {
                    hasError = false
                    session.webView.goForward()
                } else {
                    Toast.makeText(context, "Tidak ada riwayat berikutnya", Toast.LENGTH_SHORT).show()
                }
            },
            onReload = {
                hasError = false
                session.webView.reload()
            },
            onGoHome = {
                onBackToHome()
            },
            onGoToHomepage = {
                hasError = false
                session.webView.loadUrl(tombol.alamat)
            },
            onOpenUrl = { newUrl ->
                hasError = false
                session.webView.loadUrl(newUrl)
            }
        )

        // 2. Area WebView di tengah (diapit oleh top bar dan bottom empty bar)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            // Main Persistent WebView
            AndroidView(
                factory = { _ ->
                    (session.webView.parent as? ViewGroup)?.removeView(session.webView)
                    session.webView
                },
                update = { _ -> },
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
                            (videoView.parent as? ViewGroup)?.removeView(videoView)
                            addView(videoView)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }

            // Popup Window Floating Container (for window.open, OAuth, Payments, 2FA)
            popupWebView?.let { popupView ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Popup Header Bar with Close Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Jendela Baru",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                IconButton(
                                    onClick = {
                                        (popupView.parent as? ViewGroup)?.removeView(popupView)
                                        popupView.destroy()
                                        popupWebView = null
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Tutup",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Popup content WebView
                            AndroidView(
                                factory = { _ ->
                                    (popupView.parent as? ViewGroup)?.removeView(popupView)
                                    popupView
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Thin loading progress bar at the top
            if (isLoading && !hasError) {
                LinearProgressIndicator(
                    progress = { currentProgress / 100f },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter)
                )
            }

            // Error Screen: "Halaman tidak dapat dimuat"
            if (hasError) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(24.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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
                                contentDescription = "Galat",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(56.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Halaman tidak dapat dimuat",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = onBackToHome,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text("Beranda")
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Button(
                                    onClick = {
                                        hasError = false
                                        session.webView.reload()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Bar kosong di bagian bawah (setinggi bar navigasi bawah HP) bertuliskan By : Guber Smart dengan warna hijau gelap
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF042F2E))
        ) {
            Text(
                text = "By : Guber Smart",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    // JavaScript Alert Dialog
    jsAlertData?.let { (msg, res) ->
        AlertDialog(
            onDismissRequest = {
                res.confirm()
                jsAlertData = null
            },
            title = { Text("Pemberitahuan") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = {
                    res.confirm()
                    jsAlertData = null
                }) {
                    Text("OK")
                }
            }
        )
    }

    // JavaScript Confirm Dialog
    jsConfirmData?.let { (msg, res) ->
        AlertDialog(
            onDismissRequest = {
                res.cancel()
                jsConfirmData = null
            },
            title = { Text("Konfirmasi") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = {
                    res.confirm()
                    jsConfirmData = null
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    res.cancel()
                    jsConfirmData = null
                }) {
                    Text("Batal")
                }
            }
        )
    }

    // JavaScript Prompt Dialog
    jsPromptData?.let { (msg, defVal, res) ->
        var promptText by remember { mutableStateOf(defVal) }
        AlertDialog(
            onDismissRequest = {
                res.cancel()
                jsPromptData = null
            },
            title = { Text(msg) },
            text = {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    res.confirm(promptText)
                    jsPromptData = null
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    res.cancel()
                    jsPromptData = null
                }) {
                    Text("Batal")
                }
            }
        )
    }

    // HTTP Auth Dialog (Username & Password)
    httpAuthData?.let { (host, realm, handler) ->
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                handler.cancel()
                httpAuthData = null
            },
            title = { Text("Autentikasi Diperlukan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = if (realm.isNotBlank()) "$host ($realm)" else host, fontSize = 13.sp)
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Nama Pengguna") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Kata Sandi") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    handler.proceed(username, password)
                    httpAuthData = null
                }) {
                    Text("Login")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    handler.cancel()
                    httpAuthData = null
                }) {
                    Text("Batal")
                }
            }
        )
    }

    // SSL Certificate Error Dialog
    sslErrorData?.let { (handler, _) ->
        AlertDialog(
            onDismissRequest = {
                handler.cancel()
                sslErrorData = null
            },
            title = { Text("Peringatan Sertifikat Keamanan") },
            text = { Text("Sertifikat keamanan situs ini tidak valid. Tetap melanjutkan berisiko terhadap privasi dan keamanan Anda.") },
            confirmButton = {
                Button(
                    onClick = {
                        handler.cancel()
                        sslErrorData = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Kembali")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    handler.proceed()
                    sslErrorData = null
                }) {
                    Text("Lanjutkan", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}
