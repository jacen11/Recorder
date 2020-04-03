package ru.dpastukhov.recorder.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface RecordDatabaseDao {
    @Insert
    fun insert(record: RecordingItem)

    @Update
    fun update(record: RecordingItem)

    @Query("select * from recording_table where id = :key")
    fun getRecord(key: Long): RecordingItem

    @Query("delete from recording_table")
    fun clearAll()

    @Query("delete from recording_table where id = :key")
    fun removeRecord(key: Long)

    @Query("select * from recording_table order by id desc")
    fun getAllRecord(): LiveData<MutableList<RecordingItem>>

    @Query("select count(*) from recording_table")
    fun getCount(): Int
}