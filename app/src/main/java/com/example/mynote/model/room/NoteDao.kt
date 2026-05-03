package com.example.mynote.model.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the notes and tasks database.
 *
 * Provides CRUD operations and complex queries for all entities:
 * - Note: User notes
 * - SingleTask: Standalone tasks without subtasks
 * - TaskGroup: Group tasks that can contain subtasks
 * - SubTask: Individual subtasks belonging to task groups
 *
 * The DAO uses Room's suspend functions for coroutine support
 * and LiveData/Flow for reactive UI updates.
 *
 * @see NoteDatabase
 * @see Note
 * @see SingleTask
 * @see TaskGroup
 * @see SubTask
 * @see TaskGroupWithSubtasks
 * @see TaskItem
 */
@Dao
interface NoteDao {

    //Note operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note:Note)

    @Update
    suspend fun updateNote(note:Note)

    @Delete
    suspend fun deleteNote(note:Note)

    @Query("SELECT * FROM notetable ORDER BY time DESC")
    fun getAllNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM notetable WHERE title LIKE :search OR `desc` LIKE :search ORDER BY time DESC")
    fun searchNotes(search: String): LiveData<List<Note>>

    @Query("SELECT * FROM notetable WHERE id = :noteId")
    suspend fun getEditNoteById(noteId: Int): Note?


    //Single tasks operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleTask(singleTask: SingleTask)

    @Update
    suspend fun updateSingleTask(singleTask: SingleTask)

    @Delete
    suspend fun deleteSingleTask(singleTask: SingleTask)

    @Query("SELECT * FROM single_tasks ORDER BY createdAt DESC")
    fun getAllSingleTasks(): Flow<List<SingleTask>>

    @Query("UPDATE single_tasks SET isCompleted = :isCompleted WHERE id = :singleTaskId")
    suspend fun updateSingleTaskCompletion(singleTaskId: Long, isCompleted: Boolean)

    @Query("SELECT * FROM single_tasks WHERE id = :singleTaskId")
    suspend fun getSingleTaskById(singleTaskId: Long): SingleTask


    //Task groups operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskGroup(taskGroup: TaskGroup): Long

    @Update
    suspend fun updateTaskGroup(taskGroup: TaskGroup)

    @Delete
    suspend fun deleteTaskGroup(taskGroup: TaskGroup)

    @Query("SELECT * FROM task_groups ORDER BY createdAt DESC")
    fun getAllTaskGroups(): Flow<List<TaskGroup>>

    @Query("UPDATE task_groups SET isCompleted = :isCompleted WHERE id = :groupId")
    suspend fun updateTaskGroupCompletion(groupId: Long, isCompleted: Boolean)


    //SubTasks operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTask)

    @Update
    suspend fun updateSubTask(subTask: SubTask)

    @Delete
    suspend fun deleteSubTask(subTask: SubTask)

    @Query("DELETE FROM subtasks WHERE id = :subtaskId AND taskGroupId = :groupId")
    suspend fun deleteSubTaskById(subtaskId: Long, groupId: Long)

    /**
     * Updates a subtask with full control over all its fields.
     *
     * This function allows updating the description, completion status, and creation timestamp
     * of a subtask. It differs from Room's standard @Update annotation, which only updates
     * fields that have changed and uses the current system time for createdAt.
     *
     * Use this function when you need to:
     * - Preserve the original creation timestamp
     * - Set a specific timestamp
     *
     * @param id The ID of the subtask to update
     * @param newDesc The new description (can be null)
     * @param isCompleted The new completion status
     * @param createdAt The creation timestamp to set (milliseconds since epoch)
     */
    @Query("UPDATE subtasks SET `desc` = :newDesc, isCompleted = :isCompleted, createdAt = :createdAt WHERE id = :id")
    suspend fun updateSubtaskWithData(id: Long, newDesc: String?, isCompleted: Boolean, createdAt: Long)

    /**
     * Updates the complete list of subtasks for a specific task group.
     *
     * This function handles the full synchronization of subtasks after editing a task group.
     * It performs three main operations in a single transaction:
     * - Inserts new subtasks (those with negative temporary IDs)
     * - Updates existing subtasks (those with valid positive IDs)
     * - Deletes removed subtasks (IDs present in the deletedIdList)
     *
     * **Temporary ID System:**
     *
     * When creating new subtasks in the group task editing UI, they are assigned negative temporary IDs
     * (e.g., -1L, -2L, -3L) to distinguish them from existing records in the database.
     *
     * This prevents an error where multiple new subtasks with the same ID (0L)
     * would incorrectly update each other during editing.
     *
     * Before inserting into the database, these temporary IDs are converted to 0L,
     * which allows Room to generate correct, automatically incrementing IDs.
     *
     * @param groupId ID of the parent task group
     * @param newListSubtasks The complete list of subtasks after editing (includes both new and existing)
     * @param deletedIdList List of subtask IDs that were removed from the group
     *
     * @see insertSubTask
     * @see updateSubtaskWithData
     * @see deleteSubTaskById
     */
    @Transaction
    suspend fun updateAllSubtasksForGroupId(groupId: Long, newListSubtasks :  List<SubTask>, deletedIdList:  List<Long>){

        newListSubtasks.forEach { subTask ->

            if (subTask.id < 0L) {
                val updatedSubtask = subTask.copy(id = 0L)
                insertSubTask(updatedSubtask)
            }
            else{
                updateSubtaskWithData(
                    subTask.id,
                    subTask.desc,
                    subTask.isCompleted,
                    subTask.createdAt
                )
            }
        }
        deletedIdList.forEach { id ->
            if (id > 0){
                deleteSubTaskById(id, groupId)
            }
        }
    }

    @Query("SELECT * FROM subtasks WHERE taskGroupId = :groupId ORDER BY createdAt ASC")
    fun getSubtasksByGroupId(groupId: Long): Flow<List<SubTask>>

    @Query("SELECT * FROM subtasks WHERE taskGroupId = :groupId AND id = :subtaskId")
    suspend fun getSubtaskByIdInGroup(groupId: Long, subtaskId: Long): SubTask

    @Query("SELECT taskGroupId FROM subtasks WHERE id = :subtaskId")
    suspend fun getGroupIdFromSubtaskId(subtaskId: Long): Long?

    @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE id = :subtaskId AND taskGroupId = :taskGroupId")
    suspend fun updateSubTaskCompletion(taskGroupId: Long, subtaskId: Long, isCompleted: Boolean)

    @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE taskGroupId = :groupId")
    suspend fun updateAllSubtasksCompletion(groupId: Long, isCompleted: Boolean)

    /**
     * Deletes all subtasks belonging to a specific task group.
     *
     * @param groupId ID of the task group whose subtasks will be deleted
     */
    @Query("DELETE FROM subtasks WHERE taskGroupId = :groupId")
    suspend fun deleteAllSubtasksForGroup(groupId: Long)


    // TaskGroupWithSubtasks

    /**
     * Retrieves all task groups along with their associated subtasks.
     *
     * This function uses Room's @Relation feature to automatically fetch subtasks
     * for each task group. Room looks at the **return type** (Flow<List<TaskGroupWithSubtasks>>)
     * to determine which related entities to load, not the table being queried.
     *
     * The returned TaskGroupWithSubtasks class is a **read-only** data class.
     * It is designed for displaying data in the UI and should not be used
     * for database write operations.
     *
     * @return Flow emitting a list of task groups, each containing its subtasks
     *
     * @see TaskGroupWithSubtasks
     * @see TaskGroup
     * @see SubTask
     */
    @Transaction
    @Query("SELECT * FROM task_groups ORDER BY createdAt DESC")
    fun getAllTaskGroupsWithSubtasks(): Flow<List<TaskGroupWithSubtasks>>


    //Complex operations for tasks

    /**
     * Inserts a task group with its subtasks in a single transaction.
     *
     * Creates a new task group and then inserts all provided subtasks associated with it,
     * using the current system time for createdAt.
     * Both operations are performed atomically — if any
     * operation fails, the entire transaction is rolled back.
     *
     * @param groupDesc Description of the task group
     * @param subtaskDesc List of subtask descriptions (can be empty)
     * @return The auto-generated ID of the newly created task group
     *
     * @see insertTaskGroup
     * @see insertSubTask
     */
    @Transaction
    suspend fun insertTaskGroupWithSubtasks(groupDesc: String, subtaskDesc: List<String>): Long {
        // Create task group
        val taskGroup = TaskGroup(desc = groupDesc)
        val groupId = insertTaskGroup(taskGroup)

        // Create subtasks
        subtaskDesc.forEach { description ->
            val subTask = SubTask(taskGroupId = groupId, desc = description)
            insertSubTask(subTask)
        }

        return groupId
    }

    /**
     * Deletes a task group and all its subtasks in a single transaction.
     *
     * Removes both the task group and its associated subtasks from the database.
     * The operation is atomic — if deletion fails, nothing is changed.
     *
     * @param taskGroupWithSubtasks The task group with its subtasks to delete
     *
     * @see deleteTaskGroup
     * @see deleteAllSubtasksForGroup
     */
    @Transaction
    suspend fun deleteTaskGroupWithSubtasks(taskGroupWithSubtasks: TaskGroupWithSubtasks){

        deleteTaskGroup(taskGroupWithSubtasks.taskGroup)
        deleteAllSubtasksForGroup(taskGroupWithSubtasks.taskGroup.id)

    }

    /**
     * Retrieves a task group with all its subtasks by the group ID.
     *
     * Room automatically loads the associated subtasks using the @Relation
     * annotation in TaskGroupWithSubtasks.
     *
     * @param groupId The ID of the task group to retrieve
     * @return The task group with its subtasks
     *
     * @throws NullPointerException if no task group with the given ID exists
     */
    @Transaction
    @Query("SELECT * FROM task_groups WHERE id = :groupId ")
    suspend fun getTaskGroupWithSubtasksByGroupId(groupId: Long): TaskGroupWithSubtasks

    /**
     * Retrieves all subtasks for a task group synchronously.
     *
     * This function returns a List (not Flow) specifically for use within
     * @Transaction blocks. Flow-based reading (getSubtasksByGroupId)
     * is asynchronous and would break transaction atomicity.
     *
     * @param groupId ID of the parent task group
     * @return List of all subtasks belonging to the group
     *
     * @see getSubtasksByGroupId (Flow-based version for UI observation)
     */
    @Transaction
    @Query("SELECT * FROM subtasks WHERE taskGroupId = :groupId")
    suspend fun getSubtasksSync(groupId: Long): List<SubTask>


    /**
     * Updates a subtask's completion status within a group task and
     * automatically checks and updates the completion status of the parent task group.
     * Reads all subtasks synchronously (in the same transaction).
     *
     * This function performs three operations in a single transaction:
     * 1. Updates the specified subtask's completion status
     * 2. Reads ALL subtasks in the group synchronously to check their completion status
     * 3. Updates the parent task group's completion status based on the check
     *
     * **Transaction Safety:**
     * All operations are wrapped in @Transaction, ensuring atomicity. If any operation
     * fails, the entire transaction is rolled back.
     *
     * **Business Logic:**
     * - The task group is marked as COMPLETED only when ALL its subtasks are completed
     * - Otherwise, the task group is marked as NOT COMPLETED
     *
     * @param taskGroupId ID of the parent task group
     * @param subtaskId ID of the subtask to update
     * @param isCompleted New completion status for the subtask
     *
     * @see updateSubTaskCompletion
     * @see getSubtasksSync
     * @see updateTaskGroupCompletion
     */
    @Transaction
    suspend fun updateSubTaskCompletionTaskGroupCompletion(taskGroupId: Long, subtaskId: Long, isCompleted: Boolean){
        updateSubTaskCompletion(taskGroupId, subtaskId, isCompleted)

        // Read ALL subtasks SYNCHRONOUSLY (in the same transaction)
        val subTaskList = getSubtasksSync(taskGroupId)
        val completed = subTaskList.all { it.isCompleted }

        updateTaskGroupCompletion(taskGroupId, completed)
    }

    /**
     * Updates a task group's completion status and optionally all its subtasks.
     *
     * When marking a task group as completed (isCompleted = true), all its subtasks
     * are automatically marked as completed in the same transaction.
     *
     * When marking a task group as incomplete (isCompleted = false), only the
     * group's status changes — subtasks retain their individual statuses.
     *
     * @param groupId ID of the task group to update
     * @param isCompleted New completion status for the task group
     *
     * @see updateTaskGroupCompletion
     * @see updateAllSubtasksCompletion
     */
    @Transaction
    suspend fun updateTaskGroupCompletionSubTaskCompletion(groupId: Long, isCompleted: Boolean){
        updateTaskGroupCompletion(groupId, isCompleted)
        if (isCompleted){
            updateAllSubtasksCompletion(groupId, true)
        }
    }

}




