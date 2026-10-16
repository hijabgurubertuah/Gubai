package com.example.browser

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import java.io.File

class DownloadHandler(private val context: Context) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val activeDownloads = mutableMapOf<Long, String>()
    private var isReceiverRegistered = false

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L) {
                    val fileName = activeDownloads.remove(downloadId) ?: "berkas"
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = cursor.getInt(statusIndex)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            Toast.makeText(context, "Unduhan selesai: $fileName", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Unduhan gagal", Toast.LENGTH_SHORT).show()
                        }
                        cursor.close()
                    } else {
                        Toast.makeText(context, "Unduhan selesai: $fileName", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun register() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(downloadReceiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    fun unregister() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(downloadReceiver)
            } catch (e: Exception) {
                Log.w("DownloadHandler", "Failed to unregister receiver: ${e.message}")
            }
            isReceiverRegistered = false
        }
    }

    fun startDownload(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        try {
            var fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            if (fileName.isNullOrBlank() || fileName == "default.bin") {
                val lastSegment = Uri.parse(url).lastPathSegment
                if (!lastSegment.isNullOrBlank()) {
                    fileName = lastSegment
                } else {
                    fileName = "unduhan_${System.currentTimeMillis()}"
                }
            }

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                userAgent?.let { addRequestHeader("User-Agent", it) }
                try {
                    val cookies = android.webkit.CookieManager.getInstance().getCookie(url)
                    if (!cookies.isNullOrBlank()) {
                        addRequestHeader("Cookie", cookies)
                    }
                } catch (e: Exception) {
                    // ignore
                }
                addRequestHeader("Referer", url)
                setDescription("Mengunduh $fileName")
                setTitle(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = downloadManager.enqueue(request)
            activeDownloads[downloadId] = fileName
            Toast.makeText(context, "Mengunduh: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("DownloadHandler", "Error starting download: ${e.message}")
            Toast.makeText(context, "Unduhan gagal", Toast.LENGTH_SHORT).show()
        }
    }
}
