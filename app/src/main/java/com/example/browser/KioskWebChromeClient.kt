package com.example.browser

import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class KioskWebChromeClient(
    private val tombolId: String,
    private val onProgressChanged: (Int) -> Unit,
    private val onShowFileChooser: (ValueCallback<Array<Uri>>?, FileChooserParams?) -> Boolean,
    private val onPermissionRequest: (PermissionRequest) -> Unit,
    private val onGeolocationPrompt: (String, GeolocationPermissions.Callback) -> Unit,
    private val onShowCustomView: (View, CustomViewCallback) -> Unit,
    private val onHideCustomView: () -> Unit,
    private val onCreatePopup: (WebView) -> Unit,
    private val onClosePopup: () -> Unit,
    private val onJsAlertRequest: (message: String, result: JsResult) -> Unit,
    private val onJsConfirmRequest: (message: String, result: JsResult) -> Unit,
    private val onJsPromptRequest: (message: String, defaultValue: String, result: JsPromptResult) -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return onShowFileChooser(filePathCallback, fileChooserParams)
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        onPermissionRequest(request)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback
    ) {
        onGeolocationPrompt(origin, callback)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        onHideCustomView()
    }

    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)
        onClosePopup()
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        if (result != null && message != null) {
            onJsAlertRequest(message, result)
            return true
        }
        return super.onJsAlert(view, url, message, result)
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        if (result != null && message != null) {
            onJsConfirmRequest(message, result)
            return true
        }
        return super.onJsConfirm(view, url, message, result)
    }

    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        if (result != null && message != null) {
            onJsPromptRequest(message, defaultValue ?: "", result)
            return true
        }
        return super.onJsPrompt(view, url, message, defaultValue, result)
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        val ctx = view?.context ?: return false

        // Create popup WebView with same profile to support window.open and opener
        val popupWebView = WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Attach profile of this button
            WebViewProfileManager.attachProfileToWebView(this, tombolId)

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
                userAgentString = view.settings.userAgentString
            }

            try {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            } catch (e: Exception) {
                // ignore
            }

            webChromeClient = object : WebChromeClient() {
                override fun onCloseWindow(window: WebView?) {
                    onClosePopup()
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }
            }
        }

        val transport = resultMsg?.obj as? WebView.WebViewTransport
        if (transport != null) {
            transport.webView = popupWebView
            resultMsg.sendToTarget()
            onCreatePopup(popupWebView)
            return true
        }
        return false
    }
}
