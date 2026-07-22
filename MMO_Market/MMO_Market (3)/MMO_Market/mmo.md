# Tab 1

1.Use case:

- Linh : guest  
- Cường: staff  
- Nhật: admin  
- Đăng: seller  
- Việt Anh: customer

2\. Swimlane:

- Linh: Mua hàng  
- Nhật: Quản lý người dùng, Quản lý cấu hình hệ thống, Quản lý nhân viên & phân quyền, Xem báo cáo doanh thu, Xem nhật ký hệ thống, Xem bảng điều khiển Admin  
- Việt Anh: Đăng ký shop  
- Đăng: quản lý kho  
- Cường: Đăng sản phẩm

3\. Use case specification:

- Linh : UC Register User Account, Use case Make order  
- Cường: UC Review KYC Request, UC Resolve Order Complaint, UC Manage Shop Flags, UC Review Withdrawal Request  
- Nhật: UC View Admin Dashboard, UC Manage User Accounts, UC Manage Staff & Permissions, UC Manage System Configuration, UC View Revenue Reports, UC View System Audit Logs  
- Đăng:   
- Việt Anh: 


  
4\. 

# Use case

#### 

 

1. Use case Make order

| UC ID and Name: | Make Order |  |  |
| ----: | :---- | ----- | :---- |
| Created By: | Linhntn | Date Created: | 25/5/2026 |
| Primary Actor: | Customer | Secondary Actors: |  None |
| Trigger: | Customer initiatives a payment for an order |  |  |
| Description: | As a customer, I want to make a payment for my order using my VND wallet balance so that I can immediately receive access to the digital assets   |  |  |
| Preconditions: | PRE-1: The Customer has logged into MMO Market. PRE-2: The Customer has chosen a product, selected a specific ProductVariant, and entered the desired quantity. PRE-3: The Customer's wallet balance is sufficient to cover the total payment amount |  |  |
| Postconditions: | POST-1: Payment is successful The transaction status is updated to Held (escrow hold state). The system automatically delivers the digital assets and redirects to the transaction detail. POST-2: Payment fails The transaction is aborted and rolled back. The system logs the error and displays a warning toast message.  |  |  |
| Normal Flow: | Customer selects the product variant and quantity to purchase in product detail. The system displays checkout details (including price, quantity). Customer confirms the order. The system validates the customer’s balance, variant stock, and available digital assets. If valid, the system deducts the money from balance and decrements variant stock. The transaction status is updated to Held and digital assets are auto-assigned. The system displays the success popup with the delivered credentials (username/password or product key). |  |  |
| Alternative Flows: |  |  |  |
| Exceptions: | E1 – The customer’s balance \< required money: The system displays an insufficient balance error and suggests/shows the link to the top-up page. E2 – System crash or database error: The transaction is aborted and rolled back automatically.  |  |  |
| Priority: | High |  |  |
| Frequency of Use: | Very frequent (every time a purchase is made). |  |  |
| Business Rules: |  |  |  |
| Other Information: | None |  |  |
| Assumptions: | The Customer's VND wallet balance is always synchronized in real-time |  |  |

   

   

2. .UC Register User Account

| UC ID and Name: | Register User Account  |  |  |
| ----: | :---- | :---- | :---- |
| Created By: | Linhntn | Date Created: | 25/5/2026 |
| Primary Actor: | Guest | Second Actors | Google Gateway |
| Trigger: | A guest wants to create a new account on the system. |  |  |
| Description: |  As a guest, I want to register a new account using either my email and password or my Google account, so that I can log in and use the system’s features. |  |  |
| Preconditions: | PRE-1: The user has not registered before (unique email). PRE-2: The system is connected to Google Authentication API. |  |  |
| Postconditions: | POST-1 : A new user account is successfully created and stored in the system. POST-2: The user can log in immediately after registration. |  |  |
| Normal Flow: | 1\. Guest selects the “Sign up” option. 2\. The system displays the registration form (Email, Password). 3\. Guest enters valid information and clicks “Sign up” 4\. The system validates the input (email uniqueness, password strength). 5\. The system sends an OTP code to the Guest’s email. 6\. Guest enters the OTP code into the system. 7\. The system saves the information in the database and creates a new account. 8\. Guest is logged in and redirected to the homepage as a Customer  |  |  |
| Alternative Flows: | Step1\_1. Registration via Google 1\. Guest selects “Register with Google.” 2\. The system redirects to Google Authentication Service. 3\. Guest successfully authenticates with Google. 4\. The system retrieves user data (name, email). 5\. The system creates and activates the account immediately. 6\. Guest is logged in automatically.  |  |  |
| Exceptions: | E1 \- Database not responding → registration cannot be completed. E2 \- Email service unavailable → OTP cannot be sent, registration delayed. E3 \- User inputs duplicate email  E4 \- User input invalid OTP E5 \- Google Authentication Failed |  |  |
| Priority: | High |  |  |
| Frequency of Use: |  High, frequently used. |  |  |
| Business Rules: |  |  |  |
| Other Information: | OTP resend limit may be enforced to prevent abuse. |  |  |
| Assumptions: | \- Guest has access to the email used for registration   \-Google and Email services are functioning normally. |  |  |

# swimlane

# use case specification

Như Cường  /// STAFF

1. Review KYC Request

| UC ID and Name: | Review KYC Request |  |  |
| ----: | :---- | ----- | :---- |
| Created By: | CuongNN1312 | Date Created: | 17/7/2026 |
| Primary Actor: | Staff (or Admin) | Secondary Actors: | Customer(The person who submitted the request will receive notification of the results) |
| Trigger: | Staff access the KYC pending approval list on the system and select a specific request to check. |  |  |
| Description: |   As a staff member, I want to review (approve or reject) customer KYC verification requests to update their verification status in the system, thereby allowing them to register for a shop or use advanced features. |  |  |
| Preconditions: | PRE-1: Staff have successfully logged into the system administration page and have a valid role . PRE-2: At least one customer KYC identity verification request is pending. PRE-3: The system has fully uploaded the KYC profile details including: Full name, Date of birth, Address, Document number, Document type (Citizen ID/Passport) and 3 uploaded photos (Front-side photo, Back-side photo, Selfie photo). |  |  |
| Postconditions: | POST-1: Upon successful approval (Approved)                   \-The KYC Request status is updated to APPROVED.                   \-The system records the reviewer's information (reviewedBy) and the review time (reviewedAt).                   \-The system saves the changes to the database and automatically generates a success notification (severity \= SUCCESS) to send to the Customer, informing them that they can proceed with registering to open a Shop. Post-2: Upon rejection (Rejected)                  \-The KYC Request status is updated to REJECTED.                  \-The system saves the rejection reason (rejectionReason).                   \-The activeUserId constraint on the KYCRequests table is                  released (set to NULL), allowing the Customer to recreate and submit a new KYC request.                   \-The system saves the changes and automatically generates a rejection notification (severity \= DANGER) to send to the Customer with the specific reason.  |  |  |
| Normal Flow: | 1.Staff access the KYC list screen (/staff/kyc) and select a profile with the status PENDING. 2.The system displays detailed identification information and links to profile images (front, back, and selfie photos). 3.Staff check the validity and clarity of the documents, then click the "Approve" button. 4.The system checks the current status of the request to ensure it remains PENDING and compares the version code to avoid data conflicts. 5.The system updates the request status to APPROVED, records the staff member's approval information, and stores it in the database. 6.The system generates and sends a successful approval notification to the customer's account. |  |  |
| Alternative Flows: | A1 – Staff Rejects KYC Request: 1.At step 3 of the normal flow, the staff member finds the information or image invalid and clicks the "Reject" button. 2.The system displays a field for entering the reason for rejection. 3.The staff member enters the reason (e.g., "The ID card image is blurry, the text is unclear") and clicks confirm/submit. 4.The system checks the request status (PENDING) and version code. 5.The system updates the request status to REJECTED, saves the rejection reason, and releases the activeUserId field of the user. 6.The system generates and sends a rejection notification with the reason to the customer's account. |  |  |
| Exceptions: | E1 – Network failure during payment E2 – Unexpected system crash during transaction: Transaction is aborted; the system rolls back changes and logs the error. E3 \- The customer’s balance \< required money |  |  |
| Priority: | High |  |  |
| Frequency of Use: | Regularly (Every time a customer submits an account verification request). |  |  |
| Business Rules: | KYC approval/rejection can only be performed by users with the Staff or Admin role. The KYC profile status can only be changed from PENDING to APPROVED or REJECTED; it is not possible to revert or update completed statuses. Rejections must include a reason for rejection. |  |  |
| Other Information: | None |  |  |
| Assumptions: | Document images uploaded by users are stored securely, and staff can view them through a secure endpoint that verifies permissions. |  |  |

   

   

1.  Resolve Order Complaint 

| UC ID and Name: | Resolve Order Complaint |  |  |
| ----: | :---- | ----- | :---- |
| Created By: | CuongNN1312 | Date Created: | 17/7/2026 |
| Primary Actor: | Staff (or Admin) | Secondary Actors: |  Customer (Buyer), Seller  |
| Trigger: | Staff access the list of pending complaints and issue a ruling to close the complaint. |  |  |
| Description: |   As a staff member, I want to initiate a confrontation, monitor the three-party negotiation process, and make a decision to close the complaint (refund the Buyer or disburse funds to the Seller, flag a warning if necessary) to ensure financial fairness on the platform. |  |  |
| Preconditions: | PRE-1: Staff has successfully logged into the system with the Staff or Admin role. PRE-2: Customer has submitted a complaint regarding a previous order transaction (Complaint has PENDING\_REVIEW status and the transaction has a Disputed status with frozen escrow funds). |  |  |
| Postconditions: | POST-1: Initiating Confrontation (In\_Progress):                \-Complaint status changes to In\_Progress.                \-The system automatically creates a chat message to activate a 3-party confrontation chat room.                \-Notifications are sent to the Customer and Seller to participate in the confrontation. Post-2: Complaint Accepted (Customer Wins \- Resolved):                \-Complaint status is updated to Resolved (or Completed).                \-Transaction status changes to Refunded.                \-The dynamic (pro-rata) refund is calculated based on the number of unused days for the Customer (refunded to the available wallet).                \-The remaining amount (after deducting commission) is disbursed to the Seller's available wallet.                \-A WalletTransaction is created to record balance changes and notify both parties of the result.                \-The Seller's shop level (shop\_level) is automatically re-evaluated.                \-A warning flag (ShopFlag) is attached to the Seller if configured by Staff. POST-3: Claim Rejected (Seller Wins \- Rejected):               \-Claim status is updated to Rejected.               \-Transaction status changes to Completed.               \-100% of the amount (after deducting exchange commission) is disbursed to the Seller's available wallet.              \-WalletTransaction is created to record the balance change and a notification of the result is sent to both parties. |  |  |
| Normal Flow: | 1.The staff accesses the complaint management page (/staff/complaints) and selects the complaint in PENDING\_REVIEW status. 2.The staff clicks the "Start Dispute" button. 3.The system changes the complaint status to In\_Progress, posts an automated message to open a dispute chat room, and redirects the staff to /staff/chat. 4.The staff monitors the parties exchanging evidence in the chat room (Staff have read-only access). 5.After the dispute period, the staff makes a decision, chooses to update the status to "Resolved" or "Completed," enters the resolution, and selects the flag level (flagLevel, flagReason). 6.The system calculates the number of days used (based on the time from purchase to complaint and the product variant package): Refund to Buyer \= Math.ceil(total amount / total package days \* unused days). Amount paid to Seller \= total amount \- refund \- corresponding commission fee. 7.The system updates wallet balances, creates wallet transactions, and sends result notifications to both Buyer and Seller. |  |  |
| Alternative Flows: | A1 – Staff Rejects Claim (Seller Wins):       1.At step 5 of the normal flow, the Staff decides the Customer's claim is invalid and selects "Rejected".       2.The Staff enters the reason for rejecting the judgment and clicks save.       3.The system updates the transaction status to Completed, releasing the entire amount (excluding exchange commission) to the Seller's available wallet.       4.The system records the wallet transaction history and sends a disbursement notification to the Seller and a rejection notification to the Buyer. |  |  |
| Exceptions: | E1 – Concurrent Update Conflict:                \-Condition: Two staff members are processing a complaint resolution at the same time.                \-System Behavior: The system rejects the update due to a version code discrepancy, performs a rollback, and displays a message requesting a page reload.  |  |  |
| Priority: | High |  |  |
| Frequency of Use: | Frequently (Whenever there is a dispute over an order that cannot be resolved through negotiation). |  |  |
| Business Rules: | \-Only staff/admins have the authority to change complaint status and make decisions. \-Staff have read-only access in the chat room to maintain objectivity. \-Refunds are calculated pro-rata based on the actual number of days the digital product was used (e.g., 1-month, 3-month, 1-year account packages). |  |  |
| Other Information: | None |  |  |
| Assumptions: | Transaction funds are locked in a secure intermediary wallet (Disputed status) until a final decision is made by the staff. |  |  |

   

   

1. Manage Shop Flags 

| UC ID and Name: | Manage Shop Flags  |  |  |
| ----: | :---- | ----- | :---- |
| Created By: | CuongNN1312 | Date Created: | 17/7/2026 |
| Primary Actor: | Staff (or Admin) | Secondary Actors: |  Seller |
| Trigger: | Staff access the list of warning flags or notice that a shop shows signs of violating regulations and needs to have its disciplinary information adjusted. |  |  |
| Description: |   As a staff member, I want to review and update the severity or reasons for warning flags, as well as remove warning flags for violating shops, in order to maintain the transparency and credibility of the exchange. |  |  |
| Preconditions: | PRE-1: Staff has successfully logged in as either Staff or Admin. PRE-2: The system already has a record of a warning flag for the violating shop (this may be automatically created after the complaint is resolved). |  |  |
| Postconditions: | POST-1: Flag Update Successful:               \-The flagLevel, reason, and status fields have been updated in the database. Post-2: Soft Delete Successful:              \-The offending flag is marked as soft deleted (isDelete \= true).              \-This flag no longer appears in the list of active flags. |  |  |
| Normal Flow: | 1.Staff access the flag management page (/staff/flags). 2.The system displays a list of flags along with statistics (Total number of flags, active flags, removed flags, number of flags by level: Danger, Warning, Critical). 3.Staff click the "Details" button for a flag to view detailed information (Seller flagged, staff member who issued the flag, related complaint, reason, level, and current status). 4.Staff then change the flag level (flagLevel: Warning, Critical, Danger), flag status (status: Effect, Removed), or edit the reason for the violation. 5.Staff click the "Update" button. 6.The system saves the changes to the database and displays the message: "Flag updated successfully". |  |  |
| Alternative Flows: | A1 – Soft Delete Warning Flag:       \-At step 4 of the normal flow, the Staff decides to withdraw the penalty flag and clicks the "Remove Flag" button.      \-The system updates the isDelete field to true for that ShopFlag record.      \-The system saves the data and redirects the Staff back to the flag list page with the message: "Flag successfully removed".  |  |  |
| Exceptions: | E1 – Warning flag does not exist or has been previously removed:          \-Conditions: The flag ID does not exist in the database or the isDelete attribute is already true.         \-System behavior: The system refuses to display and redirects Staff back to the flag list page /staff/flags. |  |  |
| Priority: | Medium |  |  |
| Frequency of Use: | Infrequently (Only performed when necessary to adjust booth discipline). |  |  |
| Business Rules: | \-It is mandatory to use the Soft Delete method by setting the isDelete flag to true instead of physically deleting data in the database in order to retain the confrontation and audit history. \-The standard flag levels supported by the system are: Warning, Critical, Danger. |  |  |
| Other Information: | None |  |  |
| Assumptions: | None |  |  |

   

   

1. Review Withdrawal Request  

| UC ID and Name: | Review Withdrawal Request   |  |  |
| ----: | :---- | ----- | :---- |
| Created By: | CuongNN1312 | Date Created: | 17/7/2026 |
| Primary Actor: | Staff (or Admin) | Secondary Actors: |  Seller |
| Trigger: | Staff access the list of pending withdrawal requests on the dashboard. |  |  |
| Description: | As a staff member, I want to verify the seller's withdrawal request, perform a manual external bank transfer, and then update the status (Approved with transaction proof or Rejected withdrawal request) on the system. |  |  |
| Preconditions: | PRE-1: Staff has successfully logged in as either Staff or Admin. PRE-2: Seller has created a valid withdrawal request (the request is in Pending status, the withdrawal amount has been temporarily deducted from the Seller's available wallet). PRE-3: The system has fully loaded the Seller's bank account information (SellerBankInfo) including: Bank name, Account number, Account holder name, Withdrawal amount, and Transaction fee. |  |  |
| Postconditions: | POST-1: Withdrawal Approved (Completed):               \-The withdrawal request status changes to Completed (or Approved).               \-The database trigger trg\_UpdateWithdrawalProof automatically assigns the name of the bank transfer proof file (proof\_file \= 'proof\_bank\_{id}.jpg').                \-The system generates a success message (severity \= SUCCESS) and sends it to the Seller. POST-2: Withdrawal Rejected (Rejected):                \-The withdrawal request status changes to Rejected.                \-The system generates a rejection message (severity \= DANGER) and sends it to the Seller.  |  |  |
| Normal Flow: | 1.Staff access the withdrawal management page (/staff/withdrawals) and filter by Pending status. 2.The system displays a list of pending withdrawal requests. 3.Staff click "Detail" to view the Seller's bank information and amount. 4.Staff then proceed to make the actual external transfer via the exchange's bank to the Seller's account number. 5.After the transfer is successful, Staff selects "Approved" or "Completed" on the details form. 6.Staff clicks "Update Status". 7.The system updates the status, triggers the DB to automatically assign the supporting file information, saves it to the database, and sends a completion notification to the Seller. |  |  |
| Alternative Flows: | A1 – Staff Rejects Withdrawal Request:        \-At step 4 of the normal flow, the staff detects that the Seller's bank information is invalid or the account has unusual activity.        \-The staff clicks the "Reject Withdrawal" button or selects the status as "Rejected" and then clicks save.         \-The system updates the request status to "Rejected" and sends a notification to the Seller to inform them of the reason for the failed transaction.  |  |  |
| Exceptions: | E1 – Withdrawal Request Not Found: \-Condition: The withdrawal request code does not exist in the system. \-System Behavior: The system displays an error and redirects Staff to the /staff/withdrawals request list.  |  |  |
| Priority: | High |  |  |
| Frequency of Use: | Very frequently (Every time the Seller requests a cash withdrawal from the system). |  |  |
| Business Rules: | The minimum withdrawal limit is dynamically configured at 50,000 VND (automatically checked by the trg\_CheckWithdrawalMin trigger). Withdrawal fees are calculated dynamically based on system configuration (default is 1.5% of the withdrawal amount and the minimum is 10,000 VND). |  |  |
| Other Information: | None |  |  |
| Assumptions: | The staff successfully completed the actual external money transfer before updating the status to "successful" on the system. |  |  |


Như Long Nhật /// Admin

### 1. UC View Admin Dashboard

| UC ID and Name: | View Admin Dashboard (Xem bảng điều khiển Admin) |  |  |
| ----: | :---- | ----- | :---- |
| Created By: | Long Nhật /// Admin | Date Created: | 18/7/2026 |
| Primary Actor: | Admin | Secondary Actors: | None |
| Trigger: | Admin navigates to the Admin Panel home screen. |  |  |
| Description: | As an Admin, I want to view a quick summary dashboard showing critical user base metrics (total accounts, active/locked/verified users, total sellers/staff) to get an instantaneous overview of platform utilization. |  |  |
| Preconditions: | PRE-1: Admin is logged in and authorized. |  |  |
| Postconditions: | POST-1: Correct dashboard summary statistics are displayed on screen. |  |  |
| Normal Flow: | 1. Admin accesses the Admin Dashboard URL (/admin/users).<br>2. Frontend calls GET `/api/admin/user-management/summary`. <br>3. The backend AdminUserManagementService fetches all accounts from the database and groups them into counters: total accounts, active accounts, locked accounts, staff accounts, verified accounts, and seller accounts.<br>4. Backend returns the counters in a JSON map.<br>5. Frontend renders the metrics as visual cards/widgets. |  |  |
| Alternative Flows: | None |  |  |
| Exceptions: | E1 – Database query timeout: The system displays an error toast message: "Không thể lấy thông tin tổng quan hệ thống." |  |  |
| Priority: | Medium |  |  |
| Frequency of Use: | Frequent (whenever returning to admin home). |  |  |
| Business Rules: | Statistics must reflect the current non-deleted users (`isDelete = 0`). |  |  |
| Other Information: | None |  |  |
| Assumptions: | Users count matches the database records in real-time. |  |  |

---

### 2. UC Manage User Accounts

| UC ID and Name: | Manage User Accounts (Quản lý tài khoản người dùng) |  |  |
| ----: | :---- | ----- | :---- |
| Created By: | Long Nhật /// Admin | Date Created: | 18/7/2026 |
| Primary Actor: | Admin | Secondary Actors: | User (Customer/Seller/Staff) |
| Trigger: | Admin accesses the User Management section. |  |  |
| Description: | As an Admin, I want to view, search, filter, update roles, toggle locking status, and soft-delete user accounts on the system. |  |  |
| Preconditions: | PRE-1: Admin is logged in and authorized.<br>PRE-2: The target user account exists in the database. |  |  |
| Postconditions: | POST-1: Target user account's role, lock status, or soft-deleted flag (isDelete = 1) is modified in the database.<br>POST-2: Active sessions for the modified user are terminated or re-validated.<br>POST-3: Action is written to System Audit Logs. |  |  |
| Normal Flow: | 1. Admin navigates to the user management panel.<br>2. The UI sends a GET request to `/api/admin/user-management/users` with optional filter params (email, role, status).<br>3. System returns a paginated list of matching users.<br>4. Admin selects a user, opens the edit drawer, changes the role (e.g. Customer to Seller) and clicks "Save".<br>5. Frontend sends a PUT request to `/api/admin/user-management/users/{userId}/role` containing the new role.<br>6. The system checks permissions, updates the Users table, writes an entry to the AuditLog, and returns 200 OK. |  |  |
| Alternative Flows: | A1 – Lock/Unlock User Accounts:<br>1. In the user list, Admin clicks "Khóa/Mở khóa" on a specific user row.<br>2. Frontend calls POST `/api/admin/user-management/users/{userId}/toggle-lock`. <br>3. Backend toggles the isLocked flag in the database, writes an entry to the AuditLog, and returns a JSON response indicating the new lock status.<br><br>A2 – Soft-delete User Accounts:<br>1. In the user list, Admin clicks the "Xóa" button for a user.<br>2. The system displays a confirmation modal.<br>3. Admin confirms the action.<br>4. Frontend calls DELETE `/api/admin/user-management/users/{userId}`.<br>5. Backend updates the target user's record setting `isDelete = 1`, writes to the AuditLog, and updates the list. |  |  |
| Exceptions: | E1 – User Not Found: Target userId does not exist. System returns 404 Not Found.<br>E2 – Self-Demotion/Self-Locking: Admin attempts to modify or lock their own account. The service blocks this action and returns 400 Bad Request: "Bạn không thể tự khóa hoặc thay đổi quyền của chính mình." |  |  |
| Priority: | High |  |  |
| Frequency of Use: | Regularly. |  |  |
| Business Rules: | - Soft delete is mandatory (`isDelete = 1`), physical deletes are forbidden for audit safety.<br>- Admins cannot lock or demote their own accounts. |  |  |
| Other Information: | None |  |  |
| Assumptions: | System checks role rules on every subsequent API authentication check. |  |  |

---

### 3. UC Manage Staff & Permissions

| UC ID and Name: | Manage Staff & Permissions (Quản lý nhân viên & phân quyền) |  |  |
| ----: | :---- | ----- | :---- |
| Created By: | Long Nhật /// Admin | Date Created: | 18/7/2026 |
| Primary Actor: | Admin | Secondary Actors: | Staff |
| Trigger: | Admin wants to add new Staff, edit Staff details, or adjust active Staff permissions. |  |  |
| Description: | As an Admin, I want to onboard new Staff members and manage their specific operational permissions (such as KYC_REVIEW, COMPLAINT_RESOLVE, WITHDRAWAL_APPROVE) to control what tasks staff can handle. |  |  |
| Preconditions: | PRE-1: Admin is logged in and authorized. |  |  |
| Postconditions: | POST-1: A new Staff account is registered, or an existing Staff's profile is updated in the database.<br>POST-2: Selected permissions are mapped to the Staff in the database (UserPermissions table) and logged in AuditLog. |  |  |
| Normal Flow: | 1. Admin navigates to the Staff Management tab and clicks "Thêm nhân viên".<br>2. Admin fills in details: Email, password, full name, phone number, and clicks "Create".<br>3. Frontend calls POST `/api/admin/user-management/staff`. <br>4. The system validates the inputs, checks for duplicate email, hashes the password with BCrypt, sets role to 'Staff', saves the record, writes to AuditLog, and returns the created user DTO. |  |  |
| Alternative Flows: | A1 – Edit Staff Info:<br>1. Admin selects a staff, edits credentials, and clicks save.<br>2. System calls PUT `/api/admin/user-management/staff/{staffId}` and saves modifications to DB.<br><br>A2 – Assign Staff Permissions:<br>1. Admin selects a Staff account and clicks "Phân quyền".<br>2. System loads the list of available permissions and checks current assignments.<br>3. Admin selects permissions (e.g. KYC, COMPLAINT) and clicks "Xác nhận gán".<br>4. System sends POST to `/api/admin/staff-permissions/assign` with userIds and permissionNames.<br>5. Backend creates new rows in UserPermissions table mapping the staff to the permissions, updates logs, and returns 200 OK.<br><br>A3 – Revoke Staff Permissions:<br>1. Admin unchecks permissions for a staff and clicks "Thu hồi".<br>2. System sends POST to `/api/admin/staff-permissions/revoke` with the staff userId and permissionNames.<br>3. Backend removes mapped entries from the UserPermissions table. |  |  |
| Exceptions: | E1 – Email already exists: System displays "Email đã tồn tại trên hệ thống" and rejects creation. |  |  |
| Priority: | High |  |  |
| Frequency of Use: | Occasionally (on onboarding/offboarding). |  |  |
| Business Rules: | Mappings are managed via UserPermissions table linking Users and Permissions. Only Admin can assign/revoke permissions. |  |  |
| Other Information: | None |  |  |
| Assumptions: | Permissions updates take effect immediately on next API request check. |  |  |

---

### 4. UC Manage System Configuration

| UC ID and Name: | Manage System Configuration (Quản lý cấu hình hệ thống) |  |  |
| ----: | :---- | ----- | :---- |
| Created By: | Long Nhật /// Admin | Date Created: | 18/7/2026 |
| Primary Actor: | Admin | Secondary Actors: | None |
| Trigger: | Admin needs to configure fee rates, limits, or toggle maintenance mode. |  |  |
| Description: | As an Admin, I want to edit system-wide configurations, such as minimum withdrawable amount, maintenance state, and commission rates/fees. |  |  |
| Preconditions: | PRE-1: Admin is logged in and authorized. |  |  |
| Postconditions: | POST-1: Configuration values are updated in the SystemConfigurations table and memory cache. |  |  |
| Normal Flow: | 1. Admin navigates to "System Configurations" page.<br>2. The system loads configurations via GET `/api/admin/system-config`. <br>3. Admin adjusts general configurations (e.g. minimum withdrawal limit, support phone, description) and clicks "Save General Config".<br>4. System sends PUT request to `/api/admin/system-config/general`. <br>5. Backend updates the SystemConfigurations database table, updates memory cache, writes to AuditLog, and returns 200 OK. |  |  |
| Alternative Flows: | A1 – Setup Commission Rates & Fees:<br>1. Admin navigates to the Fee configuration section.<br>2. Admin adjusts transaction commission percentage, topup commission percentage, withdrawal fee percent and minimum fee amount.<br>3. Admin clicks "Save Commission Config".<br>4. System sends PUT to `/api/admin/system-config/commissions` with updated figures.<br>5. Backend validates numbers (must be non-negative, percentages must be <= 100%), updates DB, logs the action, and returns success.<br><br>A2 – Manage Maintenance Mode:<br>1. Admin toggles the Maintenance Mode switch.<br>2. System sends POST request to `/api/admin/notifications/toggle-maintenance`. <br>3. Backend updates the configuration key, triggers MaintenanceInterceptor to block non-admin logins/API calls, broadcasts system notice via NotificationController, and logs to AuditLog. |  |  |
| Exceptions: | E1 – Invalid Fee Range: Percentages exceed 100 or are negative. The system returns 400 Bad Request with an error description: "Giá trị cấu hình không hợp lệ." |  |  |
| Priority: | High |  |  |
| Frequency of Use: | Infrequently. |  |  |
| Business Rules: | - Minimum withdrawal limit is checked against database configuration. Defaults to 50,000 VND.<br>- All system parameters are stored key-value pairs in SystemConfigurations table. |  |  |
| Other Information: | None |  |  |
| Assumptions: | System configuration changes propagate immediately system-wide. |  |  |

---

### 5. UC View Revenue Reports

| UC ID and Name: | View Revenue Reports (Xem báo cáo doanh thu) |  |  |
| ----: | :---- | ----- | :---- |
| Created By: | Long Nhật /// Admin | Date Created: | 18/7/2026 |
| Primary Actor: | Admin | Secondary Actors: | None |
| Trigger: | Admin accesses the revenue reporting tool. |  |  |
| Description: | As an Admin, I want to review financial summaries (net income, total volume, pending funds in escrow), filter individual cashflow transactions, and export them as CSV reports. |  |  |
| Preconditions: | PRE-1: Admin is logged in and authorized. |  |  |
| Postconditions: | POST-1: Financial summary cards are rendered with exact metrics.<br>POST-2: Transaction details are displayed in a filterable grid.<br>POST-3: CSV data is compiled and downloaded to the client machine on export. |  |  |
| Normal Flow: | 1. Admin accesses the Revenue Reports menu.<br>2. System fetches summary metrics via GET `/api/admin/revenue/summary`. Backend aggregates statistics from Transactions and WalletTransactions tables (net revenue, volume, commission) and returns it.<br>3. System fetches cashflow transaction records via GET `/api/admin/revenue/transactions`. <br>4. Admin applies filters (transaction type, keyword, start/end dates).<br>5. System returns filtered paginated listings from database. |  |  |
| Alternative Flows: | A1 – View Cash Flow / Export to CSV:<br>1. With or without filters applied, Admin clicks the "Xuất file CSV" button.<br>2. System sends GET request to `/api/admin/revenue/export` with the filters.<br>3. Backend generates CSV records from WalletTransactions and Transactions tables, writes raw byte stream, and pushes it with header `Content-Disposition: attachment; filename=bao-cao-doanh-thu.csv`. |  |  |
| Exceptions: | E1 – Invalid Date Format: Dates input do not match expected format. System prompts the admin to select a valid date range. |  |  |
| Priority: | Medium |  |  |
| Frequency of Use: | Periodic (weekly/monthly checks). |  |  |
| Business Rules: | Cash flow amounts are based in VND integers (BIGINT) to avoid rounding discrepancies. |  |  |
| Other Information: | None |  |  |
| Assumptions: | Transactions list represents accurate, complete records of the ledger. |  |  |

---

### 6. UC View System Audit Logs

| UC ID and Name: | View System Audit Logs (Xem nhật ký hệ thống) |  |  |
| ----: | :---- | ----- | :---- |
| Created By: | Long Nhật /// Admin | Date Created: | 18/7/2026 |
| Primary Actor: | Admin | Secondary Actors: | None |
| Trigger: | Admin needs to review system events or check operator actions. |  |  |
| Description: | As an Admin, I want to view, search, and export system audit logs capturing administrator/staff actions (logins, config changes, permission gán) and automated system jobs. |  |  |
| Preconditions: | PRE-1: Admin is logged in and authorized. |  |  |
| Postconditions: | POST-1: AuditLog list is returned with timestamp, actor email, action name, details, and client IP address.<br>POST-2: Audit log CSV is generated and downloaded. |  |  |
| Normal Flow: | 1. Admin goes to the Audit Logs page.<br>2. System queries database via GET `/api/admin/audit-logs` using filters like query search or action type.<br>3. Backend returns a paginated list of logs.<br>4. Admin reviews action details (IP address, actor info, details JSON string). |  |  |
| Alternative Flows: | A1 – Export Audit Logs to CSV:<br>1. Admin clicks "Xuất file CSV" button.<br>2. System calls GET `/api/admin/audit-logs/export` with search/action parameters.<br>3. Backend returns CSV binary stream with filename `nhat-ky-he-thong.csv`. |  |  |
| Exceptions: | None |  |  |
| Priority: | Low |  |  |
| Frequency of Use: | On demand (for verification/investigation). |  |  |
| Business Rules: | - Audit Logs are immutable; they can never be modified or deleted via any controller endpoint.<br>- IP addresses must be retrieved from request headers (handling proxies). |  |  |
| Other Information: | None |  |  |
| Assumptions: | AuditLog table captures all operations successfully. |  |  |



