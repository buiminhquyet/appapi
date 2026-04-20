package com.qdev.pro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.*
import java.io.IOException

class BankNotificationService : NotificationListenerService() {

    private lateinit var settings: SettingsManager
    private val client = OkHttpClient()
    private val CHANNEL_ID = "BankMonitorChannel"
    private val NOTIFICATION_ID = 99

    override fun onCreate() {
        super.onCreate()
        settings = SettingsManager(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        sendStatusBroadcast("Dịch vụ TIỀN TUYẾN đã khởi động thành công.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Giám sát Ngân hàng",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kênh này giúp App chạy ngầm vĩnh viễn."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("QDev Sync PRO is ACTIVE")
            .setContentText("Hệ thống đang giám sát 24/7.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!settings.isServiceEnabled) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val subText = extras.getCharSequence("android.subText")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        val tickerText = sbn.notification.tickerText?.toString() ?: ""

        // DEEP DUMP QUÉT MỌI DỮ LIỆU ẨN
        var dump = ""
        try {
            val keys = extras.keySet()
            for (key in keys) {
                dump += "[$key: ${extras.get(key)}] "
            }
        } catch (e: Exception) {}
        
        val fullReport = "App: $packageName | T: $title | C: $text | S: $subText | B: $bigText | Tick: $tickerText | Dump: $dump".trim()
        
        // NHẬT KÝ CHI TIẾT
        if (fullReport.isNotEmpty()) {
            sendStatusBroadcast("QUÉT: $fullReport")
        }
        
        // KIỂM TRA TỪ KHÓA NẠP TIỀN (Hỗ trợ NAPTIEN, QUYETDEV, QDEV, QD)
        if (fullReport.contains("NAPTIEN", ignoreCase = true) ||
            fullReport.contains("QUYETDEV", ignoreCase = true) || 
            fullReport.contains("QDEV", ignoreCase = true) || 
            fullReport.contains("QD", ignoreCase = true)) {
            
            sendStatusBroadcast("==> KHỚP TỪ KHÓA! Đang gửi đến Server nạp tiền...")
            sendToServer(title, fullReport)
        }
    }

    private fun sendToServer(title: String, content: String) {
        val url = settings.apiUrl
        val token = settings.apiToken

        if (url.isEmpty()) {
            sendStatusBroadcast("Lỗi: Chưa cấu hình URL.")
            return
        }

        val body = FormBody.Builder()
            .add("token", token)
            .add("title", title)
            .add("content", content)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("X-Private-Token", token)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                sendStatusBroadcast("Gửi thất bại: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                if (code == 200) {
                    sendStatusBroadcast("NẠP TIỀN THÀNH CÔNG! ✅")
                } else {
                    sendStatusBroadcast("Lỗi Server ($code)")
                }
                response.close()
            }
        })
    }
    
    private fun sendStatusBroadcast(message: String) {
        val intent = Intent("com.qdev.pro.LOG_UPDATE")
        intent.putExtra("message", message)
        sendBroadcast(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }
}
