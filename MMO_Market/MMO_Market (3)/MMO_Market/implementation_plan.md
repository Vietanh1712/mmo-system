# Fix Shop Follow Functionality

Fix the unique key constraint violation error on `ShopFollowers(follower_id, seller_id)` when a user tries to follow a seller whom they have previously unfollowed (where a soft-deleted record with `isDelete = 1` already exists in the database).

## User Review Required

> [!NOTE]
> This change updates the follow/unfollow logic to look up any existing `ShopFollower` record (including soft-deleted ones). If a record is found, it will toggle the `isDelete` flag instead of attempting to insert a new row, which avoids triggering the database's unique constraint violation.

## Open Questions

None. The database constraint behaves as expected, and the application logic simply needs to align with it.

## Proposed Changes

---

### Repository Layer

#### [MODIFY] [ShopFollowerRepository.java](file:///c:/Users/pc/MMO_new1/MMO_Market/MMO_Market%20%283%29/MMO_Market/src/main/java/dal/ShopFollowerRepository.java)

- Add a method to find an existing follow record regardless of the `isDelete` status:
  ```java
  @Query("SELECT sf FROM ShopFollower sf WHERE sf.follower.id = :followerId AND sf.seller.id = :sellerId")
  Optional<ShopFollower> findByFollowerIdAndSellerId(@Param("followerId") Long followerId, @Param("sellerId") Long sellerId);
  ```

---

### Controller Layer

#### [MODIFY] [ProductSearchController.java](file:///c:/Users/pc/MMO_new1/MMO_Market/MMO_Market%20%283%29/MMO_Market/src/main/java/controller/ProductSearchController.java)

- In `toggleFollowSeller` method:
  - Replace the call to `shopFollowerRepository.findByFollowerIdAndSellerIdAndIsDeleteFalse(activeUserId, sellerId)` with `shopFollowerRepository.findByFollowerIdAndSellerId(activeUserId, sellerId)`.
  - Update logic: if the record exists, toggle its `isDelete` status (meaning if `isDelete` is `true`, set it to `false`, and vice versa). If it does not exist, build and save a new `ShopFollower` with `isDelete = false`.

## Verification Plan

### Automated Tests
- None.

### Manual Verification
1. Log in as a customer.
2. Navigate to any shop page (e.g., `/shop/{sellerId}`).
3. Click "Theo dõi" (Follow) and verify that the button changes to "Đang theo dõi" (Following) and the toast message says "Theo dõi cửa hàng thành công!".
4. Click "Đang theo dõi" (Following) to unfollow. Verify that the button changes back to "Theo dõi" and the toast message says "Bỏ theo dõi cửa hàng thành công!".
5. Click "Theo dõi" (Follow) again. Verify that the follow operation succeeds without throwing an error (no unique constraint violation).
