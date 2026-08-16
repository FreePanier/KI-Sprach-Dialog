package com.sprachbruecke.translator.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversation_blocks WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getBlocksForSession(sessionId: Long): Flow<List<ConversationBlock>>

    @Query("SELECT * FROM conversation_blocks ORDER BY timestamp ASC")
    suspend fun getAllBlocks(): List<ConversationBlock>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: ConversationBlock): Long

    @Update
    suspend fun updateBlock(block: ConversationBlock)

    @Delete
    suspend fun deleteBlock(block: ConversationBlock)

    @Query("DELETE FROM conversation_blocks WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("SELECT MAX(sessionId) FROM conversation_blocks")
    suspend fun getLatestSessionId(): Long?
}
