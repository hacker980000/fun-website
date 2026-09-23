package com.socialaiassistant.keyboard.backend

object BackendErrorMessages {
    fun userMessage(error: BackendException): String = when (error.code) {
        "AUTH_REQUIRED", "SESSION_EXPIRED" -> "AI ব্যবহার করতে আগে Login করুন।"
        "ACCOUNT_BLOCKED" -> "এই account টি blocked আছে। Support-এর সাথে যোগাযোগ করুন।"
        "DEVICE_MISMATCH" -> "এই account অন্য একটি device-এ active আছে। Device Reset Request ব্যবহার করুন।"
        "DEVICE_IN_USE" -> "এই device-এ অন্য account active থাকলেও v35.3.1-এ logout/login করে একাধিক account ব্যবহার করা যায়; আবার Login করুন।"
        "DEVICE_TRANSFER_COOLDOWN" -> "এই account নতুন device-এ নেওয়ার পর 24 ঘণ্টার self-service cooldown চলছে।"
        "DEVICE_RESET_PENDING" -> "Device Reset Request Admin approval-এর অপেক্ষায় আছে।"
        "DEVICE_PROOF_REQUIRED", "DEVICE_PROOF_INVALID", "DEVICE_PROOF_EXPIRED" -> "নিরাপদ device verification ব্যর্থ হয়েছে। Logout করে আবার Login করুন।"
        "SUBSCRIPTION_INACTIVE" -> "AI ব্যবহার করতে ১৯৯ টাকার subscription payment approve হতে হবে।"
        "SUBSCRIPTION_EXPIRED" -> "Subscription মেয়াদ শেষ। Renewal approve হলে AI আবার চালু হবে।"
        "PRODUCT_ACCESS_REQUIRED" -> "এই account-এ Social AI Keyboard access সক্রিয় নেই। Admin থেকে Keyboard product access চালু করুন।"
        "PRODUCT_ACCESS_EXPIRED" -> "Social AI Keyboard access-এর মেয়াদ শেষ। Admin থেকে Keyboard access renew করুন।"
        "DAILY_QUOTA_EXCEEDED" -> "আজকের ২০০ AI request limit শেষ হয়েছে। Dhaka time রাত ১২টার পর আবার ব্যবহার করুন।"
        "CYCLE_QUOTA_EXCEEDED" -> "এই subscription cycle-এর ৪,০০০ AI request limit শেষ হয়েছে।"
        "UPDATE_REQUIRED" -> "Social AI Keyboard update প্রয়োজন।"
        "PASSWORD_CHANGE_REQUIRED" -> "Admin reset-এর পর নতুন password সেট না করা পর্যন্ত AI ব্যবহার করা যাবে না।"
        "RATE_LIMITED" -> "অনেক দ্রুত request হয়েছে। কিছুক্ষণ পরে আবার চেষ্টা করুন।"
        "WAITING_FOR_RECIPIENT" -> "নতুন Flirty Reply তৈরির আগে প্রাপকের নতুন বার্তার অপেক্ষা করুন।"
        else -> error.message.ifBlank { "Managed AI request সম্পন্ন করা যায়নি।" }
    }
}
