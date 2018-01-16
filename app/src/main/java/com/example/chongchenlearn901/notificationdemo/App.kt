package com.example.chongchenlearn901.notificationdemo

import android.app.Application
import android.content.Intent
import com.example.chongchenlearn901.notificationdemo.constant.ConstantStrings

/**
 * Created by chongchen on 2018-01-15.
 */

class App: Application(){
    companion object {
        private val TAG = "App"
    }
    override fun onCreate() {
        super.onCreate()
        sendBroadcast(Intent(ConstantStrings.ACTION_RESTART))
    }
}