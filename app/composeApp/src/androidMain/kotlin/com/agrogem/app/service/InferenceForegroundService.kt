package com.agrogem.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.agrogem.app.MainActivity
import com.agrogem.app.R

/**
 * Lightweight foreground service whose only job is to elevate process priority
 * while a Gemma inference is running. The model occupies ~2.5GB and Android's
 * Low-Memory Killer is happy to evict the largest cached process when other
 * apps demand memory; declaring the inference as user-visible ongoing work
 * via a foreground service makes the OS treat the process as critical.
 *
 * Lifecycle is reference-counted so chat + analysis can overlap without
 * stopping each other (see [InferenceLifecycle]).
 */
class InferenceForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        // Per Android docs: startForeground MUST be called within 5s of
        // startForegroundService or the OS kills the process with
        // ForegroundServiceDidNotStartInTimeException — so we post the
        // notification in onCreate, before any onStartCommand work.
        val notification = buildNotification(text = DEFAULT_TEXT)
        startInForegroundCompat(notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val label = intent?.getStringExtra(EXTRA_LABEL)?.takeIf { it.isNotBlank() }
        if (label != null) {
            // Update the notification text on subsequent start calls — e.g. when
            // chat starts during an analysis run, the most recent label wins.
            val mgr = getSystemService(android.app.NotificationManager::class.java)
            mgr?.notify(NOTIFICATION_ID, buildNotification(text = label))
        }
        // Don't auto-restart if the system kills us — the engine state is gone
        // with the process, so a relaunch with no client wouldn't accomplish
        // anything useful.
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires the type to match the manifest declaration.
            // dataSync fits "processing user data on-device" and avoids the
            // Play-Console-rationale hoops of specialUse.
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AgroGem")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "inference_channel"
        const val EXTRA_LABEL = "label"
        private const val NOTIFICATION_ID = 1001
        private const val DEFAULT_TEXT = "Procesando con Gemma…"
        private const val TAG = "InferenceFgService"

        fun newStartIntent(context: Context, label: String): Intent =
            Intent(context, InferenceForegroundService::class.java)
                .putExtra(EXTRA_LABEL, label)

        fun newStopIntent(context: Context): Intent =
            Intent(context, InferenceForegroundService::class.java)

        fun logState(message: String) {
            Log.d(TAG, message)
        }
    }
}
