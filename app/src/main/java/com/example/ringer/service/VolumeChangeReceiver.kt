package com.example.ringer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

// Optional receiver if you want to listen to volume broadcast (not reliable on all devices)
class VolumeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
            val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
            if (streamType == AudioManager.STREAM_MUSIC) {
                // Volume changed - can be handled in service if needed
            }
        }
    }
}