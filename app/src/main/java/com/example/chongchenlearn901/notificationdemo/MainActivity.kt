package com.example.chongchenlearn901.notificationdemo

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


class MainActivity : AppCompatActivity() {
    companion object {
        val NOTIFICATION_ID = "notification_id_01"
        private val TAG = "MainActivity"
        private val SERVER_URI = "tcp://192.168.2.75:1883"
    }
    private lateinit var mqttAndroidClient: MqttAndroidClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btnSend = findViewById<Button>(R.id.btnSend)

        mqttAndroidClient = MqttAndroidClient(this, SERVER_URI, "android")

        mqttAndroidClient.setCallback(object : MqttCallbackExtended{
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

        mqttAndroidClient.connect(mqttOption, null, object: IMqttActionListener{
            override fun onSuccess(p0: IMqttToken?) {
                val disconnectBufferOption = DisconnectedBufferOptions()
                disconnectBufferOption.isBufferEnabled = true
                disconnectBufferOption.bufferSize = 100
                disconnectBufferOption.isPersistBuffer = false
                disconnectBufferOption.isDeleteOldestMessages = false

                mqttAndroidClient.setBufferOpts(disconnectBufferOption)
                mqttAndroidClient.subscribe("test", 2, subscribe)
            }

            override fun onFailure(p0: IMqttToken?, p1: Throwable?) {
                Log.e(TAG, "failed to connect to $SERVER_URI")
            }
        })

        btnSend.setOnClickListener {
            val message = MqttMessage()
            message.payload = "send from android app".toByteArray(Charsets.UTF_8)
            mqttAndroidClient.publish("test", message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttAndroidClient.disconnect()
    }

    val subscribe = IMqttMessageListener{
        topic, message ->
            message?.let {
                val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://developer.android.com/reference/android/app/Notification.html"))
                val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)

                val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle("First notification")
                        .setContentText(String(message.payload))
                        .setSubText("first sub text")
                        .setContentIntent(pendingIntent)
                        // let notification popup
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setPriority(NotificationManager.IMPORTANCE_HIGH)

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1, builder.build())

                Log.d(TAG, "topic: $topic, message: ${String(message.payload)}")
            }?: run{
                Log.d(TAG, "topic: $topic, message is empty.")
            }
    }
}
