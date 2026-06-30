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

Trang phân xử khiếu nại cung cấp phòng đối chất trực tuyến thời gian thực (Dispute Room) kết nối trực tiếp 3 bên: Người mua (Buyer) khiếu nại, Người bán (Seller) bị tố cáo và Nhân viên kiểm duyệt (Staff) đóng vai trò trung gian phân xử. 

Trang hỗ trợ xem chứng cứ lỗi, đàm thoại thảo luận và thực thi phán quyết chuyển tiền: **Hoàn tiền về ví khả dụng cho Buyer (đóng đơn)** hoặc **Giải ngân về ví khả dụng cho Seller (bác đơn khiếu nại)**.

**Cấu trúc trang:**
1. **Trang danh sách khiếu nại (`/staff/complaints`):** Bảng quản lý hiển thị các khiếu nại đang có trạng thái `OPEN` kèm nút bấm dẫn vào chi tiết.
2. **Trang phòng đối chất chi tiết (`/staff/complaints/{complaintId}`):**
   * *Cột trái (Chi tiết & Chứng cứ):* Hiển thị thông tin đơn hàng bị tranh chấp, mô tả lỗi của Buyer và hình ảnh/video bằng chứng đi kèm.
   * *Cột phải (Khung chat WebSocket):* Giao diện chat thời gian thực hiển thị tin nhắn phân biệt màu sắc theo vai trò (Staff màu vàng nổi bật, Buyer màu xanh đậm, Seller màu xanh lá).
   * *Thanh phán quyết (Action Bar):* Gồm 2 nút bấm lớn: "Hoàn tiền cho Buyer" và "Giải ngân cho Seller".

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

### 4.1 Khung Chat Thời Gian Thực — `.stf-chat-container`
* Chiều cao cố định `450px`, tích hợp thanh cuộn dọc tự động cuộn xuống dưới cùng khi có tin nhắn mới.
* Tin nhắn hiển thị dạng bong bóng thoại (Speech bubble) bo góc:
  * Tin nhắn của Staff: Căn lề phải, nền `var(--ds-chat-staff-bg)`.
  * Tin nhắn của Buyer: Căn lề trái, nền `var(--ds-chat-buyer-bg)`.
  * Tin nhắn của Seller: Căn lề trái, nền `var(--ds-chat-seller-bg)`.

### 4.2 Thanh Phán Quyết Tài Chính — `.stf-decision-bar`
* Chứa 2 nút hành động lớn chiếm trọn bề ngang ở dưới cùng trang:
  * Nút Hoàn tiền (`.btn-action-refund`): Đỏ cam nổi bật, click kích hoạt modal phê duyệt hoàn ví khả dụng cho Buyer.
  * Nút Bác đơn (`.btn-action-payout`): Xanh lá, click kích hoạt modal phê duyệt giải ngân ví khả dụng cho Seller.

---

## 5. LUỒNG XỬ LÝ JS & REAL-TIME WEBSOCKET

### 5.1 Kết nối WebSocket trực tuyến:
* Khi load trang đối chất `/staff/complaints/{complaintId}`, JS tự động khởi tạo kết nối WebSocket Stomp:
  ```javascript
  const socket = new SockJS('/ws-complaints');
  const stompClient = Stomp.over(socket);
  stompClient.connect({ 'Authorization': 'Bearer ' + token }, function (frame) {
      stompClient.subscribe('/topic/complaint/' + complaintId, function (messageOutput) {
          appendChatMessage(JSON.parse(messageOutput.body));
      });
  });
  ```
* Lắng nghe gói tin chèn tin nhắn mới vào `.stf-chat-container` và chạy hiệu ứng cuộn mượt.

### 5.2 Xử lý gửi tin nhắn của Staff:
* Staff nhập nội dung và nhấn Enter/nút Gửi:
  * JS gửi gói tin qua WebSocket:
    ```javascript
    stompClient.send('/app/complaint/chat/' + complaintId, {}, JSON.stringify({
        senderId: currentStaffId,
        content: messageText
    }));
    ```

### 5.3 Gửi quyết định phán quyết:
1. **Chấp nhận hoàn tiền cho Buyer:**
   * Gửi API:
     * **Endpoint:** `POST /api/complaints/resolve/{complaintId}`
     * **Headers:** `Content-Type: application/json`, `Authorization: Bearer <token>`
     * **Payload:** `{ "resolutionReason": "Mã thẻ bị trùng lặp, Seller không đưa được code thay thế" }`
   * **Thành công (HTTP 200):** Tự động đóng phòng chat, hiển thị thông báo phân xử thành công và hoàn tiền về ví khả dụng Buyer.
2. **Bác đơn khiếu nại (Giải ngân cho Seller):**
   * Gửi API:
     * **Endpoint:** `POST /api/complaints/reject/{complaintId}`
     * **Headers:** `Content-Type: application/json`, `Authorization: Bearer <token>`
     * **Payload:** `{ "resolutionReason": "Người mua không đưa ra được bằng chứng chứng minh mã thẻ lỗi" }`
   * **Thành công (HTTP 200):** Chuyển tiền từ hold balance về ví khả dụng Seller (trừ phí hoa hồng), đóng phòng chat.

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