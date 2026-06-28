# SPEC — Client Scripting & AJAX Core Interceptor
> **Feature ID:** `feat-frontend-js`
> **Version:** 1.0 | **Status:** Implemented
> **Author:** Team | **Last Updated:** 2026-06-27

---

## 1. TỔNG QUAN LỚP CLIENT SCRIPTING (JS UTILS)
Tất cả các lời gọi API từ JavaScript phía Client lên Spring Boot Backend đều phải qua lớp xử lý tập trung để tự động chèn Access Token và tự động gia hạn token khi gặp mã lỗi 401.

---

## 2. FILE CỐT LÕI
```
apps/frontend/static/js/
├── api-client.js           ← Cấu hình fetch wrapper tự động đính JWT header
└── toast.js                ← Hiển thị thông báo toast thành công/lỗi
```

---

## 3. LUỒNG XỬ LÝ INTERCEPTOR TOKEN
```
   AJAX Request ──► [ Đính Header: Bearer <accessToken> ] ──► Spring Boot API
                                                                  │
   [ Hoàn thành ] ◄─── [ 200 OK ] ◄───────────────────────────────┤
                                                                  ▼
   [ Yêu cầu Đăng Nhập ] ◄── [ Thất bại ] ◄── [ 401 Unauthorized ]
             │
             ▼
     Gọi: POST /api/auth/refresh (kèm refreshToken)
             │
             ├──► [ Thành công ] ──► Lưu accessToken mới ──► Retry Request gốc
             │
             └──► [ Thất bại ] ────► Xóa localStorage ────► Redirect sang /login
```