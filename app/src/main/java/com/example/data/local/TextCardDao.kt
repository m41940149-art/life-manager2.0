package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TextCardDao {
    @Query("SELECT * FROM text_cards ORDER BY updatedAt DESC")
    fun getAllCards(): Flow<List<TextCardEntity>>

    @Query("SELECT * FROM text_cards WHERE folderId = :folderId ORDER BY updatedAt DESC")
    fun getCardsByFolder(folderId: String): Flow<List<TextCardEntity>>

    @Query("SELECT * FROM text_cards WHERE folderId IS NULL ORDER BY updatedAt DESC")
    fun getRootCards(): Flow<List<TextCardEntity>>

    @Query("SELECT * FROM text_cards WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteCards(): Flow<List<TextCardEntity>>

    @Query("SELECT * FROM text_cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: String): TextCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: TextCardEntity)

    @Update
    suspend fun updateCard(card: TextCardEntity)

    @Query("UPDATE text_cards SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE text_cards SET folderId = :newFolderId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun moveCardToFolder(id: String, newFolderId: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM text_cards WHERE id = :id")
    suspend fun deleteCard(id: String)

    @Query("DELETE FROM text_cards WHERE folderId = :folderId")
    suspend fun deleteCardsByFolder(folderId: String)

    @Query("SELECT COUNT(*) FROM text_cards")
    suspend fun getCardCount(): Int

    @Query("SELECT * FROM text_cards")
    suspend fun getAllCardsSync(): List<TextCardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<TextCardEntity>)
}
