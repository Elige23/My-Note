package com.example.mynote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mynote.model.repository.NoteRepository

/**
 * Factory class for creating instances of [NoteViewModel].
 *
 * This factory is required when a ViewModel has constructor parameters.
 * It provides the necessary dependencies (repository) to the ViewModel.
 *
 * Usage:
 * ```
 * val viewModel: NoteViewModel by viewModels {
 *     NoteViewModelFactory(repository)
 * }
 * ```
 *
 * @param repository The repository that the ViewModel needs for data operations
 *
 * @see NoteViewModel
 * @see ViewModelProvider.Factory
 * @see NoteRepository
 */

class NoteViewModelFactory(private val repository: NoteRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        return when {
            modelClass.isAssignableFrom(NoteViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                NoteViewModel(repository) as T
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

