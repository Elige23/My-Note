package com.example.mynote.model.room

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Entity representing a note in the database.
 *
 * Implements Parcelable via Kotlin's @Parcelize annotation
 * @property id Auto-generated primary key
 * @property title Note title
 * @property desc Note description
 * @property time Creation timestamp (milliseconds since epoch)
 */
@Entity(tableName = "notetable")
@Parcelize
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String?,
    val desc: String?,
    @ColumnInfo
    val time: Long = System.currentTimeMillis()
): Parcelable
