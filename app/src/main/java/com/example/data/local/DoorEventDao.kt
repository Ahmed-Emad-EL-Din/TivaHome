package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DoorEventDao {
  @Query("SELECT * FROM door_events ORDER BY timestampMillis DESC, id DESC")
  fun observeAllEvents(): Flow<List<DoorEventEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEvent(event: DoorEventEntity)

  @Query("DELETE FROM door_events")
  suspend fun clearAll()
}
