package com.example.mynote.view.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.mynote.R
import com.example.mynote.model.room.SubTask
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog fragment for confirming subtask deletion.
 *
 * Displays a confirmation dialog with the subtask description and options to delete or cancel.
 * When confirmed, sends a result via FragmentResult API to the parent fragment.
 *
 * @see SubTask
 * @see androidx.fragment.app.FragmentResultListener
 */
class DeleteSubtaskConfirmationDialog: DialogFragment() {

    companion object{
        const val BUNDLE_STRING_KEY = "subtask_desc"
        const val BUNDLE_ID_KEY = "subtask_id"
        const val BUNDLE_SUBTASK_GROUP_ID_KEY = "subtask_group_ID"
        const val DELETE_DIALOG_TAG = "delete_dialog"
        const val DELETE_SUBTASK_DIALOG_REQUEST = "delete_subtask"
        const val DELETE_ACTION = "delete_action"
        const val DELETE_SUBTASK_ID = "delete_ID"
        const val DELETE_SUBTASK_GROUP_ID = "delete_subtask_group_ID"

        /**
         * Shows the delete confirmation dialog for a subtask.
         *
         * @param fragmentManager FragmentManager to show the dialog
         * @param subtask The subtask to delete (its description is shown in the dialog)
         *
         * @see DeleteSubtaskConfirmationDialog
         */
        fun showDeleteSubtaskConfirmationDialog(fragmentManager: FragmentManager, subtask: SubTask){
            DeleteSubtaskConfirmationDialog().apply {

                arguments = Bundle().apply {
                    putString(BUNDLE_STRING_KEY, subtask.desc)
                    putLong(BUNDLE_ID_KEY, subtask.id)
                    putLong(BUNDLE_SUBTASK_GROUP_ID_KEY, subtask.taskGroupId)
                }
            }.show(fragmentManager, DELETE_DIALOG_TAG)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val subtaskTitle = requireArguments().getString(BUNDLE_STRING_KEY) ?: ""
        val subtaskId = requireArguments().getLong(BUNDLE_ID_KEY, 0L)
        val groupId = requireArguments().getLong(BUNDLE_SUBTASK_GROUP_ID_KEY, 0L)

        return MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MyNote_MaterialAlertDialog)
            .setTitle(getString(R.string.dialog_title_delete_subtask_edit_bottom_sheet))
            .setMessage(
                getString(
                    R.string.dialog_message_delete_subtask_edit_bottom_sheet,
                    subtaskTitle
                ))
            .setPositiveButton(getString(R.string.dialog_action_delete_subtask_edit_bottom_sheet)){ dialog, _ ->

                val positiveBundle = Bundle().apply {
                    putString(DELETE_ACTION, "delete")
                    putLong(DELETE_SUBTASK_ID, subtaskId)
                    putLong(DELETE_SUBTASK_GROUP_ID, groupId)
                }
                parentFragmentManager.setFragmentResult(DELETE_SUBTASK_DIALOG_REQUEST, positiveBundle)
            }
            .setNegativeButton(getString(R.string.dialog_action_cancel_edit_bottom_sheet)){ dialog, which ->
                dialog.dismiss()
            }
            .create()
    }
}