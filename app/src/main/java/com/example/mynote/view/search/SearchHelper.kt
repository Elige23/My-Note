package com.example.mynote.view.search

import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import com.example.mynote.R
import kotlinx.coroutines.Runnable

/**
 * Helper class for implementing search functionality with debounce mechanism.
 *
 * Features:
 * - Debounces search queries to avoid excessive database calls
 * - Handles both text change (with debounce) and submit (immediate) events
 * - Configures SearchView with proper input settings
 *
 * The debounce mechanism prevents the search from triggering on every character
 * typed. Instead, it waits for the user to stop typing (300ms) before performing
 * the search, reducing unnecessary database queries.
 *
 * Usage:
 * ```
 * val searchHelper = SearchHelper()
 * searchHelper.setUpSearchView(searchView, object : SearchHelper.SearchListener {
 *     override fun onSearchPerformed(search: String) {
 *         noteViewModel.setSearchQuery(search)
 *     }
 * })
 * ```
 *
 * @see SearchView
 */
class SearchHelper{

    private val debounceDelay: Long = 300
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable = Runnable { }
    private var lastSearchString: String = ""
    private var searchListener: SearchListener? = null
    private var searchView: SearchView? = null
    private var queryTextListener: SearchView.OnQueryTextListener? = null

    /**
     * Listener interface for search events.
     */
    interface SearchListener {
        /**
         * Called when a search should be performed.
         *
         * @param search The search query string (already trimmed and non-empty)
         */
        fun onSearchPerformed(search: String)
    }

    /**
     * Configures the SearchView and sets up the search listener.
     *
     * - Configures query hint, IME options, and input type
     * - Sets up debounce mechanism for text changes
     * - Handles search submission (keyboard search button)
     *
     * @param searchView The SearchView to configure
     * @param listener Callback for search events
     */
    fun setUpSearchView(searchView: SearchView, listener: SearchListener){

        this.searchView = searchView
        this.searchListener = listener
        queryTextListener = object : SearchView.OnQueryTextListener{

            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    if (it.isNotBlank()){
                        performSearch(it, listener)
                        searchView.clearFocus()
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {

                    handler.removeCallbacks(searchRunnable)
                    searchRunnable = Runnable { performSearch(it, listener)}
                    handler.postDelayed(searchRunnable, debounceDelay)
                }
                return true
            }
        }

        searchView.apply {
            queryHint = context.getString(R.string.hint_search_note)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

            setOnQueryTextListener(queryTextListener)
        }
    }

    /**
     * Performs the actual search if the query has changed.
     *
     * @param newSearch The search query to execute
     */
    private fun performSearch(newSearch: String, listener: SearchListener){
        // Skip if the search query hasn't changed
        if (newSearch == lastSearchString) return

        lastSearchString = newSearch
        listener.onSearchPerformed(newSearch)
    }

    fun cleanup() {
        handler.removeCallbacks(searchRunnable)
        searchView?.setOnQueryTextListener(null)
        queryTextListener = null
        searchListener = null
        searchView = null
    }
}