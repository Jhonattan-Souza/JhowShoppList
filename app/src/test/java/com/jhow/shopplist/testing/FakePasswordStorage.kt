package com.jhow.shopplist.testing

import com.jhow.shopplist.domain.sync.PasswordStorage

class FakePasswordStorage : PasswordStorage {
    var password: String? = null

    override suspend fun save(password: String) {
        this.password = password
    }

    override suspend fun load(): String? = password

    override suspend fun clear() {
        password = null
    }
}
