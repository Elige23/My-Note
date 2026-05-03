package com.example.mynote.view.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.example.mynote.databinding.ItemSubtaskEntryBottomSheetBinding
import java.lang.ref.WeakReference

/**
 * RecyclerView adapter for managing subtasks in the entry bottom sheet.
 *
 * This adapter handles dynamic addition, removal, and editing of subtasks.
 * It supports:
 * - Adding new subtasks via ENTER key or IME action
 * - Removing subtasks via long-click
 * - Real-time editing with TextWatcher
 * - Focus management after add/delete operations
 *
 * @param onSubtaskRemoved Callback invoked when a subtask is removed (long-click)
 * @param onSubtaskChanged Callback invoked when subtask text changes (real-time)
 * @param onSubtaskAdd Callback invoked when a new subtask is added (ENTER key or NEXT action)
 */
class SubtaskEntryBottomSheetAdapter(
    private val onSubtaskRemoved: (Int) -> Unit,
    private val onSubtaskChanged: (Int, String) -> Unit,
    private val onSubtaskAdd: (Int, String) -> Unit
) : RecyclerView.Adapter<SubtaskEntryBottomSheetAdapter.SubtaskViewHolder>() {

    private var subtasks: MutableList<String> = mutableListOf()
    private val focusedEditText = mutableMapOf<Int, WeakReference<EditText>>()
    private val textWatchersEditText = mutableMapOf<Int, WeakReference<TextWatcher>>()
    private var currentlyFocusedEditTextId: Int = -1
    private var lastFocusedPosition: Int = -1
    private var recyclerView: RecyclerView? = null

    /**
     * Updates the list of subtasks displayed in the adapter.
     *
     * @param list New list of subtask descriptions
     */
    fun submitList(list: List<String>) {
        subtasks = list.toMutableList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtaskViewHolder {
        val binding = ItemSubtaskEntryBottomSheetBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return SubtaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubtaskViewHolder, position: Int) {

        holder.etSubtask.setOnFocusChangeListener(null)
        holder.etSubtask.setOnLongClickListener(null)

        holder.bind(position)
    }

    override fun getItemCount(): Int = subtasks.size


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    override fun onViewRecycled(holder: SubtaskViewHolder) {
        // When reusing, delete old records
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            focusedEditText.remove(position)
        }
        holder.currentTextWatcher?.let {
            holder.etSubtask.removeTextChangedListener(it)
            holder.currentTextWatcher = null
        }
        holder.etSubtask.setOnFocusChangeListener(null)

        super.onViewRecycled(holder)
    }

    /**
     * ViewHolder for a single subtask in entry mode.
     *
     * Manages:
     * - Text input with real-time change tracking
     * - Checkbox for completion status
     * - Long-click for deletion
     * - Focus tracking and correct focus restoration after list updates
     * - Adding new subtasks using the ENTER key or an IME action
     */
    inner class SubtaskViewHolder( private val binding: ItemSubtaskEntryBottomSheetBinding): RecyclerView.ViewHolder(binding.root) {

        val etSubtask: EditText = binding.descSubTask
        var currentTextWatcher: TextWatcher? = null

        init {
            setupEnterKeyListener()
        }

        fun bind(position: Int) {

            // Remove the old TextWatcher
            currentTextWatcher?.let {
                etSubtask.removeTextChangedListener(it)
                currentTextWatcher = null
            }

            val subtask = subtasks[position]
            val subtasksEditText = etSubtask.text.toString()
            etSubtask.post {
                if (subtask != subtasksEditText) {
                    etSubtask.setText(subtasks[position])
                }
            }

            // Create a new TextWatcher
            currentTextWatcher = object : TextWatcher {
                var previousText = subtasks[position]

                override fun beforeTextChanged(s: CharSequence?,
                                               start: Int, count: Int, after: Int) {
                    previousText = s?.toString() ?: ""
                }

                override fun onTextChanged(s: CharSequence?,
                                           start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    val currentPos = adapterPosition.takeIf { it != RecyclerView.NO_POSITION }

                    if (currentPos != null && currentPos < subtasks.size) {

                        val newText = s?.toString() ?: ""
                        onSubtaskChanged.invoke(currentPos,newText)
                    }
                }
            }

            etSubtask.addTextChangedListener(currentTextWatcher)

            etSubtask.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    currentlyFocusedEditTextId = view.id
                    lastFocusedPosition = position

                    // Save the link
                    if (!focusedEditText.containsKey(position)) {
                        focusedEditText[position] = WeakReference(etSubtask)
                    }
                    if (!textWatchersEditText.containsKey(position)) {
                        textWatchersEditText[position] = WeakReference(currentTextWatcher)
                    }

                }
            }

            etSubtask.setOnLongClickListener {

                onSubtaskRemoved.invoke(position)
                true
            }
        }

        private fun setupEnterKeyListener() {
            etSubtask.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN) {

                    val text = etSubtask.text.toString().trim()
                    if (text.isNotEmpty()) {
                        val currentPosition = adapterPosition
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            subtasks[currentPosition] = text

                            // Add new subtask at next position
                            onSubtaskAdd.invoke(currentPosition + 1, "")

                            // Request focus on new field
                            focusedEditText[currentPosition + 1]?.get()?.requestFocus()
                        }
                    }
                    return@setOnKeyListener true
                }
                false
            }

            etSubtask.setOnEditorActionListener { _, actionId, _ ->
                val currentPosition = adapterPosition
                val text = etSubtask.text.toString().trim()
                when (actionId) {

                    EditorInfo.IME_ACTION_NEXT -> {
                        if (text.isNotEmpty()) {
                            onSubtaskAdd.invoke(currentPosition + 1, "")
                        }
                        true
                    }
                    else -> false
                }
            }

        }
    }

    fun cleanUp(){
        focusedEditText.clear()
        textWatchersEditText.clear()
        recyclerView = null
    }

    fun updateFocusedEditTextMapAfterDelete(position : Int) {
        // Delete the element at the deleted position
        focusedEditText.remove(position)

        // Create a new map with shifted keys
        val updatedMap = mutableMapOf<Int, WeakReference<EditText>>()

        focusedEditText.forEach { (key, value) ->
            when {
                key < position -> updatedMap[key] = value
                key > position -> updatedMap[key - 1] = value
            }
        }

        // Updating the original map
        focusedEditText.clear()
        focusedEditText.putAll(updatedMap)
    }

    fun updateFocusedAfterDelete(position: Int){

        // Updating lastFocusedPosition
        when {
            lastFocusedPosition > position -> {
                lastFocusedPosition -= 1
            }
            lastFocusedPosition == position -> {
                lastFocusedPosition = if (lastFocusedPosition > 0) lastFocusedPosition - 1 else -1
            }
        }

        // Get the EditText for focus
        val editTextRef = focusedEditText[lastFocusedPosition]
        val editText = editTextRef?.get()

        // Request focus if the EditText exists
        editText?.let {
            // Request focus with a slight delay to allow the RecyclerView to update.
            it.postDelayed({
                it.requestFocus()
                // Set the cursor to the end of the text
                it.setSelection(it.text?.length ?: 0)
            }, 100)
        }
    }
}
