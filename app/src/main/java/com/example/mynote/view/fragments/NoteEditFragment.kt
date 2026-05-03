package com.example.mynote.view.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.mynote.R
import com.example.mynote.databinding.FragmentEditNoteBinding
import com.example.mynote.model.repository.NoteApplicationRepository
import com.example.mynote.model.room.Note
import com.example.mynote.utils.preventViewPagerInterceptionFully
import com.example.mynote.view.fragments.dialogs.DeleteNoteConfirmationDialogFragment
import com.example.mynote.view.fragments.dialogs.UnsavedChangesNoteDialogFragment
import com.example.mynote.viewmodel.NoteViewModel
import com.example.mynote.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.launch

/**
 * Fragment for editing existing notes.
 *
 * Features:
 * - Edit title and description
 * - Save changes or delete note
 * - Toolbar with delete button, save button and back navigation
 * - Unsaved changes confirmation on exit
 * - Cursor position restoration after configuration changes
 * - Keyboard focus management
 *
 * @see NoteViewModel
 * @see Note
 */
class NoteEditFragment : Fragment() {
    private var _binding: FragmentEditNoteBinding? = null
    private val binding get() = _binding!!
    private val repository by lazy {
        (requireActivity().application as NoteApplicationRepository).noteRepository
    }
    private val noteViewModel: NoteViewModel by viewModels { NoteViewModelFactory(repository) }
    private val args: NoteEditFragmentArgs by navArgs()
    private lateinit var originalNote: Note
    private var titleTextWatcher: TextWatcher? = null
    private var descTextWatcher: TextWatcher? = null

    //for the cursor position in the edittext
    private var cursorStart: Int = 0
    private var savedCursorPosition: Int = -1
    private var focusedEditText: EditText? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditNoteBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editTitleNoteFragment.isSaveEnabled = false
        binding.editDescriptionNoteFragment.isSaveEnabled = false

        if (savedInstanceState != null){
            when(savedInstanceState.getString("view_focus")){

                "editDesc" -> {
                    val position = savedInstanceState.getInt("edittext_start_cursor")
                    savedCursorPosition = position
                    focusedEditText = binding.editDescriptionNoteFragment
                }

                "editTitle"->{
                    val position = savedInstanceState.getInt("edittext_start_cursor")
                    savedCursorPosition = position
                    focusedEditText = binding.editTitleNoteFragment
                }
            }
        }

        setUpObservers()
        setUpToolbar()
        setUpFragmentResultListener()
        setUpTextWatchers()
        setUpCallbacks()
        setupEditTextScroll()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        _binding?.let { binding ->
            if(binding.editDescriptionNoteFragment.hasFocus()){
                cursorStart = binding.editDescriptionNoteFragment.selectionStart
                outState.apply {
                    putString("view_focus", "editDesc")
                    putInt("edittext_start_cursor", cursorStart)
                }

            }
            if(binding.editTitleNoteFragment.hasFocus()){
                cursorStart = binding.editTitleNoteFragment.selectionStart
                outState.apply {
                    putString("view_focus", "editTitle")
                    putInt("edittext_start_cursor", cursorStart)
                }

            }
        }

    }

    override fun onDestroyView() {
        clearFocusAndKeyboard()
        clearTextWatchers()
        _binding = null
        super.onDestroyView()
    }

    private fun setUpToolbar() {
        binding.editToolbar.apply {
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationContentDescription("Back home")
            setNavigationOnClickListener {
                handleBackNavigation()
            }
            inflateMenu(R.menu.note_edit_menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.saveNote -> {
                        if (hasChanges()) {
                            saveNewNote()
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.toast_note_saved),
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().navigate(
                                NoteEditFragmentDirections.actionEditNoteFragmentToNoteHomeFragment(
                                    true,
                                    false
                                )
                            )
                            true
                        } else {
                            findNavController().popBackStack()
                            true
                        }
                    }

                    R.id.deleteNote -> {
                        hideKeyboard()
                        val dialog =
                            DeleteNoteConfirmationDialogFragment.newInstance()
                        dialog.show(childFragmentManager, "DeleteDialogTag")
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun saveNewNote() {
        val newTitle = binding.editTitleNoteFragment.text.toString()
        val newDesc = binding.editDescriptionNoteFragment.text.toString()
        val newNote = Note(id = originalNote.id, title = newTitle, desc = newDesc)
        noteViewModel.updateNote(newNote)
    }

    private fun hasChanges(): Boolean {
        val newTitle = binding.editTitleNoteFragment.text.toString()
        val newDesc = binding.editDescriptionNoteFragment.text.toString()
        return newTitle != originalNote.title || newDesc != originalNote.desc
    }

    private fun handleBackNavigation() {
        if (hasChanges()) {
            hideKeyboard()
            UnsavedChangesNoteDialogFragment.showUnsavedChangesNoteDialog(childFragmentManager)
        } else {
            findNavController().popBackStack()
        }
    }

    private fun setUpFragmentResultListener() {

        childFragmentManager.setFragmentResultListener(
            UnsavedChangesNoteDialogFragment.UNSAVED_CHANGES_DIALOG_REQUEST,
            viewLifecycleOwner
        ) { requestKey, bundle ->
            when (bundle.getString(UnsavedChangesNoteDialogFragment.UNSAVED_CHANGES_DIALOG_KEY)) {
                UnsavedChangesNoteDialogFragment.UNSAVED_CHANGES_POSITIVE_KEY -> {
                    saveNewNote()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_note_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                    clearData()
                    findNavController().navigate(
                        NoteEditFragmentDirections.actionEditNoteFragmentToNoteHomeFragment(
                            true,
                            false
                        )
                    )
                }

                UnsavedChangesNoteDialogFragment.UNSAVED_CHANGES_NEGATIVE_KEY -> {
                    clearData()
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun setUpObservers() {
        val editId = args.noteIdFromHome
        if (noteViewModel.getOriginalNote() == null) {
            viewLifecycleOwner.lifecycleScope.launch {
                noteViewModel.getEditNoteById(editId)
                originalNote = noteViewModel.getOriginalNote() ?: Note(title = "", desc = "")
            }
        } else {
            originalNote = noteViewModel.getOriginalNote()!!
        }

        noteViewModel.editNote.observe(viewLifecycleOwner) { note ->

            val title = binding.editTitleNoteFragment.text.toString()
            val desc = binding.editDescriptionNoteFragment.text.toString()
            if (title != note?.title) {
                binding.editTitleNoteFragment.setText(note?.title ?: "")
            }
            if (desc != note?.desc) {
                binding.editDescriptionNoteFragment.setText(note?.desc ?: "")
            }

            // Restore the cursor
            if (savedCursorPosition != -1) {
                    val textLength = focusedEditText?.text?.length ?: 0

                    val positionToRestore = if (savedCursorPosition <= textLength) {
                        savedCursorPosition
                    } else {
                        textLength
                    }

                // Restore focus and cursor, set the keyboard
                focusedEditText?.post {
                    focusedEditText?.requestFocus()
                    focusedEditText?.setSelection(positionToRestore)
                    forceShowKeyboard(focusedEditText?: binding.editTitleNoteFragment)
                }
                savedCursorPosition = -1
            }
        }
    }

    private fun setUpTextWatchers() {
        titleTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {
                noteViewModel.updateEditTitle(s.toString())
            }
        }

        descTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {

                noteViewModel.updateEditDesc(s.toString())
            }
        }

        binding.editTitleNoteFragment.addTextChangedListener(titleTextWatcher)
        binding.editDescriptionNoteFragment.addTextChangedListener(descTextWatcher)
    }

    private fun clearTextWatchers() {
        binding.editTitleNoteFragment.removeTextChangedListener(titleTextWatcher)
        titleTextWatcher = null
        binding.editDescriptionNoteFragment.removeTextChangedListener(descTextWatcher)
        descTextWatcher = null
    }


    private fun clearData() {
        binding.editTitleNoteFragment.text.clear()
        binding.editDescriptionNoteFragment.text.clear()
    }

    private fun clearFocusAndKeyboard() {

        binding.editTitleNoteFragment.clearFocus()
        binding.editDescriptionNoteFragment.clearFocus()
        focusedEditText = null
        hideKeyboard()
    }

    private fun setUpCallbacks(){

        //Handles an event when the user presses the back button (or swipes)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackNavigation()
                }
            }
        )
    }

    private fun forceShowKeyboard(view: EditText) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager

        // Save the cursor position locally
        val positionToRestore = savedCursorPosition

        // Instead of several attempts with fixed delays,
        // we use one, but with a stability check
        checkFocusStabilityAndShowKeyboard(view, imm, positionToRestore, attempt = 1)
    }

    private fun checkFocusStabilityAndShowKeyboard(
        view: EditText,
        imm: InputMethodManager,
        positionToRestore: Int,
        attempt: Int
    ) {
        if (attempt > 6) {
            return
        }

        val delay = when (attempt) {
            1 -> 200L
            2 -> 400L
            3 -> 600L
            4 -> 800L
            5 -> 1000L
            6 -> 1200L
            else -> 1000
        }

        view.postDelayed({
            // Check if the fragment and view are still alive
            if (!isAdded || !view.isAttachedToWindow) {
                return@postDelayed
            }

            if (view.hasFocus()) {

                // Restore the cursor
                if (positionToRestore != -1) {
                    val textLength = view.text?.length ?: 0
                    val finalPosition = if (positionToRestore <= textLength)
                        positionToRestore else textLength
                    view.setSelection(finalPosition)
                }

                // Trying to open the keyboard
                val result = imm.showSoftInput(view, InputMethodManager.SHOW_FORCED)
                Log.d("KeyboardControl", "showSoftInput result: $result")

                // Check after 300ms whether it has opened
                view.postDelayed({
                    if (imm.isActive) {
                        savedCursorPosition = -1
                    } else {
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                    }
                }, 300)
            } else {
                // Trying to request focus
                view.requestFocus()

                // Continue checking
                checkFocusStabilityAndShowKeyboard(view, imm, positionToRestore, attempt + 1)
            }
        }, delay)
    }

    fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    /**
     * Prevents the ViewPager2 from scrolling when the user scrolls inside an EditText.
     *
     * Without this, diagonal swipes on the EditText would be interpreted as
     * ViewPager2 page-swiping gestures instead of vertical text scrolling.
     *
     * @see preventViewPagerInterceptionFully
     */
    private fun setupEditTextScroll() {
        val editTextNoteTitle = binding.editTitleNoteFragment
        val editTextNoteDesc = binding.editDescriptionNoteFragment
        editTextNoteTitle.preventViewPagerInterceptionFully()
        editTextNoteDesc.preventViewPagerInterceptionFully()
    }
}