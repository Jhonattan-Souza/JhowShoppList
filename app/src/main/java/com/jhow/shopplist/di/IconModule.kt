package com.jhow.shopplist.di

import android.content.Context
import com.jhow.shopplist.data.icon.AssetDictionaryLoader
import com.jhow.shopplist.data.icon.DictionaryLoader
import com.jhow.shopplist.domain.icon.DefaultIconMatcher
import com.jhow.shopplist.domain.icon.DefaultTextNormalizer
import com.jhow.shopplist.domain.icon.IconMatcher
import com.jhow.shopplist.domain.icon.TextNormalizer
import com.jhow.shopplist.presentation.icon.IconResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IconModule {

    @Provides
    @Singleton
    fun provideTextNormalizer(): TextNormalizer = DefaultTextNormalizer()

    @Provides
    @Singleton
    fun provideDefaultIconMatcher(normalizer: TextNormalizer): DefaultIconMatcher =
        DefaultIconMatcher(emptyMap(), normalizer)

    @Provides
    @Singleton
    fun provideIconMatcher(matcher: DefaultIconMatcher): IconMatcher = matcher

    @Provides
    @Singleton
    fun provideDictionaryLoader(@ApplicationContext context: Context): DictionaryLoader =
        AssetDictionaryLoader { name -> context.assets.open(name) }

    @Provides
    @Singleton
    fun provideIconResolver(matcher: IconMatcher): IconResolver = IconResolver(matcher)
}
