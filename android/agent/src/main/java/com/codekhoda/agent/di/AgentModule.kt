package com.codekhoda.agent.di

import android.content.Context
import com.codekhoda.agent.scanner.PackageAnalyzer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AgentModule {

    @Provides
    @Singleton
    fun providePackageAnalyzer(@ApplicationContext context: Context): PackageAnalyzer {
        return PackageAnalyzer(context)
    }

    @Provides
    @Singleton
    fun provideAppOpsWrapper(@ApplicationContext context: Context): com.codekhoda.agent.monitor.AppOpsWrapper {
        return com.codekhoda.agent.monitor.AppOpsWrapperImpl(context)
    }
}
