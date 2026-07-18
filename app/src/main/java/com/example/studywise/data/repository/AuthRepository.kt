package com.example.studywise.data.repository

import com.example.studywise.Appwrite
import javax.inject.Inject

class AuthRepository @Inject constructor()
{
    suspend fun register(email: String, password: String) = Appwrite.onRegister(email, password)
    suspend fun login(email: String, password: String) {
        val currentSession = Appwrite.getCurrentSessionOrNull()
        if (currentSession != null) {
            Appwrite.onLogout()
        }
        Appwrite.onLogin(email, password)
    }

    suspend fun logout() = Appwrite.onLogout()
    suspend fun getCurrentUser() = Appwrite.getCurrentUser()
    suspend fun isSessionActive() = Appwrite.isSessionActive()
    suspend fun sendVerification(url: String) = Appwrite.sendVerification(url)
    suspend fun confirmVerification(userId: String, secret: String) =
        Appwrite.confirmVerification(userId, secret)
}