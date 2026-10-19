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
    private val onJsPromptRequest: (message: String, defaultValue: String, result: JsPromptResult) -> Unit,
    private val onReceivedTitleCallback: ((String) -> Unit)? = null
) : WebChromeClient() {

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        if (!title.isNullOrBlank()) {
            onReceivedTitleCallback?.invoke(title)
        }
    }

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
        val transport = resultMsg?.obj as? WebView.WebViewTransport
        if (transport != null && view != null) {
            // Instead of opening a separate popup window, load the target URL directly in the main WebView
            // This ensures dropdown sub-menus, login redirects, and target="_blank" links open seamlessly in the main column.
            val dummyWebView = WebView(view.context).apply {
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString()
                        if (!url.isNullOrEmpty()) {
                            view.post {
                                view.loadUrl(url)
                                try { v?.destroy() } catch (e: Exception) {}
                            }
                        }
                        return true
                    }
                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(v: WebView?, url: String?): Boolean {
                        if (!url.isNullOrEmpty()) {
                            view.post {
                                view.loadUrl(url)
                                try { v?.destroy() } catch (e: Exception) {}
                            }
                        }
                        return true
                    }
                    override fun onRenderProcessGone(v: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                        try { v?.destroy() } catch (e: Exception) {}
                        return true
                    }
                }
            }
            transport.webView = dummyWebView
            resultMsg.sendToTarget()
            return true
        }
        return false
    }
}
