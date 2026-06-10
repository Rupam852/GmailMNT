package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDao {

    // --- Email Accounts ---
    @Query("SELECT * FROM email_accounts")
    fun getAllAccounts(): Flow<List<EmailAccount>>

    @Query("SELECT * FROM email_accounts")
    suspend fun getAllAccountsDirect(): List<EmailAccount>

    @Query("SELECT * FROM email_accounts WHERE email = :email LIMIT 1")
    suspend fun getAccountByEmail(email: String): EmailAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: EmailAccount)

    @Query("DELETE FROM email_accounts WHERE email = :email")
    suspend fun deleteAccount(email: String)


    // --- Email Messages ---
    @Query("SELECT * FROM email_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<EmailMessage>>

    @Query("SELECT * FROM email_messages WHERE accountEmail = :email ORDER BY timestamp DESC")
    fun getMessagesForAccount(email: String): Flow<List<EmailMessage>>

    @Query("SELECT * FROM email_messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): EmailMessage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<EmailMessage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: EmailMessage)

    @Update
    suspend fun updateMessage(message: EmailMessage)

    @Query("DELETE FROM email_messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("UPDATE email_messages SET isRead = :isRead WHERE id = :id")
    suspend fun updateMessageReadStatus(id: String, isRead: Boolean)

    @Query("UPDATE email_messages SET isRead = :isRead")
    suspend fun updateAllMessagesReadStatus(isRead: Boolean)

    @Query("UPDATE email_messages SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateMessageStarredStatus(id: String, isStarred: Boolean)

    @Query("UPDATE email_messages SET label = :label WHERE id = :id")
    suspend fun updateMessageLabel(id: String, label: String)

    @Query("""
        SELECT * FROM email_messages 
        WHERE (sender LIKE '%' || :query || '%' 
           OR senderName LIKE '%' || :query || '%' 
           OR subject LIKE '%' || :query || '%' 
           OR body LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun searchMessages(query: String): Flow<List<EmailMessage>>
}
