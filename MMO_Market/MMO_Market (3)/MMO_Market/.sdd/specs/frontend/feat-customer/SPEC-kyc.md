# SPEC — KYC Verification Upload
> **Route:** `/account/kyc` | **Template:** `templates/account/kyc.html`
> **JS Script:** `static/js/account-kyc.js` | **Prefix:** `ckyc-`

---

## 1. MÔ TẢ TRANG
Khách hàng tải lên ảnh giấy tờ chứng minh nhân dân (mặt trước, mặt sau) và ảnh selfie chân dung để gửi yêu cầu định danh KYC.

---

## 2. MOCKUP GIAO DIỆN
```
┌────────────────────────────────────────────────────────┐
│  Xác thực danh tính KYC                                │
├────────────────────────────────────────────────────────┤
│  Số CCCD/CMND: [ 001095012345 ]                        │
│  ┌──────────────────┐ ┌──────────────────┐             │
│  │ Mặt trước CCCD   │ │ Mặt sau CCCD     │             │
│  │ [ Tải ảnh lên ]  │ │ [ Tải ảnh lên ]  │             │
│  └──────────────────┘ └──────────────────┘             │
│  ┌──────────────────┐                                  │
│  │ Ảnh chân dung    │                                  │
│  │ [ Tải ảnh lên ]  │                                  │
│  └──────────────────┘                                  │
│                 [ Gửi Yêu Cầu Xác Thực ]               │
└────────────────────────────────────────────────────────┘
```

---

## 3. LUỒNG XỬ LÝ & GỌI API
1. Người dùng chọn 3 tệp ảnh.
2. JavaScript đọc tệp dưới dạng File objects.
3. Khi click "Gửi Yêu Cầu":
   * Khởi tạo `FormData()`.
   * Gắn các trường file và số CCCD.
   * Gửi `POST /api/kyc/submit` (Multipart/form-data).
   * Thành công -> Chuyển trạng thái giao diện thành "Đang chờ duyệt".