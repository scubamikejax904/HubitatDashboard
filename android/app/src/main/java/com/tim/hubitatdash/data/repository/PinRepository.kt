package com.tim.hubitatdash.data.repository

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinRepository @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    // BCrypt MUST run on Dispatchers.IO (CPU-intensive ~50-150ms at cost 12)
    suspend fun setPin(plainPin: String) = withContext(Dispatchers.IO) {
        val hash = BCrypt.withDefaults().hashToString(12, plainPin.toCharArray())
        settingsRepository.setPinHash(hash)
    }

    suspend fun verifyPin(plainPin: String): Boolean = withContext(Dispatchers.IO) {
        val hash = settingsRepository.pinHash
        if (hash.isBlank()) return@withContext false
        BCrypt.verifyer().verify(plainPin.toCharArray(), hash).verified
    }

    fun isPinSet(): Boolean = settingsRepository.pinHash.isNotBlank()
}

