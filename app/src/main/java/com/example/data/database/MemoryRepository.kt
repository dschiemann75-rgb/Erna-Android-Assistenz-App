package com.example.data.database

import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()
    val profileFlow: Flow<ProfileEntity?> = memoryDao.getProfileFlow()

    suspend fun insertMemory(content: String, category: String = "general") {
        memoryDao.insertMemory(MemoryEntity(content = content, category = category))
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemory(id)
    }

    suspend fun clearAll() {
        memoryDao.clearAllMemories()
    }

    suspend fun searchMemories(query: String): List<MemoryEntity> {
        return memoryDao.searchMemories(query)
    }

    suspend fun getProfileDirect(): ProfileEntity? {
        return memoryDao.getProfile()
    }

    suspend fun saveProfile(profile: ProfileEntity) {
        memoryDao.saveProfile(profile)
    }
}
