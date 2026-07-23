let currentProfile = null;
var accountSidebar = null;
let profileMessageTimer = null;

registerAccountPage('/js/customer/profile.js', initializeProfilePage);

function initializeProfilePage() {
    accountSidebar = new AccountSidebar();
    document.getElementById('editProfileButton').addEventListener('click', openEditMode);
    document.getElementById('cancelEditButton').addEventListener('click', closeEditMode);
    document.getElementById('profileEditForm').addEventListener('submit', saveProfile);
    
    document.querySelectorAll('.toggle-visibility-btn').forEach(btn => {
        btn.addEventListener('click', toggleFieldVisibility);
    });

    loadProfile();
}

function toggleFieldVisibility(event) {
    const btn = event.currentTarget;
    const icon = btn.querySelector('i');
    const dd = btn.closest('dd');
    const span = dd.querySelector('span[data-masked]');
    
    if (span.dataset.masked === 'true') {
        span.textContent = span.dataset.fullValue;
        span.dataset.masked = 'false';
        icon.className = 'fa fa-eye';
        btn.setAttribute('aria-label', 'Ẩn');
    } else {
        span.textContent = span.dataset.maskedValue;
        span.dataset.masked = 'true';
        icon.className = 'fa fa-eye-slash';
        btn.setAttribute('aria-label', 'Hiện');
    }
}

async function loadProfile() {
    const token = sessionStorage.getItem('accessToken');
    if (!token || token === 'null' || token === 'undefined') {
        window.location.href = '/login';
        return;
    }

    const message = document.getElementById('profileMessage');
    const details = document.getElementById('profileDetails');

    try {
        const response = await authFetch('/v1/profile');
        if (!response.ok) {
            throw new Error('Không thể tải thông tin cá nhân.');
        }

        const profile = await response.json();
        currentProfile = profile;
        renderProfile(profile);
        updateCachedProfile(profile);

        message.hidden = true;
        if (isEditModeFromUrl()) {
            openEditMode();
        } else {
            details.hidden = false;
            document.getElementById('profileActions').hidden = false;
        }
    } catch (error) {
        message.textContent = error.message;
        setAlertState(message, 'danger');
        message.hidden = false;
    }
}

function openEditMode(event) {
    if (event) {
        event.preventDefault();
    }

    if (!currentProfile) {
        return;
    }

    document.getElementById('profileFullNameInput').value = currentProfile.fullName || '';
    document.getElementById('profileEmailInput').value = currentProfile.email || '';
    document.getElementById('profilePhoneInput').value = currentProfile.phone || '';
    document.getElementById('profileNationalIdInput').value = currentProfile.nationalId || '';
    
    const genderValue = currentProfile.gender || '';
    const genderRadio = document.querySelector(`input[name="gender"][value="${genderValue}"]`);
    if (genderRadio) {
        genderRadio.checked = true;
    } else {
        document.querySelectorAll('input[name="gender"]').forEach(r => r.checked = false);
    }
    
    const dob = currentProfile.dateOfBirth || '';
    let dobDisplay = '';
    if (dob) {
        const parts = dob.split('-');
        if (parts.length === 3) {
            dobDisplay = `${parts[2]}/${parts[1]}/${parts[0]}`;
        } else {
            dobDisplay = dob;
        }
    }
    document.getElementById('profileDateOfBirthInput').value = dob;
    
    const dobDisplayInput = document.getElementById('profileDateOfBirthDisplay');
    if (dobDisplayInput) {
        dobDisplayInput.value = dobDisplay;
    }

    document.getElementById('profileAddressInput').value = currentProfile.address || '';
    clearFormErrors();

    document.getElementById('profileDetails').hidden = true;
    document.getElementById('profileActions').hidden = true;
    document.getElementById('profileEditForm').hidden = false;
    setProfileModeInUrl('edit');
}

function closeEditMode() {
    document.getElementById('profileEditForm').hidden = true;
    document.getElementById('profileDetails').hidden = false;
    document.getElementById('profileActions').hidden = false;
    clearFormErrors();
    setProfileModeInUrl('view');
}

async function saveProfile(event) {
    event.preventDefault();

    const fullName = document.getElementById('profileFullNameInput').value.trim();
    const phone = document.getElementById('profilePhoneInput').value.trim();
    const dateOfBirth = document.getElementById('profileDateOfBirthInput').value.trim();
    const address = document.getElementById('profileAddressInput').value.trim();
    const gender = document.querySelector('input[name="gender"]:checked')?.value || '';
    const nationalId = document.getElementById('profileNationalIdInput').value.trim();
    
    if (!validateProfile(fullName, phone, address, nationalId)) {
        return;
    }

    const saveButton = document.getElementById('saveProfileButton');
    saveButton.disabled = true;
    saveButton.textContent = 'Đang lưu...';

    try {
        const response = await authFetch('/v1/profile', {
            method: 'PUT',
            body: JSON.stringify({ fullName, phone, dateOfBirth, address, gender, nationalId })
        });
        const responseBody = await readResponseBody(response);

        if (!response.ok) {
            throw new Error(getApiErrorMessage(responseBody));
        }

        currentProfile = responseBody;
        renderProfile(currentProfile);
        updateCachedProfile(currentProfile);
        closeEditMode();
        showProfileMessage('Cập nhật thông tin cá nhân thành công.', 'success');
    } catch (error) {
        showFormMessage(error.message || 'Không thể cập nhật thông tin cá nhân.');
    } finally {
        saveButton.disabled = false;
        saveButton.textContent = 'Lưu thay đổi';
    }
}

function validateProfile(fullName, phone, address, nationalId) {
    clearFormErrors();
    let valid = true;

    if (fullName.length < 3) {
        document.getElementById('profileFullNameError').textContent = 'Họ và tên phải có ít nhất 3 ký tự.';
        valid = false;
    }

    if (fullName.length > 255) {
        document.getElementById('profileFullNameError').textContent = 'Họ và tên không được vượt quá 255 ký tự.';
        valid = false;
    }

    if (phone && !/^0\d{9}$/.test(phone)) {
        document.getElementById('profilePhoneError').textContent =
            'Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0.';
        valid = false;
    }

    if (address && address.length > 500) {
        document.getElementById('profileAddressError').textContent = 'Địa chỉ không được vượt quá 500 ký tự.';
        valid = false;
    }

    if (nationalId && nationalId.length > 20) {
        document.getElementById('profileNationalIdError').textContent = 'Số CCCD/CMND không được vượt quá 20 ký tự.';
        valid = false;
    }

    return valid;
}

function clearFormErrors() {
    document.getElementById('profileFullNameError').textContent = '';
    document.getElementById('profilePhoneError').textContent = '';
    document.getElementById('profileDateOfBirthError').textContent = '';
    document.getElementById('profileAddressError').textContent = '';
    document.getElementById('profileFormMessage').hidden = true;
}

async function readResponseBody(response) {
    try {
        return await response.json();
    } catch {
        return {};
    }
}

function getApiErrorMessage(responseBody) {
    return responseBody.detail || responseBody.message || 'Không thể cập nhật thông tin cá nhân.';
}

function showFormMessage(message) {
    const formMessage = document.getElementById('profileFormMessage');
    formMessage.textContent = message;
    setAlertState(formMessage, 'danger');
    formMessage.hidden = false;
}

function showProfileMessage(message, type) {
    const profileMessage = document.getElementById('profileMessage');
    profileMessage.textContent = message;
    setAlertState(profileMessage, type);
    profileMessage.hidden = false;

    if (profileMessageTimer) {
        clearTimeout(profileMessageTimer);
        profileMessageTimer = null;
    }

    if (type === 'success') {
        profileMessageTimer = setTimeout(() => {
            profileMessage.hidden = true;
            profileMessageTimer = null;
        }, 3000);
    }
}

function isEditModeFromUrl() {
    return new URLSearchParams(window.location.search).get('mode') === 'edit';
}

function setProfileModeInUrl(mode) {
    const nextUrl = mode === 'edit' ? '/profile?mode=edit' : '/profile';
    window.history.pushState({ profileMode: mode }, '', nextUrl);
}

function setAlertState(element, type) {
    element.classList.remove(
        'profile-message--error',
        'profile-message--success',
        'ds-alert-info',
        'ds-alert-danger',
        'ds-alert-success',
        'ds-alert-warning'
    );

    if (type === 'success') {
        element.classList.add('profile-message--success', 'ds-alert-success');
        return;
    }

    if (type === 'warning') {
        element.classList.add('ds-alert-warning');
        return;
    }

    element.classList.add('profile-message--error', 'ds-alert-danger');
}

function renderProfile(profile) {
    const fullName = profile.fullName || 'Người dùng';
    const rawRole = parseRole(profile.role);
    const isSeller = String(profile.role || '').toLowerCase().includes('seller') || rawRole.toLowerCase().includes('seller');
    const balance = formatProfileBalance(profile.balanceVnd);

    document.getElementById('profileAvatar').textContent = fullName.charAt(0).toUpperCase();
    document.getElementById('profileFullName').textContent = fullName;
    document.getElementById('profileGender').textContent = profile.gender || '-';
    
    renderMaskedField('profileEmail', profile.email, maskEmail);
    renderMaskedField('profilePhone', profile.phone, maskString);
    renderMaskedField('profileNationalId', profile.nationalId, maskString);
    
    if (profile.dateOfBirth) {
        const parts = profile.dateOfBirth.split('-');
        document.getElementById('profileDateOfBirth').textContent = parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : profile.dateOfBirth;
    } else {
        document.getElementById('profileDateOfBirth').textContent = '-';
    }
    document.getElementById('profileAddress').textContent = profile.address || '-';

    document.getElementById('profileRole').textContent = rawRole;
    document.getElementById('profileShopStatusRow').hidden = !isSeller;

    const shopStatusBadge = document.getElementById('profileShopStatus');
    if (shopStatusBadge) {
        const shopStUpper = String(profile.shopStatus || 'Active').toUpperCase();
        let badgeClass = 'ds-badge ds-badge-success';
        let shopStVi = 'Hoạt động';

        if (shopStUpper === 'PENDING') {
            badgeClass = 'ds-badge ds-badge-warning';
            shopStVi = 'Chờ duyệt';
        } else if (shopStUpper === 'REJECTED') {
            badgeClass = 'ds-badge ds-badge-danger';
            shopStVi = 'Bị từ chối';
        } else if (shopStUpper === 'WITHDRAWN' || shopStUpper === 'DELETED') {
            badgeClass = 'ds-badge ds-badge-danger';
            shopStVi = 'Đã đóng Shop';
        } else if (shopStUpper === 'SUSPENDED' || shopStUpper === 'TEMP_LOCKED' || shopStUpper === 'TEMPORARILY_CLOSED') {
            badgeClass = 'ds-badge ds-badge-warning';
            shopStVi = 'Tạm ngưng';
        } else if (shopStUpper === 'LOCKED' || shopStUpper === 'INDEFINITE_LOCKED' || shopStUpper === 'CLOSED') {
            badgeClass = 'ds-badge ds-badge-warning';
            shopStVi = 'Tạm khóa';
        } else if (shopStUpper === 'BANNED' || shopStUpper === 'PERMANENT_BANNED') {
            badgeClass = 'ds-badge ds-badge-danger';
            shopStVi = 'Khóa vĩnh viễn';
        }

        shopStatusBadge.className = badgeClass;
        shopStatusBadge.textContent = shopStVi;
    }
    document.getElementById('profileBalance').textContent = balance;

    const normalizedRole = accountSidebar.normalizeRole(profile.role);
    const isInternal = normalizedRole === 'Staff' || normalizedRole === 'Admin';
    const nationalIdRow = document.getElementById('profileNationalIdRow');
    if (nationalIdRow) {
        nationalIdRow.hidden = isInternal;
    }
    const nationalIdField = document.getElementById('profileNationalIdField');
    if (nationalIdField) {
        nationalIdField.hidden = isInternal;
    }

    accountSidebar.render(profile);
}

function renderMaskedField(elementId, value, maskFn) {
    const span = document.getElementById(elementId);
    if (!span) return;
    
    if (!value) {
        span.textContent = '-';
        span.dataset.fullValue = '';
        span.dataset.maskedValue = '';
        return;
    }

    const masked = maskFn(value);
    span.dataset.fullValue = value;
    span.dataset.maskedValue = masked;
    
    if (span.dataset.masked === 'true') {
        span.textContent = masked;
    } else {
        span.textContent = value;
    }
}

function maskEmail(email) {
    if (!email) return '-';
    const parts = email.split('@');
    if (parts.length !== 2) return maskString(email);
    
    const name = parts[0];
    const domain = parts[1];
    
    if (name.length <= 3) {
        return '***@' + domain;
    }
    
    return name.substring(0, 3) + '***@' + domain;
}

function maskString(str) {
    if (!str) return '-';
    str = String(str);
    if (str.length <= 4) return '***';
    return '*'.repeat(str.length - 4) + str.slice(-4);
}

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

function formatProfileBalance(balanceVnd) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })
        .format(balanceVnd || 0);
}

function parseRole(role) {
    if (!role) {
        return '-';
    }

    let parsed = role;
    try {
        parsed = JSON.parse(role).role || role;
    } catch {
        parsed = role;
    }

    const r = String(parsed).toUpperCase();
    if (r === 'CUSTOMER_SELLER' || r === 'SELLER') return 'Người bán (Seller)';
    if (r === 'CUSTOMER') return 'Người mua (Customer)';
    if (r === 'STAFF') return 'Nhân viên (Staff)';
    if (r === 'ADMIN') return 'Quản trị viên (Admin)';
    return parsed;
}

function updateCachedProfile(profile) {
    const cachedProfile = {
        id: profile.id,
        email: profile.email,
        fullName: profile.fullName,
        phone: profile.phone,
        role: profile.role,
        shopStatus: profile.shopStatus,
        balanceVnd: profile.balanceVnd
    };

    sessionStorage.setItem('userInfo', JSON.stringify(cachedProfile));
    sessionStorage.setItem('user', JSON.stringify(cachedProfile));
}
