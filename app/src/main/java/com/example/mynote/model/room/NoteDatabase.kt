package com.example.mynote.model.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mynote.model.converters.DateConverters

/**
 * Room database class for the application.
 *
 * This database manages all data persistence for notes and tasks.
 * It contains tables for:
 * - Notes (notetable)
 * - Single tasks (single_tasks)
 * - Task groups (task_groups)
 * - Subtasks (subtasks)
 *
 * The database uses Room's migration system to handle schema updates.
 * Current version: 2
 *
 * @see Note
 * @see SingleTask
 * @see TaskGroup
 * @see SubTask
 * @see NoteDao
 */
@Database(entities = [Note::class, SingleTask::class, TaskGroup::class, SubTask::class], version = 2, exportSchema = true)
@TypeConverters(DateConverters::class)
abstract class NoteDatabase: RoomDatabase() {

    //Provides access to the Data Access Object (DAO) for database operations.
    abstract fun getDao(): NoteDao
    companion object{
        @Volatile
        private var INSTANCE: NoteDatabase? = null
        private val lock = Any()

        /**
         * Migration from version 1 to version 2.
         *
         * This migration adds new tables for tasks functionality:
         * - single_tasks: Standalone tasks without subtasks
         * - task_groups: Group tasks that can contain subtasks
         * - subtasks: Individual subtasks belonging to task groups
         *
         * The migration creates tables with appropriate foreign key constraints
         * and indexes for optimal query performance.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2){
            override fun migrate(db: SupportSQLiteDatabase) {
                db.beginTransaction()
                try {
                    db.execSQL("""
                CREATE TABLE single_tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    desc TEXT,
                    isCompleted INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL
                )
            """)

                    db.execSQL("""
                CREATE TABLE task_groups (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    desc TEXT,
                    isCompleted INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL
                )
            """)

                    db.execSQL("""
                CREATE TABLE subtasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    taskGroupId INTEGER NOT NULL,
                    desc TEXT,
                    isCompleted INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(taskGroupId) REFERENCES task_groups(id) ON UPDATE CASCADE ON DELETE CASCADE
                )
            """)

                    // Create index for foreign key
                    db.execSQL("CREATE INDEX index_subtasks_taskGroupId ON subtasks(taskGroupId)")

                    db.setTransactionSuccessful()

                } finally {
                    db.endTransaction()
                }
            }
        }

        /**
         * Gets the singleton instance of the database.
         *
         * Uses double-checked locking for thread-safe initialization.
         * The instance is created only once and reused for the entire application.
         *
         * @param context Application context (used to get applicationContext)
         * @return The singleton NoteDatabase instance
         */
          fun getInstance(context: Context) : NoteDatabase{

                    return INSTANCE ?: synchronized(lock){
                        val instance = Room.databaseBuilder(context.applicationContext, NoteDatabase::class.java, "note_db"  )
                            .addMigrations(MIGRATION_1_2)
                            .build()
                        INSTANCE = instance
                        instance
                    }
         }
    }

}