package com.example.studywise.ui.navigation

import android.content.Intent
import android.net.Uri

data class EmailVerificationParams(
    val userId: String,
    val secret: String
)

fun Intent?.parseEmailVerificationParams(): EmailVerificationParams? {
    val data: Uri = this?.data ?: return null
    if (data.scheme != "http" || data.host != "localhost" || data.path != "/verify") return null

    val userId = data.getQueryParameter("userId") ?: return null
    val secret = data.getQueryParameter("secret") ?: return null
    return EmailVerificationParams(userId = userId, secret = secret)
}
