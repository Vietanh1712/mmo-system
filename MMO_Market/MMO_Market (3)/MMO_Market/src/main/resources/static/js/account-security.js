const SECURITY_STORAGE_KEY = 'mmoMarketSecurityMock';
const SECURITY_DEFAULT_STATE = {
    twoFactorEnabled: false,
    lastPasswordChangedAt: null
};

var accountSidebar = null;
let securityState = SECURITY_DEFAULT_STATE;
let securityMessageTimer = null;
let pendingTwoFactorAction = null;

function registerAccountPage(scriptPath, initializer) {
    window.AccountPageInitializers = window.AccountPageInitializers || {};
    window.AccountPageInitializers[scriptPath] = initializer;

    if (!document.currentScript || !document.currentScript.dataset.accountPartial) {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', initializer);
        } else {
            initializer();
        }
    }
}

registerAccountPage('/js/account-security.js', initializeSecurityPage);

function initializeSecurityPage() {
    accountSidebar = new AccountSidebar();
    bindSecurityEvents();
    loadSecurityPage();
}

function bindSecurityEvents() {
    document.getElementById('changePasswordForm').addEventListener('submit', submitChangePassword);
    document.getElementById('twoFactorToggle').addEventListener('click', requestTwoFactorChange);
    document.getElementById('twoFactorCancelButton').addEventListener('click', closeTwoFactorModal);
    document.getElementById('twoFactorConfirmButton').addEventListener('click', confirmTwoFactorChange);

}

async function loadSecurityPage() {
    const token = sessionStorage.getItem('accessToken');
    if (!token || token === 'null' || token === 'undefined') {
        window.location.href = '/login';
        return;
    }

    try {
        const response = await authFetch('/v1/profile');
        if (!response.ok) {
            throw new Error('Không thể tải thông tin tài khoản.');
        }

        const profile = await response.json();
        accountSidebar.render(profile);
        
        securityState = {
            ...securityState,
            twoFactorEnabled: profile.is2faEnabled === true,
            kycStatus: profile.kycStatus
        };
        
        renderSecurityState();
        focusModeFromUrl();
    } catch (error) {
        showSecurityMessage(error.message || 'Không thể tải màn bảo mật.', 'danger');
    }
}

function readSecurityState() {
    try {
        return JSON.parse(sessionStorage.getItem(SECURITY_STORAGE_KEY)) || SECURITY_DEFAULT_STATE;
    } catch {
        return SECURITY_DEFAULT_STATE;
    }
}

function saveSecurityState(nextState) {
    securityState = nextState;
    sessionStorage.setItem(SECURITY_STORAGE_KEY, JSON.stringify(nextState));
}

function renderSecurityState() {
    const badge = document.getElementById('securityStatusBadge');
    const twoFactorStatusBadge = document.getElementById('twoFactorStatusBadge');
    const toggle = document.getElementById('twoFactorToggle');
    const toggleText = document.getElementById('twoFactorToggleText');
    const description = document.getElementById('twoFactorDescription');

    if (securityState.twoFactorEnabled) {
        badge.textContent = 'Đã bảo vệ';
        badge.className = 'ds-badge ds-badge-success';
        twoFactorStatusBadge.textContent = 'Đã bật';
        twoFactorStatusBadge.className = 'ds-badge ds-badge-success';
        document.getElementById('twoFactorStatusText').textContent = 'Đã kích hoạt';
        toggle.classList.remove('ds-toggle-inactive');
        toggle.setAttribute('aria-pressed', 'true');
        toggleText.textContent = 'Đã kích hoạt';
        description.textContent = '2FA đang được bật. Các thao tác nhạy cảm sẽ yêu cầu mã OTP.';
    } else {
        badge.textContent = 'Nên bật 2FA';
        badge.className = 'ds-badge ds-badge-warning';
        twoFactorStatusBadge.textContent = 'Khuyến nghị';
        twoFactorStatusBadge.className = 'ds-badge ds-badge-warning';
        document.getElementById('twoFactorStatusText').textContent = 'Chưa kích hoạt';
        toggle.classList.add('ds-toggle-inactive');
        toggle.setAttribute('aria-pressed', 'false');
        toggleText.textContent = 'Chưa kích hoạt';
        description.textContent = 'Tài khoản của bạn nên bật 2FA để giảm rủi ro khi bị lộ mật khẩu.';
    }

    document.getElementById('passwordStatusText').textContent = securityState.lastPasswordChangedAt
        ? `Đổi gần nhất: ${securityState.lastPasswordChangedAt}`
        : 'Đã thiết lập';
    document.getElementById('securityKycStatusText').textContent = getKycStatusText(securityState.kycStatus);
    document.getElementById('currentSessionText').textContent = getCurrentSessionText();
    showSecurityMessage(getSecuritySummaryMessage(), securityState.twoFactorEnabled ? 'success' : 'warning');
}

async function submitChangePassword(event) {
    event.preventDefault();

    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (!validatePasswordForm(currentPassword, newPassword, confirmPassword)) {
        return;
    }

    const button = document.getElementById('changePasswordButton');
    button.disabled = true;
    const originalText = button.textContent;
    button.textContent = 'Đang lưu...';

    try {
        const response = await authFetch('/v1/profile/password', {
            method: 'PUT',
            body: JSON.stringify({ currentPassword, newPassword })
        });

        const data = await response.json();

        if (response.ok && data.success) {
            const changedAt = new Intl.DateTimeFormat('vi-VN', {
                dateStyle: 'short',
                timeStyle: 'short'
            }).format(new Date());

            saveSecurityState({ ...securityState, lastPasswordChangedAt: changedAt });
            document.getElementById('changePasswordForm').reset();
            clearPasswordErrors();
            renderSecurityState();
            showPasswordFormMessage(data.message || 'Đổi mật khẩu thành công.', 'success');
        } else {
            showPasswordFormMessage(data.message || 'Lỗi đổi mật khẩu.');
        }
    } catch (error) {
        showPasswordFormMessage('Không thể kết nối đến máy chủ.');
    } finally {
        button.disabled = false;
        button.textContent = originalText || 'Đổi mật khẩu';
    }
}

function validatePasswordForm(currentPassword, newPassword, confirmPassword) {
    clearPasswordErrors();
    let valid = true;
    const strongPassword = /^(?=.*[A-Z])(?=.*[\W_]).{6,}$/;

    if (!currentPassword) {
        setText('currentPasswordError', 'Vui lòng nhập mật khẩu hiện tại.');
        valid = false;
    }

    if (!strongPassword.test(newPassword)) {
        setText('newPasswordError', 'Mật khẩu mới phải ít nhất 6 ký tự, gồm 1 chữ hoa và 1 ký tự đặc biệt.');
        valid = false;
    }

    if (currentPassword && newPassword && currentPassword === newPassword) {
        setText('newPasswordError', 'Mật khẩu mới không được trùng mật khẩu hiện tại.');
        valid = false;
    }

    if (!confirmPassword || confirmPassword !== newPassword) {
        setText('confirmPasswordError', 'Mật khẩu xác nhận không trùng khớp.');
        valid = false;
    }

    if (!valid) {
        showPasswordFormMessage('Vui lòng kiểm tra lại thông tin mật khẩu.');
    }

    return valid;
}

function clearPasswordErrors() {
    ['currentPasswordError', 'newPasswordError', 'confirmPasswordError'].forEach(id => setText(id, ''));
    document.getElementById('passwordFormMessage').hidden = true;
}

function showPasswordFormMessage(message, type = 'danger') {
    const formMessage = document.getElementById('passwordFormMessage');
    formMessage.textContent = message;
    formMessage.className = `profile-message ds-alert ds-alert-${type}`;
    formMessage.hidden = false;
}

async function requestTwoFactorChange() {
    pendingTwoFactorAction = securityState.twoFactorEnabled ? 'disable' : 'enable';
    const isDisable = pendingTwoFactorAction === 'disable';

    if (!isDisable) {
        // Calling API to send OTP
        try {
            const response = await authFetch('/v1/profile/2fa/send-otp', { method: 'POST' });
            if (!response.ok) {
                const data = await response.json();
                throw new Error(data.message || 'Lỗi gửi OTP');
            }
        } catch (error) {
            showSecurityMessage(error.message || 'Không thể gửi mã OTP.', 'danger');
            return;
        }
    }

    document.getElementById('twoFactorModalTitle').textContent = isDisable ? 'Tắt 2FA' : 'Kích hoạt 2FA';
    document.getElementById('twoFactorModalDescription').textContent = isDisable
        ? 'Tắt 2FA sẽ làm tài khoản kém an toàn hơn. Bạn có chắc muốn tiếp tục?'
        : 'Mã OTP đã được gửi về email của bạn. Nhập mã OTP 6 số để xác nhận kích hoạt bảo mật 2 lớp.';
    document.getElementById('twoFactorConfirmButton').textContent = isDisable ? 'Tắt 2FA' : 'Xác nhận';
    document.getElementById('twoFactorConfirmButton').className = isDisable ? 'ds-btn ds-btn-danger' : 'ds-btn ds-btn-primary';
    document.getElementById('twoFactorOtpField').hidden = isDisable;
    document.getElementById('twoFactorOtp').value = '';
    setText('twoFactorOtpError', '');
    document.getElementById('twoFactorModal').hidden = false;
}

async function confirmTwoFactorChange() {
    const isDisable = pendingTwoFactorAction === 'disable';
    let requestBody = null;

    if (!isDisable) {
        const otp = document.getElementById('twoFactorOtp').value.trim();
        if (!/^\d{6}$/.test(otp)) {
            setText('twoFactorOtpError', 'Mã OTP phải gồm 6 chữ số.');
            return;
        }
        requestBody = JSON.stringify({ otp: otp });
    }

    const button = document.getElementById('twoFactorConfirmButton');
    button.disabled = true;

    try {
        const endpoint = isDisable ? '/v1/profile/2fa/disable' : '/v1/profile/2fa/enable';
        const response = await authFetch(endpoint, {
            method: 'POST',
            body: requestBody
        });

        const data = await response.json();

        if (response.ok && data.success) {
            securityState = { ...securityState, twoFactorEnabled: !isDisable };
            closeTwoFactorModal();
            renderSecurityState();
            showSecurityMessage(data.message || (isDisable ? 'Đã tắt 2FA.' : 'Đã kích hoạt 2FA thành công.'), isDisable ? 'warning' : 'success');
        } else {
            setText('twoFactorOtpError', data.message || 'Lỗi thao tác.');
        }
    } catch (error) {
        setText('twoFactorOtpError', 'Không thể kết nối đến máy chủ.');
    } finally {
        button.disabled = false;
    }
}

function closeTwoFactorModal() {
    pendingTwoFactorAction = null;
    document.getElementById('twoFactorModal').hidden = true;
}

function togglePasswordVisibility(button) {
    const input = document.getElementById(button.dataset.togglePassword);
    const icon = button.querySelector('i');
    const shouldShow = input.type === 'password';
    input.type = shouldShow ? 'text' : 'password';
    icon.className = shouldShow ? 'fa fa-eye-slash' : 'fa fa-eye';
}

function focusModeFromUrl() {
    const mode = new URLSearchParams(window.location.search).get('mode');
    if (mode === 'password') {
        document.getElementById('currentPassword').focus();
    }

    if (mode === '2fa') {
        document.getElementById('twoFactorToggle').focus();
    }
}

function getKycStatusText(status) {
    if (status === 'APPROVED') return 'Đã xác minh';
    if (status === 'PENDING') return 'Đang chờ duyệt';
    if (status === 'REJECTED') return 'Bị từ chối';
    return 'Chưa định danh';
}

function getCurrentSessionText() {
    const browser = navigator.userAgent.includes('CocCoc') ? 'Cốc Cốc' : 'Trình duyệt hiện tại';
    const os = navigator.userAgent.includes('Windows') ? 'Windows' : 'Thiết bị hiện tại';
    return `${browser} · ${os}`;
}

function getSecuritySummaryMessage() {
    return securityState.twoFactorEnabled
        ? 'Tài khoản đang bật 2FA. Trạng thái bảo mật hiện tốt hơn.'
        : 'Bạn nên bật 2FA để tăng bảo vệ cho tài khoản và giao dịch.';
}

function showSecurityMessage(message, type) {
    const messageElement = document.getElementById('securityMessage');
    messageElement.textContent = message;
    messageElement.hidden = false;
    messageElement.classList.remove('ds-alert-info', 'ds-alert-warning', 'ds-alert-danger', 'ds-alert-success');
    messageElement.classList.add(`ds-alert-${type}`);

    if (securityMessageTimer) {
        clearTimeout(securityMessageTimer);
        securityMessageTimer = null;
    }

    if (type === 'success') {
        securityMessageTimer = setTimeout(() => {
            messageElement.hidden = true;
            securityMessageTimer = null;
        }, 3000);
    }
}

function setText(id, value) {
    document.getElementById(id).textContent = value;
}
