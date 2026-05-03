package com.example.mynote.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a single task (not part of a group task).
 *
 * Single tasks are independent tasks that don't contain subtasks.
 * They are displayed in the same list as group tasks.
 *
 * @property id Unique identifier (auto-generated)
 * @property desc Single task description
 * @property isCompleted Single task completion (default: false)
 * @property createdAt Creation timestamp (milliseconds since epoch)
 */
@Entity(tableName = "single_tasks")
data class SingleTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val desc: String?,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    )
