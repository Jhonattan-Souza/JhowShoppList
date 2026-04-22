package com.jhow.shopplist.data.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalDavSecurePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun writePassword(cipherText: String, iv: String) {
        prefs.edit {
            putString(KEY_CIPHER_TEXT, cipherText)
            putString(KEY_IV, iv)
        }
    }

    internal fun readPassword(): PasswordEntry? {
        val cipherText = prefs.getString(KEY_CIPHER_TEXT, null) ?: return null
        val iv = prefs.getString(KEY_IV, null) ?: return null
        return PasswordEntry(cipherText = cipherText, iv = iv)
    }

    fun clearPassword() {
        prefs.edit {
            remove(KEY_CIPHER_TEXT)
            remove(KEY_IV)
        }
    }

    private companion object {
        const val PREFS_NAME = "caldav_secure"
        const val KEY_CIPHER_TEXT = "cipher_text"
        const val KEY_IV = "iv"
    }
}

internal data class PasswordEntry(
    val cipherText: String,
    val iv: String
)
