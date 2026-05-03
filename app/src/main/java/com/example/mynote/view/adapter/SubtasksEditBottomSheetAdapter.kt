package com.example.mynote.view.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mynote.databinding.ItemSubtaskEditBottomSheetBinding
import com.example.mynote.model.room.SubTask

/**
 * RecyclerView adapter for editing subtasks in the edit bottom sheet.
 *
 * This adapter handles real-time editing of subtasks with:
 * - Text input with automatic change tracking via TextWatcher
 * - Checkbox to mark subtask as completed/uncompleted
 * - Long-click to delete a subtask
 *
 * @param onSubtaskChanged Callback invoked when subtask text or completion status changes
 * @param onLongClick Callback invoked when a subtask is long-pressed (for deletion)
 *
 * @see SubTask
 */
class SubtasksEditBottomSheetAdapter(
   private val onSubtaskChanged: (SubTask) -> Unit,
   private val onLongClick: (SubTask) -> Unit
): ListAdapter<SubTask, SubtasksEditBottomSheetAdapter.SubtasksEditViewHolder>(diffUtil) {

    companion object{
        private val diffUtil = object : DiffUtil.ItemCallback<SubTask>(){
            override fun areItemsTheSame(
                oldItem: SubTask,
                newItem: SubTask
            ): Boolean {
                return oldItem.id == newItem.id && oldItem.taskGroupId == newItem.taskGroupId && oldItem.createdAt == newItem.createdAt
            }

            override fun areContentsTheSame(
                oldItem: SubTask,
                newItem: SubTask
            ): Boolean {
                return oldItem.desc == newItem.desc && oldItem.isCompleted == newItem.isCompleted  && oldItem.createdAt == newItem.createdAt
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SubtasksEditViewHolder {
        val binding = ItemSubtaskEditBottomSheetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubtasksEditViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SubtasksEditViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: SubtasksEditViewHolder) {
        holder.clear()
        super.onViewRecycled(holder)
    }

    /**
     * ViewHolder for a single subtask in edit mode.
     *
     * Manages:
     * - Text input with real-time change tracking
     * - Checkbox for completion status
     * - Long-click for deletion
     *
     * **Important:** TextWatcher is created once in the init block and reused
     * to avoid memory leaks and multiple listener attachments.
     */
   inner class SubtasksEditViewHolder(private val binding: ItemSubtaskEditBottomSheetBinding): RecyclerView.ViewHolder(binding.root){
       private var textWatcher: TextWatcher? = null
       private var currentSubTask: SubTask? = null

       init {
           textWatcher = createTextWatcher()
           binding.descSubTask.addTextChangedListener(textWatcher)

           binding.checkBoxSubTask.setOnCheckedChangeListener { _, isChecked ->
               currentSubTask?.let { subTask ->
                   // Check if the state has actually changed
                   if (subTask.isCompleted != isChecked) {
                       val updatedSubtask = subTask.copy(
                           isCompleted = isChecked,
                           createdAt = System.currentTimeMillis()
                       )
                       currentSubTask = updatedSubtask
                       onSubtaskChanged.invoke(updatedSubtask)
                   }
               }
           }

           binding.descSubTask.setOnLongClickListener {
               currentSubTask?.let {
                   onLongClick.invoke(it)
                   true
               } ?: false
           }

       }

       private fun createTextWatcher() : TextWatcher{
           return object : TextWatcher {

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

                    if (currentSubTask?.desc != s.toString()){
                        val updatedSubtask =
                        currentSubTask?.copy(desc = s.toString(), createdAt = System.currentTimeMillis())
                        currentSubTask = updatedSubtask
                        currentSubTask?.let {
                            onSubtaskChanged.invoke(it)
                        }
                    }
               }
           }
       }

       fun bind(subTask: SubTask){

           currentSubTask = subTask
           val subtaskEditText = binding.descSubTask.text.toString()

           binding.apply {

               checkBoxSubTask.isChecked = subTask.isCompleted
               descSubTask.imeOptions = EditorInfo.IME_ACTION_DONE

               if (subTask.desc != subtaskEditText) {
                   descSubTask.setTextKeepState(subTask.desc)
               }
           }
       }

       fun clear() {
           currentSubTask = null
       }
   }
}