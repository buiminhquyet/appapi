# Hướng dẫn Build APK cho ứng dụng QUYETDEV Bank Forwarder

Để biến mã nguồn này thành file cài đặt APK chuyên nghiệp, bạn hãy làm theo các bước đơn giản sau:

### Bước 1: Chuẩn bị công cụ
1. Tải và cài đặt **Android Studio** (Bản mới nhất - Hedgehog hoặc Iguana) từ trang chủ: [developer.android.com/studio](https://developer.android.com/studio)
2. Mở Android Studio và chọn **New Project** -> **Empty Activity**.
3. Thiết kế Project:
   - **Name:** QUYETDEV Bank
   - **Package name:** com.quyetdev.bankforwarder (Quan trọng: Phải trùng khớp 100%)
   - **Language:** Kotlin
   - **Minimum SDK:** API 24 (Android 7.0)

### Bước 2: Copy mã nguồn
Bạn hãy copy nội dung các file từ thư mục `android_source` mà mình đã tạo vào project mới trong Android Studio:

1. Copy `app/build.gradle` đè vào file `build.gradle (Module: app)`.
2. Copy `app/src/main/AndroidManifest.xml` vào file tương ứng.
3. Trong thư mục `app/src/main/java/com/quyetdev/bankforwarder/`, hãy paste các file `.kt`:
   - `MainActivity.kt`
   - `BankNotificationService.kt`
   - `SettingsManager.kt`
   - `BootReceiver.kt`
4. Trong thư mục `app/src/main/res/layout/`, hãy paste file `activity_main.xml` và `edit_text_bg.xml`.

### Bước 3: Đóng gói APK
1. Chờ Android Studio đồng bộ xong (Sync Gradle).
2. Trên thanh công cụ, chọn **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
3. Khi hoàn tất, một thông báo hiện lên ở góc dưới bên phải, nhấn vào chữ **Locate** để mở thư mục chứa file `app-debug.apk`. 

## Cách 2: Tự động hóa qua GitHub (Không cần cài phần mềm)

Nếu bạn không muốn cài Android Studio, hãy làm theo cách "chuyên nghiệp" này:

1. **Tạo GitHub Repo:** Tạo một kho lưu trữ (Repository) mới trên GitHub (để ở chế độ Private nếu muốn bảo mật).
2. **Upload Code:** Upload toàn bộ nội dung trong thư mục `android_source` (bao gồm cả thư mục ẩn `.github`) lên Repo đó.
3. **Kích hoạt Build:** 
   - Vào tab **Actions** trên GitHub.
   - Bạn sẽ thấy một tiến trình mang tên **Android Build APK** đang chạy.
   - Đợi khoảng 2-3 phút cho đến khi hiện dấu tích xanh ✅.
4. **Tải APK:** Nhấn vào tên bản build đó, kéo xuống phần **Artifacts** và tải file `bank-forwarder-debug-apk` về. 

Đây là cách nhanh nhất để bạn có file APK mà không cần lo lắng về cấu hình máy tính!

### Bước 4: Cài đặt và Sử dụng
1. Gửi file `.apk` này vào điện thoại Android của bạn và cài đặt.
2. Mở app, nhấn nút **CẤP QUYỀN TRUY CẬP THÔNG BÁO** và tìm chọn **QUYETDEV Bank** -> Bật lên.
3. Quay lại app, điền URL Webhook của bạn (ví dụ: `https://tenmien.com/api/bank_callback_private.php`).
4. Điền Token bảo mật và nhấn **LƯU CÀI ĐẶT**.

**Chúc mừng!** Hệ thống của bạn bây giờ đã có một ứng dụng chuyên nghiệp tự động đẩy thông báo ngân hàng về website.
