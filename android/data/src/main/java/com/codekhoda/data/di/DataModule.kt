package com.codekhoda.data.di

import android.content.Context
import androidx.room.Room
import com.codekhoda.data.local.AppDatabase
import com.codekhoda.data.local.dao.RiskDao
import com.codekhoda.data.repository.ThreatRepositoryImpl
import com.codekhoda.domain.repository.ThreatRepository
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
    @Singleton
    fun provideThreatRepository(repository: ThreatRepositoryImpl): ThreatRepository {
        return repository
    }

    @Provides
    @Singleton
    fun provideRetrofit(): retrofit2.Retrofit {
        return retrofit2.Retrofit.Builder()
            .baseUrl("https://codekhoda-sentinel-brain.liara.run/") // Live Liara Backend
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideCloudBrainApi(retrofit: retrofit2.Retrofit): com.codekhoda.data.remote.api.CloudBrainApi {
        return retrofit.create(com.codekhoda.data.remote.api.CloudBrainApi::class.java)
    }
}
