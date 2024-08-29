package com.example.sendsms.database.repository

import com.example.sendsms.database.entity.User
import com.example.sendsms.database.dao.UserDao
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }

    suspend fun insertUser(user: User) {
        userDao.insert(user)
    }
}
