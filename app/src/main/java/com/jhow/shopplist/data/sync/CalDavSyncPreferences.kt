package com.jhow.shopplist.data.sync

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class CalDavSyncPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    internal val dataStore: androidx.datastore.core.DataStore<Preferences> = PreferenceDataStoreFactory.create {
        File(context.filesDir, "caldav_sync.preferences_pb")
    }

    val data: Flow<Preferences>
        get() = dataStore.data

    suspend fun updateData(transform: suspend (Preferences) -> Preferences) {
        dataStore.updateData(transform)
    }
}
