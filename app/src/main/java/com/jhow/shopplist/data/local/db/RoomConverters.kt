package com.jhow.shopplist.data.local.db

import androidx.room.TypeConverter
import com.jhow.shopplist.domain.model.SyncStatus

class RoomConverters {
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
