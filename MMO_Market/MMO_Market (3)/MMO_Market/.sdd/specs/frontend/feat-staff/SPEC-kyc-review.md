# SPEC — Staff KYC Review Page
> **Routes:** `/staff/kyc`, `/staff/kyc/detail`
> **Templates:** `templates/staff/kyc.html`, `templates/staff/kyc-detail.html`
> **JS Scripts:** `static/js/staff-kyc.js`, `static/js/staff-kyc-detail.js`

---

## 1. MÔ TẢ TRANG
Nhân viên (Staff) kiểm duyệt hình ảnh và thông tin số căn cước công dân do người dùng tải lên để duyệt nâng cấp tài khoản hoạt động.

---

## 2. MOCKUP GIAO DIỆN (ASCII)
```
┌────────────────────────────────────────────────────────┐
│  Staff Console > Chi tiết kiểm duyệt KYC               │
├────────────────────────────────────────────────────────┤
│  Thành viên: Nguyễn Văn A                              │
│  Số định danh CCCD: 001095012345                       │
│                                                        │
│  ┌──────────────────┐ ┌──────────────────┐             │
│  │ Mặt trước CCCD   │ │ Mặt sau CCCD     │             │
│  │ [Ảnh mặt trước]  │ │ [Ảnh mặt sau]    │             │
│  └──────────────────┘ └──────────────────┘             │
│                                                        │
│             [ Phê Phê Duyệt ]   [ Từ Chối ]            │
└────────────────────────────────────────────────────────┘
```

---

## 3. LUỒNG XỬ LÝ DUYỆT HỒ SƠ
- **Duyệt**: Staff click "Phê Duyệt" -> Gửi `POST /api/staff/kyc/approve/{id}` -> Hệ thống chuyển user sang trạng thái hoạt động chính thức.
- **Từ chối**: Staff click "Từ Chối" -> Nhập lý do từ chối -> Gửi `POST /api/staff/kyc/reject/{id}`.