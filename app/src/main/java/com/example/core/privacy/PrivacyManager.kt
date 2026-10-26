package com.example.core.privacy

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

data class StorageStatistics(
    val internalStoragePath: String,
    val totalAttachmentSizeBytes: Long,
    val totalAttachmentsCount: Int
)

class PrivacyManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vellum_privacy_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_SALT = "key_salt"
        private const val KEY_LOCK_ENABLED = "key_lock_enabled"
        private const val KEY_TIMEOUT_MINUTES = "key_timeout_minutes"
        private const val KEY_LAST_BACKGROUND_TIME = "key_last_background_time"

        @Volatile
        private var isSessionUnlocked: Boolean = false
    }

    val isLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ENABLED, false)

    val timeoutMinutes: Int
        get() = prefs.getInt(KEY_TIMEOUT_MINUTES, 0) // 0 = Immediately

    fun isAppCurrentlyLocked(): Boolean {
        if (!isLockEnabled) return false
        if (!isSessionUnlocked) return true

        val lastBackground = prefs.getLong(KEY_LAST_BACKGROUND_TIME, 0L)
        if (lastBackground == 0L) return false

        val timeoutMillis = timeoutMinutes * 60 * 1000L
        val elapsed = System.currentTimeMillis() - lastBackground
        if (elapsed > timeoutMillis) {
            isSessionUnlocked = false
            return true
        }
        return false
    }

    fun setPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_SALT, salt)
            .putBoolean(KEY_LOCK_ENABLED, true)
            .apply()
        isSessionUnlocked = true
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_SALT, null) ?: return false
        val candidateHash = hashPin(pin, storedSalt)
        val matches = storedHash == candidateHash
        if (matches) {
            isSessionUnlocked = true
        }
        return matches
    }

    fun disablePin(): Boolean {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_SALT)
            .putBoolean(KEY_LOCK_ENABLED, false)
            .apply()
        isSessionUnlocked = true
        return true
    }

    fun setTimeoutMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_TIMEOUT_MINUTES, minutes).apply()
    }

    fun onAppBackgrounded() {
        prefs.edit().putLong(KEY_LAST_BACKGROUND_TIME, System.currentTimeMillis()).apply()
    }

    fun onAppForegrounded() {
        // Will check elapsed time in isAppCurrentlyLocked
    }

    fun unlockSession() {
        isSessionUnlocked = true
    }

    fun lockSessionNow() {
        isSessionUnlocked = false
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
