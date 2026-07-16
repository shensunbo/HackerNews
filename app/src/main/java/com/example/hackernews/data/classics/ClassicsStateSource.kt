package com.example.hackernews.data.classics

import kotlinx.coroutines.flow.Flow

/** Read/write the persisted [ClassicsState] (DataStore-backed in production). */
interface ClassicsStateSource {
    fun classicsState(): Flow<ClassicsState?>
    suspend fun saveClassicsState(state: ClassicsState)
}
