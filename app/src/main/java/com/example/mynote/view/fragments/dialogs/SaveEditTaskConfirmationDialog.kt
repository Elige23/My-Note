package com.example.mynote.view.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.mynote.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog fragment for confirming save/cancel actions for changes when editing a task.
 *
 * Displays a confirmation dialog when the user attempts to close the edit bottom sheet
 * without saving changes. Provides three options:
 * - **Save:** Save changes and close
 * - **Discard:** Discard changes and close
 * - **Cancel:** Return to editing without closing
 *
 */
class SaveEditTaskConfirmationDialog: DialogFragment() {
    companion object {
        const val SAVE_DIALOG_TAG = "save_edit_dialog"
        const val SAVE_DIALOG_REQUEST = "save_edit_task"
        const val SAVE_ACTION = "save_edit_action"

        /**
         * Shows the save changes confirmation dialog for task.
         *
         * @param fragmentManager FragmentManager to show the dialog
         *
         * @see SaveEditTaskConfirmationDialog
         */
        fun showSaveEditConfirmationDialog(fragmentManager: FragmentManager) {
            SaveEditTaskConfirmationDialog().show(fragmentManager, SAVE_DIALOG_TAG)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = MaterialAlertDialogBuilder(requireContext(),R.style.ThemeOverlay_MyNote_MaterialAlertDialog)
            .setTitle(getString(R.string.dialog_title_save_changes_task_edit_bottom_sheet))
            .setPositiveButton(getString(R.string.dialog_action_save_changes_task_edit_bottom_sheet)) { dialog, _ ->

                val positiveBundle = Bundle().apply {
                    putString(SAVE_ACTION, "save_changes")
                }
                parentFragmentManager.setFragmentResult(
                    SAVE_DIALOG_REQUEST,
                    positiveBundle
                )
            }
                .setNegativeButton(getString(R.string.dialog_action_discard_changes_task_edit_bottom_sheet)) { dialog, which ->
                    val negativeBundle = Bundle().apply {
                        putString(SAVE_ACTION, "delete_changes")
                    }
                    parentFragmentManager.setFragmentResult(
                        SAVE_DIALOG_REQUEST,
                        negativeBundle
                    )
                    dialog.dismiss()
                }
                .setNeutralButton(getString(R.string.dialog_action_cancel_task_edit_bottom_sheet)) { dialog, which ->
                    dialog.dismiss()
                }
                    .create()
        return dialog
    }
}