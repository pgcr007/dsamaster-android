package com.dsamaster.app.data.dao

import androidx.room.*
import com.dsamaster.app.data.entity.Topic
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics ORDER BY category, name")
    fun getAllTopics(): Flow<List<Topic>>

    @Query("SELECT * FROM topics WHERE id = :topicId")
    fun getTopicById(topicId: Long): Flow<Topic?>

    @Query("SELECT * FROM topics WHERE category = :category ORDER BY name")
    fun getTopicsByCategory(category: String): Flow<List<Topic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: Topic): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<Topic>)

    @Update
    suspend fun updateTopic(topic: Topic)

    @Delete
    suspend fun deleteTopic(topic: Topic)
}