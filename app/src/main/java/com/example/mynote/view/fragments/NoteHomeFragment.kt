package com.example.mynote.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.mynote.R
import com.example.mynote.databinding.FragmentNoteHomeBinding
import com.example.mynote.model.repository.NoteApplicationRepository
import com.example.mynote.model.room.Note
import com.example.mynote.view.adapter.NotesAdapter
import com.example.mynote.view.search.SearchHelper
import com.example.mynote.viewmodel.NoteViewModel
import com.example.mynote.viewmodel.NoteViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlin.getValue

/**
 * Fragment displaying the list of notes in a staggered grid layout.
 *
 * Features:
 * - Staggered grid layout with 2 columns
 * - Search functionality with SearchView
 * - Floating Action Button to add new notes
 * - Snackbar with undo action when deleting notes (the Snackbar is attached to `android.R.id.content` (Activity content view)
 * rather than the fragment's root).
 * - Scroll position restoration after returning from edit/add screens
 *
 * **Snackbar Implementation Note:**
 * If fragment-scoped Snackbar behavior is desired, refactor the layout to use
 * `CoordinatorLayout` as the root and update `showSnackbar()` accordingly.
 *
 * @see NotesAdapter
 * @see NoteViewModel
 */
class NoteHomeFragment : Fragment() {
    private var _binding: FragmentNoteHomeBinding? = null
    private val binding get() = _binding!!
    private var adapter: NotesAdapter? = null
    private lateinit var recyclerView: RecyclerView
    private val repository by lazy {
        (requireActivity().application as NoteApplicationRepository).noteRepository
    }
    private val noteViewModel: NoteViewModel by viewModels { NoteViewModelFactory(repository) }
    private var toolbar: Toolbar? = null
    private var searchView: SearchView? = null
    private var snackbar: Snackbar? = null
    private var scrollFlag = false

    /**
     * Flag indicating whether to scroll to top when returning from edit screen.
     */
    val scrollFlagFromEdit = if (arguments != null) {
       NoteHomeFragmentArgs.fromBundle(requireArguments()).scrollFlagFromEdit
    } else {
       false
    }
    /**
     * Flag indicating whether to scroll to top when returning from add screen.
     */
    val scrollFlagFromAdd = if (arguments != null) {
        NoteHomeFragmentArgs.fromBundle(requireArguments()).scrollFlagFromAdd
    } else {
        false
    }

    private lateinit var searchHelper: SearchHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNoteHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpToolbar()
        setUpSearchHelper()
        setUpRecyclerView()
        setUpObservers()
        setUpListeners()
    }

    override fun onDestroyView() {
        binding.homeRecyclerView.adapter = null
        adapter = null
        toolbar = null
        snackbar?.dismiss()
        snackbar = null
        searchView = null
        searchHelper.cleanup()
        _binding = null
        super.onDestroyView()
    }

    private fun setUpRecyclerView(){
        recyclerView = binding.homeRecyclerView
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        recyclerView.setHasFixedSize(true)

        adapter = NotesAdapter(
            onNoteClick = {note ->
                findNavController().navigate(NoteHomeFragmentDirections.actionNoteHomeFragmentToEditNoteFragment(note.id))
            },
            deleteNoteClick = {note ->
                scrollFlag = false
                noteViewModel.deleteNote(note)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun setUpListeners(){

        binding.fabAddHome.setOnClickListener {
            findNavController().navigate(NoteHomeFragmentDirections.actionNoteHomeFragmentToAddNoteFragment())

        }
    }

    private fun setUpToolbar(){
        toolbar = binding.homeToolbar
        toolbar?.inflateMenu(R.menu.note_home_menu)
        val searchMenuItem = toolbar?.menu?.findItem(R.id.searchNote)
        searchView = searchMenuItem?.actionView as SearchView
        toolbar?.title = getString(R.string.title_notes_toolbar)
    }

    private fun setUpSearchHelper(){
        searchHelper = SearchHelper()
        searchView?.let {
            searchHelper.setUpSearchView(it, object: SearchHelper.SearchListener {
                override fun onSearchPerformed(search: String) {
                    noteViewModel.setSearchQuery(search)
                }
            })
        }
    }

    /**
     * Shows a snackbar with undo action for note deletion.
     *
     * The Snackbar is attached to the Activity's content view (`android.R.id.content`)
     * rather than the fragment's root view. This ensures the Snackbar remains visible
     * across the entire notes and task feature (including during navigation between fragments)
     *
     * **Important for future maintenance:**
     * If this Snackbar should only appear within `NoteHomeFragment` (and not across
     * the entire notes graph and `TasksFragment`), replace the current implementation with:
     *  ```
     * // 1. Wrap the fragment layout in CoordinatorLayout:
     * // fragment_note_home.xml -> add CoordinatorLayout as root
     *
     * // 2. Use the root CoordinatorLayout as the anchor:
     * Snackbar.make(binding.rootCoordinatorLayout, message, duration)
     * ```
     *
     * **Alternative approach:**
     * For Snackbars that should be fragment-scoped, always use a `CoordinatorLayout`
     * as the root of the fragment layout and reference it via binding.
     *
     * @param note The note that was deleted (for undo restoration)
     */
    private fun showSnackbar (note: Note){

        val position = adapter?.currentList?.indexOfFirst {it.id == note.id }
        snackbar = Snackbar.make(requireActivity().findViewById(android.R.id.content),
            getString(R.string.message_snackbar_delete_note, note.title), 5000)
            .setAction(getString(R.string.action_cancel_snackbar)){

                if (position == 0 || position == -1)
                {
                    scrollFlag = true
                }
                noteViewModel.insertNote(note)
            }
        snackbar?.show()
        snackbar?.addCallback(object : Snackbar.Callback(){
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                noteViewModel.setSnackbarNote(null)
                super.onDismissed(transientBottomBar, event)
            }
        })
    }

    private fun setUpObservers() {
        // Observe the combined notes (all or search results)
        noteViewModel.noteAllOrSearch.observe(viewLifecycleOwner) { notes ->
            adapter?.submitList(notes){
                if (scrollFlag || scrollFlagFromEdit || scrollFlagFromAdd) {
                    binding.homeRecyclerView.smoothScrollToPosition(0)
                }
            }
        }

        noteViewModel.snackbarNote.observe(viewLifecycleOwner){ note ->
            note?.let {
                showSnackbar(it)
            }
        }
    }
}

