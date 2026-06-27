# SPEC — Customer Order Detail & Code Decryption
> **Route:** `/account/orders/{orderCode}` | **Template:** `templates/account/order-detail.html`
> **JS Script:** `static/js/account-order-detail.js` | **Prefix:** `codt-`

---

## 1. MÔ TẢ TRANG
Màn hình chi tiết của một đơn hàng, hiển thị thông tin sản phẩm kỹ thuật số đã mua cùng nút xem và sao chép mã code sản phẩm (đã được giải mã).

---

## 2. MOCKUP GIAO DIỆN
```
┌────────────────────────────────────────────────────────┐
│  Chi tiết đơn hàng #Netflix12                          │
├────────────────────────────────────────────────────────┤
│  Sản phẩm: Netflix Premium 1 tháng                     │
│  Giá tiền: 50.000đ                                     │
│  Mã thẻ bàn giao:                                      │
│  ┌──────────────────────────────────────────────────┐  │
│  │  [ **************************************** ]    │  │
│  │  [ Hiển Thị Mã Code / Giải Mã ]  [ Copy ]        │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

---

## 3. LOGIC GIẢI MÃ CLIENT-SIDE
1. Khi hiển thị trang, nội dung text code tải về ở dạng mã hóa AES.
2. Click "Hiển Thị Mã Code", JS gửi request lấy session key giải mã hoặc chạy thuật toán giải mã decrypt hiển thị giftcode dạng text thô.