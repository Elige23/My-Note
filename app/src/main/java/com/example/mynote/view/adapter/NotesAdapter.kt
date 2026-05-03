package com.example.mynote.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mynote.databinding.NoteCardBinding
import com.example.mynote.model.room.Note
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * RecyclerView adapter for displaying a list of notes.
 *
 * This adapter uses [ListAdapter] with [DiffUtil] for efficient updates.
 * It handles two user interactions:
 * - Click on a note to edit it
 * - Click on delete button to remove the note
 *
 * @param onNoteClick Callback invoked when a note item is clicked (opens edit mode)
 * @param deleteNoteClick Callback invoked when the delete button is clicked
 *
 * @see Note
 */
class NotesAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val deleteNoteClick: (Note) -> Unit): ListAdapter<Note, NotesAdapter.NoteViewHolder>(NotesDiffCallback){

    companion object {
        private val NotesDiffCallback = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean =
                oldItem.title == newItem.title &&
                        oldItem.desc == newItem.desc &&
                        oldItem.id == newItem.id
                        && oldItem.time == newItem.time
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = NoteCardBinding.inflate(LayoutInflater.from(parent.context), parent, false )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val currentNote = getItem(position)
        holder.title.text = currentNote.title
        holder.desc.text = currentNote.desc

        // Format creation date using device locale
        // ZoneId.systemDefault() uses the language/region set by the user on the phone
        // Examples:
        // - US: 12/31/2023, 2:30 PM
        // - Europe: 31.12.2023, 14:30
        val dateInstant = Instant.ofEpochMilli(currentNote.time)
        val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())
        val formattedDate = dateFormatter.format(dateInstant)
        holder.time.text = formattedDate

        holder.itemView.setOnClickListener {
            onNoteClick(currentNote)
        }
        holder.deleteButton.setOnClickListener {deleteNoteClick(currentNote) }
    }

    /**
     * ViewHolder for displaying a note item in the RecyclerView.
     *
     * Displays:
     * - Note title
     * - Note description
     * - Creation timestamp
     * - Button for deleting the note
     */
    class NoteViewHolder(binding: NoteCardBinding):RecyclerView.ViewHolder(binding.root){
        val title:TextView = binding.noteTitle
        val desc:TextView = binding.noteDescription
        val time:TextView = binding.timeCreate
        val deleteButton = binding.deleteNoteCard
    }
}