package com.ftrono.DJames.application.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log


class DJamesNotificationListener : NotificationListenerService() {

    companion object {
        private val TAG = this::class.java.simpleName

        // Static reference to check connection state:
        var instance: DJamesNotificationListener? = null
            private set
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.d(TAG, "NotificationListener CONNECTED")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        Log.d(TAG, "NotificationListener DISCONNECTED")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // TODO: Specify per-app behaviour:
//        if (sbn.packageName == "com.spotify.music") {
//            Log.d(TAG, "Spotify notification posted")
//        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // TODO
    }

    override fun onDestroy() {
        instance = null
        Log.d(TAG, "NotificationListener ENDED")
        super.onDestroy()
    }
}