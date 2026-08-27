package com.dsamaster.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "problems",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("topicId")]
)
data class Problem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topicId: Long,
    val title: String,
    val description: String,
    val difficulty: String, // "Easy" | "Medium" | "Hard"
    val companyTags: String, // comma-separated, e.g. "Amazon,Google"
    val constraints: String = "", // one constraint per line
    val examplesJson: String = "[]", // JSON array of {input, output, explanation}
    val starterCodeKotlin: String? = null,
    val starterCodeJava: String? = null,
    val starterCodePython: String? = null,
    val starterCodeCpp: String? = null,
    val testCasesJson: String, // JSON string of test cases
    val hints: String? = null // pipe-separated graduated hints
)