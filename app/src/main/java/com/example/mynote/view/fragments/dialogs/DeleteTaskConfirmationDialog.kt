package com.example.mynote.view.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.mynote.R
import com.example.mynote.model.room.TaskItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog fragment for confirming task deletion (both single and group tasks).
 *
 * Displays a confirmation dialog with the task description and options to delete or cancel.
 * Handles both:
 * - SingleTaskItem: Standalone tasks without subtasks
 * - TaskGroupWithSubtasksItem: Group tasks with subtasks
 *
 * When confirmed, sends a result via FragmentResult API to the parent fragment.
 *
 * ```
 * // Show dialog
 * DeleteTaskConfirmationDialog.showDeleteTaskConfirmationDialog(
 *      childFragmentManager,
 *      taskItem
 * )
 *  ```
 */
class DeleteTaskConfirmationDialog: DialogFragment() {

    companion object{
        const val BUNDLE_TASK_DESC_KEY = "task_desc"
        const val BUNDLE_ID_KEY = "task_id"
        const val BUNDLE_GROUP_KEY = "task_group_mode"
        const val DELETE_DIALOG_TAG = "delete_task_dialog"
        const val DELETE_TASK_DIALOG_REQUEST = "delete_task"
        const val DELETE_TASK_ACTION = "delete_task_action"

        /**
         * Shows the delete confirmation dialog for a task (both single and group tasks).
         *
         * @param fragmentManager FragmentManager to show the dialog
         * @param task The task to delete (can be SingleTaskItem or TaskGroupWithSubtasksItem)
         *
         * @see DeleteTaskConfirmationDialog
         */
        fun showDeleteTaskConfirmationDialog(fragmentManager: FragmentManager, task: TaskItem){
            DeleteTaskConfirmationDialog().apply {

                arguments = Bundle().apply {
                    putString(BUNDLE_TASK_DESC_KEY, task.desc)
                    putLong(BUNDLE_ID_KEY, task.id)
                    if (task is TaskItem.TaskGroupWithSubtasksItem){
                        putBoolean(BUNDLE_GROUP_KEY, true)
                    } else
                    {
                        putBoolean(BUNDLE_GROUP_KEY, false)
                    }
                }
            }.show(fragmentManager, DELETE_DIALOG_TAG)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val taskDesc = arguments?.getString(BUNDLE_TASK_DESC_KEY)?: ""
        val taskId = arguments?.getLong(BUNDLE_ID_KEY)?: 0L
        val isGroupMode = arguments?.getBoolean(BUNDLE_GROUP_KEY) ?: false

        val message: String = if (isGroupMode){
            getString(R.string.dialog_title_delete_taskgroup_task_fragment, taskDesc)
        } else {
            getString(R.string.dialog_title_delete_single_task_task_fragment, taskDesc)
        }

        return MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MyNote_MaterialAlertDialog)
            .setTitle(R.string.dialog_title_delete_subtask_edit_bottom_sheet)
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_action_delete_task_fragment)){ dialog, _ ->

                val positiveBundle = Bundle().apply {
                    putString(DELETE_TASK_ACTION, "delete")
                    putLong(BUNDLE_ID_KEY, taskId)
                    putBoolean(BUNDLE_GROUP_KEY, isGroupMode)
                }
                parentFragmentManager.setFragmentResult(DELETE_TASK_DIALOG_REQUEST,positiveBundle)
            }
            .setNegativeButton(getString(R.string.dialog_action_cancel_task_fragment)){ dialog, _ ->
                dismiss()
            }
            .create()
    }
}