package com.jhow.shopplist.di

import android.content.Context
import androidx.room.Room
import com.jhow.shopplist.data.local.dao.ShoppingItemDao
import com.jhow.shopplist.data.local.db.AppDatabase
import com.jhow.shopplist.data.repository.ShoppingListRepositoryImpl
import com.jhow.shopplist.domain.repository.ShoppingListRepository
import com.jhow.shopplist.core.dispatchers.IoDispatcher
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
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME).build()

    @Provides
    fun provideShoppingItemDao(database: AppDatabase): ShoppingItemDao = database.shoppingItemDao()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
