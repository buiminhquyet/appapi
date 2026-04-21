package com.qdev.pro

import android.content.*
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
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
        
        // 1. Tận dụng toàn bộ màn hình một cách ổn định
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Ẩn thanh tiêu đề một cách an toàn
        supportActionBar?.hide()
        
        settings = SettingsManager(this)
        
        // Load data
        binding.editUrl.setText(settings.apiUrl)
        binding.editToken.setText(settings.apiToken)
        binding.switchService.isChecked = settings.isServiceEnabled

        setupWebView()
        setupListeners()
        checkPermission()
        
        // Kích hoạt nhận Cookie cho Hosting (Duy trì đăng nhập)
        val cookieManager = android.webkit.CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webView, true)
        
        try {
            registerReceiver(receiver, IntentFilter("com.qdev.pro.LOG_UPDATE"))
        } catch (e: Exception) {}
        
        appendLog("App đã khởi động.")
    }

    private fun setupWebView() {
        val webView = binding.webView
        val ws = webView.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true
        ws.allowFileAccess = true
        ws.allowContentAccess = true
        ws.loadWithOverviewMode = true
        ws.useWideViewPort = true
        ws.builtInZoomControls = false
        ws.displayZoomControls = false
        ws.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        
        // --- ADDED: BRIDGE TO CONNECT WEB UI WITH ANDROID SETTINGS ---
        webView.addJavascriptInterface(WebAppInterface(settings), "AndroidBridge")
        
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                
                // 1. Xử lý các App Intent đặc biệt của chúng ta
                if (url.startsWith("appintent://")) {
                    when {
                        url.contains("request_notification") -> {
                            startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                        url.contains("request_battery") -> {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            try { startActivity(intent) } catch (e: Exception) {
                                startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
                            }
                        }
                    }
                    return true 
                }

                // 2. Xử lý Link Tài liệu Tài liệu Nội bộ (giữ trong app)
                if (url.startsWith("file:///android_asset/")) {
                    return false // Cho phép WebView load bình thường
                }

                // 3. Xử lý mở nhanh ứng dụng bên thứ 3 (FB, Zalo, Tel, Mail)
                if (url.startsWith("fb://") || url.startsWith("zalo://") || 
                    url.startsWith("tel:") || url.startsWith("mailto:") || 
                    url.startsWith("https://zalo.me/") || url.startsWith("https://m.me/")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        // Nếu không có App (ví dụ chưa cài FB), hãy để WebView thử load bản web hoặc báo lỗi nhẹ
                        Toast.makeText(this@MainActivity, "Không tìm thấy ứng dụng hỗ trợ!", Toast.LENGTH_SHORT).show()
                        return false 
                    }
                }

                // 5. Tự động đồng bộ cấu hình (Auto-Sync from Web)
                if (url.startsWith("appintent://sync_config")) {
                    try {
                        val uri = android.net.Uri.parse(url)
                        val serverUrl = uri.getQueryParameter("url") ?: ""
                        val apiToken = uri.getQueryParameter("token") ?: ""

                        if (serverUrl.isNotEmpty() && apiToken.isNotEmpty()) {
                            settings.apiUrl = serverUrl
                            settings.apiToken = apiToken
                            
                            appendLog("Đã tự động đồng bộ cấu hình từ tài khoản.")
                            Toast.makeText(this@MainActivity, "🔄 Đã đồng bộ cấu hình thành công!", Toast.LENGTH_SHORT).show()
                            
                            // Ẩn layout cài đặt nếu đang mở
                            binding.settingsLayout.visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        appendLog("Lỗi đồng bộ cấu hình: ${e.message}")
                    }
                    return true
                }

                // 6. Các link web thông thường khác cứ để load tiếp trong WebView
                return false
            }
        }
        webView.webChromeClient = android.webkit.WebChromeClient()

        // Tải giao diện Web đã được nhúng sẵn bên trong APK (Offline, không cần IP)
        webView.loadUrl("file:///android_asset/index.html")
        binding.settingsLayout.visibility = View.GONE
    }

    /**
     * Lớp cầu nối JavaScript (Bridge) để WebView có thể lấy dữ liệu từ Android
     */
    inner class WebAppInterface(private val settings: SettingsManager) {
        @android.webkit.JavascriptInterface
        fun getApiUrl(): String {
            return settings.apiUrl
        }
        
        @android.webkit.JavascriptInterface
        fun getApiToken(): String {
            return settings.apiToken
        }

        @android.webkit.JavascriptInterface
        fun showToast(message: String) {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
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
