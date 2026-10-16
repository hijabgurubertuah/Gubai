package com.example.browser

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

object WebViewProfileManager {
    private const val TAG = "WebViewProfileManager"
    private var hasNotifiedUnsupported = false

    fun isMultiProfileSupported(): Boolean {
        return try {
            WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
        } catch (e: Throwable) {
            Log.w(TAG, "Error checking multi-profile support: ${e.message}")
            false
        }
    }

    fun shouldShowUnsupportedNotice(): Boolean {
        return if (!isMultiProfileSupported() && !hasNotifiedUnsupported) {
            hasNotifiedUnsupported = true
            true
        } else {
            false
        }
    }

    fun getProfileNameForTombol(tombolId: String): String {
        val sanitized = tombolId.replace("-", "_")
        return "gubai_profile_$sanitized"
    }

    fun attachProfileToWebView(webView: WebView, tombolId: String) {
        if (!isMultiProfileSupported()) {
            return
        }
        try {
            val profileName = getProfileNameForTombol(tombolId)
            val profileStore = ProfileStore.getInstance()
            profileStore.getOrCreateProfile(profileName)
            WebViewCompat.setProfile(webView, profileName)
            Log.d(TAG, "Attached profile $profileName to WebView")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to attach profile to WebView: ${e.message}")
        }
    }

    fun clearProfileData(context: Context, tombolId: String) {
        if (isMultiProfileSupported()) {
            try {
                val profileName = getProfileNameForTombol(tombolId)
                val profileStore = ProfileStore.getInstance()
                val profile: Profile = profileStore.getProfile(profileName) ?: profileStore.getOrCreateProfile(profileName)
                
                profile.cookieManager.removeAllCookies(null)
                profile.cookieManager.flush()
                profile.webStorage.deleteAllData()
                profile.geolocationPermissions.clearAll()
                Log.d(TAG, "Cleared data for profile $profileName")
                return
            } catch (e: Throwable) {
                Log.e(TAG, "Error clearing profile data: ${e.message}")
            }
        }

        // Fallback for devices without multi-profile
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            WebStorage.getInstance().deleteAllData()
        } catch (e: Throwable) {
            Log.e(TAG, "Fallback clear cookies failed: ${e.message}")
        }
    }

    fun deleteProfile(context: Context, tombolId: String) {
        clearProfileData(context, tombolId)
        if (isMultiProfileSupported()) {
            try {
                val profileName = getProfileNameForTombol(tombolId)
                val profileStore = ProfileStore.getInstance()
                profileStore.deleteProfile(profileName)
                Log.d(TAG, "Deleted profile $profileName")
            } catch (e: Throwable) {
                Log.e(TAG, "Error deleting profile: ${e.message}")
            }
        }
    }
}
