package com.example.mynote.model.repository

import android.app.Application
import com.example.mynote.model.room.NoteDatabase

/**
 * Custom Application class that provides global access to the repository.
 *
 * Initializes the database singleton and creates a single instance of NoteRepository
 * that can be accessed from anywhere in the application.
 *
 * @see NoteRepository
 * @see NoteDatabase
 */
class NoteApplicationRepository: Application() {

    val noteRepository: NoteRepository by lazy {
        NoteRepository(NoteDatabase.getInstance(this).getDao())
    }
}

