package com.socialaiassistant.keyboard

import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.socialaiassistant.keyboard.ai.GatewayMode
import com.socialaiassistant.keyboard.backend.BackendConfig
import com.socialaiassistant.keyboard.backend.BackendErrorMessages
import com.socialaiassistant.keyboard.backend.BackendException
import com.socialaiassistant.keyboard.backend.WebNavigationPolicy
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val state = UUID.randomUUID().toString()
    private var exchangeStarted = false
    private var ownsAuthGuard = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (!AUTH_IN_PROGRESS.compareAndSet(false, true)) {
            Toast.makeText(this, "Login already in progress.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        ownsAuthGuard = true

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

        // Login / Switch Account starts from a clean browser credential surface.
        // API bearer state is separate and is replaced only after a successful code exchange.
        SecureWebViewSupport.clearHostedSession {
            runOnUiThread {
                if (!isFinishing && !isDestroyed && ::webView.isInitialized) loadAuthPage()
            }
        }
    }

    private fun loadAuthPage() {
        val authUri = Uri.parse(BackendConfig.AUTH_PAGE).buildUpon()
            .appendQueryParameter("state", state)
            .appendQueryParameter("redirect_uri", BackendConfig.ANDROID_AUTH_CALLBACK)
            .build()
        webView.loadUrl(authUri.toString())
    }

    private fun handleNavigation(rawUrl: String?): Boolean = when (
        WebNavigationPolicy.classify(rawUrl, allowAuthCallback = true)
    ) {
        WebNavigationPolicy.Decision.ALLOW_TRUSTED -> false
        WebNavigationPolicy.Decision.AUTH_CALLBACK -> handleCallback(rawUrl?.let(Uri::parse))
        WebNavigationPolicy.Decision.OPEN_EXTERNAL -> {
            val opened = rawUrl?.let { SecureWebViewSupport.openExternal(this, it) } == true
            if (!opened) Toast.makeText(this, "External link could not be opened securely.", Toast.LENGTH_SHORT).show()
            true
        }
        WebNavigationPolicy.Decision.BLOCK -> {
            Toast.makeText(this, "Blocked an unsafe login navigation.", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun handleCallback(uri: Uri?): Boolean {
        if (uri == null || !WebNavigationPolicy.isExactAuthCallback(uri.toString())) return true
        if (exchangeStarted) return true
        if (uri.getQueryParameter("state") != state) {
            Toast.makeText(this, "Login security state mismatch.", Toast.LENGTH_LONG).show()
            SecureWebViewSupport.clearHostedSession()
            finish()
            return true
        }
        val code = uri.getQueryParameter("code").orEmpty()
        if (code.isBlank()) {
            Toast.makeText(this, "Login authorization code was not returned.", Toast.LENGTH_LONG).show()
            SecureWebViewSupport.clearHostedSession()
            finish()
            return true
        }
        exchangeStarted = true
        lifecycleScope.launch {
            val app = application as SocialAiApplication
            try {
                app.backendClient.exchangeAuth(code)
                app.settingsRepository.setGatewayMode(GatewayMode.MANAGED)
                Toast.makeText(this@AuthActivity, "Login successful. Managed AI is active.", Toast.LENGTH_LONG).show()
            } catch (error: CancellationException) {
                throw error
            } catch (error: BackendException) {
                SecureWebViewSupport.clearHostedSession()
                Toast.makeText(this@AuthActivity, BackendErrorMessages.userMessage(error), Toast.LENGTH_LONG).show()
            } catch (error: Throwable) {
                SecureWebViewSupport.clearHostedSession()
                Toast.makeText(this@AuthActivity, error.message ?: "Login failed.", Toast.LENGTH_LONG).show()
            } finally {
                finish()
            }
        }
        return true
    }

    override fun onDestroy() {
        if (::webView.isInitialized) SecureWebViewSupport.destroy(webView)
        if (ownsAuthGuard) AUTH_IN_PROGRESS.set(false)
        super.onDestroy()
    }

    companion object {
        private val AUTH_IN_PROGRESS = AtomicBoolean(false)
    }
}
