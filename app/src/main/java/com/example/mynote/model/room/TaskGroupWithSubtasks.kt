package com.example.mynote.model.room

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Data class combining a task group with its associated subtasks.
 *
 * The helper class does NOT modify the database schema. It's purely a data holder for query results.
 * This class is used by Room database to return a task group along with all its subtasks in a single
 * query result. It represents a complete task item that can be displayed in the UI.
 *
 * The relationship is defined using Room's @Embedded and @Relation annotations:
 * - TaskGroup is embedded directly into the result
 * - Subtasks are fetched separately and linked via the taskGroupId foreign key
 *
 * The relationship is defined by:
 * - parentColumn = "id" (the ID column in TaskGroup)
 * - entityColumn = "taskGroupId" (the foreign key column in SubTask)
 *
 * Subtasks are automatically loaded by Room when querying TaskGroupWithSubtasks.
 *
 * @param taskGroup The parent task group
 * @param subtasks List of subtasks belonging to this group
 *
 * @see TaskGroup
 * @see SubTask
 */

data class TaskGroupWithSubtasks(

    @Embedded
    val taskGroup: TaskGroup,

    @Relation(
        parentColumn = "id",
        entityColumn = "taskGroupId"
    )

    val subtasks: List<SubTask>
)