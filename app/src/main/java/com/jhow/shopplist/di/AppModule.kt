package com.jhow.shopplist.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.jhow.shopplist.data.local.dao.ShoppingItemDao
import com.jhow.shopplist.data.local.db.AppDatabase
import com.jhow.shopplist.data.repository.ShoppingListRepositoryImpl
import com.jhow.shopplist.data.sync.AndroidKeystorePasswordStorage
import com.jhow.shopplist.data.sync.CalDavDiscoveryService
import com.jhow.shopplist.data.sync.CalDavShoppingSyncGateway
import com.jhow.shopplist.data.sync.DataStoreCalDavConfigRepository
import com.jhow.shopplist.data.sync.NoOpCalDavDiscoveryService
import com.jhow.shopplist.data.sync.WorkManagerShoppingSyncScheduler
import com.jhow.shopplist.domain.repository.ShoppingListRepository
import com.jhow.shopplist.core.dispatchers.IoDispatcher
import com.jhow.shopplist.domain.sync.CalDavConfigRepository
import com.jhow.shopplist.domain.sync.PasswordStorage
import com.jhow.shopplist.domain.sync.ShoppingListSyncGateway
import com.jhow.shopplist.domain.sync.ShoppingSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindModule {
    @Binds
    @Singleton
    abstract fun bindShoppingListRepository(
        impl: ShoppingListRepositoryImpl
    ): ShoppingListRepository

    @Binds
    @Singleton
    abstract fun bindShoppingSyncScheduler(
        impl: WorkManagerShoppingSyncScheduler
    ): ShoppingSyncScheduler

    @Binds
    @Singleton
    abstract fun bindShoppingListSyncGateway(
        impl: CalDavShoppingSyncGateway
    ): ShoppingListSyncGateway

    @Binds
    @Singleton
    abstract fun bindCalDavConfigRepository(
        impl: DataStoreCalDavConfigRepository
    ): CalDavConfigRepository

    @Binds
    @Singleton
    abstract fun bindPasswordStorage(
        impl: AndroidKeystorePasswordStorage
    ): PasswordStorage

    @Binds
    @Singleton
    abstract fun bindCalDavDiscoveryService(
        impl: NoOpCalDavDiscoveryService
    ): CalDavDiscoveryService
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideShoppingItemDao(database: AppDatabase): ShoppingItemDao = database.shoppingItemDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
