package com.example.mynote.model.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a group of tasks.
 *
 * A group task can contain an unlimited number of subtasks.
 *
 * @property id Unique identifier (auto-generated)
 * @property desc Group task description
 * @property isCompleted Completion status (affects all subtasks when true)
 * @property createdAt Creation timestamp for sorting
 */
@Entity(tableName = "task_groups")
data class TaskGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val desc: String?,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
