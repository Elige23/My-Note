package com.example.mynote.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mynote.databinding.FragmentTasksBinding
import com.example.mynote.model.repository.NoteApplicationRepository
import com.example.mynote.model.room.TaskItem
import com.example.mynote.view.adapter.TasksAdapter
import com.example.mynote.view.fragments.dialogs.DeleteSubtaskConfirmationDialog
import com.example.mynote.view.fragments.dialogs.DeleteTaskConfirmationDialog
import com.example.mynote.view.fragments.bottomSheets.TaskEditBottomSheetDialog
import com.example.mynote.view.fragments.bottomSheets.TaskEntryBottomSheetDialog
import com.example.mynote.viewmodel.FormViewModel
import com.example.mynote.viewmodel.NoteViewModel
import com.example.mynote.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.launch
import kotlin.getValue

/**
 * Fragment for displaying and managing tasks (both single and grouped).
 *
 * Features:
 * - Display tasks in a RecyclerView with two types: single tasks and group tasks with subtasks
 * - Add new tasks via bottom sheet dialog
 * - Edit existing tasks via bottom sheet dialog
 * - Delete tasks with confirmation dialog
 * - Delete subtasks with a confirmation dialog
 * - Mark tasks as completed in real time (single tasks, group tasks, or individual subtasks)
 * - Automatic scroll to top after adding new task
 *
 * @see TasksAdapter
 * @see TaskEntryBottomSheetDialog
 * @see TaskEditBottomSheetDialog
 */
class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private val repository by lazy {
        (requireActivity().application as NoteApplicationRepository).noteRepository
    }
    private val noteViewModel: NoteViewModel by viewModels { NoteViewModelFactory(repository) }
    private val formViewModel: FormViewModel by viewModels()
    private lateinit var tasksAdapter: TasksAdapter
    private var shouldScrollToTop = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpRecyclerView()
        setupObservers()
        setupBottomSheetListenerObserver()
        setUpFragmentResultListener()

        binding.fabAddTask.setOnClickListener {
            showTaskEntryBottomSheet()
            shouldScrollToTop = true
        }

        //Block the automatic keyboard for the entire fragment
        requireActivity().window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )
    }

    override fun onResume() {
        super.onResume()

        // Restore normal behavior for the keyboard
        requireActivity().window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
    }

    override fun onDestroyView() {
        binding.homeTaskRecyclerView.adapter = null

        val bottomSheet = childFragmentManager.findFragmentByTag(
            TaskEntryBottomSheetDialog.TAG
        ) as? TaskEntryBottomSheetDialog

        bottomSheet?.setOnTaskSavedListener(null)
        _binding = null
        super.onDestroyView()
    }

    private fun setUpRecyclerView(){
        tasksAdapter = TasksAdapter(
            onSingleTaskChecked = { id, isChecked ->
                noteViewModel.updateSingleTaskCompletion(id, isChecked)
                true
            },
            onSubTaskChecked = { groupId, subTaskId, isChecked ->
                noteViewModel.updateSubTaskCompletionTaskGroupCompletion(groupId,subTaskId,isChecked)
                true
            },
            onGroupTaskChecked = {groupId, isChecked ->
                noteViewModel.updateTaskGroupCompletionSubTaskCompletion(groupId, isChecked)
                true
            },
            onTaskGroupDelete = {taskGroupWithSubtasks ->
                val taskItem = TaskItem.TaskGroupWithSubtasksItem(taskGroupWithSubtasks)
                DeleteTaskConfirmationDialog.showDeleteTaskConfirmationDialog(childFragmentManager, taskItem)
            },
            onSingleTaskDelete = {singleTask ->
                val taskItem = TaskItem.SingleTaskItem(singleTask)
                DeleteTaskConfirmationDialog.showDeleteTaskConfirmationDialog(childFragmentManager, taskItem)
            },
            onSubTaskDelete = {subTask ->
                DeleteSubtaskConfirmationDialog.showDeleteSubtaskConfirmationDialog(childFragmentManager, subTask)
            },
            onTaskClicked = {task ->
                formViewModel.startEditing(task)
                showTaskEditBottomSheet()
                true
            }
        )

        binding.homeTaskRecyclerView.apply {
            adapter = tasksAdapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            setHasFixedSize(true)
        }
    }

    /**
     * Observes the list of all tasks and updates the adapter.
     * Handles automatic scroll to top when a new task is added.
     */
    private fun setupObservers(){

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                noteViewModel.allTasks
                    .collect { tasks ->
                    tasksAdapter.submitList(tasks)

                    if (shouldScrollToTop) {
                        binding.homeTaskRecyclerView.post {
                            binding.homeTaskRecyclerView.smoothScrollToPosition(0 )
                        }
                        shouldScrollToTop = false
                    }
                }
            }
        }
    }

    /**
     * Monitors the EntryBottomSheet open state and reattaches the save listener
     * after configuration changes (e.g., screen rotation).
     *
     * This is necessary because the bottom sheet is automatically restored by the system
     * via InstantState without re-calling showDialog().
     */
    private fun setupBottomSheetListenerObserver() {

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                formViewModel.isOpenEntryBottomSheet.collect { isOpen ->

                    if (isOpen) {
                        // Find BottomSheet in childFragmentManager
                        val bottomSheet = childFragmentManager.findFragmentByTag(
                            TaskEntryBottomSheetDialog.TAG
                        ) as? TaskEntryBottomSheetDialog

                        if (bottomSheet != null) {

                            bottomSheet.setOnTaskSavedListener { single, group, subtasks ->
                                if (single.isNotEmpty()) {
                                    noteViewModel.insertSingleTask(single)
                                } else if (group.isNotEmpty()) {
                                    noteViewModel.insertTaskGroupWithSubtasks(group, subtasks)
                                }
                            }
                            shouldScrollToTop = true
                        } else {
                            formViewModel.clearState()
                            binding.root.clearFocus()
                        }
                    }
                }
            }
        }
    }

    private fun showTaskEntryBottomSheet() {
        val bottomSheet = TaskEntryBottomSheetDialog.newInstance()

        bottomSheet.setOnTaskSavedListener { singleTaskDesc, groupTaskDesc, subtasks ->

            if (singleTaskDesc.isNotEmpty()){
                noteViewModel.insertSingleTask(singleTaskDesc)
            } else {
                noteViewModel.insertTaskGroupWithSubtasks(groupTaskDesc, subtasks)
            }
        }
        bottomSheet.show(childFragmentManager, TaskEntryBottomSheetDialog.TAG)
        formViewModel.updateIsOpenEnterBottomSheet(true)
    }

    private fun showTaskEditBottomSheet(){

        val bottomSheet = TaskEditBottomSheetDialog.newInstance()
        bottomSheet.show(childFragmentManager,TaskEditBottomSheetDialog.TAG)
    }

    /**
     * Sets up fragment result listeners for various dialogs:
     * - TaskEditBottomSheetDialog (save/delete edited task)
     * - DeleteTaskConfirmationDialog (confirm task deletion)
     * - DeleteSubtaskConfirmationDialog (confirm subtask deletion)
     */
    private fun setUpFragmentResultListener(){

        childFragmentManager.setFragmentResultListener(TaskEditBottomSheetDialog.REQUEST_KEY, viewLifecycleOwner ){
            requestKey, bundle ->
            when(bundle.getString(TaskEditBottomSheetDialog.ACTION_KEY)){
                TaskEditBottomSheetDialog.SAVE_KEY -> {
                    val saveTask = formViewModel.editTask.value
                    val deletedTaskIdList = formViewModel.deletedSubtasksIdList
                    when(saveTask){
                        is TaskItem.SingleTaskItem ->{
                            val compareDesc = formViewModel.compareDescEditTaskWithOriginalTask()
                            val compareIsCompleted = formViewModel.compareIsCompletedEditTaskWithOriginalTask()
                            if (!compareDesc || !compareIsCompleted){
                                noteViewModel.updateSingleTask(saveTask.singleTask)
                                formViewModel.clearTaskData()
                                shouldScrollToTop = true
                            } else {
                                formViewModel.clearTaskData()
                            }
                        }
                        is TaskItem.TaskGroupWithSubtasksItem -> {
                            val compareDesc = formViewModel.compareDescEditTaskWithOriginalTask()
                            val compareSubtasks = formViewModel.compareSubtasksEditTaskWithOriginalTask()
                            val compareIsCompleted = formViewModel.compareIsCompletedEditTaskWithOriginalTask()
                            when(compareIsCompleted) {
                                true -> {
                                    if (!compareDesc && compareSubtasks) {
                                        noteViewModel.updateTaskGroup(saveTask.taskGroupWithSubtasks.taskGroup)
                                        formViewModel.clearTaskData()
                                        shouldScrollToTop = true
                                    } else if (compareDesc && !compareSubtasks) {
                                        noteViewModel.updateAllSubtasksForGroupId(
                                            saveTask.taskGroupWithSubtasks.taskGroup.id,
                                            saveTask.taskGroupWithSubtasks.subtasks,
                                            deletedTaskIdList
                                        )
                                        shouldScrollToTop = false
                                        formViewModel.clearTaskData()
                                    } else if (!compareDesc && !compareSubtasks) {
                                        noteViewModel.updateTaskGroupWithSubtasks(saveTask.taskGroupWithSubtasks, deletedTaskIdList)
                                        shouldScrollToTop = true
                                        formViewModel.clearTaskData()
                                    } else if (compareDesc && compareSubtasks) {
                                        noteViewModel.updateTaskGroupWithSubtasks(saveTask.taskGroupWithSubtasks, deletedTaskIdList)
                                        shouldScrollToTop = true
                                        formViewModel.clearTaskData()
                                    }
                                    else {
                                        formViewModel.clearTaskData()
                                    }
                                }
                                false -> {
                                    if (!compareDesc && compareSubtasks) {
                                        noteViewModel.updateTaskGroup(saveTask.taskGroupWithSubtasks.taskGroup)
                                        shouldScrollToTop = true
                                        formViewModel.clearTaskData()
                                    } else if (compareDesc && !compareSubtasks) {
                                        noteViewModel.updateAllSubtasksForGroupId(
                                            saveTask.taskGroupWithSubtasks.taskGroup.id,
                                            saveTask.taskGroupWithSubtasks.subtasks,
                                            deletedTaskIdList
                                        )
                                        shouldScrollToTop = false
                                        formViewModel.clearTaskData()
                                    } else if (!compareDesc && !compareSubtasks) {
                                        noteViewModel.updateTaskGroupWithSubtasks(saveTask.taskGroupWithSubtasks, deletedTaskIdList)
                                        shouldScrollToTop = true
                                        formViewModel.clearTaskData()
                                    } else if (compareDesc && compareSubtasks) {
                                        noteViewModel.updateTaskGroupWithSubtasks(saveTask.taskGroupWithSubtasks, deletedTaskIdList)
                                        shouldScrollToTop = true
                                        formViewModel.clearTaskData()
                                    }
                                    else {
                                        formViewModel.clearTaskData()
                                    }
                                }
                            }
                        }
                    }
                }

                TaskEditBottomSheetDialog.DELETE_KEY -> {
                    formViewModel.clearTaskData()
                    formViewModel.isEditing = false
                }
            }
        }

        childFragmentManager.setFragmentResultListener(DeleteTaskConfirmationDialog.DELETE_TASK_DIALOG_REQUEST,
            viewLifecycleOwner){ requestKey, bundle ->

            when(bundle.getString(DeleteTaskConfirmationDialog.DELETE_TASK_ACTION)){
                "delete" -> {
                    val id = bundle.getLong(DeleteTaskConfirmationDialog.BUNDLE_ID_KEY)
                    val isGroupMode = bundle.getBoolean(DeleteTaskConfirmationDialog.BUNDLE_GROUP_KEY)

                    if (isGroupMode){
                        viewLifecycleOwner.lifecycleScope.launch {
                            val taskGroupWithSubtasks = noteViewModel.getTaskGroupWithSubtasksbyGroupId(id)
                            noteViewModel.deleteTaskGroupWithSubtasks(taskGroupWithSubtasks)
                        }
                    } else {
                        viewLifecycleOwner.lifecycleScope.launch {
                            val singleTask = noteViewModel.getSingleTaskById(id)
                            noteViewModel.deleteSingleTask(singleTask)
                        }
                    }
                }
            }
        }

        childFragmentManager.setFragmentResultListener(DeleteSubtaskConfirmationDialog.DELETE_SUBTASK_DIALOG_REQUEST,
            viewLifecycleOwner){
            requestKey, bundle ->
            when(bundle.getString(DeleteSubtaskConfirmationDialog.DELETE_ACTION)){
                "delete" ->{
                    val subtaskId = bundle.getLong(DeleteSubtaskConfirmationDialog.DELETE_SUBTASK_ID)
                    val groupId = bundle.getLong(DeleteSubtaskConfirmationDialog.DELETE_SUBTASK_GROUP_ID)

                    viewLifecycleOwner.lifecycleScope.launch {
                        noteViewModel.deleteSubTaskById(subtaskId, groupId).join()
                        noteViewModel.checkAllSubTaskAndUpdateTaskGroupCompletion(groupId)
                    }
                }
            }
        }
    }
}