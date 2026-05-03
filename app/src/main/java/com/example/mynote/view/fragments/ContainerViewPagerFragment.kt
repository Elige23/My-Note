package com.example.mynote.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.mynote.R
import com.example.mynote.view.adapter.FragmentStateAdapterViewPager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Fragment that hosts a ViewPager2 with two pages (Notes and Tasks) and a TabLayout.
 *
 * This fragment acts as a container that manages:
 * - ViewPager2 for swiping between NotesFragment and TasksFragment
 * - TabLayout for navigation between the two pages
 * - TabLayoutMediator to synchronize TabLayout with ViewPager2
 *
 * The fragment creates and manages a single TabLayoutMediator instance.
 *
 * @see FragmentStateAdapterViewPager
 * @see NotesFragment
 * @see TasksFragment
 */
class ContainerViewPagerFragment : Fragment() {

    private lateinit var pagerAdapter: FragmentStateAdapterViewPager
    private var tabLayoutMediator: TabLayoutMediator? = null
    private lateinit var tabLayout: TabLayout
    val viewPager: ViewPager2 by lazy { requireView().findViewById(R.id.viewPager) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_container_view_pager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tabLayout)

        setupViewPager()
        setupTabLayout()
    }

    override fun onDestroyView() {
        tabLayoutMediator?.detach()
        tabLayoutMediator = null
        viewPager.adapter = null
        super.onDestroyView()
    }

    private fun setupViewPager() {
        pagerAdapter = FragmentStateAdapterViewPager(this)
        viewPager.adapter = pagerAdapter
    }

    private fun setupTabLayout() {

        tabLayoutMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getString(pagerAdapter.getTabTitle(position))
            tab.icon = ContextCompat.getDrawable(requireContext(), pagerAdapter.getTabIcon(position))
        }
        tabLayoutMediator?.attach()
    }
}
