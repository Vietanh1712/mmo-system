# SPEC — Cấu Hình Hệ Thống & Phí (Admin Configurations & Fees View)

> **Feature ID:** `feat-admin` | **Page:** `Admin Console (System Config & Commissions Sub-views)`
> **Route:** `/admin/users#system-config` / `/admin/users#commissions`
> **Template:** `templates/admin/users.html`
> **CSS Script:** `static/css/admin/admin.css`
> **JS Script:** `static/js/admin-console.js`
> **Version:** 2.0 | **Status:** Active
> **Backend ref:** `feat-admin/SPEC.md`
> **Last Updated:** 2026-07-16

---

## 1. TỔNG QUAN PHÂN VÙNG

Hai phân vùng cấu hình này cung cấp giao diện tập trung để Admin thay đổi các tham số vận hành tài chính (tỷ lệ hoa hồng, phí rút tiền, hạn mức nạp/rút), các tham số bảo mật hệ thống (phiên làm việc, OTP timeout) và bật/tắt các tùy chọn đăng nhập/đăng ký.

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--ds-primary:         #2563eb;
--ds-border:          #cbd5e1;
--ds-text:            #1e293b;
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

### 3.1 Màn hình Cấu hình chung (`#system-configView`)
```
┌────────────────────────────────────────────────────────────────────────┐
│  CẤU HÌNH HỆ THỐNG                                    [ 💾 Lưu cấu hình]│
├────────────────────────────────────────────────────────────────────────┤
│  Tham số vận hành (Lưu trực tiếp vào CSDL và áp dụng thời gian thực)  │
│  * Thời gian hết hạn đăng nhập (phút): [ 15 ]                          │
│  * Thời gian OTP (phút):               [ 5  ]                          │
│  * Số lần đăng nhập sai tối đa:        [ 5  ]                          │
│  * Thời gian khóa tài khoản (phút):    [ 15 ]                          │
│  * Thời gian giam tiền Escrow (giờ):   [ 72 ]                          │
│                                                                        │
│  Tính năng hệ thống                                                    │
│  [o] Cho phép đăng nhập bằng Google                                    │
│  [o] Cho phép người dùng đăng ký mới                                   │
│  [o] Bắt buộc dùng xác thực 2 bước (2FA) khi rút tiền                  │
└────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Màn hình Phí & Hoa hồng (`#commissionsView`)
```
┌────────────────────────────────────────────────────────────────────────┐
│  PHÍ & HOA HỒNG                                     [ 💾 Lưu cấu hình ]│
├────────────────────────────────────────────────────────────────────────┤
│  (Lưu trực tiếp vào CSDL và áp dụng thời gian thực)                   │
│  * Hoa hồng C2C (%):                 [ 5.0  ]                          │
│  * Phí rút tiền (%):                 [ 1.5  ]                          │
│  * Phí mở shop (VNĐ):                [ 50000]                          │
│  * Hạn mức rút tối thiểu (VNĐ):      [ 50000]                          │
│  * Hạn mức rút tối đa (VNĐ):        [ 50000000]                        │
│  * Hạn mức nạp tối thiểu (VNĐ):      [ 10000]                          │
│  * Hạn mức nạp tối đa (VNĐ):        [ 50000000]                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Khối cấu hình hệ thống chung — `#system-configView`
* Chứa các ô nhập kiểu số đại diện cho: **Thời gian hết hạn đăng nhập (phút)** (`SESSION_TIMEOUT_MINS` - thời gian hiệu lực JWT token), thời gian OTP (`OTP_TIMEOUT_MINS`), giới hạn đăng nhập sai (`MAX_LOGIN_RETRIES`), thời gian khóa tài khoản (`LOCK_DURATION_MINS`), số giờ giam tiền escrow (`ESCROW_HOLD_HOURS`).
* Tích hợp 3 nút toggle switches: `#cfgAllowGoogle`, `#cfgAllowRegister`, và `#cfgWithdraw2FA`. Khi nhấn, nút trượt thay đổi trạng thái kèm đổi màu nền.
* *Xác minh Backend:* Tất cả 8 tham số trên đều đã được đấu nối 100% trực tiếp vào logic Backend Java (`JwtTokenProvider`, `AuthenticationService`, `EmailService`, `TransactionService`, `WithdrawalService`).

### 4.2 Khối cấu hình phí & hoa hồng — `#commissionsView`
* Chứa các ô nhập dạng số thực (Base Percent, Withdrawal Percent) và các ô nhập số nguyên lớn (Shop opening fee, Min/Max Withdrawal, Min/Max Deposit) liên quan đến tiền tệ sàn.
* Tự động tải từ CSDL qua hàm JS `loadCommissionsForm()` và lưu vào CSDL qua `saveCommissions()`.

---

## 5. LUỒNG XỬ LÝ JS & AJAX

1. **Khởi tạo thông tin cấu hình:**
   * Khi Admin mở phân vùng cấu hình, gửi AJAX:
     * **Endpoint:** `GET /api/admin/system-config` và `GET /api/admin/system-config/commissions`
     * **Response (200 OK):** Tách cấu hình thành DTO hệ thống (`systemConfig`) và DTO biểu phí (`commissions`). Điền toàn bộ giá trị vào các ô nhập dữ liệu, đồng thời set thuộc tính `aria-pressed="true/false"` và thêm class `active` cho các toggle switch tương ứng.
2. **Lưu cấu hình hệ thống chung:**
   * Admin nhấn "Lưu cấu hình" ở view cấu hình hệ thống.
   * Thu thập dữ liệu từ các trường nhập, validate các giới hạn tối thiểu (Session timeout >= 5 phút, OTP >= 1 phút, Max login retries >= 1, Lock duration >= 1, Escrow hours >= 1).
   * Gửi AJAX: `PUT /api/admin/system-config/general` với payload DTO `SystemConfigUpdateRequest`.
   * Trả về thành công, hiển thị toast thông báo cập nhật thành công và ghi nhận Audit Log.
3. **Lưu cấu hình phí & hoa hồng:**
   * Admin nhấn "Lưu cấu hình" ở view Phí & Hoa hồng.
   * Thu thập dữ liệu, validate:
     * Tỷ lệ phần trăm hoa hồng và phí rút tiền nằm trong khoảng [0.0 - 100.0].
     * Các phí và hạn mức không được nhỏ hơn 0 VNĐ. Hạn mức tối đa phải >= hạn mức tối thiểu.
   * Gửi AJAX: `PUT /api/admin/system-config/commissions` với payload DTO `CommissionsUpdateRequest`.
   * Trả về thành công, hiển thị toast thông báo cập nhật thành công và ghi nhận Audit Log.
