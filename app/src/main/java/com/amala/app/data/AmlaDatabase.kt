package com.amala.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SoundEvent::class], version = 1, exportSchema = false)
abstract class AmlaDatabase : RoomDatabase() {
    abstract fun soundEventDao(): SoundEventDao

    companion object {
        @Volatile
        private var instance: AmlaDatabase? = null

        fun get(context: Context): AmlaDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AmlaDatabase::class.java,
                "amla.db",
            ).build().also { instance = it }
        }
    }
}
