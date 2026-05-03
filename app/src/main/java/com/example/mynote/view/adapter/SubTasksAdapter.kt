package com.example.mynote.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mynote.databinding.TaskCardBinding
import com.example.mynote.model.room.SubTask
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * RecyclerView adapter for displaying a list of subtasks in [TasksAdapter].
 *
 * This adapter uses [ListAdapter] with [DiffUtil] for efficient updates.
 * It handles two user interactions:
 * - Checkbox click to mark subtask as completed/uncompleted
 * - Long-click to delete a subtask
 *
 * @see SubTask
 */
class SubTasksAdapter(): ListAdapter<SubTask, SubTasksAdapter.SubTaskViewHolder>(SubTaskDiffCallback) {

    private var onSubTaskChecked: ((Long, Boolean) -> Unit)? = null
    private var onSubTaskDelete: ((SubTask) -> Unit)? = null

    fun setOnSubTaskCheckedListener(listener: ((Long, Boolean) -> Unit)?) {
        onSubTaskChecked = listener
    }

    fun setOnSubTaskDeleteListener(listener: ((SubTask) -> Unit)?) {
        onSubTaskDelete = listener
    }

    companion object{
        private val SubTaskDiffCallback = object: DiffUtil.ItemCallback<SubTask>(){
            override fun areItemsTheSame(
                oldItem: SubTask,
                newItem: SubTask
            ): Boolean {
                return oldItem.id == newItem.id && oldItem.taskGroupId == newItem.taskGroupId
            }

            override fun areContentsTheSame(
                oldItem: SubTask,
                newItem: SubTask
            ): Boolean {
                return oldItem.id == newItem.id &&
                        oldItem.taskGroupId == newItem.taskGroupId &&
                        oldItem.desc == newItem.desc &&
                        oldItem.isCompleted == newItem.isCompleted &&
                        oldItem.createdAt == newItem.createdAt
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SubTaskViewHolder {
        val binding = TaskCardBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return SubTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubTaskViewHolder, position: Int) {
        holder.bindSubTask(getItem(position))
    }

    inner class SubTaskViewHolder( private val binding: TaskCardBinding): RecyclerView.ViewHolder(binding.root) {

        private var currentSubTask: SubTask? = null

        fun bindSubTask(subTask: SubTask) {
             currentSubTask = subTask

            binding.apply {

                checkBoxSingleTask.setOnClickListener(null)
                root.setOnLongClickListener(null)

                if (subTask.isCompleted != checkBoxSingleTask.isChecked) {
                    checkBoxSingleTask.isChecked = subTask.isCompleted
                    }

                descSingleTask.text = subTask.desc
                timeSingleTaskCreate.text = formatTime(subTask.createdAt)

                checkBoxSingleTask.setOnCheckedChangeListener { _, isChecked ->

                    currentSubTask?.let { task ->
                            onSubTaskChecked?.invoke(task.id, isChecked)
                    }
                }

                root.setOnLongClickListener {
                    onSubTaskDelete?.invoke(subTask)
                    true
                }
            }
        }

        fun forceUpdateCheckBox(isCompleted: Boolean) {
            if (binding.checkBoxSingleTask.isChecked != isCompleted) {
                binding.checkBoxSingleTask.isChecked = isCompleted
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
    private fun formatTime(time: Long): String {
        val dateInstant = Instant.ofEpochMilli(time)
        val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
        val formattedDate = dateFormatter.format(dateInstant)
        return formattedDate
    }
}
