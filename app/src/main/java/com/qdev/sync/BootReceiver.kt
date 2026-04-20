package com.qdev.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Android tự động kích hoạt lại NotificationListenerService
            // Không cần code thêm ở đây, nhưng class này giúp hệ thống nhận biết
            // app có quyền nhận sự kiện Boot.
        }
    }
}
