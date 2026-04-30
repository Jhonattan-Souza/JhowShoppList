package com.jhow.shopplist.di

import com.jhow.shopplist.domain.icon.DefaultIconMatcher
import com.jhow.shopplist.domain.icon.DefaultTextNormalizer
import com.jhow.shopplist.domain.icon.IconBucket
import com.jhow.shopplist.domain.icon.IconMatcher
import com.jhow.shopplist.domain.icon.TextNormalizer
import com.jhow.shopplist.presentation.icon.IconResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideIconMatcher(normalizer: TextNormalizer): IconMatcher {
        val dictionary = mapOf(
            "leite" to IconBucket.DAIRY,
            "milk" to IconBucket.DAIRY,
            "iogurte" to IconBucket.DAIRY,
            "yogurt" to IconBucket.DAIRY,
            "maçã" to IconBucket.FRUIT,
            "maca" to IconBucket.FRUIT,
            "apple" to IconBucket.FRUIT,
            "banana" to IconBucket.FRUIT,
            "pão" to IconBucket.BREAD,
            "pao" to IconBucket.BREAD,
            "bread" to IconBucket.BREAD,
            "arroz" to IconBucket.PANTRY_CANNED,
            "rice" to IconBucket.PANTRY_CANNED,
            "feijão" to IconBucket.PANTRY_CANNED,
            "feijao" to IconBucket.PANTRY_CANNED,
            "beans" to IconBucket.PANTRY_CANNED
        )
        return DefaultIconMatcher(dictionary, normalizer)
    }

    @Provides
    @Singleton
    fun provideIconResolver(matcher: IconMatcher): IconResolver = IconResolver(matcher)
}
