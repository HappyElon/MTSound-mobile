package ru.happyelon.webappv2

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val channelId = "MyChannel"
    private val notificationId = 1
    private val ACTION_MUTE = "ru.mtsound.ACTION_MUTE"
    private var isMuted = false

    inner class MusicBinder : Binder() {
        fun getService(): MusicService {
            return this@MusicService
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return MusicBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val musicUrl = "http://air.radioulitka.ru:8000/ulitka_128"

        if (intent?.action == ACTION_MUTE) {
            isMuted = !isMuted
            if (isMuted) {
                mediaPlayer?.setVolume(0f, 0f) // Mute
            } else {
                mediaPlayer?.setVolume(1f, 1f) // Unmute
            }

            // Determine sound state and update notification
            showNotification("Now Playing", "Livestream...", isMuted)
        } else {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
                mediaPlayer?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                mediaPlayer?.setDataSource(musicUrl)
                mediaPlayer?.prepareAsync()
                mediaPlayer?.setOnPreparedListener { mediaPlayer ->
                    Log.d("MusicService", "MediaPlayer prepared")
                    try {
                        showNotification("Now Playing", "Livestream...", isMuted)
                        mediaPlayer.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        return START_STICKY
    }

    private fun showNotification(title: String, content: String, isMuted: Boolean) {
        Log.d("MusicService", "showNotification called")
        createNotificationChannel()

        val notificationIntent = Intent(this, MusicService::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(this, MusicService::class.java)
        muteIntent.action = ACTION_MUTE
        val mutePendingIntent = PendingIntent.getService(this, 0, muteIntent, PendingIntent.FLAG_IMMUTABLE)

        val muteActionText = if (isMuted) "Unmute" else "Mute"

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.player_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_mute, muteActionText, mutePendingIntent)
            .build()

        startForeground(notificationId, notification)
        Log.d("MusicService", "Notification started")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "My Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        Log.d("MusicService", "Notification channel created")
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}


