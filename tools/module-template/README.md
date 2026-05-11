# Module template

Thư mục này là template mẫu để tạo module Android (library/module) trong dự án.

Hướng dẫn nhanh:

1. Copy toàn bộ thư mục `module-template` sang vị trí mới ví dụ `:feature:yourmodule` hoặc `modules/yourmodule`.
2. Đổi tên `namespace` trong `build.gradle.kts` (ví dụ `com.yourorg.yourmodule`).
3. Cập nhật `settings.gradle.kts` ở gốc để include module mới, ví dụ:

   include(":modules:yourmodule")

4. Cập nhật dependencies trong `build.gradle.kts` nếu cần.
5. Sửa package trong `src/main/java/...` và `AndroidManifest.xml` theo `namespace` mới.

File có trong template:
- `build.gradle.kts` - script gradle cho module (Kotlin DSL)
- `src/main/AndroidManifest.xml` - manifest cơ bản
- `src/main/java/com/example/module/ModuleTemplate.kt` - ví dụ mã nguồn
- `src/main/res/values/strings.xml` - ví dụ string
- `proguard-rules.pro` - rules mặc định

Ghi chú:
- Template này dùng `com.android.library`. Nếu module của bạn là `app` standalone, đổi plugin tương ứng.
- Thay các placeholder (`com.example.module`, version, sdk) theo nhu cầu dự án.

