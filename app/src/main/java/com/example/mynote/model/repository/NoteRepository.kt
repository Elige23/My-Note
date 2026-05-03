package com.example.mynote.model.repository

import androidx.lifecycle.LiveData
import com.example.mynote.model.room.Note
import com.example.mynote.model.room.NoteDao
import com.example.mynote.model.room.SingleTask
import com.example.mynote.model.room.SubTask
import com.example.mynote.model.room.TaskGroup
import com.example.mynote.model.room.TaskGroupWithSubtasks
import com.example.mynote.model.room.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Repository for managing notes and tasks data.
 *
 * Acts as a single source of truth between the ViewModel and the Room database.
 * Provides clean API for all data operations and encapsulates complex business logic.
 *
 * @param dao Data Access Object for database operations
 *
 * @see NoteDao
 * @see com.example.mynote.viewmodel.NoteViewModel
 */
class NoteRepository(private val dao: NoteDao) {

    //Note operations
    suspend fun insertNote(note: Note) = dao.insertNote(note)

    suspend fun updateNote(note: Note) = dao.updateNote(note)

    suspend fun deleteNote(note: Note) = dao.deleteNote(note)

    /**
     * Gets all notes into a single list, sorted by creation time in descending order (newest first).
     */
    fun getAllNotes(): LiveData<List<Note>> = dao.getAllNotes()

    fun searchNotes(search: String): LiveData<List<Note>> = dao.searchNotes("%$search%")

    suspend fun getEditNoteById(noteId: Int): Note? = dao.getEditNoteById(noteId)


    //Single tasks operations
    suspend fun insertSingleTask(singleTask: String) = dao.insertSingleTask(SingleTask(desc = singleTask))

    suspend fun updateSingleTask(singleTask: SingleTask) = dao.updateSingleTask(singleTask)

    suspend fun deleteSingleTask(singleTask: SingleTask) = dao.deleteSingleTask(singleTask)

    fun getAllSingleTasks(): Flow<List<SingleTask>> = dao.getAllSingleTasks()

    suspend fun updateSingleTaskCompletion(singleTaskId: Long, isCompleted: Boolean) =
        dao.updateSingleTaskCompletion(singleTaskId, isCompleted)

    suspend fun getSingleTaskById(singleTaskId: Long): SingleTask =
        dao.getSingleTaskById(singleTaskId)


    //Task groups operations
    suspend fun updateTaskGroup(taskGroup: TaskGroup) = dao.updateTaskGroup(taskGroup)



    //SubTasks operations
    suspend fun deleteSubTaskById(subtaskId: Long, groupId: Long) = dao.deleteSubTaskById(subtaskId, groupId)

    fun getSubtasksByGroupId(groupId: Long): Flow<List<SubTask>> = dao.getSubtasksByGroupId(groupId)

    /**
     * Updates only subtasks of the entire task group.
     * After editing a task group in the TaskEditBottomSheetDialog,
     *  it checks all subtasks of the group task to add new subtasks to the database and
     *  delete deleted subtasks from the database.
     */
    suspend fun updateAllSubtasksForGroupId(
        groupId: Long,
        newListSubtasks: List<SubTask>,
        deletedIdList: List<Long>
    ) = dao.updateAllSubtasksForGroupId(groupId, newListSubtasks, deletedIdList)


    // TaskGroupWithSubtasks
    fun getAllTaskGroupsWithSubtasks(): Flow<List<TaskGroupWithSubtasks>> =
        dao.getAllTaskGroupsWithSubtasks()


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
     */
    suspend fun insertTaskGroupWithSubtasks(groupDesc: String, subtaskDesc: List<String>): Long =
        dao.insertTaskGroupWithSubtasks(groupDesc, subtaskDesc)

    /**
     * Checks the completion status of all subtasks of a group task.
     * If all subtasks are completed, the group task is automatically
     * marked as completed.
     */
    suspend fun checkAllSubTaskAndUpdateTaskGroupCompletion(groupId: Long) {
        val subTaskList = getSubtasksByGroupId(groupId).first()
        val completed = subTaskList.all { it.isCompleted }
        if (completed) {
            dao.updateTaskGroupCompletion(groupId, true)
        } else {
            dao.updateTaskGroupCompletion(groupId, false)
        }

    }

    /**
     * Comprehensively updates the entire task group and its subtasks.
     * After editing the task group in the TaskEditBottomSheetDialog,
     * checks all the group task's subtasks to add new subtasks to the database and
     * delete deleted subtasks from the database.
     */
    suspend fun updateTaskGroupWithSubtasks(
        taskGroupWithSubtasks: TaskGroupWithSubtasks,
        deletedIdList: List<Long>
    ) {
        updateTaskGroup(taskGroupWithSubtasks.taskGroup)
        updateAllSubtasksForGroupId(
            taskGroupWithSubtasks.taskGroup.id,
            taskGroupWithSubtasks.subtasks,
            deletedIdList
        )

    }

    /**
     * Deletes a task group and all its subtasks in a single transaction.
     *
     * Removes both the task group and its associated subtasks from the database.
     * The operation is atomic — if deletion fails, nothing is changed.
     *
     * @param taskGroupWithSubtasks The task group with its subtasks to delete
     */
    suspend fun deleteTaskGroupWithSubtasks(taskGroupWithSubtasks: TaskGroupWithSubtasks) =
        dao.deleteTaskGroupWithSubtasks(taskGroupWithSubtasks)

    /**
     * Retrieves a task group with all its subtasks by the group ID.
     *
     * Room automatically loads the associated subtasks using the @Relation
     * annotation in TaskGroupWithSubtasks.
     *
     * @param groupId The ID of the task group to retrieve
     * @return The task group with its subtasks
     */
    suspend fun getTaskGroupWithSubtasksByGroupId(groupId: Long): TaskGroupWithSubtasks =
        dao.getTaskGroupWithSubtasksByGroupId(groupId)

    /**
     * Updates the completion status of a subtask within a group task.
     * Reads all subtasks synchronously (in the same transaction) and updates the task group's completion status
     * based on the statuses of all its subtasks.
     */
    suspend fun updateSubTaskCompletionTaskGroupCompletion(
        taskGroupId: Long,
        subtaskId: Long,
        isCompleted: Boolean
    ) = dao.updateSubTaskCompletionTaskGroupCompletion(taskGroupId, subtaskId, isCompleted)

    /**
     * Updates the completion status of a task group.
     * If the completion status is true, then all subtasks are considered completed in a single transaction.
     */
    suspend fun updateTaskGroupCompletionSubTaskCompletion(groupId: Long, isCompleted: Boolean) =
        dao.updateTaskGroupCompletionSubTaskCompletion(groupId, isCompleted)


    /**
     * Gets all tasks (all single tasks and all group tasks with subtask) combined into a single list,
     * sorted by the creation time of the single task or group of tasks in descending order.
     * Subtasks are sorted first by completion status, then by createdAt in descending order (from newest to oldest).
     *
     * @return Flow emitting the combined list of all tasks
     */
    fun getAllTaskItems(): Flow<List<TaskItem>> {
        return combine(
            getAllSingleTasks(),
            getAllTaskGroupsWithSubtasks()
        ) { singleTasks, taskGroupsWithSubtasks ->

            // Casting elements to the TaskItem type
            val singleItems =
                singleTasks.map { TaskItem.SingleTaskItem(it) }

            val groupItems = taskGroupsWithSubtasks.map {

                val newOrderSubtasks = it.subtasks.sortedWith(
                    compareBy<SubTask> { it.isCompleted }
                        .thenByDescending { it.createdAt }
                )

                TaskItem.TaskGroupWithSubtasksItem(it.copy(subtasks = newOrderSubtasks))
            }

            (singleItems + groupItems).sortedByDescending { it.createdAt }
        }
    }


}