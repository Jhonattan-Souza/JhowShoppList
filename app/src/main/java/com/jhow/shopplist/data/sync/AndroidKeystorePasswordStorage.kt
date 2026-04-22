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
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, keyProvider.getOrCreateSecretKey())
        }
        val encrypted = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
        preferences.writePassword(
            cipherText = Base64.encodeToString(encrypted, Base64.NO_WRAP),
            iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        )
    }

    override suspend fun load(): String? {
        val saved = preferences.readPassword() ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                keyProvider.getOrCreateSecretKey(),
                GCMParameterSpec(128, Base64.decode(saved.iv, Base64.NO_WRAP))
            )
        }
        val decrypted = cipher.doFinal(Base64.decode(saved.cipherText, Base64.NO_WRAP))
        return decrypted.toString(Charsets.UTF_8)
    }

    override suspend fun clear() {
        preferences.clearPassword()
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
