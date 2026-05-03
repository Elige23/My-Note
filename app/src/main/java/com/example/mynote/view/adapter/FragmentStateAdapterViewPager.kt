package com.example.mynote.view.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.mynote.R
import com.example.mynote.view.fragments.NotesFragment
import com.example.mynote.view.fragments.TasksFragment

/**
 * Adapter for ViewPager2 that manages two fragments: Notes and Tasks.
 *
 * This adapter is used by [com.example.mynote.view.fragments.ContainerViewPagerFragment] to display two pages:
 * - Position 0: [NotesFragment] - for displaying notes
 * - Position 1: [TasksFragment] - for displaying tasks
 *
 * **Features:**
 * - Provides titles and icons for TabLayout via [getTabTitle] and [getTabIcon]
 *
 * @param fragment The parent fragment that hosts the ViewPager
 *
 * @see NotesFragment
 * @see TasksFragment
 * @see com.example.mynote.view.fragments.ContainerViewPagerFragment
 */
class FragmentStateAdapterViewPager(fragment: Fragment): FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> NotesFragment()
            1 -> TasksFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

    override fun getItemCount(): Int = 2

    fun getTabTitle(position: Int): Int {
        return when (position) {
            0 -> R.string.title_note_tablayout
            1 -> R.string.title_task_tablayout
            else -> 0
        }
    }

    fun getTabIcon(position: Int): Int {
        return when (position) {
            0 -> R.drawable.outline_note_stack_24
            1 -> R.drawable.outline_checklist_24
            else -> 0
        }
    }
}