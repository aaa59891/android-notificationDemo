package com.example.chongchenlearn901.notificationdemo.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.chongchenlearn901.notificationdemo.services.MQTTService

/**
 * Created by chongchen on 2018-01-15.
 */

const val ACTION_RESTART = "MQTTRestart"

class MQTTRestartReceiver: BroadcastReceiver() {
    companion object {
        val TAG = "MQTTRestartReceiver"
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "receive message")
        if(context != null && intent != null){
            val action = intent.action
            Log.d(TAG, "get action $action")
            if(action == ACTION_RESTART || action == Intent.ACTION_BOOT_COMPLETED){
                context.startService(Intent(context, MQTTService::class.java))
            }
        }
    }

}