# SPEC — Product Reviews & Ratings
> **Feature ID:** `feat-review`
> **UC Coverage:** UC-10, UC-11
> **Version:** 1.0 | **Status:** Active
> **Author:** Team | **Last Updated:** 2026-07-16

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)

### 1.1 Bối cảnh
Sau khi người mua (Customer) hoàn tất giao dịch mua sản phẩm kỹ thuật số thành công, họ cần có khả năng đánh giá mức độ hài lòng (số sao từ 1 đến 5) kèm theo nhận xét và hình ảnh/video bằng chứng để đóng góp ý kiến phản hồi cho sản phẩm đó. Đồng thời, giúp các khách hàng khác tham khảo chất lượng dịch vụ trước khi mua.

### 1.2 Mục tiêu
- Cho phép khách hàng gửi đánh giá (Review) cho sản phẩm sau khi đơn hàng ở trạng thái hoàn tất (`Completed` hoặc `Held`).
- Đảm bảo tính trung thực: Chỉ khách hàng đã mua và giao dịch thành công sản phẩm đó mới được quyền đánh giá. Mỗi đơn hàng chỉ được đánh giá một lần duy nhất.
- Hiển thị danh sách đánh giá của sản phẩm công khai trên trang chi tiết sản phẩm.
- Tính toán điểm đánh giá trung bình (Average Rating) của sản phẩm và hiển thị trên bộ lọc tìm kiếm sản phẩm.

---

## 2. ACTOR (TÁC NHÂN)

| Actor | Role | Điều kiện tiền quyết |
|---|---|---|
| **Customer** | Gửi đánh giá sản phẩm | Đã đăng nhập, đã giao dịch thành công sản phẩm đó (status đơn hàng = `Completed` hoặc `Held`). |
| **Guest / Public** | Xem danh sách đánh giá | Không cần đăng nhập. |

---

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)

| ID | EARS Requirement |
|---|---|
| **FR-REVIEW-01** | WHEN a Customer submits a review for a product with rating, comment, and transactionId, THE SYSTEM SHALL validate that the transaction is owned by the customer, matches the product, and is in `Completed` or `Held` status. |
| **FR-REVIEW-02** | THE SYSTEM SHALL restrict review submission such that a single transaction can only be reviewed once (`existsByTransactionIdAndIsDeleteFalse`). |
| **FR-REVIEW-03** | THE SYSTEM SHALL validate that the rating value is an integer between 1 and 5 (inclusive). |
| **FR-REVIEW-04** | WHEN a review is successfully submitted, THE SYSTEM SHALL save the Review record and return the `ReviewResponseDTO`. |
| **FR-REVIEW-05** | WHEN a user queries a product, THE SYSTEM SHALL aggregate and compute the average rating based on all active reviews of that product. |

---

## 4. BUSINESS RULES (Ràng buộc nghiệp vụ)

| Rule | Mô tả |
|---|---|
| **BR-REVIEW-01** | Chỉ người mua hàng thành công mới được viết đánh giá (chống spam review ảo). |
| **BR-REVIEW-02** | Giới hạn 1 review cho 1 đơn hàng (ngăn chặn spam trùng lặp). |
| **BR-REVIEW-03** | Điểm đánh giá (rating) bắt buộc từ 1 đến 5 sao. |
| **BR-REVIEW-04** | Soft delete: Khi xóa sản phẩm hoặc đánh giá, không xóa vật lý bản ghi `Reviews` mà sử dụng cờ `isDelete = 1`. |

---

## 5. DATA MODEL (Mô hình dữ liệu)

Cấu trúc bảng `Reviews` trong CSDL SQL Server:

```sql
CREATE TABLE Reviews (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id      BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    transaction_id  BIGINT NULL,
    rating          INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         NVARCHAR(MAX) NULL,
    media_url       NVARCHAR(MAX) NULL,
    created_at      DATETIME DEFAULT GETDATE(),
    isDelete        BIT DEFAULT 0,
    CONSTRAINT FK_Review_Product FOREIGN KEY (product_id) REFERENCES Products(id),
    CONSTRAINT FK_Review_User    FOREIGN KEY (user_id) REFERENCES Users(id)
);
```

---

## 6. API SPEC (Đặc tả API)

### 6.1. Gửi đánh giá sản phẩm mới
*   **Endpoint:** `POST /api/products/{productId}/reviews`
*   **Headers:** `Authorization: Bearer <Access_Token>`
*   **Request Body (JSON):**
    ```json
    {
      "rating": 5,
      "comment": "Key game kích hoạt rất nhanh, shop hỗ trợ nhiệt tình.",
      "mediaUrl": "https://mmo-market.s3.amazonaws.com/evidence-123.jpg",
      "transactionId": 42
    }
    ```
*   **Response (200 OK):**
    ```json
    {
      "id": 1,
      "userName": "Nguyen Van A",
      "rating": 5,
      "comment": "Key game kích hoạt rất nhanh, shop hỗ trợ nhiệt tình.",
      "mediaUrl": "https://mmo-market.s3.amazonaws.com/evidence-123.jpg",
      "createdAt": "2026-07-16T17:40:00"
    }
    ```
