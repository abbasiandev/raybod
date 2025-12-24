package com.codekhoda.data.di

import android.content.Context
import androidx.room.Room
import com.codekhoda.data.local.AppDatabase
import com.codekhoda.data.local.dao.NetworkDao
import com.codekhoda.data.local.dao.RiskDao
import com.codekhoda.data.repository.NetworkRepositoryImpl
import com.codekhoda.data.repository.ThreatRepositoryImpl
import com.codekhoda.data.repository.UserPreferencesRepositoryImpl
import com.codekhoda.domain.repository.NetworkRepository
import com.codekhoda.domain.repository.ThreatRepository
import com.codekhoda.domain.repository.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sentinel_db"
        ).build()
    }

    @Provides
    fun provideRiskDao(db: AppDatabase): RiskDao {
        return db.riskDao()
    }

    @Provides
    fun provideNetworkDao(db: AppDatabase): NetworkDao {
        return db.networkDao()
    }

    @Provides
    @Singleton
    fun provideThreatRepository(repository: ThreatRepositoryImpl): ThreatRepository {
        return repository
    }

    @Provides
    @Singleton
    fun provideNetworkRepository(repository: NetworkRepositoryImpl): NetworkRepository {
        return repository
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideGson(): com.google.gson.Gson {
        return com.google.gson.Gson()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): okhttp3.OkHttpClient {
        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        }
        
        return okhttp3.OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(com.codekhoda.data.remote.interceptor.RetryInterceptor(maxRetries = 3))
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: okhttp3.OkHttpClient): retrofit2.Retrofit {
        return retrofit2.Retrofit.Builder()
            .baseUrl("https://codekhoda-sentinel.liara.run/") // Updated to match Liara app name
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideCloudBrainApi(retrofit: retrofit2.Retrofit): com.codekhoda.data.remote.api.CloudBrainApi {
        return retrofit.create(com.codekhoda.data.remote.api.CloudBrainApi::class.java)
    }
}
