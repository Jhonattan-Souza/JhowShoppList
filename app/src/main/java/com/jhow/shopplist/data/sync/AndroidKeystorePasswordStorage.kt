package com.jhow.shopplist.data.sync

import android.util.Base64
import com.jhow.shopplist.domain.sync.PasswordStorage
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidKeystorePasswordStorage @Inject constructor(
    private val keyProvider: CryptoKeyProvider,
    private val preferences: CalDavSecurePreferences
) : PasswordStorage {
    override suspend fun save(password: String) {
        val passwordBytes = password.toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, keyProvider.getOrCreateSecretKey())
        }
        val encrypted = cipher.doFinal(passwordBytes)
        try {
            preferences.writePassword(
                cipherText = Base64.encodeToString(encrypted, Base64.NO_WRAP),
                iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            )
        } finally {
            passwordBytes.fill(0)
            encrypted.fill(0)
        }
    }

    override suspend fun load(): String? {
        return loadPasswordOrNull(
            saved = preferences.readPassword(),
            clearStoredPassword = preferences::clearPassword
        ) { saved ->
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    keyProvider.getOrCreateSecretKey(),
                    GCMParameterSpec(128, Base64.decode(saved.iv, Base64.NO_WRAP))
                )
            }
            val decrypted = cipher.doFinal(Base64.decode(saved.cipherText, Base64.NO_WRAP))
            decrypted.decodeUtf8AndClear()
        }
    }

    override suspend fun hasSavedPassword(): Boolean = preferences.readPassword() != null

    override suspend fun clear() {
        preferences.clearPassword()
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

internal fun ByteArray.decodeUtf8AndClear(): String {
    return try {
        toString(Charsets.UTF_8)
    } finally {
        fill(0)
    }
}

internal fun loadPasswordOrNull(
    saved: PasswordEntry?,
    clearStoredPassword: () -> Unit,
    decrypt: (PasswordEntry) -> String
): String? {
    if (saved == null) return null

    return try {
        decrypt(saved)
    } catch (_: Exception) {
        clearStoredPassword()
        null
    }
}
