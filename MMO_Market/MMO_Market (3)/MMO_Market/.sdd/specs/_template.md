# SPEC — [TÊN_TÍNH_NĂNG]
> **Feature ID:** `feat-[id]`
> **Use Case Coverage:** UC-XX, UC-YY
> **Version:** 1.0 | **Status:** Draft
> **Author:** [Tên người viết] | **Last Updated:** YYYY-MM-DD

---

## 1. CONTEXT & GOAL (BỐI CẢNH & MỤC TIÊU)
*   **Bối cảnh**: [Mô tả vấn đề / nhu cầu nghiệp vụ mà tính năng này giải quyết]
*   **Mục tiêu**: [Mục tiêu cụ thể, có thể đo lường được]
*   **Tại sao cần?**: [Giá trị mang lại cho dự án / người dùng]

## 2. ACTOR (TÁC NHÂN)
Định nghĩa các đối tượng tương tác với tính năng và điều kiện tiền quyết.
| Actor | Role / Mô tả ngắn | Điều kiện tiền quyết (Precondition) |
|---|---|---|
| Student | Học sinh làm bài | Đã đăng nhập, subscription còn hạn |

## 3. FUNCTIONAL REQUIREMENTS (Cú pháp EARS)
Yêu cầu chức năng phải được mô tả bằng cú pháp EARS (Easy Approach to Requirements Syntax):
*   `WHEN [trigger] THE SYSTEM SHALL [behavior]` (Khi có sự kiện kích hoạt -> hệ thống sẽ xử lý)
*   `WHILE [state] THE SYSTEM SHALL [behavior]` (Trong khi ở một trạng thái -> hệ thống duy trì hành vi)
*   `IF [condition] THEN THE SYSTEM SHALL [response]` (Nếu điều kiện đúng -> hệ thống phản hồi)
*   `THE SYSTEM SHALL [ubiquitous requirement]` (Hệ thống luôn luôn thực hiện yêu cầu này)

### 3.1. [Tên Sub-feature / Happy Path]
| ID | EARS Requirement |
|---|---|
| FR-01 | WHEN user clicks 'Submit', THE SYSTEM SHALL save the attempt. |

### 3.2. [Alternate Paths / Edge Cases]
| ID | EARS Requirement |
|---|---|
| FR-02 | IF score is lower than passing score, THEN THE SYSTEM SHALL lock the next lesson. |

## 4. NON-FUNCTIONAL REQUIREMENTS (Yêu cầu phi chức năng)
| ID | Category | Requirement |
|---|---|---|
| NFR-01 | Performance | API response time < 500ms p95 |
| NFR-02 | Security | Yêu cầu xác thực JWT cho mọi API của feature này |

## 5. DATA MODEL (Mô hình dữ liệu)
Định nghĩa bảng cơ sở dữ liệu và quan hệ (nếu có).
```sql
-- Ví dụ cấu trúc bảng
CREATE TABLE quiz_attempts (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    score INT CHECK (score >= 0),
    created_at TIMESTAMP
);
```

## 6. API SPEC (Đặc tả API)
Mô tả chi tiết endpoints.

### Endpoint: POST /api/quiz-attempts
**Request Body:**
```json
{ "quizId": "Long - ID bài quiz", "answers": "List - các đáp án" }
```
**Response Body (200):**
```json
{ "status": 200, "message": "Success", "data": { "attemptId": 123, "score": 85 } }
```

## 7. ERROR HANDLING (Xử lý lỗi)
Bảng ánh xạ các trường hợp lỗi nghiệp vụ / hệ thống.

| HTTP Code | Error Code | Message | Lý do kích hoạt |
|---|---|---|---|
| 422 | VALIDATION_FAILED | "Quiz already locked" | Thử nộp bài cho quiz đã khóa |

## 8. ACCEPTANCE CRITERIA (Tiêu chí nghiệm thu)
Viết theo cú pháp Gherkin (Given - When - Then).

| ID | Scenario | Given (Bối cảnh) | When (Hành động) | Then (Kết quả) |
|---|---|---|---|---|
| AC-01 | Nộp bài thành công | User ở màn hình Quiz | Click 'Nộp bài' | Lưu kết quả & chuyển sang màn điểm số |

## 9. OUT OF SCOPE (Ngoài phạm vi)
❌ Không làm tính năng chia sẻ kết quả lên mạng xã hội trong phase này.
❌ Không phân tích thống kê điểm số nâng cao ở màn hình này.
