package br.com.sicoi.mobile.core.di

import android.content.Context
import br.com.sicoi.mobile.core.database.AppDatabase
import br.com.sicoi.mobile.core.database.dao.WorkOrderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt que fornece as instâncias singleton do Room Database e dos DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideWorkOrderDao(database: AppDatabase): WorkOrderDao {
        return database.workOrderDao()
    }
}
