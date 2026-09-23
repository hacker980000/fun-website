package com.socialaiassistant.keyboard.backend

object BackendConfig {
    const val API_BASE = "https://super-boat-4aba.madigitalstudio2018.workers.dev"
    const val PRODUCT_CODE = "KEYBOARD"
    const val USER_PORTAL_BASE = "$API_BASE/account/"
    const val AUTH_PAGE = "${USER_PORTAL_BASE}auth.html"
    const val ACCOUNT_PAGE = "${USER_PORTAL_BASE}index.html"
    const val RELEASE_CHANNEL = "CHROMIUM"
    const val APP_VERSION = "35.3.1"

    // Existing v35.3.1 hosted auth only accepts browser-identity HTTPS callbacks.
    // Android AuthActivity hosts that flow in a WebView and intercepts this callback
    // before any network request leaves the app.
    const val ANDROID_AUTH_CALLBACK = "https://aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.chromiumapp.org/auth"
}
