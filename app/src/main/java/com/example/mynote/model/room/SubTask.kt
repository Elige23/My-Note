package com.example.mynote.model.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a subtask of a group task.
 *
 * Subtasks belong to exactly one group task (foreign key to TaskGroup).
 * They are sorted by completion status and creation date.
 *
 * @property id Unique identifier (auto-generated)
 * @property taskGroupId ID of the parent group task (foreign key)
 * @property desc Subtask description
 * @property isCompleted Subtask completion (default: false)
 * @property createdAt Creation timestamp (milliseconds since epoch) for sorting within the group
 *
 * @see TaskGroup
 */
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskGroup::class,
            parentColumns = ["id"],
            childColumns = ["taskGroupId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskGroupId")]
)
data class SubTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskGroupId: Long,
    val desc: String?,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

