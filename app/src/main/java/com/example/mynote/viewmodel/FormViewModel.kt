package com.example.mynote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynote.model.room.SingleTask
import com.example.mynote.model.room.SubTask
import com.example.mynote.model.room.TaskItem
import com.example.mynote.model.room.TaskItem.SingleTaskItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * ViewModel for managing form state across task entry and edit bottom sheets.
 *
 * This ViewModel handles two main use cases:
 * 1. **Task Entry** - Creating new tasks (single or group with subtasks)
 * 2. **Task Editing** - Editing existing tasks with change tracking
 *
 * Features:
 * - StateFlow for reactive UI updates
 * - Debounced sharing (5 seconds after last subscriber)
 * - Change detection for unsaved changes
 * - Temporary ID generation for new subtasks
 *
 * @see TaskItem
 * @see SubTask
 * @see com.example.mynote.view.fragments.bottomSheets.TaskEntryBottomSheetDialog
 * @see com.example.mynote.view.fragments.bottomSheets.TaskEditBottomSheetDialog
 */
class FormViewModel: ViewModel() {

    // Entry Task BottomSheetDialogFragment

    /**
     * Indicates whether the entry bottom sheet is currently open.
     * Used to reattach listeners after configuration changes.
     */
    private val _isOpenEntryBottomSheet = MutableStateFlow(false)
    val isOpenEntryBottomSheet: StateFlow<Boolean> = _isOpenEntryBottomSheet.asStateFlow()

    /**
     * Updates the open state of the entry bottom sheet.
     *
     * @param isOpen true when bottom sheet is open, false when closed
     */
    fun updateIsOpenEnterBottomSheet(isOpen: Boolean) {
       _isOpenEntryBottomSheet.value = isOpen
    }

    /**
     * UI state for the task entry dialog.
     *
     * @property isGroupMode True for group task mode (with subtasks)
     * @property groupTitle Title of the group task (used in group mode)
     * @property singleTitle Title of the single task (used in single mode)
     * @property subtasks List of subtask descriptions (used in group mode)
     */
    data class TaskDialogUiState(
        val isGroupMode: Boolean = false,
        val groupTitle: String = "",
        val singleTitle: String = "",
        val subtasks: List<String> = emptyList()
    )

    private val _taskDialogUiState = MutableStateFlow(TaskDialogUiState())
    val taskDialogUiState: StateFlow<TaskDialogUiState> = _taskDialogUiState.asStateFlow()

    // Derived state flows
    // Additional computed properties for convenience
    val isGroupMode: StateFlow<Boolean> = _taskDialogUiState
        .map { it.isGroupMode }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    val groupTitle: String
        get() = taskDialogUiState.value.groupTitle

    val singleTitle: String
        get() = taskDialogUiState.value.singleTitle

    val subtasks: StateFlow<List<String>> = _taskDialogUiState
        .map { it.subtasks }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )


    // Task Entry Methods for TaskEntryBottomSheetDialog
    fun updateGroupMode(isGroup: Boolean) {
        _taskDialogUiState.update {
            it.copy(isGroupMode = isGroup)
        }
    }

    fun updateGroupTitle(title: String) {
        _taskDialogUiState.update {
            it.copy(groupTitle = title)
        }
    }

    fun updateSingleTitle(title: String) {
        _taskDialogUiState.update {
            it.copy(singleTitle = title)
        }
    }

    /**
     * Adds a new empty subtask to the list.
     *
     * @param text Initial text for the subtask (default empty string)
     */
     fun addSubtask(text: String = "") {
         _taskDialogUiState.update { state ->
             state.copy(subtasks = state.subtasks + text)
         }
     }

    /**
     * Removes a subtask at the specified position.
     *
     * @param position Index of the subtask to remove
     */
    fun removeSubtask(position: Int) {
        _taskDialogUiState.update { state ->
            if (position !in state.subtasks.indices) return@update state
            val newList: List<String> = subtasks.value.toMutableList().apply { removeAt(position) }
            state.copy(
                subtasks = newList
            )
        }
    }

    /**
     * Updates a subtask at the specified position.
     *
     * @param position Index of the subtask to update
     * @param text New text for the subtask
     */
     fun updateEntrySubtask(position: Int, text: String) {
         _taskDialogUiState.update { state ->
             val newList = state.subtasks.toMutableList().apply {
                 if (position in indices) this[position] = text
             }
             state.copy(subtasks = newList)
         }
     }

    /**
     * Clears all subtasks from the list.
     */
    fun clearSubtasks() {
        _taskDialogUiState.update { state ->
            state.copy(subtasks = emptyList())
        }
    }

    /**
     * Resets the entire task entry state to default values.
     */
    fun clearState(){
        _taskDialogUiState.value = TaskDialogUiState()
    }


    // Task Edit Methods for TaskEditBottomSheetDialog

    /**
     * The task currently being edited.
     */
    private val _editTask = MutableStateFlow<TaskItem>(TaskItem.SingleTaskItem(SingleTask(desc = "")))
    val editTask: StateFlow<TaskItem> = _editTask.asStateFlow()

    /**
     * Snapshot of the original task before editing.
     * Used for change detection.
     */
    private var originalTask: TaskItem? = null

    /**
     * Flag indicating whether editing is in progress.
     * Prevents unnecessary UI updates during bulk operations.
     */
    var isEditing: Boolean = false

    /**
     * List of subtask IDs that have been deleted during editing.
     * Used to delete them from database when saving.
     */
    private var _deletedSubtasksIdList: MutableList<Long> = mutableListOf()
    val deletedSubtasksIdList: List<Long> get() = _deletedSubtasksIdList

    /**
     * Adds a subtask ID to the deleted list.
     *
     * @param id ID of the deleted subtask
     */
    fun addDeletedId(id: Long){
        _deletedSubtasksIdList.add(id)
    }

    /**
     * Starts editing a task by storing the original version.
     *
     * @param task The task to start editing
     */
    fun startEditing(task: TaskItem) {
        originalTask = task
        _editTask.value = task
    }

    /**
     * Updates the description of the task (both single task description
     * and group task description) being edited.
     *
     * @param newDesc New description text
     */
    fun updateDescTitle(newDesc: String){
        val currentTask = _editTask.value
        val updateTask =
            when(currentTask){
                is SingleTaskItem -> currentTask.copy( singleTask = currentTask.singleTask.copy(desc = newDesc, createdAt = System.currentTimeMillis()))
                is TaskItem.TaskGroupWithSubtasksItem -> currentTask.copy(taskGroupWithSubtasks = currentTask.taskGroupWithSubtasks.copy(taskGroup = currentTask.taskGroupWithSubtasks.taskGroup.copy(desc = newDesc, createdAt = System.currentTimeMillis())))
            }
        _editTask.value = updateTask
    }

    /**
     * Updates the completion status of the task (both single and group tasks) being edited.
     *
     * @param isChecked New completion status
     */
    fun editIsCompletedTitle(isChecked: Boolean){
        val currentTask = _editTask.value
        val updateTask =
            when(currentTask){
                is SingleTaskItem -> currentTask.copy( singleTask = currentTask.singleTask.copy(isCompleted = isChecked))
                is TaskItem.TaskGroupWithSubtasksItem -> currentTask.copy(taskGroupWithSubtasks = currentTask.taskGroupWithSubtasks.copy(taskGroup = currentTask.taskGroupWithSubtasks.taskGroup.copy(isCompleted = isChecked)))//, createdAt = System.currentTimeMillis())))
            }
        _editTask.value = updateTask
    }

    /**
     * Flow of subtasks for the task being edited.
     * Returns empty list for single tasks.
     */
    val subtasksEdit: StateFlow<List<SubTask>> = _editTask
        .map { it.subtasks }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /**
     * Updates a single subtask in the task being edited.
     *
     * @param newSubTask The updated subtask
     */
    fun updateSingleSubTaskFunctional(newSubTask: SubTask) {

        val currentTask = _editTask.value
        if (currentTask is TaskItem.TaskGroupWithSubtasksItem) {
            val updatedSubtasks = currentTask.taskGroupWithSubtasks.subtasks
                .map {
                    if (it.id == newSubTask.id) newSubTask else it
                }
            _editTask.value = currentTask.copy(
                taskGroupWithSubtasks = currentTask.taskGroupWithSubtasks.copy(
                    subtasks = updatedSubtasks
                )
            )
        }
    }

    // Methods for detecting changes during editing

    /**
     * Compares the edited task description with the original task description before editing.
     *
     * @return true if description has changed, false otherwise
     */
    fun compareDescEditTaskWithOriginalTask(): Boolean{
        return editTask.value.desc == originalTask?.desc
    }

    /**
     * Compares the edited subtasks of a group task with the original list of subtasks.
     *
     * @return true if subtasks have changed, false otherwise
     */
    fun compareSubtasksEditTaskWithOriginalTask(): Boolean{
        return editTask.value.subtasks == originalTask?.subtasks
    }

    /**
     * Compares the edited task completion status (both single and group tasks) with the original.
     *
     * @return true if completion status has changed, false otherwise
     */
    fun compareIsCompletedEditTaskWithOriginalTask(): Boolean{
        return editTask.value.isCompleted == originalTask?.isCompleted
    }

    /**
    * Compares the entire edited task with the original.
    *
    * @return true if task has any changes, false if identical
    */
    fun compareEditTaskWithOriginalTask(): Boolean{
        return editTask.value == originalTask
    }


    // Managing temporary ID

    /**
     * Counter for generating temporary negative IDs for new subtasks.
     * Negative IDs distinguish newly created (unsaved) subtasks from existing ones.
     */
    private var tempIdCounter = -1L

    /**
     * Adds an empty subtask to the task being edited.
     *
     * Uses a temporary negative ID that will be replaced with a real database ID
     * when the task is saved.
     */
    fun addEmptySubtask(){
        val newEmptySubtask = SubTask( id = tempIdCounter, desc = "", taskGroupId = editTask.value.id)
        tempIdCounter --

        _editTask.update { task ->
            when(task) {
                is TaskItem.SingleTaskItem -> task
                is TaskItem.TaskGroupWithSubtasksItem -> {
                    val newList =  listOf(newEmptySubtask) + task.subtasks
                    task.copy(taskGroupWithSubtasks = task.taskGroupWithSubtasks.copy(subtasks = newList))
                }
            }
        }
    }

    /**
    * Deletes a subtask by ID from the task being edited.
    *
    * @param subtaskId ID of the subtask to delete
    */
    fun deleteSubtaskById(subtaskId: Long){

        addDeletedId(subtaskId)
        _editTask.update { taskItem ->
            when(taskItem){
                is TaskItem.SingleTaskItem ->{ taskItem }
                is TaskItem.TaskGroupWithSubtasksItem -> {
                    val position = taskItem.subtasks.indexOfFirst { it.id == subtaskId }

                    if (position != -1) {
                        val newList = taskItem.subtasks.toMutableList().apply { removeAt(position) }
                        taskItem.copy(taskGroupWithSubtasks = taskItem.taskGroupWithSubtasks.copy(subtasks = newList))
                    }
                    else {
                        taskItem
                    }
                }
            }
        }
    }

    /**
     * Clears all task editing data.
     * Resets state to default values.
     */
    fun clearTaskData(){
        _editTask.value = TaskItem.SingleTaskItem(SingleTask(desc = ""))
        originalTask = null
        tempIdCounter = -1L
        _deletedSubtasksIdList = mutableListOf()
    }
}