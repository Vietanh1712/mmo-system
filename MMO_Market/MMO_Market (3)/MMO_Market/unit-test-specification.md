# ĐẶC TẢ CHI TIẾT TOÀN BỘ UNIT TEST CỦA HỆ THỐNG MMO MARKET

Tài liệu này đặc tả chi tiết danh sách tất cả các lớp kiểm thử (Test Classes) và phương thức kiểm thử (Test Methods) cho toàn bộ hệ thống **MMO Market**, được phân bổ theo mô hình kiến trúc phân lớp phẳng.

---
## 📂 1. PACKAGE: com.mmo.controller (Kiểm thử API Controller & RBAC)

## 📄 Lớp kiểm thử: ComplaintControllerTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **listEscalated_returns200**
        * *Mô tả nghiệp vụ*: Lấy danh sách escalated trả về mã HTTP 200 OK.
    * 🔍 **coApprove_returnsSuccess**
        * *Mô tả nghiệp vụ*: Co approve returns thực hiện thành công.


## 📄 Lớp kiểm thử: ComplaintControllerRbacTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **list_withoutAuth_unauthorized**
        * *Mô tả nghiệp vụ*: Lấy danh sách khi không có xác thực lỗi chưa đăng nhập (401 Unauthorized).


## 📄 Lớp kiểm thử: ProductControllerTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **delete_returns204**
        * *Mô tả nghiệp vụ*: Xóa trả về mã HTTP 204 No Content.


## 📄 Lớp kiểm thử: ProductControllerRbacTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **list_withoutAuth_unauthorized**
        * *Mô tả nghiệp vụ*: Lấy danh sách khi không có xác thực lỗi chưa đăng nhập (401 Unauthorized).


## 📄 Lớp kiểm thử: AuthenticationControllerTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **login_returnsTokens**
        * *Mô tả nghiệp vụ*: Đăng nhập returns tokens.


## 📄 Lớp kiểm thử: StaffComplaintControllerTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **list_requiressellerId**
        * *Mô tả nghiệp vụ*: Lấy danh sách yêu cầu sản phẩm/cửa hàng/sản phẩm/cửa hàng id.


## 📄 Lớp kiểm thử: PublicProductControllerTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **listActive_returnsOnlyWhatServiceProvides**
        * *Mô tả nghiệp vụ*: Lấy danh sách hoạt động returns only what service provides.


---

## 📂 2. PACKAGE: com.mmo.entity (Kiểm thử thực thể thực tế)

## 📄 Lớp kiểm thử: UserEntityTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **testUserGettersAndSetters**
        * *Mô tả nghiệp vụ*: Test user getters and setters.
    * 🔍 **testDefaultValues**
        * *Mô tả nghiệp vụ*: Test default values.
    * 🔍 **testRoleEnum**
        * *Mô tả nghiệp vụ*: Test role enum.
    * 🔍 **testStatusEnum**
        * *Mô tả nghiệp vụ*: Test trạng thái enum.


---

## 📂 3. PACKAGE: com.mmo.security (Kiểm thử kiểm duyệt phân quyền)

## 📄 Lớp kiểm thử: SellerAccessValidatorTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **admin_bypassesAssignmentCheck**
        * *Mô tả nghiệp vụ*: Quản trị viên bypasses assignment check.
    * 🔍 **staff_unassigned_forbidden**
        * *Mô tả nghiệp vụ*: Quản lý unassigned lỗi không có quyền truy cập (403 Forbidden).


## 📄 Lớp kiểm thử: ComplaintStatusValidatorTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **validTransition_openToInvestigating**
        * *Mô tả nghiệp vụ*: Hợp lệ transition open to investigating.
    * 🔍 **validTransition_investigatingToResolved**
        * *Mô tả nghiệp vụ*: Hợp lệ transition investigating to resolved.
    * 🔍 **validTransition_resolvedToClosed**
        * *Mô tả nghiệp vụ*: Hợp lệ transition resolved to closed.
    * 🔍 **invalidTransition_openToResolved_throws**
        * *Mô tả nghiệp vụ*: Không hợp lệ transition open to resolved ném ra lỗi.
    * 🔍 **invalidTransition_openToClosed_throws**
        * *Mô tả nghiệp vụ*: Không hợp lệ transition open to closed ném ra lỗi.
    * 🔍 **invalidTransition_closedToAnything_throws**
        * *Mô tả nghiệp vụ*: Không hợp lệ transition closed to anything ném ra lỗi.
    * 🔍 **invalidTransition_sameStatus_throws**
        * *Mô tả nghiệp vụ*: Không hợp lệ transition same trạng thái ném ra lỗi.
    * 🔍 **invalidTransition_nullCurrent_throws**
        * *Mô tả nghiệp vụ*: Không hợp lệ transition null current ném ra lỗi.
    * 🔍 **invalidTransition_nullNext_throws**
        * *Mô tả nghiệp vụ*: Không hợp lệ transition null next ném ra lỗi.
    * 🔍 **resolutionNotes_resolvedWithSufficientNotes_ok**
        * *Mô tả nghiệp vụ*: Resolution notes resolved with sufficient notes ok.
    * 🔍 **resolutionNotes_resolvedWithShortNotes_throws**
        * *Mô tả nghiệp vụ*: Resolution notes resolved with short notes ném ra lỗi.
    * 🔍 **resolutionNotes_resolvedWithNullNotesButExistingSufficient_ok**
        * *Mô tả nghiệp vụ*: Resolution notes resolved with null notes but existing sufficient ok.
    * 🔍 **resolutionNotes_resolvedWithNullNotesAndNoExisting_throws**
        * *Mô tả nghiệp vụ*: Resolution notes resolved with null notes and no existing ném ra lỗi.
    * 🔍 **resolutionNotes_notResolved_noValidation**
        * *Mô tả nghiệp vụ*: Resolution notes không resolved no validation.


---

## 📂 4. PACKAGE: com.mmo.service (Kiểm thử Logic nghiệp vụ & Escrow)

## 📄 Lớp kiểm thử: ComplaintServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **coApprove_withoutstaffEscalate_throws**
        * *Mô tả nghiệp vụ*: Co approve khi không có quản lý escalate ném ra lỗi.
    * 🔍 **coApprove_afterstaffEscalate_approvesAndSettles**
        * *Mô tả nghiệp vụ*: Co approve after quản lý escalate approves and settles.


## 📄 Lớp kiểm thử: ProductServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **create_rejectsJavascriptCtaUrl**
        * *Mô tả nghiệp vụ*: Tạo mới từ chối javascript cta đường dẫn URL.
    * 🔍 **create_rejectsInvalidColorTheme**
        * *Mô tả nghiệp vụ*: Tạo mới từ chối không hợp lệ color theme.
    * 🔍 **create_normalizesRelativeCtaAndTheme**
        * *Mô tả nghiệp vụ*: Tạo mới normalizes relative cta and theme.
    * 🔍 **listActive_usesSortOrderAscCreatedAtDesc**
        * *Mô tả nghiệp vụ*: Lấy danh sách hoạt động uses sort order asc created at desc.
    * 🔍 **create_acceptsHttpCtaUrl**
        * *Mô tả nghiệp vụ*: Tạo mới chấp nhận http cta đường dẫn URL.
    * 🔍 **create_acceptsHttpsCtaUrl**
        * *Mô tả nghiệp vụ*: Tạo mới chấp nhận https cta đường dẫn URL.


## 📄 Lớp kiểm thử: SellerServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **create_defaultsInactive**
        * *Mô tả nghiệp vụ*: Tạo mới mặc định không hoạt động.
    * 🔍 **update_activateWithoutstaff_conflicts**
        * *Mô tả nghiệp vụ*: Cập nhật kích hoạt khi không có quản lý conflicts.
    * 🔍 **assignstaff_rejectsNonActiveUser**
        * *Mô tả nghiệp vụ*: Assign quản lý từ chối non hoạt động user.
    * 🔍 **assignstaff_swapsActiveAssignment**
        * *Mô tả nghiệp vụ*: Assign quản lý swaps hoạt động assignment.


## 📄 Lớp kiểm thử: SystemConfigurationServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **getSettings_returnsDefaults_whenNoRows**
        * *Mô tả nghiệp vụ*: Get settings returns mặc định khi no rows.
    * 🔍 **getSettings_returnsStoredValues**
        * *Mô tả nghiệp vụ*: Get settings returns stored values.
    * 🔍 **getDepositPercentage_returnsFraction**
        * *Mô tả nghiệp vụ*: Get deposit percentage returns fraction.
    * 🔍 **getDepositPercentage_returnsDefault_whenMissing**
        * *Mô tả nghiệp vụ*: Get deposit percentage returns default khi missing.
    * 🔍 **updateSettings_depositBelow10_throws**
        * *Mô tả nghiệp vụ*: Cập nhật settings deposit below10 ném ra lỗi.
    * 🔍 **updateSettings_depositAbove50_throws**
        * *Mô tả nghiệp vụ*: Cập nhật settings deposit above50 ném ra lỗi.
    * 🔍 **updateSettings_deposit40_ok**
        * *Mô tả nghiệp vụ*: Cập nhật settings deposit40 ok.
    * 🔍 **updateSettings_invalidEmail_throws**
        * *Mô tả nghiệp vụ*: Cập nhật settings không hợp lệ hộp thư email ném ra lỗi.
    * 🔍 **updateSettings_validEmail_ok**
        * *Mô tả nghiệp vụ*: Cập nhật settings hợp lệ hộp thư email ok.
    * 🔍 **updateSettings_emptyEmail_allowed**
        * *Mô tả nghiệp vụ*: Cập nhật settings empty hộp thư email allowed.


## 📄 Lớp kiểm thử: AdminUserManagementServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **suspendCustomer_revokesRefreshTokens_andLogs**
        * *Mô tả nghiệp vụ*: Tạm đình chỉ khách hàng revokes refresh tokens and logs.
    * 🔍 **suspendCustomer_revokesRefreshTokens_andLogs**
        * *Mô tả nghiệp vụ*: Tạm đình chỉ khách hàng revokes refresh tokens and logs.
    * 🔍 **activateCustomer_logsWithoutRevoke**
        * *Mô tả nghiệp vụ*: Kích hoạt khách hàng logs khi không có revoke.
    * 🔍 **suspendInactiveCustomer_throwsConflict**
        * *Mô tả nghiệp vụ*: Tạm đình chỉ không hoạt động khách hàng ném ra lỗi xung đột dữ liệu.
    * 🔍 **updateUser_customerInactiveTarget_rejected**
        * *Mô tả nghiệp vụ*: Cập nhật user khách hàng không hoạt động target rejected.
    * 🔍 **updateUser_staffActiveInactive_unchanged**
        * *Mô tả nghiệp vụ*: Cập nhật user quản lý hoạt động không hoạt động unchanged.
    * 🔍 **updateCustomerStatus_nonCustomer_throws**
        * *Mô tả nghiệp vụ*: Cập nhật khách hàng trạng thái non khách hàng ném ra lỗi.
    * 🔍 **updateCustomerStatus_missingUser_throwsNotFound**
        * *Mô tả nghiệp vụ*: Cập nhật khách hàng trạng thái missing user ném ra lỗi không tìm thấy.


## 📄 Lớp kiểm thử: AuthenticationServiceCoverageTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **stubMail**
        * *Mô tả nghiệp vụ*: Stub mail.
    * 🔍 **register_emailExists_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Đăng ký hộp thư email exists ném ra lỗi business exception.
    * 🔍 **register_newEmail_savesInactiveWithOtp**
        * *Mô tả nghiệp vụ*: Đăng ký new hộp thư email lưu trữ không hoạt động with mã OTP.
    * 🔍 **register_smtpNotConfigured_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Đăng ký smtp không configured ném ra lỗi business exception.
    * 🔍 **verifyOtp_unknownEmail_throwsNotFound**
        * *Mô tả nghiệp vụ*: Xác minh mã OTP unknown hộp thư email ném ra lỗi không tìm thấy.
    * 🔍 **verifyOtp_wrongCode_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Xác minh mã OTP wrong code ném ra lỗi business exception.
    * 🔍 **verifyOtp_wrongCode_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Xác minh mã OTP wrong code ném ra lỗi business exception.
    * 🔍 **verifyOtp_expired_throwsOtpExpiredException**
        * *Mô tả nghiệp vụ*: Xác minh mã OTP hết hạn ném ra lỗi mã OTP hết hạn exception.
    * 🔍 **verifyOtp_valid_activatesAndClearsOtp**
        * *Mô tả nghiệp vụ*: Xác minh mã OTP hợp lệ activates and xóa sạch mã OTP.
    * 🔍 **verifyOtp_staffInactive_cannotSelfActivate**
        * *Mô tả nghiệp vụ*: Xác minh mã OTP nhân viên không hoạt động cannot self kích hoạt.
    * 🔍 **resendOtp_unknownEmail_throwsNotFound**
        * *Mô tả nghiệp vụ*: Resend mã OTP unknown hộp thư email ném ra lỗi không tìm thấy.
    * 🔍 **resendOtp_alreadyActive_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Resend mã OTP already hoạt động ném ra lỗi business exception.
    * 🔍 **resendOtp_alreadyActive_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Resend mã OTP already hoạt động ném ra lỗi business exception.
    * 🔍 **resendOtp_alreadyActive_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Resend mã OTP already hoạt động ném ra lỗi business exception.
    * 🔍 **resendOtp_inactive_issuesNewOtp**
        * *Mô tả nghiệp vụ*: Resend mã OTP không hoạt động issues new mã OTP.
    * 🔍 **login_unknownEmail_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Đăng nhập unknown hộp thư email ném ra lỗi business exception.
    * 🔍 **login_googleOnlyAccount_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Đăng nhập google only account ném ra lỗi business exception.
    * 🔍 **login_googleOnlyAccount_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Đăng nhập google only account ném ra lỗi business exception.
    * 🔍 **login_wrongPassword_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Đăng nhập wrong password ném ra lỗi business exception.
    * 🔍 **login_inactive_throwsAccountNotVerified**
        * *Mô tả nghiệp vụ*: Đăng nhập không hoạt động ném ra lỗi account không verified.
    * 🔍 **resolveGoogleSignIn_existingGoogleId_returnsTokens**
        * *Mô tả nghiệp vụ*: Resolve google sign in existing google id returns tokens.
    * 🔍 **resolveGoogleSignIn_emailSuspended_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Resolve google sign in hộp thư email suspended ném ra lỗi business exception.
    * 🔍 **resolveGoogleSignIn_activeLocal_setsAvatarWhenMissing**
        * *Mô tả nghiệp vụ*: Resolve google sign in hoạt động local sets avatar khi missing.
    * 🔍 **resolveGoogleSignIn_newUser_nullFullName_usesEmail**
        * *Mô tả nghiệp vụ*: Resolve google sign in new user null full name uses hộp thư email.
    * 🔍 **completeGoogleLink_invalidToken_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Complete google link không hợp lệ token ném ra lỗi business exception.
    * 🔍 **completeGoogleLink_invalidToken_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Complete google link không hợp lệ token ném ra lỗi business exception.
    * 🔍 **completeGoogleLink_invalidToken_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Complete google link không hợp lệ token ném ra lỗi business exception.
    * 🔍 **completeGoogleLink_unknownEmail_throwsNotFound**
        * *Mô tả nghiệp vụ*: Complete google link unknown hộp thư email ném ra lỗi không tìm thấy.
    * 🔍 **completeGoogleLink_suspended_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Complete google link suspended ném ra lỗi business exception.
    * 🔍 **completeGoogleLink_setsAvatarAndBlankFullName**
        * *Mô tả nghiệp vụ*: Complete google link sets avatar and blank full name.
    * 🔍 **forgotPassword_localAccount_sendsOtp**
        * *Mô tả nghiệp vụ*: Forgot password local account sends mã OTP.
    * 🔍 **resetPassword_unknownEmail_genericOtpError_noDisclosure**
        * *Mô tả nghiệp vụ*: Reset password unknown hộp thư email generic mã OTP error no disclosure.
    * 🔍 **resetPassword_valid_updatesHashAndRevokesTokens**
        * *Mô tả nghiệp vụ*: Reset password hợp lệ updates hash and revokes tokens.
    * 🔍 **refreshToken_unknown_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Refresh token unknown ném ra lỗi business exception.
    * 🔍 **refreshToken_expired_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Refresh token hết hạn ném ra lỗi business exception.
    * 🔍 **refreshToken_valid_rotatesAndReturnsNewTokens**
        * *Mô tả nghiệp vụ*: Refresh token hợp lệ rotates and returns new tokens.
    * 🔍 **logout_knownToken_revokes**
        * *Mô tả nghiệp vụ*: Logout known token revokes.
    * 🔍 **logout_unknownToken_noException**
        * *Mô tả nghiệp vụ*: Logout unknown token no exception.
    * 🔍 **issueOtp_mailSendFails_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Issue mã OTP mail send fails ném ra lỗi business exception.
    * 🔍 **register_blankMailFrom_fallsBackToJavaMailSenderImplUsername**
        * *Mô tả nghiệp vụ*: Đăng ký blank mail from falls back to java mail sender impl username.
    * 🔍 **loginWithGoogle_invalidToken_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Đăng nhập with google không hợp lệ token ném ra lỗi business exception.


## 📄 Lớp kiểm thử: AuthenticationServiceP1aTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **refreshToken_suspended_denied**
        * *Mô tả nghiệp vụ*: Refresh token suspended denied.
    * 🔍 **refreshToken_suspended_denied**
        * *Mô tả nghiệp vụ*: Refresh token suspended denied.
    * 🔍 **refreshToken_inactive_denied**
        * *Mô tả nghiệp vụ*: Refresh token không hoạt động denied.
    * 🔍 **forgotPassword_unknownEmail_noException_noSideEffects**
        * *Mô tả nghiệp vụ*: Forgot password unknown hộp thư email no exception no side effects.
    * 🔍 **forgotPassword_googleOnly_noException_noOtp**
        * *Mô tả nghiệp vụ*: Forgot password google only no exception no mã OTP.
    * 🔍 **forgotPassword_googleOnly_noException_noOtp**
        * *Mô tả nghiệp vụ*: Forgot password google only no exception no mã OTP.
    * 🔍 **resetPassword_unknownEmail_genericOtpError**
        * *Mô tả nghiệp vụ*: Reset password unknown hộp thư email generic mã OTP error.
    * 🔍 **verifyOtp_staffInactive_blocked**
        * *Mô tả nghiệp vụ*: Xác minh mã OTP nhân viên không hoạt động blocked.
    * 🔍 **verifyOtp_customerInactive_activates**
        * *Mô tả nghiệp vụ*: Xác minh mã OTP khách hàng không hoạt động activates.


## 📄 Lớp kiểm thử: AuthenticationServicePhaseCTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **stubMail**
        * *Mô tả nghiệp vụ*: Stub mail.
    * 🔍 **resolveGoogleSignIn_activeLocal_autoLinks**
        * *Mô tả nghiệp vụ*: Resolve google sign in hoạt động local auto links.
    * 🔍 **resolveGoogleSignIn_inactiveLocal_requiresOtpLink**
        * *Mô tả nghiệp vụ*: Resolve google sign in không hoạt động local yêu cầu mã OTP link.
    * 🔍 **completeGoogleLink_activatesAndLinks**
        * *Mô tả nghiệp vụ*: Complete google link activates and links.
    * 🔍 **resendOtp_fourthSendInHour_rejected**
        * *Mô tả nghiệp vụ*: Resend mã OTP fourth send in hour rejected.
    * 🔍 **resolveGoogleSignIn_newUser_createsActive**
        * *Mô tả nghiệp vụ*: Resolve google sign in new user creates hoạt động.


## 📄 Lớp kiểm thử: AuthenticationServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **login_suspendedUser_denied**
        * *Mô tả nghiệp vụ*: Đăng nhập suspended user denied.
    * 🔍 **login_suspendedUser_denied**
        * *Mô tả nghiệp vụ*: Đăng nhập suspended user denied.
    * 🔍 **login_activeUser_returnsTokens**
        * *Mô tả nghiệp vụ*: Đăng nhập hoạt động user returns tokens.
    * 🔍 **login_activeUser_returnsTokens**
        * *Mô tả nghiệp vụ*: Đăng nhập hoạt động user returns tokens.
    * 🔍 **refreshToken_revoked_denied**
        * *Mô tả nghiệp vụ*: Refresh token revoked denied.


## 📄 Lớp kiểm thử: EscrowCalculationServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **resolveTier_ge7_days_100**
        * *Mô tả nghiệp vụ*: Resolve tier ge7 days 100.
    * 🔍 **resolveTier_3to6_days_50**
        * *Mô tả nghiệp vụ*: Resolve tier 3to6 days 50.
    * 🔍 **resolveTier_lt3_days_0**
        * *Mô tả nghiệp vụ*: Resolve tier lt3 days 0.
    * 🔍 **resolveTier_pendingDeposit_0**
        * *Mô tả nghiệp vụ*: Resolve tier pending deposit 0.
    * 🔍 **preview_ge7_uses100PercentOfDeposit**
        * *Mô tả nghiệp vụ*: Preview ge7 uses100percent of deposit.
    * 🔍 **preview_3to6_uses50PercentOfDeposit**
        * *Mô tả nghiệp vụ*: Preview 3to6 uses50percent of deposit.
    * 🔍 **cancel_3to6_days_marksDepositPartiallyRefundedAt50Percent**
        * *Mô tả nghiệp vụ*: Cancel 3to6 days marks deposit partially refunded at50percent.
    * 🔍 **cancel_ge7_days_marksDepositFullyRefunded**
        * *Mô tả nghiệp vụ*: Cancel ge7 days marks deposit fully refunded.
    * 🔍 **cancel_checkedIn_rejected**
        * *Mô tả nghiệp vụ*: Cancel checked in rejected.
    * 🔍 **cancel_pendingDeposit_noRefundMark**
        * *Mô tả nghiệp vụ*: Cancel pending deposit no refund mark.
    * 🔍 **cancel_lt3_days_depositNotMarkedRefunded**
        * *Mô tả nghiệp vụ*: Cancel lt3 days deposit không marked refunded.
    * 🔍 **nonCustomer_preview_forbidden**
        * *Mô tả nghiệp vụ*: Non khách hàng preview lỗi không có quyền truy cập (403 Forbidden).


## 📄 Lớp kiểm thử: OrderModifyServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **modify_nonConfirmed_rejected**
        * *Mô tả nghiệp vụ*: Modify non confirmed rejected.
    * 🔍 **modify_statusBlockOverlap_conflict**
        * *Mô tả nghiệp vụ*: Modify trạng thái block overlap xung đột dữ liệu.
    * 🔍 **modify_dates_recalculatesPriceDelta**
        * *Mô tả nghiệp vụ*: Modify dates tính toán lại price delta.


## 📄 Lớp kiểm thử: OrderExpiredServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **markNoShow_confirmedPast24h_becomesNoShow_depositUnchanged**
        * *Mô tả nghiệp vụ*: Mark no show confirmed past24h becomes no show deposit unchanged.
    * 🔍 **markNoShow_notYetPast24h_skipped**
        * *Mô tả nghiệp vụ*: Mark no show không yet past24h skipped.
    * 🔍 **markNoShow_checkedIn_notInCandidateQuery_skipped**
        * *Mô tả nghiệp vụ*: Mark no show checked in không in candidate query skipped.
    * 🔍 **markNoShow_idempotent_alreadyConfirmedOnly**
        * *Mô tả nghiệp vụ*: Mark no show idempotent already confirmed only.


## 📄 Lớp kiểm thử: OrderDeliveryServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **checkIn_remainingUnpaidWithoutDeskCollection_denied**
        * *Mô tả nghiệp vụ*: Check in remaining unpaid khi không có desk collection denied.


## 📄 Lớp kiểm thử: OrderServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **createorder_success_setsHoldExpiresAtAndPendingDeposit**
        * *Mô tả nghiệp vụ*: Tạo mới đơn hàng/giao dịch thực hiện thành công sets hold expires at and pending deposit.
    * 🔍 **createorder_overlap_throws409**
        * *Mô tả nghiệp vụ*: Tạo mới đơn hàng/giao dịch overlap throws409.
    * 🔍 **createorder_checkoutNotAfterCheckin_rejected**
        * *Mô tả nghiệp vụ*: Tạo mới đơn hàng/giao dịch checkout không after checkin rejected.
    * 🔍 **createorder_checkInInPast_rejected**
        * *Mô tả nghiệp vụ*: Tạo mới đơn hàng/giao dịch check in in past rejected.
    * 🔍 **createorder_nonCustomer_forbidden**
        * *Mô tả nghiệp vụ*: Tạo mới đơn hàng/giao dịch non khách hàng lỗi không có quyền truy cập (403 Forbidden).
    * 🔍 **createorder_productNotFound_404**
        * *Mô tả nghiệp vụ*: Tạo mới đơn hàng/giao dịch mặt hàng/sản phẩm/cửa hàng không tìm thấy 404.
    * 🔍 **createorder_blockedproductStatus_conflict**
        * *Mô tả nghiệp vụ*: Tạo mới đơn hàng/giao dịch blocked mặt hàng/sản phẩm/cửa hàng trạng thái xung đột dữ liệu.
    * 🔍 **createorder_statusBlockOverlap_conflict**
        * *Mô tả nghiệp vụ*: Tạo mới đơn hàng/giao dịch trạng thái block overlap xung đột dữ liệu.
    * 🔍 **createorder_sameDayTurnover_allowedWhenNoOverlap**
        * *Mô tả nghiệp vụ*: Tạo mới đơn hàng/giao dịch same day turnover allowed khi no overlap.
    * 🔍 **createorder_guestCountExceedsCapacity_rejected**
        * *Mô tả nghiệp vụ*: Tạo mới đơn hàng/giao dịch guest count exceeds capacity rejected.
    * 🔍 **cancelorderForstaff_confirmed_refunds100PercentDeposit**
        * *Mô tả nghiệp vụ*: Cancel đơn hàng/giao dịch for quản lý confirmed refunds100percent deposit.
    * 🔍 **cancelorderForstaff_blankReason_rejected**
        * *Mô tả nghiệp vụ*: Cancel đơn hàng/giao dịch for quản lý blank reason rejected.
    * 🔍 **getstaffCancellationPreview_returns100Percent**
        * *Mô tả nghiệp vụ*: Get quản lý cancellation preview returns100percent.
    * 🔍 **cancelorder_pendingDeposit_noRefund_andVoidsPendingPayment**
        * *Mô tả nghiệp vụ*: Cancel đơn hàng/giao dịch pending deposit no refund and voids pending thanh toán.
    * 🔍 **cancelorder_staffViaCustomerApi_forbidden**
        * *Mô tả nghiệp vụ*: Cancel đơn hàng/giao dịch quản lý via khách hàng api lỗi không có quyền truy cập (403 Forbidden).
    * 🔍 **cancelorder_confirmedGe7_marksDepositRefunded**
        * *Mô tả nghiệp vụ*: Cancel đơn hàng/giao dịch confirmed ge7 marks deposit refunded.
    * 🔍 **markAsCheckedIn_remainingUnpaidWithoutCollection_conflict**
        * *Mô tả nghiệp vụ*: Mark as checked in remaining unpaid khi không có collection xung đột dữ liệu.
    * 🔍 **markAsCheckedIn_remainingCollectedCash_success**
        * *Mô tả nghiệp vụ*: Mark as checked in remaining collected cash thực hiện thành công.
    * 🔍 **markAsCheckedIn_keyNotHanded_rejected**
        * *Mô tả nghiệp vụ*: Mark as checked in key không handed rejected.
    * 🔍 **markAsCheckedIn_beforeCheckInDate_rejected**
        * *Mô tả nghiệp vụ*: Mark as checked in before check in date rejected.
    * 🔍 **markAsCheckedIn_notConfirmed_rejected**
        * *Mô tả nghiệp vụ*: Mark as checked in không confirmed rejected.
    * 🔍 **markAsCheckedOut_pendingInspectionWithoutPassed_conflict**
        * *Mô tả nghiệp vụ*: Mark as checked out pending inspection khi không có passed xung đột dữ liệu.
    * 🔍 **markAsCheckedOut_pendingInspectionMissing_conflict**
        * *Mô tả nghiệp vụ*: Mark as checked out pending inspection missing xung đột dữ liệu.
    * 🔍 **markAsCheckedOut_checkedIn_movesToPendingInspection**
        * *Mô tả nghiệp vụ*: Mark as checked out checked in moves to pending inspection.
    * 🔍 **markAsCheckedOut_inspectionPassed_completesCheckout**
        * *Mô tả nghiệp vụ*: Mark as checked out inspection passed completes checkout.
    * 🔍 **markAsCheckedOut_inspectionPassed_completesActiveContract**
        * *Mô tả nghiệp vụ*: Mark as checked out inspection passed completes hoạt động contract.
    * 🔍 **markAsCheckedOut_failedWithcomplaint_unpaid_conflict**
        * *Mô tả nghiệp vụ*: Mark as checked out thất bại with khiếu nại unpaid xung đột dữ liệu.
    * 🔍 **markAsCheckedOut_failedWithcomplaint_paid_completesCheckout**
        * *Mô tả nghiệp vụ*: Mark as checked out thất bại with khiếu nại paid completes checkout.
    * 🔍 **markAsCheckedOut_pendingcomplaintPayment_conflict**
        * *Mô tả nghiệp vụ*: Mark as checked out pending khiếu nại thanh toán xung đột dữ liệu.
    * 🔍 **markAsCheckedOut_keyNotReturned_rejected**
        * *Mô tả nghiệp vụ*: Mark as checked out key không returned rejected.
    * 🔍 **modifyorder_overlap_conflict**
        * *Mô tả nghiệp vụ*: Modify đơn hàng/giao dịch overlap xung đột dữ liệu.
    * 🔍 **modifyorder_confirmed_sameTotal_zeroDeltaMessage**
        * *Mô tả nghiệp vụ*: Modify đơn hàng/giao dịch confirmed same total zero delta message.
    * 🔍 **modifyorder_blockedproduct_conflict**
        * *Mô tả nghiệp vụ*: Modify đơn hàng/giao dịch blocked mặt hàng/sản phẩm/cửa hàng xung đột dữ liệu.
    * 🔍 **modifyorder_invalidDateRange_rejected**
        * *Mô tả nghiệp vụ*: Modify đơn hàng/giao dịch không hợp lệ date range rejected.
    * 🔍 **cancelExpiredDepositHolds_unpaid_cancels**
        * *Mô tả nghiệp vụ*: Cancel hết hạn deposit holds unpaid cancels.
    * 🔍 **cancelExpiredDepositHolds_depositAlreadyPaid_skipped**
        * *Mô tả nghiệp vụ*: Cancel hết hạn deposit holds deposit already paid skipped.
    * 🔍 **getMyorders_customer_withStatusFilter**
        * *Mô tả nghiệp vụ*: Get my đơn hàngs khách hàng with trạng thái filter.
    * 🔍 **getMyorders_nonCustomer_forbidden**
        * *Mô tả nghiệp vụ*: Get my đơn hàngs non khách hàng lỗi không có quyền truy cập (403 Forbidden).
    * 🔍 **getAllorders_filtersByStatus**
        * *Mô tả nghiệp vụ*: Get all đơn hàngs filters by trạng thái.
    * 🔍 **getordersForstaffScoped_emptyAssignments_returnsEmpty**
        * *Mô tả nghiệp vụ*: Get đơn hàngs for quản lý scoped empty assignments returns empty.
    * 🔍 **getordersForstaffScoped_withsellerId**
        * *Mô tả nghiệp vụ*: Get đơn hàngs for quản lý scoped with sản phẩm/cửa hàng/sản phẩm/cửa hàng id.
    * 🔍 **getorderDetail_owner_success**
        * *Mô tả nghiệp vụ*: Get đơn hàng/giao dịch detail owner thực hiện thành công.
    * 🔍 **getorderDetail_otherCustomer_forbidden**
        * *Mô tả nghiệp vụ*: Get đơn hàng/giao dịch detail other khách hàng lỗi không có quyền truy cập (403 Forbidden).
    * 🔍 **getorderDetailForstaff_scoped**
        * *Mô tả nghiệp vụ*: Get đơn hàng/giao dịch detail for quản lý scoped.
    * 🔍 **buildPageable_blankDefaultsToCreatedAtDesc**
        * *Mô tả nghiệp vụ*: Build pageable blank mặc định to created at desc.
    * 🔍 **buildPageable_disallowedFieldFallsBack**
        * *Mô tả nghiệp vụ*: Build pageable disallowed field falls back.
    * 🔍 **buildPageable_allowedAsc**
        * *Mô tả nghiệp vụ*: Build pageable allowed asc.
    * 🔍 **markAsCheckedIn_legacy_blocked**
        * *Mô tả nghiệp vụ*: Mark as checked in legacy blocked.
    * 🔍 **markAsCheckedOut_legacy_blocked**
        * *Mô tả nghiệp vụ*: Mark as checked out legacy blocked.
    * 🔍 **cancelorder_alreadyCancelled_rejected**
        * *Mô tả nghiệp vụ*: Cancel đơn hàng/giao dịch already cancelled rejected.
    * 🔍 **cancelorder_wrongOwner_forbidden**
        * *Mô tả nghiệp vụ*: Cancel đơn hàng/giao dịch wrong owner lỗi không có quyền truy cập (403 Forbidden).
    * 🔍 **getCancellationPreview_pendingDeposit_zeroRefund**
        * *Mô tả nghiệp vụ*: Get cancellation preview pending deposit zero refund.
    * 🔍 **getCancellationPreview_midTier_50**
        * *Mô tả nghiệp vụ*: Get cancellation preview mid tier 50.


## 📄 Lớp kiểm thử: EscrowContractServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **processContractGenerateFromOutbox_persistsPdfAndEnqueuesEmail**
        * *Mô tả nghiệp vụ*: Process contract generate from outbox persists pdf and enqueues hộp thư email.
    * 🔍 **resendContractEmail_staff_enqueuesOutboxWithoutSyncSend**
        * *Mô tả nghiệp vụ*: Resend contract hộp thư email quản lý enqueues outbox khi không có sync send.
    * 🔍 **processContractResendFromOutbox_logsResentAndUpdatesSentAt**
        * *Mô tả nghiệp vụ*: Process contract resend from outbox logs resent and updates sent at.
    * 🔍 **downloadContractPdf_missingFile_doesNotRegenerate**
        * *Mô tả nghiệp vụ*: Download contract pdf missing file does không regenerate.


## 📄 Lớp kiểm thử: CustomerComplaintServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **dispute_within24h_setsDisputedAndNotifiesAdmin**
        * *Mô tả nghiệp vụ*: Dispute within24h sets disputed and notifies quản trị viên.
    * 🔍 **dispute_after24h_rejected**
        * *Mô tả nghiệp vụ*: Dispute after24h rejected.
    * 🔍 **dispute_nonOwner_forbidden**
        * *Mô tả nghiệp vụ*: Dispute non owner lỗi không có quyền truy cập (403 Forbidden).
    * 🔍 **list_returnsCustomerReports**
        * *Mô tả nghiệp vụ*: Lấy danh sách returns khách hàng reports.


## 📄 Lớp kiểm thử: CustomerDashboardServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **getDashboard_nonCustomer_forbidden**
        * *Mô tả nghiệp vụ*: Get dashboard non khách hàng lỗi không có quyền truy cập (403 Forbidden).
    * 🔍 **getDashboard_nonCustomer_forbidden**
        * *Mô tả nghiệp vụ*: Get dashboard non khách hàng lỗi không có quyền truy cập (403 Forbidden).
    * 🔍 **getDashboard_activeExcludesPendingDeposit_andLimitsLists**
        * *Mô tả nghiệp vụ*: Get dashboard hoạt động loại trừ pending deposit and giới hạn lists.


## 📄 Lớp kiểm thử: ComplaintSettlementServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **applyApprovedFee_createssepayPendingcomplaintPayment**
        * *Mô tả nghiệp vụ*: Apply approved fee creates cổng sepay pending khiếu nại thanh toán.
    * 🔍 **markcomplaintReportPaidFororder_unlocksCheckoutPath**
        * *Mô tả nghiệp vụ*: Mark khiếu nại report paid for đơn hàng/giao dịch unlocks checkout path.
    * 🔍 **applyApprovedFee_zeroAmount_throws**
        * *Mô tả nghiệp vụ*: Apply approved fee zero amount ném ra lỗi.
    * 🔍 **applyApprovedFee_missingorder_throws**
        * *Mô tả nghiệp vụ*: Apply approved fee missing đơn hàng/giao dịch ném ra lỗi.
    * 🔍 **applyApprovedFee_existingPendingcomplaint_skipsNewPayment**
        * *Mô tả nghiệp vụ*: Apply approved fee existing pending khiếu nại skips new thanh toán.
    * 🔍 **markcomplaintReportPaidFororder_orderMissing_throws**
        * *Mô tả nghiệp vụ*: Mark khiếu nại report paid for đơn hàng/giao dịch đơn hàng/giao dịch missing ném ra lỗi.
    * 🔍 **markcomplaintReportPaidFororder_notPendingcomplaint_skipsStatusChange**
        * *Mô tả nghiệp vụ*: Mark khiếu nại report paid for đơn hàng/giao dịch không pending khiếu nại skips trạng thái change.


## 📄 Lớp kiểm thử: ComplaintManagerServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **approve_escalated_keepsPendingAndDoesNotSettle**
        * *Mô tả nghiệp vụ*: Approve escalated keeps pending and does không settle.
    * 🔍 **approve_belowThreshold_setsApprovedAndSettles**
        * *Mô tả nghiệp vụ*: Approve below threshold sets approved and settles.


## 📄 Lớp kiểm thử: StaffComplaintServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **create_notifiesCustomerstaffAndstaffs**
        * *Mô tả nghiệp vụ*: Tạo mới notifies khách hàng employee and managers.


## 📄 Lớp kiểm thử: StaffDashboardServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **getDashboard_nosellerAssignment_returnsEmptyFlags**
        * *Mô tả nghiệp vụ*: Get dashboard no sản phẩm/cửa hàng/sản phẩm/cửa hàng assignment returns empty flags.
    * 🔍 **getDashboard_withAssignments_mapsCountsAndLimits**
        * *Mô tả nghiệp vụ*: Get dashboard with assignments maps counts and giới hạn.
    * 🔍 **getDashboard_openStatusesMatchContract**
        * *Mô tả nghiệp vụ*: Get dashboard open statuses match contract.


## 📄 Lớp kiểm thử: PreOrderServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **start_movesPendingToInProgress_andSetsproductCleaning**
        * *Mô tả nghiệp vụ*: Start moves pending to in progress and sets mặt hàng/sản phẩm/cửa hàng cleaning.
    * 🔍 **finish_movesInProgressToCompleted_andSetsproductAvailable**
        * *Mô tả nghiệp vụ*: Finish moves in progress to completed and sets mặt hàng/sản phẩm/cửa hàng available.
    * 🔍 **start_rejectsNonPending**
        * *Mô tả nghiệp vụ*: Start từ chối non pending.
    * 🔍 **finish_rejectsWithoutStart**
        * *Mô tả nghiệp vụ*: Finish từ chối khi không có start.
    * 🔍 **start_rejectsUnassignedTask**
        * *Mô tả nghiệp vụ*: Start từ chối unassigned task.
    * 🔍 **finish_rejectsWrongAssignee**
        * *Mô tả nghiệp vụ*: Finish từ chối wrong assignee.
    * 🔍 **finish_rejectsCompleted**
        * *Mô tả nghiệp vụ*: Finish từ chối completed.
    * 🔍 **start_rejectsCancelled**
        * *Mô tả nghiệp vụ*: Start từ chối cancelled.


## 📄 Lớp kiểm thử: SellerVerificationServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **list_filtersByStatusWhenProvided**
        * *Mô tả nghiệp vụ*: Lấy danh sách filters by trạng thái khi provided.
    * 🔍 **pass_setsPassedAndInspectedBy**
        * *Mô tả nghiệp vụ*: Pass sets passed and inspected by.
    * 🔍 **fail_requiresNote**
        * *Mô tả nghiệp vụ*: Fail yêu cầu note.
    * 🔍 **fail_blocksWhenClaimedByAnotherstaff**
        * *Mô tả nghiệp vụ*: Fail blocks khi claimed by another employee.
    * 🔍 **pass_blocksWhenPendingButAssignedToAnotherstaff**
        * *Mô tả nghiệp vụ*: Pass blocks khi pending but assigned to another employee.
    * 🔍 **fail_setsFailedWithcomplaint**
        * *Mô tả nghiệp vụ*: Fail sets thất bại with khiếu nại.


## 📄 Lớp kiểm thử: StaffPermissionServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **assignstaff_rejectsWhenAlreadyActiveElsewhere**
        * *Mô tả nghiệp vụ*: Assign employee từ chối khi already hoạt động elsewhere.
    * 🔍 **assignstaff_rejectsSuspendedstaff**
        * *Mô tả nghiệp vụ*: Assign employee từ chối suspended employee.
    * 🔍 **listByseller_deniesstaffWithoutScope**
        * *Mô tả nghiệp vụ*: Lấy danh sách by sản phẩm/cửa hàng/sản phẩm/cửa hàng denies quản lý khi không có scope.
    * 🔍 **createstaff_sendsInviteAndLogs**
        * *Mô tả nghiệp vụ*: Tạo mới employee sends invite and logs.
    * 🔍 **reassignstaff_inactivatesOldAndCreatesNew**
        * *Mô tả nghiệp vụ*: Reassign employee inactivates old and creates new.
    * 🔍 **reassignstaff_inactivatesOldAndCreatesNew**
        * *Mô tả nghiệp vụ*: Reassign employee inactivates old and creates new.
    * 🔍 **reassignstaff_inactivatesOldAndCreatesNew**
        * *Mô tả nghiệp vụ*: Reassign employee inactivates old and creates new.
    * 🔍 **reassignstaff_rejectsNonAdmin**
        * *Mô tả nghiệp vụ*: Reassign employee từ chối non quản trị viên.
    * 🔍 **updatestaffStatus_logsChange**
        * *Mô tả nghiệp vụ*: Cập nhật employee trạng thái logs change.


## 📄 Lớp kiểm thử: CategoryServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **createForstaff_duplicateFloorNumber_throwsConflict**
        * *Mô tả nghiệp vụ*: Tạo mới for quản lý trùng lặp floor number ném ra lỗi xung đột dữ liệu.
    * 🔍 **createForstaff_valid_savesFloor**
        * *Mô tả nghiệp vụ*: Tạo mới for quản lý hợp lệ lưu trữ floor.
    * 🔍 **deleteForstaff_floorWithproducts_throwsConflict**
        * *Mô tả nghiệp vụ*: Xóa for quản lý floor with sản phẩm/cửa hàngs ném ra lỗi xung đột dữ liệu.
    * 🔍 **deleteForstaff_emptyFloor_deletes**
        * *Mô tả nghiệp vụ*: Xóa for quản lý empty floor deletes.
    * 🔍 **updateForstaff_wrongsellerPath_throwsNotFound**
        * *Mô tả nghiệp vụ*: Cập nhật for quản lý wrong sản phẩm/cửa hàng/sản phẩm/cửa hàng path ném ra lỗi không tìm thấy.
    * 🔍 **getStructureForstaff_unassigned_propagatesForbidden**
        * *Mô tả nghiệp vụ*: Get structure for quản lý unassigned propagates lỗi không có quyền truy cập (403 Forbidden).
    * 🔍 **getStructure_returnsFloorsSorted**
        * *Mô tả nghiệp vụ*: Get structure returns floors sorted.
    * 🔍 **updateForstaff_duplicateNumber_throwsConflict**
        * *Mô tả nghiệp vụ*: Cập nhật for quản lý trùng lặp number ném ra lỗi xung đột dữ liệu.


## 📄 Lớp kiểm thử: PreOrderTaskServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **onorderCheckedOut_createsPendingTask**
        * *Mô tả nghiệp vụ*: On đơn hàng/giao dịch checked out creates pending task.
    * 🔍 **onorderCheckedOut_idempotent_skipsWhenTaskExists**
        * *Mô tả nghiệp vụ*: On đơn hàng/giao dịch checked out idempotent skips khi task exists.
    * 🔍 **assignTaskForstaff_rejectsCrosssellerstaff**
        * *Mô tả nghiệp vụ*: Assign task for quản lý từ chối cross sản phẩm/cửa hàng/sản phẩm/cửa hàng employee.
    * 🔍 **cancelTaskForstaff_fromInProgress_setsPendingCleaning**
        * *Mô tả nghiệp vụ*: Cancel task for quản lý from in progress sets pending cleaning.
    * 🔍 **cancelTaskForstaff_rejectsCompleted**
        * *Mô tả nghiệp vụ*: Cancel task for quản lý từ chối completed.
    * 🔍 **createTaskForstaff_rejectsWhenScopeDenied**
        * *Mô tả nghiệp vụ*: Tạo mới task for quản lý từ chối khi scope denied.
    * 🔍 **listTasksForAdmin_doesNotRequirestaffScope**
        * *Mô tả nghiệp vụ*: Lấy danh sách tasks for quản trị viên does không require quản lý scope.


## 📄 Lớp kiểm thử: TransactionAdjustmentServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **adjust_whenDepositPaid_updatesRemainingOnly**
        * *Mô tả nghiệp vụ*: Adjust khi deposit paid updates remaining only.


## 📄 Lớp kiểm thử: TransactionServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **issuePair_createsDepositAndRemaining_idempotent**
        * *Mô tả nghiệp vụ*: Issue pair creates deposit and remaining idempotent.
    * 🔍 **issuePair_skipsWhenAlreadyExists**
        * *Mô tả nghiệp vụ*: Issue pair skips khi already exists.


## 📄 Lớp kiểm thử: TransactionStatusSyncServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **markPendingPayment_fromUnpaid**
        * *Mô tả nghiệp vụ*: Mark pending thanh toán from unpaid.
    * 🔍 **markPaid_setsPaidAtAndLogs**
        * *Mô tả nghiệp vụ*: Mark paid sets paid at and logs.
    * 🔍 **syncFromPayment_failedResetsToUnpaid**
        * *Mô tả nghiệp vụ*: Sync from thanh toán thất bại resets to unpaid.


## 📄 Lớp kiểm thử: SupportTicketServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **staffClose_requiresNoteMin10**
        * *Mô tả nghiệp vụ*: Quản lý close yêu cầu note min10.
    * 🔍 **staffClose_fromResolved_ok**
        * *Mô tả nghiệp vụ*: Quản lý close from resolved ok.
    * 🔍 **staffClose_whenAlreadyClosed_blocked**
        * *Mô tả nghiệp vụ*: Quản lý close khi already closed blocked.
    * 🔍 **staffResolve_storesWorkNote_notResolutionNote**
        * *Mô tả nghiệp vụ*: Employee resolve stores work note không resolution note.
    * 🔍 **staffCannotSkipToResolvedFromAssigned**
        * *Mô tả nghiệp vụ*: Employee cannot skip to resolved from assigned.


## 📄 Lớp kiểm thử: NotificationServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **dispatch_createsNotification_andPushesWs**
        * *Mô tả nghiệp vụ*: Dispatch creates notification and pushes ws.
    * 🔍 **dispatch_skipsDuplicateDedupeKey**
        * *Mô tả nghiệp vụ*: Dispatch skips trùng lặp loại trùng key.
    * 🔍 **dispatch_skipsInactiveUser**
        * *Mô tả nghiệp vụ*: Dispatch skips không hoạt động user.


## 📄 Lớp kiểm thử: AuditLogServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **enqueueContractGenerate_persistsPendingEvent**
        * *Mô tả nghiệp vụ*: Enqueue contract generate persists pending event.
    * 🔍 **enqueueContractResend_persistsPendingEvent**
        * *Mô tả nghiệp vụ*: Enqueue contract resend persists pending event.


## 📄 Lớp kiểm thử: TopupServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **createsepayPaymentUrl_deposit_createsPendingAndReturnsUrl**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL deposit creates pending and returns đường dẫn URL.
    * 🔍 **createsepayPaymentUrl_remaining_whenConfirmed_returnsUrl**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL remaining khi confirmed returns đường dẫn URL.
    * 🔍 **createsepayPaymentUrl_remaining_whenCheckedIn_blocked**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL remaining khi checked in blocked.
    * 🔍 **createsepayPaymentUrl_complaintFee_returnsUrl**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL khiếu nại fee returns đường dẫn URL.
    * 🔍 **createsepayPaymentUrl_complaintFee_disputed_blocked**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL khiếu nại fee disputed blocked.
    * 🔍 **createsepayPaymentUrl_reusesExistingPendingPayment**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL reuses existing pending thanh toán.
    * 🔍 **createsepayPaymentUrl_orderNotFound_throws**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL đơn hàng/giao dịch không tìm thấy ném ra lỗi.
    * 🔍 **createsepayPaymentUrl_otherCustomer_throwsForbidden**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL other khách hàng ném ra lỗi lỗi không có quyền truy cập (403 Forbidden).
    * 🔍 **createsepayPaymentUrl_invalidType_throwsIllegalArgument**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL không hợp lệ type ném ra lỗi illegal argument.
    * 🔍 **createsepayPaymentUrl_deposit_wrongStatus_throwsBusiness**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL deposit wrong trạng thái ném ra lỗi business.
    * 🔍 **createsepayPaymentUrl_deposit_holdExpired_throwsBusiness**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL deposit hold hết hạn ném ra lỗi business.
    * 🔍 **createsepayPaymentUrl_remaining_wrongStatus_throwsBusiness**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL remaining wrong trạng thái ném ra lỗi business.
    * 🔍 **createsepayPaymentUrl_complaintFee_wrongStatus_throwsBusiness**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL khiếu nại fee wrong trạng thái ném ra lỗi business.
    * 🔍 **createsepayPaymentUrl_zeroAmount_throwsBusiness**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL zero amount ném ra lỗi business.
    * 🔍 **createsepayPaymentUrl_nullAmount_throwsBusiness**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL null amount ném ra lỗi business.
    * 🔍 **createsepayPaymentUrl_alreadyPaid_throwsBusiness**
        * *Mô tả nghiệp vụ*: Tạo mới cổng sepay thanh toán đường dẫn URL already paid ném ra lỗi business.
    * 🔍 **completesepayReturn_paymentNotFound_throws**
        * *Mô tả nghiệp vụ*: Complete cổng sepay return thanh toán không tìm thấy ném ra lỗi.
    * 🔍 **completesepayReturn_success_whenFailed_throwsBusiness**
        * *Mô tả nghiệp vụ*: Complete cổng sepay return thực hiện thành công khi thất bại ném ra lỗi business.
    * 🔍 **completesepayReturn_failure_whenAlreadyPaid_leavesPaid**
        * *Mô tả nghiệp vụ*: Complete cổng sepay return failure khi already paid leaves paid.
    * 🔍 **completesepayReturn_deposit_contractException_stillConfirms**
        * *Mô tả nghiệp vụ*: Complete cổng sepay return deposit contract exception still confirms.
    * 🔍 **completesepayReturn_deposit_whenorderAlreadyConfirmed_skipsStatusChange**
        * *Mô tả nghiệp vụ*: Complete cổng sepay return deposit khi đơn hàng/giao dịch already confirmed skips trạng thái change.
    * 🔍 **getMyPayments_withoutStatus_listsCustomerPayments**
        * *Mô tả nghiệp vụ*: Get my payments khi không có trạng thái lists khách hàng payments.
    * 🔍 **getMyPayments_withStatus_filters**
        * *Mô tả nghiệp vụ*: Get my payments with trạng thái filters.
    * 🔍 **getMyPayments_invalidStatus_treatedAsAll**
        * *Mô tả nghiệp vụ*: Get my payments không hợp lệ trạng thái treated as all.
    * 🔍 **getAllPayments_withFiltersAndSort_returnsPage**
        * *Mô tả nghiệp vụ*: Get all payments with filters and sort returns page.
    * 🔍 **getAllPayments_blankSortAndSearch_defaultsCreatedAtDesc**
        * *Mô tả nghiệp vụ*: Get all payments blank sort and search mặc định created at desc.
    * 🔍 **getAllPayments_disallowedSortField_fallsBackToCreatedAt**
        * *Mô tả nghiệp vụ*: Get all payments disallowed sort field falls back to created at.
    * 🔍 **getPaymentsForstaffScoped_withsellerId_validatesScope**
        * *Mô tả nghiệp vụ*: Get payments for quản lý scoped with sản phẩm/cửa hàng/sản phẩm/cửa hàng id validates scope.
    * 🔍 **getPaymentsForstaffScoped_withoutseller_usesAssignments**
        * *Mô tả nghiệp vụ*: Get payments for quản lý scoped khi không có sản phẩm/cửa hàng/sản phẩm/cửa hàng uses assignments.
    * 🔍 **getPaymentsForstaffScoped_noAssignedProperties_returnsEmpty**
        * *Mô tả nghiệp vụ*: Get payments for quản lý scoped no assigned properties returns empty.
    * 🔍 **getPaymentDetail_customerOwner_returnsDetail**
        * *Mô tả nghiệp vụ*: Get thanh toán detail khách hàng owner returns detail.
    * 🔍 **getPaymentDetail_staff_validatesScope**
        * *Mô tả nghiệp vụ*: Get thanh toán detail quản lý validates scope.
    * 🔍 **getPaymentDetail_otherCustomer_throwsForbidden**
        * *Mô tả nghiệp vụ*: Get thanh toán detail other khách hàng ném ra lỗi lỗi không có quyền truy cập (403 Forbidden).
    * 🔍 **getPaymentDetail_notFound_throws**
        * *Mô tả nghiệp vụ*: Get thanh toán detail không tìm thấy ném ra lỗi.


## 📄 Lớp kiểm thử: WithdrawalServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **verifyPayment_deferred_td011_rejectsstaff**
        * *Mô tả nghiệp vụ*: Xác minh thanh toán deferred td011 từ chối quản lý.
    * 🔍 **verifyPayment_deferred_td011_rejectsCustomer**
        * *Mô tả nghiệp vụ*: Xác minh thanh toán deferred td011 từ chối khách hàng.


## 📄 Lớp kiểm thử: TopupWebhookServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **completesepayReturn_deposit_confirmsorder_andContract**
        * *Mô tả nghiệp vụ*: Complete cổng sepay return deposit confirms đơn hàng/giao dịch and contract.
    * 🔍 **completesepayReturn_remaining_marksPaid_withoutReconfirm**
        * *Mô tả nghiệp vụ*: Complete cổng sepay return remaining marks paid khi không có reconfirm.
    * 🔍 **completesepayReturn_complaintFee_marksPaid_andSettles**
        * *Mô tả nghiệp vụ*: Complete cổng sepay return khiếu nại fee marks paid and settles.
    * 🔍 **completesepayReturn_complaintFee_disputed_blocked**
        * *Mô tả nghiệp vụ*: Complete cổng sepay return khiếu nại fee disputed blocked.
    * 🔍 **completesepayReturn_idempotent_whenAlreadyPaid**
        * *Mô tả nghiệp vụ*: Complete cổng sepay return idempotent khi already paid.
    * 🔍 **completesepayReturn_failure_marksFailed**
        * *Mô tả nghiệp vụ*: Complete cổng sepay return failure marks thất bại.
    * 🔍 **completesepayReturn_success_storesGatewayTransactionId**
        * *Mô tả nghiệp vụ*: Complete cổng sepay return thực hiện thành công stores cổng thanh toán giao dịch id.


## 📄 Lớp kiểm thử: SellerReportServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **getorderTrendReport_countsOnlyConfirmedCheckedInCheckedOut**
        * *Mô tả nghiệp vụ*: Get đơn hàng/giao dịch trend report counts only confirmed checked in checked out.
    * 🔍 **getOccupancyReport_deniesUnassignedseller**
        * *Mô tả nghiệp vụ*: Get occupancy report denies unassigned sản phẩm/cửa hàng/sản phẩm/cửa hàng.
    * 🔍 **getOccupancyReportAdmin_skipsstaffScope**
        * *Mô tả nghiệp vụ*: Get occupancy report quản trị viên skips quản lý scope.


## 📄 Lớp kiểm thử: PublicProductSearchServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **getSearchSuggestions_shortQuery_returnsPopularWithoutNameFilter**
        * *Mô tả nghiệp vụ*: Get search suggestions short query returns popular khi không có name filter.
    * 🔍 **getSearchSuggestions_validQuery_filtersByName**
        * *Mô tả nghiệp vụ*: Get search suggestions hợp lệ query filters by name.


## 📄 Lớp kiểm thử: AdminRevenueServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **getRevenueReportForstaff_withseller_validatesScope**
        * *Mô tả nghiệp vụ*: Get revenue report for quản lý with sản phẩm/cửa hàng/sản phẩm/cửa hàng validates scope.
    * 🔍 **getRevenueReportForstaff_unassigned_throwsForbidden**
        * *Mô tả nghiệp vụ*: Get revenue report for quản lý unassigned ném ra lỗi lỗi không có quyền truy cập (403 Forbidden).
    * 🔍 **getRevenueReportForstaff_nullseller_usesAssignedOnly**
        * *Mô tả nghiệp vụ*: Get revenue report for quản lý null sản phẩm/cửa hàng/sản phẩm/cửa hàng uses assigned only.


## 📄 Lớp kiểm thử: ReviewServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **submitReview_duplicate_throwsConflict**
        * *Mô tả nghiệp vụ*: Submit review trùng lặp ném ra lỗi xung đột dữ liệu.
    * 🔍 **submitReview_duplicate_throwsConflict**
        * *Mô tả nghiệp vụ*: Submit review trùng lặp ném ra lỗi xung đột dữ liệu.
    * 🔍 **recalculate_publishedOnly**
        * *Mô tả nghiệp vụ*: Tính toán lại published only.
    * 🔍 **moderateForstaff_crossseller_forbidden**
        * *Mô tả nghiệp vụ*: Kiểm duyệt for quản lý cross sản phẩm/cửa hàng/sản phẩm/cửa hàng lỗi không có quyền truy cập (403 Forbidden).
    * 🔍 **moderateForstaff_hide_logsAndRecalculates**
        * *Mô tả nghiệp vụ*: Kiểm duyệt for quản lý hide logs and tính toán lại.


## 📄 Lớp kiểm thử: ProductVariantServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **occupiedBeatsMaintenance**
        * *Mô tả nghiệp vụ*: Occupied beats maintenance.
    * 🔍 **pendingDepositBeatsReserved**
        * *Mô tả nghiệp vụ*: Pending deposit beats reserved.
    * 🔍 **availableIsLowest**
        * *Mô tả nghiệp vụ*: Available is lowest.


## 📄 Lớp kiểm thử: ProductSearchServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **getById_inactiveseller_throwsNotFound**
        * *Mô tả nghiệp vụ*: Get by id không hoạt động sản phẩm/cửa hàng/sản phẩm/cửa hàng ném ra lỗi không tìm thấy.
    * 🔍 **getById_activeseller_returnsDetail**
        * *Mô tả nghiệp vụ*: Get by id hoạt động sản phẩm/cửa hàng/sản phẩm/cửa hàng returns detail.
    * 🔍 **getAll_invalidDateRange_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Get all không hợp lệ date range ném ra lỗi business exception.
    * 🔍 **getAll_pastCheckIn_throwsBusinessException**
        * *Mô tả nghiệp vụ*: Get all past check in ném ra lỗi business exception.
    * 🔍 **getFeatured_usesActivesellerQuery**
        * *Mô tả nghiệp vụ*: Get featured uses hoạt động sản phẩm/cửa hàng/sản phẩm/cửa hàng query.
    * 🔍 **getAll_validDates_delegatesToRepository**
        * *Mô tả nghiệp vụ*: Get all hợp lệ dates delegates to repository.


## 📄 Lớp kiểm thử: ProductManagementServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **softDelete_setsDeletedAt_whenNoBlockingorders**
        * *Mô tả nghiệp vụ*: Xóa mềm xóa sets deleted at khi no blocking đơn hàngs.
    * 🔍 **softDelete_conflict_whenBlockingorders**
        * *Mô tả nghiệp vụ*: Xóa mềm xóa xung đột dữ liệu khi blocking đơn hàngs.
    * 🔍 **softDelete_notFound_whenAlreadyDeleted**
        * *Mô tả nghiệp vụ*: Xóa mềm xóa không tìm thấy khi already deleted.
    * 🔍 **create_rejectsDuplicateproductNumber**
        * *Mô tả nghiệp vụ*: Tạo mới từ chối trùng lặp mặt hàng/sản phẩm/cửa hàng number.
    * 🔍 **createForstaff_defaultsAvailable_andLogs**
        * *Mô tả nghiệp vụ*: Tạo mới for quản lý mặc định available and logs.
    * 🔍 **uploadImages_rejectsWhenAtMaxCap**
        * *Mô tả nghiệp vụ*: Upload images từ chối khi at max cap.
    * 🔍 **uploadImages_rejectsInvalidMime**
        * *Mô tả nghiệp vụ*: Upload images từ chối không hợp lệ mime.


## 📄 Lớp kiểm thử: ProductServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **getMonthAvailability_halfOpen_excludesCheckoutDay**
        * *Mô tả nghiệp vụ*: Get month availability half open loại trừ checkout day.
    * 🔍 **getMonthAvailabilityForstaff_reusesDaysAndAddsorderRefs**
        * *Mô tả nghiệp vụ*: Get month availability for quản lý reuses days and adds đơn hàng/giao dịch refs.
    * 🔍 **getMonthAvailability_blockOnlyPaintsRangeNotWholeMonth**
        * *Mô tả nghiệp vụ*: Get month availability block only paints range không whole month.
    * 🔍 **updateStatus_availableRejectedWhenHkOpen**
        * *Mô tả nghiệp vụ*: Cập nhật trạng thái available rejected khi hk open.
    * 🔍 **updateStatus_maintenanceRejectedOnorderOverlap**
        * *Mô tả nghiệp vụ*: Cập nhật trạng thái maintenance rejected on đơn hàng/giao dịch overlap.
    * 🔍 **updateStatus_maintenancePersistsBlock**
        * *Mô tả nghiệp vụ*: Cập nhật trạng thái maintenance persists block.
    * 🔍 **checkAvailability_statusBlockOverlap_notAvailable**
        * *Mô tả nghiệp vụ*: Check availability trạng thái block overlap không available.


## 📄 Lớp kiểm thử: UserServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **getMyProfile_returnsSelfFields**
        * *Mô tả nghiệp vụ*: Get my profile returns self fields.
    * 🔍 **updateMyProfile_updatesAllowedFields_keepsEmailRoleStatus**
        * *Mô tả nghiệp vụ*: Cập nhật my profile updates allowed fields keeps hộp thư email role trạng thái.
    * 🔍 **updateMyProfile_partial_omittedFieldsUnchanged**
        * *Mô tả nghiệp vụ*: Cập nhật my profile partial omitted fields unchanged.
    * 🔍 **updateMyProfile_partial_omittedFieldsUnchanged**
        * *Mô tả nghiệp vụ*: Cập nhật my profile partial omitted fields unchanged.
    * 🔍 **updateMyProfile_blankPhoneAndAvatar_clears**
        * *Mô tả nghiệp vụ*: Cập nhật my profile blank phone and avatar xóa sạch.
    * 🔍 **updateMyProfile_blankFullName_throws**
        * *Mô tả nghiệp vụ*: Cập nhật my profile blank full name ném ra lỗi.
    * 🔍 **updateMyProfile_invalidPhone_throws**
        * *Mô tả nghiệp vụ*: Cập nhật my profile không hợp lệ phone ném ra lỗi.
    * 🔍 **updateMyProfile_avatarMustBeHttp**
        * *Mô tả nghiệp vụ*: Cập nhật my profile avatar must be http.
    * 🔍 **updateMyProfile_fullNameMax255**
        * *Mô tả nghiệp vụ*: Cập nhật my profile full name max255.
    * 🔍 **getMyProfile_unknownUser_throws**
        * *Mô tả nghiệp vụ*: Get my profile unknown user ném ra lỗi.


## 📄 Lớp kiểm thử: SePayServiceTest
* **Các ca kiểm thử (Test Cases)**:
    * 🔍 **createOrder_includesSignedHashAndPaymentId**
        * *Mô tả nghiệp vụ*: Tạo mới order includes signed hash and thanh toán id.
    * 🔍 **verifySignature_acceptsMatchingHash**
        * *Mô tả nghiệp vụ*: Xác minh signature chấp nhận matching hash.
    * 🔍 **verifySignature_rejectsTamperedHash**
        * *Mô tả nghiệp vụ*: Xác minh signature từ chối tampered hash.


---

## 📂 5. PACKAGE: com.mmo.support (Công cụ sinh dữ liệu giả lập)

## 📄 Lớp: TestFixtures
* *Mô tả*: Cung cấp các hàm tĩnh sinh nhanh thực thể User, Product, WalletTransaction phục vụ viết mã Mock.


## 📄 Lớp: TestSecurityUtils
* *Mô tả*: Giả lập Spring Security Principal chứa vai trò (Role) và thông tin đăng nhập của người dùng.
