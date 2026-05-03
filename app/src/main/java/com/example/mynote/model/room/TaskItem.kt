package com.example.mynote.model.room

/**
 * Sealed class representing a unified task item for display in the UI.
 *
 * This class combines two different types of tasks into a single type TaskItem
 * that can be displayed together:
 * - TaskItem.SingleTaskItem: A single task without subtasks
 * - TaskItem.TaskGroupWithSubtasksItem: A group task with its associated subtasks.
 *
 * The sealed class provides common properties (id, isCompleted, createdAt, desc, subtasks)
 * through the class hierarchy, that work consistently across both task types.
 *
 * @see SingleTask
 * @see TaskGroupWithSubtasks
 */
sealed class TaskItem {

    /**
     * A single task without subtasks.
     */
    data class SingleTaskItem(val singleTask: SingleTask): TaskItem()

    /**
     * A group task with its associated subtasks.
     */
    data class TaskGroupWithSubtasksItem(val taskGroupWithSubtasks: TaskGroupWithSubtasks): TaskItem()

    /**
     * Unique identifier for the task.
     *
     * @return For single tasks returns the SingleTask.id,
     *
     * for group tasks returns the TaskGroup.id.
     */
    val id: Long
        get() = when(this){
            is SingleTaskItem -> singleTask.id
            is TaskGroupWithSubtasksItem -> taskGroupWithSubtasks.taskGroup.id
        }

    /**
     * Completion status of the task.
     *
     * @return For single tasks returns SingleTask.isCompleted,
     *
     * for group tasks returns TaskGroup.isCompleted.
     */
    val isCompleted: Boolean
        get() = when(this){
            is SingleTaskItem -> singleTask.isCompleted
            is TaskGroupWithSubtasksItem -> taskGroupWithSubtasks.taskGroup.isCompleted
        }

    /**
     * Creation timestamp of the task (milliseconds since epoch).
     *
     * @return For single tasks returns SingleTask.createdAt,
     *
     * for group tasks returns TaskGroup.createdAt.
     */
    val createdAt: Long
        get() = when(this){
            is SingleTaskItem -> singleTask.createdAt
            is TaskGroupWithSubtasksItem -> taskGroupWithSubtasks.taskGroup.createdAt
        }

    /**
     * Task description.
     *
     * @return For single tasks, returns SingleTask.desc,
     *
     * for group tasks, returns TaskGroup.desc.
     */
    val desc: String?
        get() = when(this){
            is SingleTaskItem -> singleTask.desc
            is TaskGroupWithSubtasksItem -> taskGroupWithSubtasks.taskGroup.desc
        }

    /**
     * List of subtasks belonging to this task.
     *
     * @return For SingleTaskItem returns an empty list (no subtasks),
     *
     * for TaskGroupWithSubtasksItem returns the list of subtasks TaskGroupWithSubtasks.subtasks
     */
    val subtasks: List<SubTask>
        get() = when(this) {
            is SingleTaskItem -> emptyList()
            is TaskGroupWithSubtasksItem -> taskGroupWithSubtasks.subtasks
        }

}