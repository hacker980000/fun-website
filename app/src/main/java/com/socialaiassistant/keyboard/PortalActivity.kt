package com.socialaiassistant.keyboard

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.socialaiassistant.keyboard.backend.BackendConfig
import com.socialaiassistant.keyboard.backend.WebNavigationPolicy

class PortalActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        webView = WebView(this)
        setContentView(webView)
        SecureWebViewSupport.configure(webView)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (request != null && !request.isForMainFrame) return false
                return handleNavigation(request?.url?.toString())
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean =
                handleNavigation(url)
        }
        webView.loadUrl(BackendConfig.ACCOUNT_PAGE)
    }

    private fun handleNavigation(rawUrl: String?): Boolean = when (
        WebNavigationPolicy.classify(rawUrl, allowAuthCallback = false)
    ) {
        WebNavigationPolicy.Decision.ALLOW_TRUSTED -> false
        WebNavigationPolicy.Decision.OPEN_EXTERNAL -> {
            val opened = rawUrl?.let { SecureWebViewSupport.openExternal(this, it) } == true
            if (!opened) Toast.makeText(this, "External link could not be opened securely.", Toast.LENGTH_SHORT).show()
            true
        }
        WebNavigationPolicy.Decision.AUTH_CALLBACK,
        WebNavigationPolicy.Decision.BLOCK -> {
            Toast.makeText(this, "Blocked an unsafe account-portal navigation.", Toast.LENGTH_SHORT).show()
            true
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) SecureWebViewSupport.destroy(webView)
        super.onDestroy()
    }
}
