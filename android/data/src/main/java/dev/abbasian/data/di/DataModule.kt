package dev.abbasian.data.di

import android.content.Context
import androidx.room.Room
import dev.abbasian.data.local.AppDatabase
import dev.abbasian.data.local.dao.NetworkDao
import dev.abbasian.data.local.dao.RiskDao
import dev.abbasian.data.repository.NetworkRepositoryImpl
import dev.abbasian.data.repository.ThreatRepositoryImpl
import dev.abbasian.data.repository.UserPreferencesRepositoryImpl
import dev.abbasian.domain.repository.NetworkRepository
import dev.abbasian.domain.repository.ThreatRepository
import dev.abbasian.domain.repository.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.abbasian.data.remote.BackendEndpoint
import dev.abbasian.data.remote.interceptor.HostOverrideInterceptor
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

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
        return com.google.gson.GsonBuilder()
            .serializeSpecialFloatingPointValues()
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        deviceIdInterceptor: dev.abbasian.data.remote.interceptor.DeviceIdInterceptor
    ): okhttp3.OkHttpClient {
        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
        }

        BackendEndpoint.prefetchAsync()

        return okhttp3.OkHttpClient.Builder()
            .dns(dev.abbasian.data.remote.NetworkDns.preferIpv4)
            .hostnameVerifier { _, session ->
                HttpsURLConnection.getDefaultHostnameVerifier()
                    .verify(BackendEndpoint.HOST, session)
            }
            .addInterceptor(HostOverrideInterceptor(BackendEndpoint.HOST))
            .addInterceptor(deviceIdInterceptor)
            .addInterceptor(logging)
            .addInterceptor(dev.abbasian.data.remote.interceptor.RetryInterceptor(maxRetries = 3))
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: okhttp3.OkHttpClient): retrofit2.Retrofit {
        return retrofit2.Retrofit.Builder()
            .baseUrl(BackendEndpoint.httpsBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideCloudBrainApi(retrofit: retrofit2.Retrofit): dev.abbasian.data.remote.api.CloudBrainApi {
        return retrofit.create(dev.abbasian.data.remote.api.CloudBrainApi::class.java)
    }
}
