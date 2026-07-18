# SPEC — Phòng Đối Chất & Phân Xử Khiếu Nại (Staff Complaints Dispute Room)

> **Feature ID:** `feat-complaint` | **Pages:** `ComplaintList`, `ComplaintDetail`
> **Routes:** `/staff/complaints`, `/staff/complaints/{complaintId}`
> **Templates:** `templates/staff/complaints.html`, `templates/staff/complaint-detail.html`
> **CSS Script:** `static/css/customer/style.css`
> **JS Scripts:** `static/js/staff-complaints.js`, `static/js/staff-complaint-detail.js`
> **Version:** 2.0 | **Status:** Draft
> **Backend ref:** `feat-complaint/UC-10-complaint-dispute.md`

---

## 1. TỔNG QUAN TRANG

Trang phân xử khiếu nại cung cấp phòng đối chất trực tuyến (Dispute Room) cho phép Nhân viên kiểm duyệt (Staff) kiểm tra quá trình đàm thoại thương lượng trực tiếp giữa Người mua (Buyer) và Người bán (Seller). 

Trang hỗ trợ xem chứng cứ lỗi, xem lịch sử đối thoại thương lượng và thực thi phán quyết chuyển tiền: **Hoàn tiền về ví khả dụng cho Buyer** hoặc **Giải ngân về ví khả dụng cho Seller**, kèm theo chức năng **gắn cờ cảnh cáo shop người bán** nếu phát hiện lỗi vi phạm.

**Cấu trúc trang:**
1. **Trang danh sách khiếu nại (`/staff/complaints`):** Bảng quản lý hiển thị các khiếu nại đang có trạng thái `OPEN` kèm nút bấm dẫn vào chi tiết.
2. **Trang phòng đối chất chi tiết (`/staff/complaints/{complaintId}`):**
   * *Cột trái (Chi tiết, Chứng cứ & Chat):* Hiển thị thông tin đơn hàng bị tranh chấp, mô tả lỗi của Buyer, ảnh/video bằng chứng, và **Hộp thoại trò chuyện thương lượng thương thảo (Read-only)** hiển thị lịch sử đối chất của Buyer và Seller.
   * *Cột phải (Thanh xử lý):* Giao diện chọn phương án phân xử (Hoàn tiền / Bác đơn), ghi chú kết quả, checkbox kích hoạt gắn cờ shop người bán, dropdown chọn cấp độ cờ (Warning, Critical, Danger) và lý do phạt shop.
   * *Thanh phán quyết (Action Bar):* Gồm 2 nút bấm lớn: "Giải quyết" (Chấp nhận hoàn tiền) và "Từ chối" (Bác khiếu nại, giải ngân).

---

## 2. DESIGN TOKENS ÁP DỤNG

```css
/* Color Palette */
--ds-primary:         #2563eb;
--ds-primary-hover:   #1d4ed8;
--ds-bg:              #f8fafc;
--ds-card:            #ffffff;
--ds-border:          #cbd5e1;
--ds-error:           #ef4444;
--ds-success:         #10b981;
--ds-warning:         #f59e0b;

/* Chat Box bubble colors */
--ds-chat-staff-bg:   #fef3c7; /* Vàng nhạt */
--ds-chat-buyer-bg:   #dbeafe; /* Xanh dương nhạt */
--ds-chat-seller-bg:  #d1fae5; /* Xanh lá nhạt */

/* Shape & Spacing */
--ds-radius-md:       8px;
--ds-radius-lg:       12px;
--ds-shadow:          0 4px 6px -1px rgba(0,0,0,0.1);
```

---

## 3. LAYOUT TỔNG THỂ & MOCKUP

```
┌────────────────────────────────────────────────────────┐
│ [Logo] MMO Market Staff Console                [Staff] │
├────────────────────────────────────────────────────────┤
│  PHÒNG PHÂN XỬ TRANH CHẤP #CMP98                       │
│  ────────────────────────────────────────────────────  │
│  ┌────────────────────────┐ ┌────────────────────────┐ │
│  │ CHI TIẾT TRANH CHẤP    │ │ HỘP CHAT ĐỐI CHẤT      │ │
│  │ Đơn hàng: #OR852       │ │ [Staff]: Chào hai bạn..│ │
│  │ Mô tả lỗi: Mã đã dùng  │ │ [Buyer]: Code đã bị... │ │
│  │ ┌────────────────────┐ │ │ [Seller]: Tôi đã check │ │
│  │ │    [Ảnh chứng cứ]  │ │ │ ┌────────────────────┐ │ │
│  │ └────────────────────┘ │ │ │[Nhập tin nhắn...]  │ │ │
│  └────────────────────────┘ │ └────────────────────┘ │ │
│  ┌──────────────────────────────────────────────────┐ │
│  │  [ PHÁN QUYẾT: HOÀN TIỀN ] [ PHÁN QUYẾT: BÁC ĐƠN]│ │
│  └──────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 4. CÁC THÀNH PHẦN GIAO DIỆN CHÍNH

### 4.1 Khung Chat Tranh Chấp — `#disputeChatHistory`
* Chế độ **Read-only** cho Staff. Staff chỉ được đọc và theo dõi diễn biến trao đổi của hai bên mà không có quyền gửi tin nhắn (nút gửi tin nhắn và ô nhập text bị ẩn/không có đối với vai trò Staff).
* Phân biệt bong bóng thoại bằng màu sắc:
  * Tin nhắn của Buyer: Căn lề trái, nền `var(--ds-chat-buyer-bg)`.
  * Tin nhắn của Seller: Căn lề trái, nền `var(--ds-chat-seller-bg)`.

### 4.2 Thanh Phán Quyết & Gắn Cờ Cảnh Cáo Shop
* **Giao diện xử lý khiếu nại:** Dropdown lựa chọn trạng thái xử lý và ghi chú lý do phân xử.
* **Hộp kiểm Gắn cờ (Shop Flagging):** Khi check chọn, hiển thị thêm:
  * Dropdown cấp độ cờ: Warning (Cảnh báo), Critical (Nghiêm trọng), Danger (Nguy hiểm).
  * Textarea nhập lý do gắn cờ phạt shop.

---

## 5. LUỒNG XỬ LÝ JS & REST API

### 5.1 Tải lịch sử chat đối chất:
* Thực hiện gửi request:
  * **Endpoint:** `GET /api/complaints/{complaintId}/chats`
  * **Headers:** `Authorization: Bearer <token>`
* Kết quả (HTTP 200) trả về danh sách các tin nhắn trao đổi của Buyer và Seller, được render phân biệt màu sắc vào khung chat `#disputeChatHistory`.

### 5.2 Gửi quyết định phán quyết & Gắn cờ shop:
* Staff bấm nút "Giải quyết" (Resolved) hoặc "Từ chối" (Rejected):
  * Thực hiện gửi API:
    * **Endpoint:** `PUT /api/complaints/{complaintId}/status`
    * **Headers:** `Content-Type: application/json`, `Authorization: Bearer <token>`
    * **Payload:** 
      ```json
      {
        "status": "Resolved", // hoặc "Rejected"
        "resolution": "Ghi chú lý do phân xử của Staff...",
        "flagLevel": "Warning", // "Warning", "Critical", "Danger" hoặc null/None
        "flagReason": "Lý do phạt/gắn cờ cảnh cáo shop..."
      }
      ```
  * **Thành công (HTTP 200):** Cập nhật trạng thái giao dịch, hoàn trả/giải ngân ví tương ứng, đồng thời tự động lưu vết `ShopFlag` cảnh cáo shop người bán nếu được chọn. Hiển thị thông báo thành công và chuyển về trang danh sách khiếu nại.

---

## 6. RESPONSIVE

* **Viewport ≥ 992px:** Layout chia 2 cột song song đều nhau (Trái: thông tin + chứng cứ, Phải: khung chat).
* **Viewport < 992px:** Chuyển sang dạng 1 cột. Khung thông tin chứng cứ đặt ở trên, khung chat đối chất đặt xuống dưới.

---

## 7. ACCESSIBILITY

- Sử dụng `aria-live="assertive"` cho vùng hiển thị tin nhắn mới để thông báo tin nhắn đối thoại tức thì cho người dùng khiếm thị.
- Ô nhập tin nhắn có nhãn ẩn `aria-label="Nhập nội dung tin nhắn đối chất"`.

---

## 8. OUT OF SCOPE

- ❌ Gọi điện đàm thoại trực tiếp (Voice Call/Video Call) giữa các bên trong phòng đối chất.