package com.dsamaster.app.data.repository

import com.dsamaster.app.data.dao.TopicDao
import com.dsamaster.app.data.entity.Topic
import kotlinx.coroutines.flow.Flow

class TopicRepository(private val topicDao: TopicDao) {
    fun getAllTopics(): Flow<List<Topic>> = topicDao.getAllTopics()

    fun getTopicById(topicId: Long): Flow<Topic?> = topicDao.getTopicById(topicId)

    fun getTopicsByCategory(category: String): Flow<List<Topic>> =
        topicDao.getTopicsByCategory(category)

    suspend fun insertTopic(topic: Topic): Long = topicDao.insertTopic(topic)

    suspend fun insertTopics(topics: List<Topic>) = topicDao.insertTopics(topics)

    suspend fun updateTopic(topic: Topic) = topicDao.updateTopic(topic)

    suspend fun deleteTopic(topic: Topic) = topicDao.deleteTopic(topic)
}