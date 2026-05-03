package com.example.mynote.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynote.model.repository.NoteRepository
import com.example.mynote.model.room.Note
import com.example.mynote.model.room.SingleTask
import com.example.mynote.model.room.SubTask
import com.example.mynote.model.room.TaskGroup
import com.example.mynote.model.room.TaskGroupWithSubtasks
import com.example.mynote.model.room.TaskItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.Long

/**
 * ViewModel for managing notes and tasks data.
 *
 * This ViewModel handles three main data domains:
 * 1. **Notes** - CRUD operations with search functionality
 * 2. **Tasks** - Single tasks and group tasks with subtasks
 * 3. **Edit operations** - Tracking changes during note editing
 *
 * Features:
 * - LiveData for reactive UI updates for notes
 * - MediatorLiveData for switching between all notes and search results fot notes
 * - StateFlow for tasks (with WhileSubscribed sharing)
 * - Coroutine support for database operations
 *
 * @param repository Data source for notes and tasks
 *
 * @see NoteRepository
 * @see TaskItem
 */
class NoteViewModel( private val repository: NoteRepository): ViewModel() {

    //Note operations

    /** All notes from the database */
    val allNotes: LiveData<List<Note>> =  repository.getAllNotes()

    /** Snackbar event for displaying deleted note with undo option */
    private var _snackbarNote = MutableLiveData<Note?>()
    val snackbarNote: LiveData<Note?>  = _snackbarNote

    /** Search results */
    private var _searchNotes = MutableLiveData<List<Note>>()
    val searchNotes: LiveData<List<Note>>  = _searchNotes

    /** Current search query */
    private var _searchQuery = MutableLiveData("")

    /**
     * MediatorLiveData that switches between allNotes and searchNotes
     * based on whether a search query is active.
     */
    private var _noteAllOrSearch = MediatorLiveData<List<Note>>()
    val noteAllOrSearch : LiveData<List<Note>> get()= _noteAllOrSearch

    // Search state management
    private var currentSearchLiveData: LiveData<List<Note>>? = null
    private var searchObserver: Observer<List<Note>>? = null
    private var searchQueryObserver: Observer<String>? = null

    // For NoteEditFragment
    private var _editNote = MutableLiveData<Note?>()
    val editNote: LiveData<Note?> = _editNote
    private var originalNote: Note? = null

    //Note operations
    init {
        setupMediatorLiveData()
        observeSearchQueryChanges()
    }

    override fun onCleared() {
        // Clean up search query observer
        searchQueryObserver?.let { _searchQuery.removeObserver(it) }

        // Clean up search observer
        cleanupPreviousSearch()

        super.onCleared()
    }


    /**
     * Sets up MediatorLiveData to switch between all notes and search results.
     *
     * When search query is empty: shows allNotes
     * When search query is not empty: shows searchNotes
     */
    fun setupMediatorLiveData() {
        _noteAllOrSearch.addSource(allNotes){ notes ->
            if (_searchQuery.value.isNullOrBlank()) {
                _noteAllOrSearch.value = notes
            }
        }
        _noteAllOrSearch.addSource(searchNotes){ notes ->
            if (!_searchQuery.value.isNullOrBlank()) {
                _noteAllOrSearch.value = notes
            }
        }
    }

    /**
     * Observes search query changes and triggers search when query is not empty.
     */
    private fun observeSearchQueryChanges() {
        // Remove previous observer to avoid duplicates
        searchQueryObserver?.let {
            _searchQuery.removeObserver(it)
        }

        // Create and store new observer
        searchQueryObserver = Observer { query ->
            if (query.isNotBlank()) {
                performSearch(query)
            } else {
                // Clear search results when search is empty
                _searchNotes.value = emptyList()
                // Refresh from allNotes (in case allNotes changed during search)
                _noteAllOrSearch.value = allNotes.value ?: emptyList()
            }
        }
        _searchQuery.observeForever(searchQueryObserver!!)
    }

    /**
     * Executes a search for the given query.
     *
     * @param query The search string
     */
    private fun performSearch(query: String) {

        // Remove previous search observer
        cleanupPreviousSearch()

        // Get new search LiveData
        val searchLiveData = repository.searchNotes(query)
        currentSearchLiveData = searchLiveData

        // Create and store new observer
        searchObserver = Observer { results ->
            _searchNotes.value = results
        }

        // Add observer
        searchLiveData.observeForever(searchObserver!!)
    }

    /**
     * Cleans up resources from the previous search.
     * Removes observers to prevent memory leaks.
     */
    private fun cleanupPreviousSearch() {
        // Remove observer from previous search LiveData
        searchObserver?.let { observer ->
            currentSearchLiveData?.removeObserver(observer)
        }
        searchObserver = null
        currentSearchLiveData = null
    }

    /**
     * Sets the current search query.
     *
     * @param query The search string
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }


    // Note CRUD
    fun insertNote(note: Note) = viewModelScope.launch {
        repository.insertNote(note)
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        repository.updateNote(note)
    }

    /**
     * Deletes a note and shows a snackbar with undo option.
     *
     * @param note The note to delete
     */
    fun deleteNote(note: Note) = viewModelScope.launch {
        repository.deleteNote(note)
        withContext(Dispatchers.Main){
            setSnackbarNote(note)
        }
    }

    /**
     * Sets the note for snackbar display.
     *
     * @param note The deleted note (for undo restoration)
     */
    fun setSnackbarNote( note: Note? ){
        _snackbarNote.value = note
    }


    // Note Edit Methods For NoteEditFragment

    /**
     * Returns the original note being edited.
     *
     * @return Original note or null if not editing
     */
    fun getOriginalNote(): Note? = originalNote

    /**
     * Sets the original note for change detection.
     *
     * @param note The original note
     */
    fun setOriginalNote(note: Note){
        originalNote = note
    }

    /**
     * Sets the note being edited.
     *
     * @param note The note to edit
     */
    fun setEditNote(note: Note){
        _editNote.value = note
    }

    /**
     * Loads a note by ID for editing.
     * Sets the original note for change detection.
     * Sets the note being edited.
     *
     * @param noteId ID of the note to edit
     */
    suspend fun getEditNoteById(noteId: Int){
        val originalNote = repository.getEditNoteById(noteId)?: Note(title = "", desc = "")
        setOriginalNote(originalNote)
        setEditNote(originalNote)
    }

    fun updateEditTitle(newTitle: String){
        val newNote = _editNote.value?.copy(title = newTitle)
        _editNote.value = newNote
    }

    fun updateEditDesc(newDesc: String){
        val newNote = _editNote.value?.copy(desc = newDesc)
        _editNote.postValue(newNote)
    }



    // Tasks operations

    /**
     * All tasks (single and grouped) as StateFlow.
     * WhileSubscribed(5000) keeps the flow active for 5 seconds after last subscriber.
     */
    private val _allTasks : StateFlow<List<TaskItem>> = repository.getAllTaskItems().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val allTasks: StateFlow<List<TaskItem>> = _allTasks


    // SingleTask operations
    fun insertSingleTask (singleTask: String) = viewModelScope.launch {
        repository.insertSingleTask(singleTask)
    }

    fun updateSingleTask(singleTask: SingleTask) = viewModelScope.launch {
        repository.updateSingleTask(singleTask)
    }

    fun deleteSingleTask(singleTask: SingleTask) = viewModelScope.launch {
        repository.deleteSingleTask(singleTask)
    }

    fun updateSingleTaskCompletion(singleTaskId: Long, isCompleted: Boolean) = viewModelScope.launch {
        repository.updateSingleTaskCompletion(singleTaskId, isCompleted)
    }

    suspend fun getSingleTaskById(singleTaskId: Long): SingleTask = repository.getSingleTaskById(singleTaskId)



    // GroupTask operations
    fun updateTaskGroup(taskGroup: TaskGroup) = viewModelScope.launch {
        repository.updateTaskGroup(taskGroup)
    }



    // SubTasks operations
    fun deleteSubTaskById(subTaskId: Long, groupId: Long) = viewModelScope.launch {
        repository.deleteSubTaskById(subTaskId, groupId)
    }

    /**
     * Updates the completion status of a subtask within a group task.
     * Reads all subtasks synchronously (in the same transaction) and updates the task group's completion status
     * based on the statuses of all its subtasks.
     */
    fun updateSubTaskCompletionTaskGroupCompletion(groupId: Long, subtaskId: Long, isCompleted: Boolean) = viewModelScope.launch {
        repository.updateSubTaskCompletionTaskGroupCompletion(groupId,subtaskId,isCompleted)
    }

    /**
     * Updates the completion status of a task group.
     * If the completion status is true, then all subtasks are considered completed in a single transaction.
     */
    fun updateTaskGroupCompletionSubTaskCompletion(groupId: Long, isCompleted: Boolean)= viewModelScope.launch {
        repository.updateTaskGroupCompletionSubTaskCompletion(groupId, isCompleted)
    }


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
    fun insertTaskGroupWithSubtasks(groupDesc: String, subtaskDesc: List<String>) = viewModelScope.launch {
        repository.insertTaskGroupWithSubtasks(groupDesc, subtaskDesc)
    }

    /**
     * Checks the completion status of all subtasks of a group task.
     * If all subtasks are completed, the group task is automatically
     * marked as completed.
     *
     * @param groupId ID of the group task
     */
    fun checkAllSubTaskAndUpdateTaskGroupCompletion(groupId: Long) = viewModelScope.launch {
        repository.checkAllSubTaskAndUpdateTaskGroupCompletion(groupId)

    }

    /**
     * Comprehensively updates the entire task group and its subtasks.
     * After editing the task group in the TaskEditBottomSheetDialog,
     * checks all the group task's subtasks to add new subtasks to the database and
     * delete deleted subtasks from the database.
     *
     * @param taskGroupWithSubtasks The complete group with subtasks after editing
     * @param deletedIdList IDs of subtasks that were deleted
     */
    fun updateTaskGroupWithSubtasks(taskGroupWithSubtasks: TaskGroupWithSubtasks, deletedIdList:  List<Long>) = viewModelScope.launch{
        repository.updateTaskGroupWithSubtasks(taskGroupWithSubtasks, deletedIdList)
    }

    /**
     *  Updates only subtasks of the entire task group.
     *  After editing a task group in the TaskEditBottomSheetDialog,
     *  it checks all subtasks of the group task to add new subtasks to the database and
     *  delete deleted subtasks from the database.
     *
     * @param groupId ID of the group
     * @param newListSubtasks The complete list of subtasks
     * @param deletedIdList IDs of subtasks to delete
     */
    fun updateAllSubtasksForGroupId(groupId: Long, newListSubtasks :  List<SubTask>, deletedIdList: List<Long>)= viewModelScope.launch{
        repository.updateAllSubtasksForGroupId(groupId, newListSubtasks, deletedIdList)
    }

    /**
     * Deletes a task group and all its subtasks in a single transaction.
     *
     * Removes both the task group and its associated subtasks from the database.
     * The operation is atomic — if deletion fails, nothing is changed.
     *
     * @param taskGroupWithSubtasks The task group with its subtasks to delete
     */
    fun deleteTaskGroupWithSubtasks(taskGroupWithSubtasks: TaskGroupWithSubtasks) = viewModelScope.launch {
        repository.deleteTaskGroupWithSubtasks(taskGroupWithSubtasks)
    }

    /**
     * Retrieves a task group with all its subtasks by the group ID.
     *
     * Room automatically loads the associated subtasks using the @Relation
     * annotation in TaskGroupWithSubtasks.
     *
     * @param groupId The ID of the task group to retrieve
     * @return The task group with its subtasks
     */
    suspend fun getTaskGroupWithSubtasksbyGroupId(groupId: Long): TaskGroupWithSubtasks {
        return repository.getTaskGroupWithSubtasksByGroupId(groupId)
    }
}