package com.example.sendsms.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.sendsms.database.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User)

    @Query("SELECT * FROM user WHERE username = :username LIMIT 1")
    fun getUserByUsername(username: String): Flow<User?>

    @Query("DELETE FROM user")
    suspend fun deleteAll()
}
