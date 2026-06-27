# MASTER-SPEC — Hướng dẫn tạo Console (Admin, Seller, Staff)
> **Phiên bản:** 1.0 | **Cập nhật:** 2026-06-27
> **Design Ref:** `feat-frontend-design/SPEC.md`

---

## 1. CẤU TRÚC THƯ MỤC TÀI NGUYÊN TĨNH
Tất cả tài nguyên tĩnh của frontend được bố trí trong thư mục `/static/`:

```
static/
├── css/
│   ├── seller-console.css  ← Phong cách giao diện người bán
│   ├── admin-console.css   ← Phong cách giao diện Admin
│   └── components/         ← Các component chung (modal, toast)
└── js/
    ├── auth.js             ← Luồng đăng ký, đăng nhập, quên mật khẩu
    ├── seller-console.js   ← Cổng quản lý bán hàng
    ├── admin-console.js    ← Báo cáo doanh thu & quản lý RBAC
    └── toast.js            ← Component hiển thị thông báo
```

---

## 2. GIẢI PHẪU MỘT TRANG CONSOLE ĐIỂN HÌNH
Giao diện quản lý bao gồm sidebar điều hướng và vùng nội dung chính:

```
┌────────────────────────────────────────────────────────┐
│                        TOP HEADER                      │
├─────────────┬──────────────────────────────────────────┤
│             │                                          │
│   SIDEBAR   │               CONTENT BODY               │
│  NAVIGATION │          (Báo cáo, Bảng dữ liệu,         │
│             │            Nút thao tác nhanh)           │
│             │                                          │
└─────────────┴──────────────────────────────────────────┘
```

### 2.1 Cập nhật bảng dữ liệu động (Data Tables)
JavaScript đảm nhiệm việc xóa dữ liệu cũ và render dòng mới:
```javascript
function renderTable(dataList) {
    const tbody = document.querySelector("#dataTable tbody");
    tbody.innerHTML = ""; // Xóa skeleton
    dataList.forEach(item => {
        const row = `<tr>
            <td>${item.id}</td>
            <td>${item.name}</td>
            <td>${formatCurrency(item.price)} VNĐ</td>
        </tr>`;
        tbody.insertAdjacentHTML('beforeend', row);
    });
}
```