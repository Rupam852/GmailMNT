package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EmailAccount::class, EmailMessage::class, OutboxMessage::class, Attachment::class], version = 7, exportSchema = false)
abstract class EmailDatabase : RoomDatabase() {
    abstract fun emailDao(): EmailDao

    companion object {
        @Volatile
        private var INSTANCE: EmailDatabase? = null

        fun getDatabase(context: Context): EmailDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        EmailDatabase::class.java,
                        "gemini_mail_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                } catch (e: Exception) {
                    Log.e("EmailDatabase", "Failed to create database, attempting fallback", e)
                    // If database creation fails, delete the corrupted database and retry
                    context.deleteDatabase("gemini_mail_database")
                    Room.databaseBuilder(
                        context.applicationContext,
                        EmailDatabase::class.java,
                        "gemini_mail_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                }
                INSTANCE = instance
                instance
            }
        }
    }
}
