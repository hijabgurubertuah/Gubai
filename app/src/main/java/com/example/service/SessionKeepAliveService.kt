package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.browser.WebViewSessionManager

class SessionKeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALL_SESSIONS) {
            WebViewSessionManager.closeAllSessions()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val activeCount = intent?.getIntExtra(EXTRA_SESSION_COUNT, 0) ?: 0
        if (activeCount <= 0) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = buildNotification(activeCount)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sesi GubAI Latar Belakang",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menjaga sesi browser tetap aktif di latar belakang saat aplikasi di-minimize"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(activeCount: Int) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("GubAI aktif")
            .setContentText("$activeCount sesi berjalan di latar belakang")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Akhiri semua sesi",
                PendingIntent.getService(
                    this,
                    1,
                    Intent(this, SessionKeepAliveService::class.java).apply {
                        action = ACTION_STOP_ALL_SESSIONS
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    companion object {
        const val CHANNEL_ID = "gubai_keep_alive_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_ALL_SESSIONS = "com.example.ACTION_STOP_ALL_SESSIONS"
        const val EXTRA_SESSION_COUNT = "extra_session_count"

        fun startOrUpdate(context: Context, sessionCount: Int) {
            try {
                val intent = Intent(context, SessionKeepAliveService::class.java).apply {
                    putExtra(EXTRA_SESSION_COUNT, sessionCount)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // If app is strictly restricted from background start, fail gracefully
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, SessionKeepAliveService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
