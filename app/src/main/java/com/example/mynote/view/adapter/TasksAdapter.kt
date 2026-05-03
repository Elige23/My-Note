package com.example.mynote.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mynote.databinding.TaskCardBinding
import com.example.mynote.databinding.TaskGroupCardBinding
import com.example.mynote.model.room.SingleTask
import com.example.mynote.model.room.SubTask
import com.example.mynote.model.room.TaskGroupWithSubtasks
import com.example.mynote.model.room.TaskItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * RecyclerView adapter for displaying tasks (both single and grouped) in a unified list.
 *
 * This adapter handles two types of items:
 * - **SingleTaskItem**: Standalone tasks without subtasks
 * - **TaskGroupWithSubtasksItem**: Group tasks with expandable subtask list
 *
 * **Features:**
 * - Different layouts for each task type
 * - Checkbox for completion status (affects single tasks or entire groups)
 * - Subtask management within group tasks
 * - Click to edit, long-click to delete
 *
 * @param onTaskClicked Callback when a task is clicked (opens edit mode)
 * @param onSingleTaskChecked Callback when a single task checkbox is toggled
 * @param onGroupTaskChecked Callback when a group task checkbox is toggled
 * @param onSubTaskChecked Callback when a subtask checkbox is toggled (groupId, subtaskId, isChecked)
 * @param onSingleTaskDelete Callback when a single task is long-pressed (delete)
 * @param onTaskGroupDelete Callback when a task group is long-pressed (delete)
 * @param onSubTaskDelete Callback when a subtask is long-pressed (delete)
 *
 * @see TaskItem
 * @see SingleTask
 * @see TaskGroupWithSubtasks
 */
class TasksAdapter(
    private val onTaskClicked:(TaskItem) -> Boolean,
    private val onSingleTaskChecked:(Long, Boolean)  -> Boolean,
    private val onGroupTaskChecked:(Long, Boolean)  -> Boolean,
    private val onSubTaskChecked:(Long, Long, Boolean) -> Boolean,  // groupId, subtaskId, isChecked
    private val onSingleTaskDelete: (SingleTask) -> Unit,
    private val onTaskGroupDelete: (TaskGroupWithSubtasks) -> Unit,
    private val onSubTaskDelete: (SubTask) -> Unit
    ): ListAdapter<TaskItem, RecyclerView.ViewHolder>(TaskItemDiffCallback) {

        companion object{
            private const val TYPE_SINGLE_TASK_VIEW = 0
            private const val TYPE_GROUP_TASK_VIEW = 1

            private val TaskItemDiffCallback = object : DiffUtil.ItemCallback<TaskItem>() {
                override fun areItemsTheSame(
                    oldItem: TaskItem,
                    newItem: TaskItem
                ): Boolean {
                    return when{
                        oldItem is TaskItem.SingleTaskItem && newItem is TaskItem.SingleTaskItem -> {
                            oldItem.singleTask.id == newItem.singleTask.id
                        }
                        oldItem is TaskItem.TaskGroupWithSubtasksItem && newItem is TaskItem.TaskGroupWithSubtasksItem -> {
                            oldItem.taskGroupWithSubtasks.taskGroup.id == newItem.taskGroupWithSubtasks.taskGroup.id
                        }
                        else -> false
                    }
                }

                override fun areContentsTheSame(
                    oldItem: TaskItem,
                    newItem: TaskItem
                ): Boolean {
                    return when {
                        oldItem is TaskItem.SingleTaskItem && newItem is TaskItem.SingleTaskItem ->
                            oldItem.singleTask == newItem.singleTask

                        oldItem is TaskItem.TaskGroupWithSubtasksItem && newItem is TaskItem.TaskGroupWithSubtasksItem -> {

                            // First we compare the group
                            if (oldItem.taskGroupWithSubtasks.taskGroup != newItem.taskGroupWithSubtasks.taskGroup) {
                                return false
                            }

                            // Then we compare the subtasks element by element
                            val oldSubtasks = oldItem.taskGroupWithSubtasks.subtasks
                            val newSubtasks = newItem.taskGroupWithSubtasks.subtasks

                            // Checking the size
                            if (oldSubtasks.size != newSubtasks.size) {
                                return false
                            }

                            // We check each element
                            for (i in oldSubtasks.indices) {
                                if (oldSubtasks[i] != newSubtasks[i]) {
                                    return false
                                }
                            }
                            return true
                        }
                        else -> false // Should never reach here if areItemsTheSame is correct
                    }
                }
            }
        }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)){
            is TaskItem.SingleTaskItem -> TYPE_SINGLE_TASK_VIEW
            is TaskItem.TaskGroupWithSubtasksItem -> TYPE_GROUP_TASK_VIEW
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when(viewType){
            TYPE_SINGLE_TASK_VIEW -> {
                val binding = TaskCardBinding.
                inflate(LayoutInflater.from(parent.context), parent, false)

                SingleTaskViewHolder(binding)
            }
            TYPE_GROUP_TASK_VIEW -> {
                val binding = TaskGroupCardBinding.
                inflate(LayoutInflater.from(parent.context), parent, false)

                GroupTaskViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when(holder){
           is SingleTaskViewHolder -> {
               val item = getItem(position) as TaskItem.SingleTaskItem
               holder.bindSingleTask(item.singleTask)
           }
            is GroupTaskViewHolder -> {
                val item = getItem(position) as TaskItem.TaskGroupWithSubtasksItem
                holder.bindGroup(item.taskGroupWithSubtasks)
            }
        }
    }

    /**
     * ViewHolder for a single task (without subtasks).
     *
     * Displays:
     * - Checkbox for completion status
     * - Single task description
     * - Creation timestamp
     *
     * Handles:
     * - Checkbox toggle -> updates completion status
     * - Click -> opens edit mode
     * - Long-click -> deletes the task
     */
    inner class SingleTaskViewHolder(private val binding: TaskCardBinding): RecyclerView.ViewHolder(binding.root){

        fun bindSingleTask(singleTask: SingleTask){
            binding.apply {
                checkBoxSingleTask.setOnCheckedChangeListener(null)
                root.setOnLongClickListener(null)
                descSingleTask.text = singleTask.desc

                if (singleTask.isCompleted != checkBoxSingleTask.isChecked){
                checkBoxSingleTask.isChecked = singleTask.isCompleted
                }
                timeSingleTaskCreate.text = formatTime(singleTask.createdAt)
                checkBoxSingleTask.setOnCheckedChangeListener { _, isChecked ->
                    onSingleTaskChecked(singleTask.id, isChecked)
                }
                root.setOnLongClickListener {
                    onSingleTaskDelete(singleTask)
                    true
                }

                root.setOnClickListener {
                    onTaskClicked(TaskItem.SingleTaskItem(singleTask))

                }
            }
        }
    }

    /**
     * ViewHolder for a group task with subtasks.
     *
     * Displays:
     * - Checkbox for group completion status
     * - Group task description
     * - Creation timestamp
     * - RecyclerView for subtasks
     *
     * Handles:
     * - Group checkbox toggle -> updates the completion status of the group and
     * all subtasks if the group status is "completed".
     * - Subtask checkbox toggle -> updates the completion status of individual subtasks
     * and, depending on the total number of completed subtasks,
     * updates the completion status of the entire group.
     * - Click → opens edit mode
     * - Long-click → deletes entire group
     */
    inner class GroupTaskViewHolder(private val binding: TaskGroupCardBinding): RecyclerView.ViewHolder(binding.root){

        private val subTasksAdapter: SubTasksAdapter = SubTasksAdapter()

        init {
            binding.subtasksRecyclerView.apply {
                adapter = subTasksAdapter
                layoutManager = LinearLayoutManager(context,  RecyclerView.VERTICAL, false)
                isNestedScrollingEnabled = false
            }
        }

        fun bindGroup(taskGroupWithSubtasks: TaskGroupWithSubtasks) {
            binding.apply {

                subTasksAdapter.setOnSubTaskCheckedListener(null)
                checkBoxGroupTask.setOnCheckedChangeListener(null)
                root.setOnLongClickListener(null)

                descGroupTask.text = taskGroupWithSubtasks.taskGroup.desc
                timeGroupTaskCreate.text = formatTime(taskGroupWithSubtasks.taskGroup.createdAt)

                if (taskGroupWithSubtasks.taskGroup.isCompleted != checkBoxGroupTask.isChecked){
                    checkBoxGroupTask.isChecked = taskGroupWithSubtasks.taskGroup.isCompleted
                }

                subTasksAdapter.setOnSubTaskCheckedListener { subtaskId, isChecked ->
                    onSubTaskChecked(taskGroupWithSubtasks.taskGroup.id, subtaskId, isChecked)
                }
                subTasksAdapter.setOnSubTaskDeleteListener { subTask ->
                    onSubTaskDelete(subTask)
                }

                // Submit subtask list
                subTasksAdapter.submitList(taskGroupWithSubtasks.subtasks.toList()) {
                    // After applying the list, we force all visible checkboxes to be updated
                    for (i in 0 until binding.subtasksRecyclerView.childCount) {
                        val holder = binding.subtasksRecyclerView.getChildViewHolder(
                            binding.subtasksRecyclerView.getChildAt(i)
                        ) as? SubTasksAdapter.SubTaskViewHolder

                        holder?.let {
                            val position = it.adapterPosition
                            if (position != RecyclerView.NO_POSITION) {
                                val subtask = taskGroupWithSubtasks.subtasks[position]
                                it.forceUpdateCheckBox(subtask.isCompleted)
                            }
                        }
                    }
                }

                checkBoxGroupTask.setOnCheckedChangeListener { _, isChecked ->
                        onGroupTaskChecked(taskGroupWithSubtasks.taskGroup.id, isChecked)
                }

                root.setOnLongClickListener {
                    onTaskGroupDelete(taskGroupWithSubtasks)
                    true
                }

                root.setOnClickListener {
                    onTaskClicked(TaskItem.TaskGroupWithSubtasksItem(taskGroupWithSubtasks))
                }
            }
        }
    }

    /**
     * Formats a timestamp into a localized date/time string.
     *
     * Uses the device's locale settings:
     * - US format: 12/31/2023, 2:30 PM
     * - European format: 31.12.2023, 14:30
     *
     * @param time Milliseconds since epoch
     * @return Formatted date and time string
     */
    private fun formatTime(time: Long): String{
        val dateInstant = Instant.ofEpochMilli(time)
        val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())
        val formattedDate = dateFormatter.format(dateInstant)
        return formattedDate
    }
}