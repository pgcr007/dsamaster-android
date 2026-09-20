package com.dsamaster.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = LearningModule::class,
            parentColumns = ["id"],
            childColumns = ["moduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("moduleId")]
)
data class Lesson(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val moduleId: Long,
    val title: String,
    val content: String,
    val diagramType: String? = null, // reuses TopicDiagram's supported types (array, stack, queue, tree, graph, etc.)
    val codeExample: String? = null, // optional Python snippet, rendered in its own monospace card
    val orderIndex: Int
)