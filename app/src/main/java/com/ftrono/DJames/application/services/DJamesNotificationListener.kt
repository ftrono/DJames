package com.ftrono.DJames.application.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log


class DJamesNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "MyNotificationListener"

        // Optional: static reference to check connection state
        var instance: DJamesNotificationListener? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "NotificationListener created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "NotificationListener disconnected")
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Optional: you can monitor when Spotify/Deezer posts media notifications
//        if (sbn.packageName == "com.spotify.music") {
//            Log.d(TAG, "Spotify notification posted")
//        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Optional
    }
}