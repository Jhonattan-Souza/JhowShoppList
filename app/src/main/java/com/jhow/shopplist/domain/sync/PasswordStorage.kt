package com.jhow.shopplist.domain.sync

interface PasswordStorage {
    suspend fun save(password: String)
    suspend fun load(): String?
    suspend fun clear()
}
