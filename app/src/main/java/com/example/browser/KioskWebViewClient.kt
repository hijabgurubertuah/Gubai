package com.example.browser

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.util.Log
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class KioskWebViewClient(
    private val context: Context,
    private val isDesktop: Boolean,
    private val onPageStartedCallback: (String) -> Unit,
    private val onPageFinishedCallback: (String) -> Unit,
    private val onErrorCallback: (String) -> Unit,
    private val onHttpAuthRequest: (host: String, realm: String, handler: HttpAuthHandler) -> Unit,
    private val onSslErrorRequest: (handler: SslErrorHandler, error: SslError) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        return handleSpecialUrl(url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false
        return handleSpecialUrl(url)
    }

    private fun handleSpecialUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase() ?: return false

        // Standard web schemes load in WebView
        if (scheme == "http" || scheme == "https" || scheme == "file" || scheme == "about" || scheme == "javascript") {
            return false
        }

        // External protocols: tel, mailto, sms, whatsapp, market, intent
        return try {
            val intent = if (scheme == "intent") {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, uri)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w("KioskWebViewClient", "Could not open external app for $url: ${e.message}")
            true
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (view != null && isDesktop) {
            WebViewSessionManager.injectDesktopIdentity(view)
        }
        url?.let { onPageStartedCallback(it) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (view != null && isDesktop) {
            WebViewSessionManager.injectDesktopIdentity(view)
        }
        url?.let { onPageFinishedCallback(it) }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val errorDesc = error?.description?.toString() ?: "Gagal memuat halaman"
            Log.e("KioskWebViewClient", "Main frame load error: $errorDesc")
            onErrorCallback(errorDesc)
        }
    }

    override fun onReceivedHttpAuthRequest(
        view: WebView?,
        handler: HttpAuthHandler?,
        host: String?,
        realm: String?
    ) {
        if (handler != null) {
            onHttpAuthRequest(host ?: "", realm ?: "", handler)
        } else {
            super.onReceivedHttpAuthRequest(view, handler, host, realm)
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        if (handler != null && error != null) {
            onSslErrorRequest(handler, error)
        } else {
            handler?.cancel()
        }
    }
}
