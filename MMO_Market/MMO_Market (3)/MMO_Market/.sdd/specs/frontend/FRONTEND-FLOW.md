# FRONTEND-FLOW — Luồng tương tác Client - Server (Thymeleaf & AJAX)
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27
> **Mục đích:** Đặc tả cách Client (HTML/Plain JS) tương tác với Server (Spring Boot / REST API).

---

## 1. MÔ HÌNH KIẾN TRÚC FRONTEND
Hệ thống MMO Market sử dụng mô hình kiến trúc **Multi-Page Application (MPA)** kết hợp **AJAX/Fetch API** để tăng trải nghiệm động cho người dùng.

```
┌──────────────┐                  HTML Page                   ┌──────────────┐
│              │ ◄──────────────────────────────────────────  │              │
│              │           (Thymeleaf Server Render)          │              │
│              │                                              │              │
│    Client    │               JSON REST API                  │ Spring Boot  │
│  (Plain JS)  │ ═══════════════════════════════════════════► │   Backend    │
│              │ ◄──────────────────────────────────────────  │              │
│              │             (Access Token Bearer)            │              │
└──────────────┘                                              └──────────────┘
```

### 1.1 Khởi tạo trang (Thymeleaf Layer)
1. Người dùng gõ URL (ví dụ: `/seller/dashboard`).
2. Spring MVC Controller nhận yêu cầu, kiểm tra quyền truy cập qua Spring Security.
3. Controller trả về file `.html` tương ứng nằm trong `templates/`.
4. File HTML được server render, tự động đính kèm các tài nguyên tĩnh trong `/static/css/` và `/static/js/`.

### 1.2 Giao tiếp dữ liệu (AJAX / Fetch API Layer)
1. Sau khi trang tải xong, tệp JavaScript nghiệp vụ tương ứng (ví dụ: `seller-console.js`) được kích hoạt.
2. JavaScript đọc các thẻ DOM để lấy dữ liệu tĩnh hoặc gửi yêu cầu lấy dữ liệu động (`Fetch API`) lên các REST Endpoint ở Backend (ví dụ: `GET /api/seller/dashboard`).
3. JavaScript nhận kết quả dạng JSON, thực hiện cập nhật hiển thị (DOM Manipulation) mà không cần reload trang.

---

## 2. QUẢN LÝ THÔNG TIN ĐĂNG NHẬP & JWT
Hệ thống lưu giữ thông tin định danh bằng cơ chế Web Token:

- **Access Token**: Lưu trữ trong `localStorage.getItem('accessToken')` để đính vào Header của các AJAX request.
- **Refresh Token**: Lưu trữ trong `localStorage.getItem('refreshToken')` để gia hạn phiên đăng nhập khi Access Token hết hạn.
- **Đính Authorization Header**:
  ```javascript
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + localStorage.getItem('accessToken')
  };
  ```

---

## 3. HỆ THỐNG THÔNG BÁO TOAST (toast.js)
Tất cả các thông báo phản hồi từ API (Thành công, Lỗi) phải được hiển thị qua module thông báo Toast động.
- **Thành công**: `showToast("Thành công!", "success")`
- **Lỗi**: `showToast("Có lỗi xảy ra: " + error.message, "danger")`