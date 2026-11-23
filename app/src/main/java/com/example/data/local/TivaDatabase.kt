package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DoorEventEntity::class], version = 1, exportSchema = false)
abstract class TivaDatabase : RoomDatabase() {
  abstract fun doorEventDao(): DoorEventDao

  companion object {
    @Volatile private var instance: TivaDatabase? = null

    fun getInstance(context: Context): TivaDatabase {
      return instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                TivaDatabase::class.java,
                "tivahome_db",
              )
              .build()
              .also { instance = it }
        }
    }
  }
}
