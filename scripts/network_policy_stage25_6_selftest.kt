package com.socialaiassistant.keyboard.network

private fun checkCase(name: String, expected: Boolean, actual: Boolean) {
    check(expected == actual) { "$name expected=$expected actual=$actual" }
}

fun main() {
    val backend = NetworkEndpointPolicy.allowedHosts.first { it.endsWith("workers.dev") }
    checkCase("backend https", true, NetworkEndpointPolicy.allows("https", backend, 443))
    checkCase("openrouter https", true, NetworkEndpointPolicy.allows("HTTPS", "OPENROUTER.AI", 443))
    checkCase("cleartext blocked", false, NetworkEndpointPolicy.allows("http", backend, 80))
    checkCase("alt port blocked", false, NetworkEndpointPolicy.allows("https", backend, 8443))
    checkCase("subdomain blocked", false, NetworkEndpointPolicy.allows("https", "evil.$backend", 443))
    checkCase("suffix confusion blocked", false, NetworkEndpointPolicy.allows("https", "$backend.evil.example", 443))
    checkCase("userinfo blocked", false, NetworkEndpointPolicy.allows("https", backend, 443, username = "token"))
    checkCase("password userinfo blocked", false, NetworkEndpointPolicy.allows("https", backend, 443, password = "secret"))
    checkCase("unknown host blocked", false, NetworkEndpointPolicy.allows("https", "example.com", 443))
    println("NetworkEndpointPolicy Stage 25.6 self-test PASS (9/9)")
}
