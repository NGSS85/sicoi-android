package br.com.sicoi.mobile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import android.content.Context
import br.com.sicoi.mobile.core.database.entity.WorkOrderEntity
import br.com.sicoi.mobile.core.database.dao.WorkOrderDao

/**
 * Room Database para modo offline.
 * Armazena OS que não puderam ser sincronizadas devido à falta de conexão.
 */
@Database(
    entities = [WorkOrderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workOrderDao(): WorkOrderDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sicoi_offline.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
