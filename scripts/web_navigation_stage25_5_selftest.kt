import com.socialaiassistant.keyboard.backend.BackendConfig
import com.socialaiassistant.keyboard.backend.WebNavigationPolicy

private var checks = 0
private fun expect(name: String, condition: Boolean) {
    check(condition) { "FAIL: $name" }
    checks++
}

fun main() {
    val backendHost = java.net.URI(BackendConfig.API_BASE).host
    val callbackHost = java.net.URI(BackendConfig.ANDROID_AUTH_CALLBACK).host

    expect("trusted account origin allowed", WebNavigationPolicy.classify(
        "${BackendConfig.API_BASE}/account/index.html?tab=payments", false
    ) == WebNavigationPolicy.Decision.ALLOW_TRUSTED)
    expect("trusted auth origin allowed", WebNavigationPolicy.classify(
        "${BackendConfig.API_BASE}/account/auth.html", true
    ) == WebNavigationPolicy.Decision.ALLOW_TRUSTED)
    expect("lookalike host externalized", WebNavigationPolicy.classify(
        "https://$backendHost.evil.example/account/", false
    ) == WebNavigationPolicy.Decision.OPEN_EXTERNAL)
    expect("userinfo confusion blocked", WebNavigationPolicy.classify(
        "https://$backendHost@evil.example/account/", false
    ) == WebNavigationPolicy.Decision.BLOCK)
    expect("backend alternate port blocked", WebNavigationPolicy.classify(
        "https://$backendHost:444/account/", false
    ) == WebNavigationPolicy.Decision.BLOCK)
    expect("callback accepted only in auth", WebNavigationPolicy.classify(
        "${BackendConfig.ANDROID_AUTH_CALLBACK}?state=s&code=c", true
    ) == WebNavigationPolicy.Decision.AUTH_CALLBACK)
    expect("callback blocked in portal", WebNavigationPolicy.classify(
        "${BackendConfig.ANDROID_AUTH_CALLBACK}?state=s&code=c", false
    ) == WebNavigationPolicy.Decision.BLOCK)
    expect("callback extra path rejected", !WebNavigationPolicy.isExactAuthCallback(
        "${BackendConfig.ANDROID_AUTH_CALLBACK}/extra?state=s&code=c"
    ))
    expect("callback fragment rejected", !WebNavigationPolicy.isExactAuthCallback(
        "${BackendConfig.ANDROID_AUTH_CALLBACK}#fragment"
    ))
    expect("callback alternate port not exact", !WebNavigationPolicy.isExactAuthCallback(
        "https://$callbackHost:444/auth?state=s&code=c"
    ))
    expect("external https handed off", WebNavigationPolicy.classify(
        "https://payments.example/checkout", false
    ) == WebNavigationPolicy.Decision.OPEN_EXTERNAL)
    expect("mailto handed off", WebNavigationPolicy.classify(
        "mailto:support@example.com", false
    ) == WebNavigationPolicy.Decision.OPEN_EXTERNAL)
    expect("tel handed off", WebNavigationPolicy.classify(
        "tel:+8801000000000", false
    ) == WebNavigationPolicy.Decision.OPEN_EXTERNAL)
    listOf(
        "http://example.com/",
        "javascript:alert(1)",
        "data:text/html,pwn",
        "file:///sdcard/token.txt",
        "content://com.example/private",
        "intent://pay#Intent;scheme=https;end"
    ).forEach { url ->
        expect("blocked scheme: $url", WebNavigationPolicy.classify(url, false) == WebNavigationPolicy.Decision.BLOCK)
    }
    expect("backslash URL rejected", WebNavigationPolicy.classify(
        "https://$backendHost\\@evil.example/account/", false
    ) == WebNavigationPolicy.Decision.BLOCK)
    expect("control character URL rejected", WebNavigationPolicy.classify(
        "https://$backendHost/account/\nhttps://evil.example", false
    ) == WebNavigationPolicy.Decision.BLOCK)

    println("Stage 25.5 Web navigation policy self-test PASS ($checks/$checks)")
}
