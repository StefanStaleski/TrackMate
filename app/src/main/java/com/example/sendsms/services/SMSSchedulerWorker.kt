package com.example.sendsms.services

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.sendsms.utils.sendSMS

class SMSSchedulerWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val phoneNumber = inputData.getString("PHONE_NUMBER") ?: return Result.failure()
        val message = inputData.getString("MESSAGE") ?: return Result.failure()
        
        return try {
            Log.d("SMSSchedulerWorker", "Sending SMS to $phoneNumber with message: $message")
            sendSMS(phoneNumber, message)
            Result.success()
        } catch (e: Exception) {
            Log.e("SMSSchedulerWorker", "Failed to send SMS", e)
            Result.failure()
        }
    }
} 