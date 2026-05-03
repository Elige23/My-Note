package com.example.mynote.view.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.mynote.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog fragment for confirming save/cancel actions for unsaved changes when exiting note editing.
 *
 * Displays a confirmation dialog when the user attempts to close the note edit screen
 * without saving changes. Provides three options:
 * - **Save:** Save changes and exit
 * - **Discard:** Discard changes and exit
 * - **Cancel:** Return to editing without exiting
 */
class UnsavedChangesNoteDialogFragment: DialogFragment() {
    companion object{
        const val UNSAVED_CHANGES_DIALOG_TAG = "unsaved_changes_dialog"
        const val UNSAVED_CHANGES_DIALOG_REQUEST = "unsaved_changes_request"
        const val UNSAVED_CHANGES_DIALOG_KEY = "unsaved_changes_save_key"
        const val UNSAVED_CHANGES_POSITIVE_KEY = "save_changes_key"
        const val UNSAVED_CHANGES_NEGATIVE_KEY = "delete_changes_key"

        /**
         * Shows the save confirmation dialog for unsaved changes for note.
         *
         * @param fragmentManager FragmentManager to show the dialog
         *
         * @see UnsavedChangesNoteDialogFragment
         */
        fun showUnsavedChangesNoteDialog(fragmentManager: FragmentManager){
            UnsavedChangesNoteDialogFragment().show(fragmentManager, UNSAVED_CHANGES_DIALOG_TAG)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return MaterialAlertDialogBuilder(requireContext(),R.style.ThemeOverlay_MyNote_MaterialAlertDialog)
            .setTitle(getString(R.string.dialog_title_save_changes_note_edit_fragment))
            .setPositiveButton(getString(R.string.dialog_action_save_changes_note_edit_fragment)) { dialog, which ->
                val saveBundle = Bundle().apply {
                    putString(UNSAVED_CHANGES_DIALOG_KEY, UNSAVED_CHANGES_POSITIVE_KEY)
                }
                parentFragmentManager.setFragmentResult(UNSAVED_CHANGES_DIALOG_REQUEST, saveBundle)
            }
            .setNegativeButton(getString(R.string.dialog_action_discard_changes_note_edit_fragment)){ dialog, which ->
                val negativeBundle = Bundle().apply {
                    putString(UNSAVED_CHANGES_DIALOG_KEY, UNSAVED_CHANGES_NEGATIVE_KEY)
                }
                parentFragmentManager.setFragmentResult(UNSAVED_CHANGES_DIALOG_REQUEST, negativeBundle)
            }
            .setNeutralButton(getString(R.string.dialog_action_cancel_note_edit_fragment)){ dialog, which ->
                dialog?.dismiss()
            }
            .create()
    }
}