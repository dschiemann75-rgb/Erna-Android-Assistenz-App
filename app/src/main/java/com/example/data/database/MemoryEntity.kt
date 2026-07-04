package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val category: String = "general", // "note", "experience", "address", "job", "milestone"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1, // Always 1 for single-user profile
    val name: String = "",
    val occupation: String = "",
    val address: String = "",
    val additionalInfo: String = ""
)
