package com.example.chongchenlearn901.notificationdemo.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.example.chongchenlearn901.notificationdemo.MainActivity
import com.example.chongchenlearn901.notificationdemo.constant.ConstantStrings
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

/**
 * Created by chongchen on 2018-01-15.
 */

class MQTTService: Service(){
    companion object {
        val NOTIFICATION_ID = "notification_id_01"
        private val TAG = "MQTTService"
        private val SERVER_URI = "tcp://192.168.2.75:1883"
    }

    private lateinit var mqttClient: MqttAndroidClient
    private var builder: NotificationCompat.Builder? = null
    private lateinit var intent:Intent
    private lateinit var pendingIntent: PendingIntent

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        return Service.START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        intent = Intent(applicationContext, MainActivity::class.java)
        pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)

        connectMqtt()
    }

    override fun onDestroy() {
        Log.d(TAG, "destroy")
        mqttClient.unregisterResources()
        sendBroadcast(Intent(ConstantStrings.ACTION_RESTART))
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved")
        sendBroadcast(Intent(ConstantStrings.ACTION_RESTART))
        super.onTaskRemoved(rootIntent)
    }


    private fun connectMqtt(){
        val androidId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        mqttClient = MqttAndroidClient(applicationContext, SERVER_URI, androidId)
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverUri: String?) {
                if(reconnect){
                    Log.d(TAG, "reconnect to $serverUri")
                }else{
                    Log.d(TAG, "connected to $serverUri")
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                message?.let {
                    Log.d(TAG, "message arrived: topic: $topic, message: ${String(message.payload)}")
                }?: run{
                    Log.d(TAG, "message arrived: topic: $topic, message is empty.")
                }
            }
            override fun deliveryComplete(p0: IMqttDeliveryToken?) {}

            override fun connectionLost(p0: Throwable?) {
                Log.d(TAG, "disconnected")
            }
        })
        val mqttOption = MqttConnectOptions()
        mqttOption.isAutomaticReconnect = true
        mqttOption.isCleanSession = false

        mqttClient.connect(mqttOption, null, mqttConnectListener)
    }

    private val mqttConnectListener = object: IMqttActionListener{
        override fun onSuccess(p0: IMqttToken?) {
            val disconnectBufferOption = DisconnectedBufferOptions()
            disconnectBufferOption.isBufferEnabled = true
            disconnectBufferOption.bufferSize = 100
            disconnectBufferOption.isPersistBuffer = false
            disconnectBufferOption.isDeleteOldestMessages = false
            mqttClient.setBufferOpts(disconnectBufferOption)

            builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    // let notification popup
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setPriority(NotificationManager.IMPORTANCE_HIGH)

            mqttClient.subscribe("test", 2, subscribe)

        }

        override fun onFailure(p0: IMqttToken?, p1: Throwable?) {
            Log.e(TAG, "failed to connect to ${SERVER_URI}")
        }
    }

    val subscribe = IMqttMessageListener{
        topic, message ->
        message?.let {
            Log.d(TAG, "topic: $topic, message: ${String(message.payload)}")

            builder?.let {
                builder!!.setContentTitle("First notification")
                        .setContentText(String(message.payload))
                        .setSubText("first sub text")
                        .setContentIntent(pendingIntent)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1, builder!!.build())
            }?: run{
                Log.d(TAG, "builder is null.")
            }

        }?: run{
            Log.d(TAG, "topic: $topic, message is empty.")
        }
    }

}