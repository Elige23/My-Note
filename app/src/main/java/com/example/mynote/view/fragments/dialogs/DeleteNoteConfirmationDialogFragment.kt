package com.example.mynote.view.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.mynote.R
import com.example.mynote.model.repository.NoteApplicationRepository
import com.example.mynote.viewmodel.NoteViewModel
import com.example.mynote.viewmodel.NoteViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog fragment for confirming the deletion of a note in the note editing fragment.
 *
 * Displays a confirmation dialog with the note title and options to delete or cancel.
 * When confirmed, deletes the note and navigates back.
 *
 * @see NoteViewModel
 * @see MaterialAlertDialogBuilder
 */
class DeleteNoteConfirmationDialogFragment: DialogFragment() {

    private val sharedViewModel: NoteViewModel by viewModels(ownerProducer = { requireParentFragment() },
        factoryProducer = {
                val repository = (requireActivity().application as NoteApplicationRepository).noteRepository
            NoteViewModelFactory(repository)
        }
    )

    companion object{

        /**
         * Creates a new instance of the delete confirmation dialog for a note.
         *
         * @see DeleteNoteConfirmationDialogFragment
         */
        fun newInstance(): DeleteNoteConfirmationDialogFragment{
            return DeleteNoteConfirmationDialogFragment()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val note = sharedViewModel.editNote.value

        return MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_MyNote_MaterialAlertDialog)
            .setTitle(R.string.dialog_title_delete_note)
            .setMessage(getString(R.string.dialog_message_delete_note, note?.title?:""))
            .setPositiveButton(getString(R.string.dialog_action_delete_note)){ dialog, which ->
                note?.let {
                    sharedViewModel.deleteNote(it)
                }
                Toast.makeText(requireContext(),getString(R.string.dialog_toast_note_deleted, note?.title), Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .setNegativeButton(getString(R.string.dialog_action_cancel_note)){ dialog, which ->
                dialog.dismiss()
            }
            .create()
    }
}