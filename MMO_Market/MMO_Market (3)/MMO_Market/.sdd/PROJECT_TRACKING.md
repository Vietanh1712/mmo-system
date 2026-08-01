# MMO Market — Project Tracking (Complete English Screen Inventory)

This document provides a 100% complete and accurate **Project Tracking Table** formatted in English, exactly matching the target schema: `Screen/Function | Feature | Actor | Screen/Function Description`.

---

## Project Tracking Table

| Screen/Function | Feature | Actor | Screen/Function Description |
| :--- | :--- | :--- | :--- |
| **User Login** | Auth | User | This is a pop-up screen or standalone page which allows the user to enter email & password to login; on this page, there are also links for user to register new information or reset the password for the case s/he forget it. |
| **User Registration** | Auth | User | Screen for new users to register an account by providing email, password, and basic profile information. |
| **Verify OTP** | Auth | User | Screen for users to enter the 6-digit OTP verification code sent via email to activate account registration. |
| **Forgot Password** | Auth | User | Form screen for users to request a password reset email by providing their registered email address. |
| **Reset Password** | Auth | User | Screen for users to set a new password after verifying the OTP reset token. |
| **Home Page** | Home | Guest, User, Seller, Staff, Admin | Main marketplace landing screen displaying search bar, featured digital categories, top products, and broadcast banners. |
| **Product Listing** | Product | User | Page displaying a grid or list of products available on the marketplace with category filters and search bar. |
| **Product Details** | Product | User | Screen showing detailed information about a specific product, including price, description, seller info, and add to cart button. |
| **Search Results** | Search | Staff, Admin, User | Search results page for products with category filters, keyword matching, price range, and sorting options. |
| **Shopping Cart** | Order | User | Screen showing items added to cart, allowing quantity adjustments and proceeding to checkout. |
| **Checkout & Payment** | Order | User | Checkout flow to select shipping address/digital delivery details, choose payment method (Wallet / SePay VietQR), and confirm the order. |
| **Pre-order Product** | Preorder | User | Screen for buyers to place a pre-order for upcoming or out-of-stock items. |
| **My Pre-orders** | Preorder | User | Dashboard screen displaying all pre-order requests placed by the user. |
| **Shop page** | Shop | User | Public store page for a specific seller displaying their profile, rating stats, warning badges, and product catalog. |
| **Support/FAQs** | Support | User | Frequently asked questions and support contact form or ticket creation screen. |
| **Live Chat** | Chat | User, Seller | Real-time chat interface for buyers to communicate directly with sellers regarding products or orders. |
| **Notification Center** | Notification | User, Seller, Staff, Admin | Unified screen displaying personal alerts (orders, wallet, KYC) and system broadcast announcements, with mark-as-read and delete options. |
| **Order History** | Order | User | Screen displaying user's purchased order history with filter options and escrow status indicators. |
| **Confirm Order & Decrypted Code** | Order | Customer, Seller | Order confirmation screen showing digital delivery items (decrypted keys/codes via AES-256), order status, and early release escrow button. |
| **Leave Feedback** | Review | User | Form screen allowing buyers to write reviews and rate products (1-5 stars) after order completion. |
| **Submit Complaint** | Complaint | User | Form to report an issue with an order or another user, allowing image attachments as evidence. |
| **My Complaints** | Complaint | User | Dashboard screen for users to track the resolution status of their submitted complaints. |
| **Submit KYC** | KYC | User | Form for sellers to submit identity verification documents (ID card / Passport) for KYC compliance. |
| **Register Shop** | Shop | User | Form for users to submit an application to upgrade to a seller account and open a shop. |
| **Wallet Dashboard** | Wallet | User, Seller | Overview of user's wallet balance (available vs hold balance), VietQR payment code, and option to deposit or withdraw funds. |
| **Deposit Topup History** | Wallet | User, Seller | Screen displaying history of deposit transactions made via VietQR SePay gateway. |
| **Cashflow Transactions** | Wallet | User, Seller | Screen displaying wallet cashflow transaction logs and balance history. |
| **My Support Tickets** | Support Ticket | User | Dashboard screen listing all technical and account support tickets submitted by the user. |
| **Security & Change Password** | Profile | User | Screen for managing password changes, enabling 2FA authentication, and account security settings. |
| **User Notifications** | Notification | User | Personal account notifications list for wallet topups, order updates, and KYC approvals. |
| **Seller Dashboard** | Dashboard | Seller | High-level overview of seller's sales, revenue, active orders, available balance, and escrow hold balance. |
| **Shop Profile & Settings** | Shop | Seller | Form for sellers to configure shop details, logo, banner, bio, and automated auto-responder messages. |
| **Close Shop Request** | Shop | Seller | Screen for sellers to submit a shop closure request and settle remaining balances. |
| **Add New Product** | Seller | Seller | Form for sellers to create a new product listing, upload images, set price, and define inventory quantities. |
| **Edit Product** | Seller | Seller | Screen for sellers to edit existing product names, descriptions, and specifications. |
| **Product Variant & AES Keys Form** | Seller | Seller | Form for sellers to add/edit digital variants and upload encrypted digital credentials (keys/accounts/cookies encrypted via AES). |
| **Inventory Management** | Seller | Seller | Dashboard for sellers to manage inventory. Sellers can only edit product names and add more stock to existing products. |
| **Order Management** | Order | Seller | Dashboard for sellers to view pending orders, update shipping status, and process fulfillments. |
| **Pre-orders Management** | Preorder | Seller | Dashboard for sellers to review, accept, or reject incoming pre-order requests from buyers. |
| **Reviews Management** | Review | Seller | Screen for sellers to view buyer ratings (1-5 stars) and submit responses to reviews. |
| **Complaints Received** | Complaint | Seller | Dashboard listing order complaints filed against the seller's store by buyers. |
| **Complaint Dispute Room** | Complaint | Seller | Dispute resolution interface for sellers to provide counter-evidence, image proofs, and communicate with Staff/Buyer. |
| **Withdrawal Management** | Withdrawal | Seller | Interface for sellers to request revenue bank withdrawals and view withdrawal history. |
| **Withdrawal Details** | Withdrawal | Seller | Screen displaying detailed withdrawal status and bank transfer receipt proof uploaded by Staff. |
| **Shop Warning Flags** | Flag / Policy | Seller | Screen displaying warning flags (Level 0), policy violation notices, and extended escrow hold duration (168 hours). |
| **Sales Statistics** | Dashboard | Seller | Analytical dashboard displaying detailed seller revenue breakdowns, sales charts, and performance metrics. |
| **Staff Dashboard** | Dashboard | Staff | High-level overview of active complaints, pending KYCs, withdrawal requests, and system support metrics. |
| **Manage Categories** | Category | Staff | Giao diện cho phép Staff tạo mới, cập nhật thông tin, ẩn/hiện hoặc sắp xếp các danh mục và phân loại sản phẩm số trên toàn hệ thống. |
| **Category Details & Form** | Category | Staff | Detail screen to view, create, or update product category parameters and icons. |
| **Documents & Policy Dashboard** | Policy | Staff | Management interface for marketplace operational policies, rules, and system documentation. |
| **Manage Complaints** | Complaint | Staff | Dashboard for staff to review active complaints, communicate with parties, and resolve disputes. |
| **Complaint Arbitration Room** | Complaint | Staff | Real-time 3-party dispute room interface for staff to inspect evidence and decide refund or release escrow. |
| **Manage Support Tickets** | Support Ticket | Staff | Receive, process, and respond to support request tickets submitted by customers or sellers regarding system issues unrelated to order complaints. |
| **Support Ticket Details** | Support Ticket | Staff | Screen for staff to read support ticket details and compose official responses to users. |
| **Review KYC** | KYC | Staff | Interface to review pending KYC applications, inspect documents, and approve or reject. |
| **Review KYC Details** | KYC | Staff | Detail inspection screen for staff to verify ID document images and approve or reject identity verification. |
| **Order & Transaction Monitoring** | Transaction | Staff | View a list and details of digital product transactions, track the escrow status countdown timer, and view the transaction history on the exchange. |
| **Transaction Details & Escrow Timer** | Transaction | Staff | Detail screen displaying digital order parameters, transaction logs, and real-time escrow countdown timer. |
| **Review Withdrawals** | Withdrawal | Staff | The interface allows you to review Seller revenue withdrawal requests, reconcile bank accounts, upload images of money transfer receipts, and approve/reject disbursements. |
| **Withdrawal Details & Receipt Upload** | Withdrawal | Staff | Screen for staff to review withdrawal request parameters, process bank transfer, and upload receipt images. |
| **Manage Seller Flags** | Flag / Policy | Staff | Manage the flagging of shops that violate the policy, set the warning level (Level 0), and activate the mechanism to increase the Escrow holding period from 72 hours to 168 hours (7 days). |
| **Flag Details & Penalty Setting** | Flag / Policy | Staff | Detail screen for staff to adjust warning level 0, apply policy violation flags, and trigger 168-hour escrow extensions. |
| **Live Chat Support** | Chat | Staff, User | Real-time chat interface for staff to provide direct support and help to users. |
| **View Reported Chats** | Chat | Staff | Interface for staff to read chat history between a buyer and seller in case of a report or dispute. |
| **Manage Shop Registrations** | Shop | Staff | Review applications for opening a shop/upgrading a seller account, check shop information, and approve or reject applications. |
| **Shop Registration Details** | Shop | Staff | Screen for staff to review proposed seller shop details, credentials, and business information. |
| **Update Shop Registration Status** | Shop | Staff | Screen/Modal for staff to approve or reject a shop application with detailed feedback comments. |
| **Deposit Reconciliation & Topup Support** | Topup | Staff | Look up deposit transaction logs (SePay/Bank gateway), support manual review or reconciliation of deposit transactions with incorrect syntax or pending payments. |
| **Deposit Topup Details & Manual Credit** | Topup | Staff | Screen for staff to inspect failed/pending deposit logs and manually credit user wallet balances. |
| **Admin Dashboard** | Admin | Admin | High-level overview of marketplace statistics, platform net revenue, active users, system alerts, and emergency maintenance mode toggle. |
| **Audit Logs** | Admin | Admin | Screen to inspect immutable system activity logs, tracking all sensitive administrative and staff actions with 100% Vietnamese action labels. |
| **Revenue & Cashflow** | Admin | Admin | Dashboard showing aggregate system platform earnings breakdown by commissions, shop opening fees, withdrawal fees, interactive SVG revenue chart, and wallet cashflow history. |
| **User Management List** | Admin | Admin | Screen to view, search, and filter all users in the system using 6 criteria (keyword, role, status, date range, sort order). |
| **Create Staff Account** | Admin | Admin | Screen with a form for admins to create a new Staff account with basic profile credentials and initial assignments. |
| **User Details & Account Status** | Admin | Admin | Screen to view detailed profile and transaction history of a specific user (non-staff role), and toggle account lock/unlock status (isLocked). |
| **Staff Details & Edit Account** | Admin | Admin | Screen to view and edit profile credentials, operational assignments, and account status of a Staff member. |
| **Staff Permission Management** | Admin | Admin | Screen for admins to manage, grant, or revoke operational permissions and roles assigned to Staff members. |
| **System Configuration** | Admin | Admin | Screen to view and update general system parameters (login session timeout, OTP duration, max login retries, 2FA requirements). |
| **Commission & Fee Settings** | Admin | Admin | Screen to configure platform default C2C commission percentages, withdrawal fee rates, escrow hold hours (72h), and shop opening fees. |
| **Manage Notifications & Maintenance** | Admin | Admin | Unified screen to manage system broadcast announcements, send platform alerts, and trigger system emergency maintenance mode banner. |
| **User Profile** | Profile | User | Screen displaying user's public profile, account overview, and public activity details. |
