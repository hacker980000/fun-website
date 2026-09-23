package com.socialaiassistant.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import com.socialaiassistant.keyboard.backend.BackendConfig
import com.socialaiassistant.keyboard.backend.WebNavigationPolicy

/** Security defaults shared by the managed-auth and account portal WebViews. */
object SecureWebViewSupport {
    @SuppressLint("SetJavaScriptEnabled")
    @Suppress("DEPRECATION")
    fun configure(webView: WebView) {
        CookieManager.getInstance().apply {
            setAcceptCookie(true) // first-party hosted session only
            setAcceptThirdPartyCookies(webView, false)
        }
        webView.settings.apply {
            javaScriptEnabled = true // hosted v35.3.1 auth/account UI requires JS
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            safeBrowsingEnabled = true
            setGeolocationEnabled(false)
            mediaPlaybackRequiresUserGesture = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            userAgentString = "$userAgentString SocialAIKeyboard/${BackendConfig.APP_VERSION}"
        }
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    }

    /**
     * Remove browser-only authentication state. This is intentionally stronger
     * than clearing the API bearer session because WebView cookies/localStorage
     * are a separate credential surface.
     */
    fun clearHostedSession(onComplete: (() -> Unit)? = null) {
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            onComplete?.invoke()
        }
    }

    fun openExternal(context: Context, rawUrl: String): Boolean {
        if (WebNavigationPolicy.classify(rawUrl, allowAuthCallback = false) != WebNavigationPolicy.Decision.OPEN_EXTERNAL) {
            return false
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun destroy(webView: WebView) {
        runCatching { webView.stopLoading() }
        runCatching { webView.clearHistory() }
        runCatching { webView.clearCache(true) }
        runCatching { webView.clearFormData() }
        runCatching { webView.removeAllViews() }
        runCatching { webView.destroy() }
    }
}
