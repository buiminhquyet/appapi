package com.qdev.pro

import android.content.*
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.qdev.pro.databinding.ActivityMainBinding
import okhttp3.*
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: SettingsManager
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.qdev.pro.LOG_UPDATE") {
                val message = intent.getStringExtra("message") ?: ""
                appendLog(message)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.hide()
        
        settings = SettingsManager(this)
        
        // Load data
        binding.editUrl.setText(settings.apiUrl)
        binding.editToken.setText(settings.apiToken)
        binding.switchService.isChecked = settings.isServiceEnabled

        setupWebView()
        setupListeners()
        checkPermission()
        
        registerReceiver(receiver, IntentFilter("com.qdev.pro.LOG_UPDATE"))
        appendLog("App đã khởi động.")
    }

    private fun setupWebView() {
        val webView = binding.webView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // Tải giao diện Web đã được nhúng sẵn bên trong APK (Offline, không cần IP)
        webView.loadUrl("file:///android_asset/index.html")
        binding.settingsLayout.visibility = View.GONE
    }

    private fun setupListeners() {
        binding.btnPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        binding.btnBattery.setOnClickListener {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback for some devices
                startActivity(Intent(Settings.ACTION_SETTINGS))
                Toast.makeText(this, "Hãy tìm mục Pin > Tối ưu hóa pin và chọn 'Không hạn chế'", Toast.LENGTH_LONG).show()
            }
        }



        binding.btnSave.setOnClickListener {
            settings.apiUrl = binding.editUrl.text.toString()
            settings.apiToken = binding.editToken.text.toString()
            settings.isServiceEnabled = binding.switchService.isChecked
            
            appendLog("Đã lưu cấu hình mới.")
            Toast.makeText(this, "Đã lưu cài đặt!", Toast.LENGTH_SHORT).show()
            
            // Reload WebView with new URL
            setupWebView()
            binding.settingsLayout.visibility = View.GONE
        }

        binding.btnTest.setOnClickListener {
            testConnection()
        }

        binding.btnClearLogs.setOnClickListener {
            binding.txtLogs.text = ""
            appendLog("Đã xóa nhật ký.")
        }
    }

    private fun testConnection() {
        val url = binding.editUrl.text.toString()
        val token = binding.editToken.text.toString()
        
        if (url.isEmpty()) {
            appendLog("Lỗi: URL không được để trống khi test.")
            return
        }

        appendLog("Đang thử kết nối tới: $url ...")
        
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("X-Private-Token", token)
            .post(FormBody.Builder()
                .add("test", "1")
                .add("token", token)
                .build())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    appendLog("Lỗi kết nối: ${e.message}")
                    Toast.makeText(this@MainActivity, "Kết nối thất bại!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val body = response.body?.string() ?: ""
                runOnUiThread {
                    if (code == 200) {
                        appendLog("Kết nối THÀNH CÔNG! Server phản hồi: $body")
                        Toast.makeText(this@MainActivity, "Kết nối OK!", Toast.LENGTH_SHORT).show()
                    } else {
                        appendLog("Lỗi Server: Mã lỗi $code | Phản hồi: $body")
                        Toast.makeText(this@MainActivity, "Lỗi Server ($code)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun checkPermission() {
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val isGranted = listeners != null && listeners.contains(packageName)
        
        if (isGranted) {
            binding.txtPermissionStatus.text = "Trạng thái quyền: ĐÃ CẤP ✅"
            binding.txtPermissionStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            appendLog("Quyền truy cập thông báo: OK.")
        } else {
            binding.txtPermissionStatus.text = "Trạng thái quyền: CHƯA CẤP ❌"
            binding.txtPermissionStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            appendLog("Cảnh báo: Quyền truy cập thông báo chưa được cấp!")
        }
    }

    private fun appendLog(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val currentText = binding.txtLogs.text.toString()
        val newText = "[$time] $message\n$currentText"
        binding.txtLogs.text = newText
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }
}
