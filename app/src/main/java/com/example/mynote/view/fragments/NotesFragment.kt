package com.example.mynote.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.mynote.R

/**
 * Fragment that serves as a container for nested navigation (NavHostFragment).
 *
 * This fragment hosts a NavHostFragment and manages custom back button behavior
 * for the nested navigation graph. It intercepts back button presses to handle
 * navigation within the nested graph before allowing the parent to handle it.
 *
 * Navigation structure:
 * - Main container: [NotesFragment] with nested NavHostFragment
 * - Nested destinations:
 *   - [NoteHomeFragment] (home screen)
 *   - [NoteAddFragment] (add new note)
 *   - [NoteEditFragment] (edit existing note)
 *
 * @see NavHostFragment
 * @see NavController
 */
class NotesFragment : Fragment() {

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpNavController()
        setUpCallbacks()
    }

    private fun setUpNavController(){
        val childNavHostFragment = childFragmentManager
            .findFragmentById(R.id.fragmentNoteContainer) as NavHostFragment
        navController = childNavHostFragment.navController
    }

    /**
     * Sets up custom back button handling for the nested navigation graph.
     *
     * Behavior:
     * - If not on the home destination (NoteHomeFragment), navigate back within the nested graph
     * - If on the home destination, delegate to parent (switch tabs or exit app)
     */
    private fun setUpCallbacks(){
        // Handles an event when the user presses the back button (or swipes)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (navController.currentDestination?.id != R.id.noteHomeFragment) {
                // Navigate back within the nested navigation graph
                navController.popBackStack()
            } else {
                // At home destination, let parent handle the back press
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}