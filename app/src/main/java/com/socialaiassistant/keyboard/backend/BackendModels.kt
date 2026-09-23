package com.socialaiassistant.keyboard.backend

data class ManagedSession(
    val token: String,
    val expiresAt: String,
    val userId: String,
    val email: String,
    val mobile: String
)

data class ProductEntitlement(
    val productCode: String,
    val status: String,
    val cycleExpiresAt: String? = null
) {
    val isActive: Boolean get() = status.equals("ACTIVE", ignoreCase = true)
}

data class ManagedAccountState(
    val userId: String = "",
    val email: String = "",
    val mobile: String = "",
    val subscriptionStatus: String = "UNKNOWN",
    val subscriptionEndsAt: String? = null,
    val entitlements: List<ProductEntitlement> = emptyList(),
    val passwordResetRequired: Boolean = false,
    val dailyUsed: Int = 0,
    val dailyLimit: Int = 200,
    val cycleUsed: Int = 0,
    val cycleLimit: Int = 4000,
    val currentVersion: String = BackendConfig.APP_VERSION,
    val minimumVersion: String = "35.0.0"
) {
    val keyboardEntitlement: ProductEntitlement
        get() = entitlements.firstOrNull { it.productCode.equals(PRODUCT_KEYBOARD, ignoreCase = true) }
            ?: ProductEntitlement(PRODUCT_KEYBOARD, "INACTIVE")

    val assistantProEntitlement: ProductEntitlement
        get() = entitlements.firstOrNull { it.productCode.equals(PRODUCT_ASSISTANT_PRO, ignoreCase = true) }
            ?: ProductEntitlement(PRODUCT_ASSISTANT_PRO, "INACTIVE")

    val isActive: Boolean get() = keyboardEntitlement.isActive

    companion object {
        const val PRODUCT_KEYBOARD = "KEYBOARD"
        const val PRODUCT_ASSISTANT_PRO = "ASSISTANT_PRO"
    }
}

class BackendException(
    val code: String,
    override val message: String,
    val status: Int = 0,
    val details: String? = null
) : Exception(message)
