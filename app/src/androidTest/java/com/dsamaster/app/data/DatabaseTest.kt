package com.dsamaster.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dsamaster.app.data.entity.Note
import com.dsamaster.app.data.entity.Problem
import com.dsamaster.app.data.entity.StreakEntry
import com.dsamaster.app.data.entity.Topic
import com.dsamaster.app.data.entity.UserProgress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: DSAMasterDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, DSAMasterDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndReadTopic() = runBlocking {
        val topic = Topic(
            name = "Binary Search",
            category = "Searching",
            explanation = "Divide and conquer search on sorted arrays",
            timeComplexity = "O(log n)",
            spaceComplexity = "O(1)",
            difficultyLevel = "Medium",
            companyTags = "Google,Amazon"
        )
        val id = db.topicDao().insertTopic(topic)
        val loaded = db.topicDao().getTopicById(id).first()

        assertEquals("Binary Search", loaded?.name)
        assertEquals("Searching", loaded?.category)
    }

    @Test
    fun insertAndReadProblem() = runBlocking {
        val topicId = db.topicDao().insertTopic(
            Topic(
                name = "Arrays",
                category = "Data Structures",
                explanation = "Contiguous memory storage",
                timeComplexity = "O(1) access",
                spaceComplexity = "O(n)",
                difficultyLevel = "Easy",
                companyTags = "Meta"
            )
        )

        val problem = Problem(
            topicId = topicId,
            title = "Two Sum",
            description = "Find two numbers that add up to target",
            difficulty = "Easy",
            companyTags = "Google,Meta,Amazon",
            testCasesJson = "[{\"input\":[2,7,11,15],\"target\":9,\"output\":[0,1]}]"
        )
        val problemId = db.problemDao().insertProblem(problem)
        val loaded = db.problemDao().getProblemById(problemId).first()

        assertEquals("Two Sum", loaded?.title)
        assertEquals(topicId, loaded?.topicId)
    }

    @Test
    fun insertAndReadUserProgress() = runBlocking {
        val topicId = db.topicDao().insertTopic(
            Topic(
                name = "Linked Lists", category = "Data Structures",
                explanation = "Node-based sequence", timeComplexity = "O(n)",
                spaceComplexity = "O(n)", difficultyLevel = "Easy", companyTags = "Apple"
            )
        )
        val problemId = db.problemDao().insertProblem(
            Problem(
                topicId = topicId, title = "Reverse Linked List",
                description = "Reverse a singly linked list",
                difficulty = "Easy", companyTags = "Apple",
                testCasesJson = "[]"
            )
        )

        val progress = UserProgress(
            problemId = problemId,
            status = "solved",
            lastAttemptDate = System.currentTimeMillis(),
            timesReviewed = 1
        )
        val id = db.userProgressDao().insertProgress(progress)
        val loaded = db.userProgressDao().getProgressForProblem(problemId).first()

        assertEquals("solved", loaded?.status)
        assertEquals(1, loaded?.timesReviewed)
    }

    @Test
    fun insertAndReadStreakEntry() = runBlocking {
        val entry = StreakEntry(
            date = "2026-08-08",
            minutesActive = 45,
            problemsSolved = 3,
            streakFreezeUsed = false
        )
        db.streakDao().insertStreakEntry(entry)
        val loaded = db.streakDao().getStreakEntryByDate("2026-08-08").first()

        assertEquals(45, loaded?.minutesActive)
        assertEquals(3, loaded?.problemsSolved)
    }

    @Test
    fun insertAndReadNote() = runBlocking {
        val topicId = db.topicDao().insertTopic(
            Topic(
                name = "Trees", category = "Data Structures",
                explanation = "Hierarchical structure", timeComplexity = "O(log n)",
                spaceComplexity = "O(n)", difficultyLevel = "Medium", companyTags = "Amazon"
            )
        )
        val note = Note(
            topicId = topicId,
            problemId = null,
            userNote = "Remember: in-order traversal gives sorted output for BST",
            timestamp = System.currentTimeMillis()
        )
        db.noteDao().insertNote(note)
        val loaded = db.noteDao().getNotesForTopic(topicId).first()

        assertEquals(1, loaded.size)
        assertEquals("Remember: in-order traversal gives sorted output for BST", loaded[0].userNote)
    }
}