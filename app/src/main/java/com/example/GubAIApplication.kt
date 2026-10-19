package com.example

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.browser.WebViewSessionManager

class GubAIApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Observe application-wide foreground/background transitions
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // Application returned to foreground: resume active webviews
                WebViewSessionManager.onResumeAllSessions()
            }

            override fun onStop(owner: LifecycleOwner) {
                // Application minimized or moved to background:
                // Flush cookies and save state to disk.
                // Do NOT call pauseTimers() or onPause() so audio, video, and web processes continue.
                WebViewSessionManager.onAppBackgrounded(this@GubAIApplication)
            }
        })
    }

    companion object {
        lateinit var instance: GubAIApplication
            private set
    }
}
