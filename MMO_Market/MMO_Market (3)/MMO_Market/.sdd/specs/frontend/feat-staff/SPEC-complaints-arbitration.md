# SPEC — Staff Complaints Dispute Room
> **Routes:** `/staff/complaints`, `/staff/complaints/detail`, `/staff/chat`
> **Templates:** `templates/staff/complaints.html`, `templates/staff/complaint-detail.html`, `templates/staff/chat.html`
> **JS Scripts:** `static/js/staff-complaints.js`, `static/js/staff-complaint-detail.js`, `static/js/staff-chat.js`

---

## 1. MÔ TẢ TRANG
Kênh phân xử tranh chấp trực tiếp giữa người mua và người bán do Staff làm trung gian. Tải về và đàm thoại realtime để giải quyết khiếu nại đơn hàng.

---

## 2. MOCKUP GIAO DIỆN (ASCII)
```
┌────────────────────────────────────────────────────────┐
│  Tranh chấp đơn hàng #OR852 — Hộp chat phân xử         │
├────────────────────────────────────────────────────────┤
│  [Staff]: Xin chào hai bạn, tôi sẽ là người phân xử... │
│  [Buyer]: Mã code Netflix shop gửi đã bị sử dụng rồi!  │
│  [Seller]: Tôi kiểm tra thấy code vẫn bình thường...   │
├────────────────────────────────────────────────────────┤
│  [ Nhập nội dung đàm phán... ]               [ Gửi ]   │
│                                                        │
│  [ PHÁN QUYẾT: HOÀN TIỀN BUYER ] [ PHÁN QUYẾT: CHUYỂN SELLER ] │
└────────────────────────────────────────────────────────┘
```

---

## 3. KẾT NỐI REALTIME (WebSocket)
1. Khi load trang `chat.html`, JS thiết lập kết nối WebSocket thông qua thư viện StompJS lên `/ws-complaints`.
2. Lắng nghe tin nhắn mới đính vào khung chat mà không làm tải lại trang.
3. Khi Staff đưa ra phán quyết, gửi request `/api/staff/complaints/resolve` để phân định luồng tiền escrow.