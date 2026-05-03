package com.example.mynote.view.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.mynote.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog fragment for confirming exit from the note creation screen.
 *
 * Displays a confirmation dialog when the user attempts to exit without saving changes.
 * Provides three options:
 * - **Save:** Save the note and exit
 * - **Delete/Discard:** Discard changes and exit
 * - **Cancel:** Return to editing/creating without exiting
 *
 */
class ExitConfirmationNoteDialogFragment: DialogFragment() {

    companion object{

        const val EXIT_DIALOG_TAG = "exit_dialog"
        const val EXIT_DIALOG_REQUEST = "exit_request"
        const val EXIT_DIALOG_KEY = "exit_save_key"
        const val EXIT_POSITIVE_KEY = "save_exit_key"
        const val EXIT_NEGATIVE_KEY = "delete_exit_key"

        /**
         * Shows the exit confirmation dialog from the note creation screen.
         *
         * @param fragmentManager FragmentManager to show the dialog
         *
         * @see ExitConfirmationNoteDialogFragment
         */
        fun showExitNoteDialog(fragmentManager: FragmentManager){
            ExitConfirmationNoteDialogFragment().show(fragmentManager, EXIT_DIALOG_TAG)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return MaterialAlertDialogBuilder(requireContext(),R.style.ThemeOverlay_MyNote_MaterialAlertDialog)
            .setTitle(getString(R.string.dialog_title_save_add_note_fragment))
            .setPositiveButton(getString(R.string.dialog_action_save_add_note_fragment)) { dialog, which ->
                val saveBundle = Bundle().apply {
                    putString(EXIT_DIALOG_KEY, EXIT_POSITIVE_KEY)
                }
                parentFragmentManager.setFragmentResult(EXIT_DIALOG_REQUEST, saveBundle)
            }
            .setNegativeButton(getString(R.string.dialog_action_delete_add_note_fragment)){ dialog, which ->
                val negativeBundle = Bundle().apply {
                    putString(EXIT_DIALOG_KEY, EXIT_NEGATIVE_KEY)
                }
                parentFragmentManager.setFragmentResult(EXIT_DIALOG_REQUEST, negativeBundle)
            }
            .setNeutralButton(getString(R.string.dialog_action_cancel_add_note_fragment)){ dialog, which ->
                dialog?.dismiss()
            }
            .create()
    }
}