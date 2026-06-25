# Fix Product Review Flow after Purchase

Integrate the product review page (`leave-feedback.html`) with the backend API (`TransactionController`) so that real transaction details are resolved dynamically and duplicate reviews are prevented correctly.

## User Review Required

> [!NOTE]
> This change will replace the mock `localStorage` based logic in the product review form with a live call to the database through `/api/transactions/{id}`. This will ensure that only valid purchases can be reviewed, and reviews will be correctly linked to their respective transaction records.

## Open Questions

None. The schema and API paths are already in place and just need to be integrated.

## Proposed Changes

---

### Backend Components

#### [MODIFY] [TransactionController.java](file:///c:/Users/pc/MMO_new1/MMO_Market/MMO_Market%20%283%29/MMO_Market/src/main/java/controller/TransactionController.java)

- Inject `ReviewRepository` using `@Autowired`.
- Update `/api/transactions/me` (lines 104-139) to check the database for existing reviews using `reviewRepository.existsByTransactionIdAndIsDeleteFalse(t.getId())` and assign it to the `OrderDto.isReviewed` property.
- Update `/api/transactions/{id}` (lines 142-201) to dynamically resolve and return the `isReviewed` property from the database instead of the hardcoded `false`.

---

### Frontend Components

#### [MODIFY] [leave-feedback.html](file:///c:/Users/pc/MMO_new1/MMO_Market/MMO_Market%20%283%29/MMO_Market/src/main/resources/templates/account/leave-feedback.html)

- Change the `loadOrderInfo` function to `async`.
- Inside `loadOrderInfo`, extract the `transactionId` from the `orderCode` parameter (using regex `/MMO-ORD-(\d+)/`).
- Call the backend REST API at `/api/transactions/{transactionId}` using `authFetch` to load the actual product name, seller name, product ID, and `isReviewed` status.
- If `isReviewed` is `true`, disable the submit button and show a warning message indicating that the transaction has already been reviewed.

## Verification Plan

### Automated Tests
- None.

### Manual Verification
1. Run the Spring Boot application.
2. Log in as a customer who has completed transactions.
3. Go to **Lịch sử mua hàng** (Order History) -> click an order details page.
4. Verify that the **Đánh giá sản phẩm** button appears if the order is completed and not yet reviewed.
5. Click **Đánh giá sản phẩm** to navigate to `/account/orders/{orderCode}/feedback`.
6. Submit a review with a star rating and comment.
7. Verify that the review is saved in the database under `Reviews` table and linked with the correct `transaction_id`.
8. Return to the order details page and verify that the button has changed to **Đã đánh giá** and is disabled.
9. Try to navigate back directly to the feedback page for that order, and verify that the submit button is disabled.
