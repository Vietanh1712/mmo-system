---
title: API Guideline
status: active
owner: Backend Team
last_updated: 2026-06-18
---

# API Guideline

## Phạm vi

Tài liệu này là quy tắc bắt buộc cho REST API của MMO Market. Danh mục endpoint
hiện tại nằm tại [API Reference](api-reference.md).

## Nguyên tắc sử dụng API

- Thymeleaf/MVC Controller chỉ trả page hoặc template.
- Dữ liệu động, persistence và business action của frontend phải đi qua REST
  API.
- Không dùng `localStorage`, `sessionStorage`, object JavaScript hard-code hoặc
  HTML hard-code làm nguồn dữ liệu production.
- Prototype chưa có backend phải ghi rõ là mock, không được dùng để cấp quyền,
  xác nhận thanh toán, KYC, số dư, tồn kho hoặc trạng thái nghiệp vụ.
- JavaScript gọi API qua helper authentication dùng chung; không tự sao chép
  logic token ở từng page.

## Route và HTTP method

- Route dùng kebab-case.
- API mới ưu tiên prefix `/api/v1/...`.
- Dùng đúng HTTP method:
  - `GET`: đọc dữ liệu, không thay đổi state.
  - `POST`: tạo resource hoặc thực hiện command không idempotent.
  - `PUT`: cập nhật toàn bộ hoặc command idempotent đã được định nghĩa rõ.
  - `PATCH`: cập nhật một phần.
  - `DELETE`: xóa mềm resource nếu entity áp dụng soft delete.
- Không đặt động từ thừa trong route CRUD. Command như `toggle-lock`, `refresh`
  hoặc `purchase` chỉ dùng khi không biểu diễn phù hợp bằng resource CRUD.
- Không đổi hoặc xóa route đang được frontend sử dụng nếu chưa có kế hoạch
  backward compatibility.

## Request và response

- API mới dùng request/response DTO; không bind hoặc trả JPA entity trực tiếp.
- Validate dữ liệu ở boundary bằng Jakarta Validation và kiểm tra nghiệp vụ ở
  Service.
- JSON dùng tên field nhất quán; timestamp dùng ISO-8601.
- Tiền dùng số nguyên VNĐ, không dùng floating point.
- Không trả password, refresh token, credential tài sản số, national ID đầy đủ
  hoặc đường dẫn file private nếu caller không có quyền.

Response thành công nên trả resource hoặc cấu trúc rõ ràng. Response lỗi phải có
format thống nhất, tối thiểu:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Dữ liệu không hợp lệ",
  "path": "/api/v1/example",
  "timestamp": "2026-06-18T10:00:00+07:00"
}
```

## HTTP status

- `200 OK`: đọc/cập nhật thành công.
- `201 Created`: tạo resource thành công.
- `204 No Content`: thành công và không cần response body.
- `400 Bad Request`: request hoặc nghiệp vụ không hợp lệ.
- `401 Unauthorized`: chưa xác thực hoặc token không hợp lệ.
- `403 Forbidden`: đã xác thực nhưng sai role/ownership.
- `404 Not Found`: resource không tồn tại hoặc không được phép để lộ.
- `409 Conflict`: trùng dữ liệu hoặc state transition xung đột.
- `422 Unprocessable Entity`: dữ liệu đúng cú pháp nhưng không thể xử lý theo
  nghiệp vụ, nếu module thống nhất sử dụng.
- `500 Internal Server Error`: lỗi hệ thống ngoài dự kiến.

Không trả `200 OK` kèm message lỗi.

## Authentication và authorization

- Endpoint public phải được khai báo rõ trong `SecurityConfig` và API Reference.
- Endpoint protected lấy user từ authenticated principal, không tin `userId`
  do frontend gửi nếu có thể suy ra từ token.
- Kiểm tra role và ownership ở Controller/Service.
- Staff/Admin/Seller API phải kiểm tra đúng role; `authenticated()` đơn thuần
  không đủ.
- File private và dữ liệu KYC phải kiểm tra ownership hoặc quyền review trước
  khi trả dữ liệu.

## List, filter và pagination

API danh sách phải tài liệu hóa:

- `page`: bắt đầu từ 0 hoặc 1.
- `size`: mặc định và giới hạn tối đa.
- `sort`: field và direction được hỗ trợ.
- Filter/search được hỗ trợ.
- Cấu trúc `content`, `page`, `size`, `totalElements`, `totalPages`.

Không tải toàn bộ bảng rồi phân trang trong memory đối với dữ liệu có thể tăng
lớn.

## Tài liệu bắt buộc

Mọi endpoint phải được liệt kê trong [API Reference](api-reference.md), gồm:

- Method và path.
- Mục đích.
- Public/authenticated role.
- Request path/query/header/body.
- Response chính.
- HTTP status và error quan trọng.
- Controller method và Service liên quan.
- Trạng thái: implemented, mock, deprecated hoặc planned.

Khi thêm, sửa hoặc xóa endpoint:

1. Cập nhật API Reference trong cùng task/commit.
2. Cập nhật tài liệu module nếu contract phục vụ một module cụ thể.
3. Cập nhật Postman/OpenAPI nếu artifact đó đang được duy trì.
4. Thêm hoặc cập nhật test cho normal flow, validation, authorization và
   ownership.

## Review checklist

- [ ] Frontend dùng API thay vì mock/browser storage cho dữ liệu thật.
- [ ] Route, method và versioning đúng quy ước.
- [ ] Có DTO và validation.
- [ ] HTTP status/error body đúng ngữ nghĩa.
- [ ] Authentication, role và ownership được kiểm tra.
- [ ] Không lộ dữ liệu nhạy cảm.
- [ ] Pagination/filter được tài liệu hóa khi cần.
- [ ] Endpoint đã có trong API Reference.
- [ ] Test API đã được cập nhật.
