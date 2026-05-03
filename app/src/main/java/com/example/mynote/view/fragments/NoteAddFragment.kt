package com.example.mynote.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.mynote.R
import com.example.mynote.databinding.FragmentAddNoteBinding
import com.example.mynote.model.repository.NoteApplicationRepository
import com.example.mynote.model.room.Note
import com.example.mynote.view.fragments.dialogs.ExitConfirmationNoteDialogFragment
import com.example.mynote.viewmodel.NoteViewModel
import com.example.mynote.viewmodel.NoteViewModelFactory
import kotlin.getValue
import com.example.mynote.utils.hideKeyboard
import com.example.mynote.utils.forceShowKeyboard
import com.example.mynote.utils.preventViewPagerInterceptionFully

/**
 * Fragment for creating new notes.
 *
 * Features:
 * - Input fields for title and description
 * - Toolbar with save button and back navigation
 * - Unsaved changes confirmation on exit
 * - Cursor position restoration after configuration changes
 * - Keyboard focus management
 *
 * @see NoteViewModel
 * @see Note
 */
class NoteAddFragment : Fragment() {

    private var _binding: FragmentAddNoteBinding? = null
    private val binding get() = _binding!!
    private lateinit var toolbar: Toolbar
    private val repository by lazy {
        (requireActivity().application as NoteApplicationRepository).noteRepository
    }
    private val noteViewModel: NoteViewModel by viewModels { NoteViewModelFactory(repository) }

    //for the cursor position in the edittext
    private var cursorStart: Int = 0
    private var savedCursorPosition: Int = -1

    private var focusedEditText: EditText? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addTitleNoteFragment.isSaveEnabled = false
        binding.addDescriptionNoteFragment.isSaveEnabled = false

        if (savedInstanceState != null){

            val title = savedInstanceState.getString("title_text")
            binding.addTitleNoteFragment.setText(title)
            val desc = savedInstanceState.getString("desc_text")
            binding.addDescriptionNoteFragment.setText(desc)

            when (savedInstanceState.getString("view_focus")){

                "editDesc" -> {
                    val position = savedInstanceState.getInt("edittext_start_cursor")
                    savedCursorPosition = position
                    focusedEditText = binding.addDescriptionNoteFragment
                }

                "editTitle"->{
                    val position = savedInstanceState.getInt("edittext_start_cursor")
                    savedCursorPosition = position
                    focusedEditText = binding.addTitleNoteFragment
                }
            }
        }

        // Waiting for the View to be completely restored by the system.
        if (savedCursorPosition != -1 && focusedEditText != null) {
            waitForTextAndRestoreCursor()
        }

        setUpToolbar()
        setUpResultListener()
        setUpCallbacks()
        setupEditTextScroll()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        _binding?.let { binding ->

            if (binding.addDescriptionNoteFragment.hasFocus()){
                cursorStart = binding.addDescriptionNoteFragment.selectionStart
                outState.apply {
                    putString("view_focus", "editDesc")
                    putInt("edittext_start_cursor", cursorStart)
                }
            }

            if (binding.addTitleNoteFragment.hasFocus()){
                cursorStart = binding.addTitleNoteFragment.selectionStart
                outState.apply {
                    putString("view_focus", "editTitle")
                    putInt("edittext_start_cursor", cursorStart)
                }
            }
            val titleText = binding.addTitleNoteFragment.text.toString()
            val descText = binding.addDescriptionNoteFragment.text.toString()
            outState.apply {
                putString("title_text", titleText)
                putString("desc_text", descText)
            }
        }
    }

    override fun onDestroyView() {
        clearFocusAndKeyboard()
        focusedEditText = null
        _binding = null
        super.onDestroyView()
    }

    private fun setUpToolbar(){
        toolbar = binding.addToolbar
        toolbar.apply {
            inflateMenu(R.menu.note_add_menu)
            setNavigationIcon(R.drawable.baseline_arrow_back_24)
            setNavigationContentDescription("Back home")
            setNavigationOnClickListener {
                handleBackNavigation()
            }

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.addNote -> {

                        if (binding.addTitleNoteFragment.text.isBlank()) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.toast_note_title_empty),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnMenuItemClickListener false
                        }
                        
                        saveNote()
                        findNavController().navigate(
                            NoteAddFragmentDirections.actionAddNoteFragmentToNoteHomeFragment(
                                false,
                                true
                            )
                        )
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun saveNote(){
        val title = binding.addTitleNoteFragment.text.toString()
        val desc = binding.addDescriptionNoteFragment.text.toString()
        val note = Note(title = title, desc = desc)

        noteViewModel.insertNote(note)
        Toast.makeText(
            requireContext(),
            getString(R.string.toast_note_saved),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun handleBackNavigation(){
        if (binding.addTitleNoteFragment.text.toString().isBlank() &&
            binding.addDescriptionNoteFragment.text.toString().isBlank()){
            findNavController().popBackStack()
        }
        else
        {
            ExitConfirmationNoteDialogFragment.showExitNoteDialog(childFragmentManager)
        }
    }

    private fun setUpResultListener(){
        childFragmentManager.setFragmentResultListener(ExitConfirmationNoteDialogFragment.EXIT_DIALOG_REQUEST,
            viewLifecycleOwner){ requestKey, bundle ->
            when (bundle.getString(ExitConfirmationNoteDialogFragment.EXIT_DIALOG_KEY)){

                ExitConfirmationNoteDialogFragment.EXIT_POSITIVE_KEY -> {
                    saveNote()
                    clearData()
                    findNavController().navigate(
                        NoteAddFragmentDirections.actionAddNoteFragmentToNoteHomeFragment(
                            false,
                            true
                        )
                    )
                }

                ExitConfirmationNoteDialogFragment.EXIT_NEGATIVE_KEY -> {
                    clearData()
                    // Exit without saving data
                    findNavController().popBackStack()
                }
            }
        }
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

    private fun clearData(){
        binding.addTitleNoteFragment.text.clear()
        binding.addDescriptionNoteFragment.text.clear()
    }

    private fun clearFocusAndKeyboard() {

        binding.addTitleNoteFragment.clearFocus()
        binding.addDescriptionNoteFragment.clearFocus()

        if (binding.addDescriptionNoteFragment.hasFocus()){
            binding.addDescriptionNoteFragment.hideKeyboard()
        }
        focusedEditText = null
    }

    private fun restoreKeyboardWithCursor(){

        // Save the state before the changes
        val cursorToRestore = savedCursorPosition
        if (cursorToRestore == -1) return

        val textLength = focusedEditText?.text?.length ?: 0

        // We check that the position is valid
        val positionToRestore = if (savedCursorPosition <= textLength) {
            savedCursorPosition
        } else {
            textLength
        }

        // Restore focus and cursor
        // Restore keyboard
        focusedEditText?.post {
            focusedEditText?.requestFocus()
            focusedEditText?.setSelection(positionToRestore)

            forceShowKeyboard(focusedEditText?: binding.addTitleNoteFragment, savedCursorPosition){
                savedCursorPosition = -1
            }
        }
    }

    private fun waitForTextAndRestoreCursor() {
        val editText = focusedEditText ?: return

        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Checking if the text has appeared
                if (editText.text?.isNotEmpty() == true) {
                    // The text has been restored. Remove the listener.
                    editText.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    // Recovering the cursor and keyboard
                    restoreKeyboardWithCursor()
                }
            }
        }
        editText.viewTreeObserver.addOnGlobalLayoutListener(listener)

        // A fallback option in case the text already exists
        editText.post {
            if (editText.text?.isNotEmpty() == true) {
                restoreKeyboardWithCursor()
            }
        }
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
        val editTextNoteTitle = binding.addTitleNoteFragment
        val editTextNoteDesc = binding.addDescriptionNoteFragment
        editTextNoteTitle.preventViewPagerInterceptionFully()
        editTextNoteDesc.preventViewPagerInterceptionFully()
    }
}