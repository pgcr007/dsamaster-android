package com.dsamaster.app.data.dao

/**
 * Flattened row for rendering the Learning Path screen: one row per lesson,
 * with its module context and current progress status already joined in, so
 * the UI doesn't have to stitch together three separate Flows.
 */
data class LessonOverviewRow(
    val lessonId: Long,
    val moduleId: Long,
    val lessonTitle: String,
    val lessonOrder: Int,
    val moduleTitle: String,
    val moduleOrder: Int,
    val status: String
)