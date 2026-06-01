let currentProfile = null;
let accountSidebar = null;

document.addEventListener('DOMContentLoaded', initializeProfilePage);

function initializeProfilePage() {
    accountSidebar = new AccountSidebar();
    document.getElementById('editProfileButton').addEventListener('click', openEditMode);
    document.getElementById('cancelEditButton').addEventListener('click', closeEditMode);
    document.getElementById('profileEditForm').addEventListener('submit', saveProfile);
    loadProfile();
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
        details.hidden = false;
        document.getElementById('profileActions').hidden = false;
    } catch (error) {
        message.textContent = error.message;
        message.classList.add('profile-message--error');
    }
}

function openEditMode() {
    if (!currentProfile) {
        return;
    }

    document.getElementById('profileFullNameInput').value = currentProfile.fullName || '';
    document.getElementById('profilePhoneInput').value = currentProfile.phone || '';
    clearFormErrors();

    document.getElementById('profileDetails').hidden = true;
    document.getElementById('profileActions').hidden = true;
    document.getElementById('profileEditForm').hidden = false;
}

function closeEditMode() {
    document.getElementById('profileEditForm').hidden = true;
    document.getElementById('profileDetails').hidden = false;
    document.getElementById('profileActions').hidden = false;
    clearFormErrors();
}

async function saveProfile(event) {
    event.preventDefault();

    const fullName = document.getElementById('profileFullNameInput').value.trim();
    const phone = document.getElementById('profilePhoneInput').value.trim();
    if (!validateProfile(fullName, phone)) {
        return;
    }

    const saveButton = document.getElementById('saveProfileButton');
    saveButton.disabled = true;
    saveButton.textContent = 'Đang lưu...';

    try {
        const response = await authFetch('/v1/profile', {
            method: 'PUT',
            body: JSON.stringify({ fullName, phone })
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

function validateProfile(fullName, phone) {
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

    return valid;
}

function clearFormErrors() {
    document.getElementById('profileFullNameError').textContent = '';
    document.getElementById('profilePhoneError').textContent = '';
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
    formMessage.classList.add('profile-message--error');
    formMessage.hidden = false;
}

function showProfileMessage(message, type) {
    const profileMessage = document.getElementById('profileMessage');
    profileMessage.textContent = message;
    profileMessage.classList.remove('profile-message--error', 'profile-message--success');
    profileMessage.classList.add(`profile-message--${type}`);
    profileMessage.hidden = false;
}

function renderProfile(profile) {
    const fullName = profile.fullName || 'Người dùng';
    const role = parseRole(profile.role);
    const isSeller = role === 'Seller' || role === 'Customer_Seller';
    const balance = formatProfileBalance(profile.balanceVnd);

    document.getElementById('profileAvatar').textContent = fullName.charAt(0).toUpperCase();
    document.getElementById('profileFullName').textContent = fullName;
    document.getElementById('profileEmail').textContent = profile.email || '-';
    document.getElementById('profilePhone').textContent = profile.phone || '-';
    document.getElementById('profileRole').textContent = role;
    document.getElementById('profileShopStatusRow').hidden = !isSeller;
    document.getElementById('profileShopStatus').textContent = profile.shopStatus || '-';
    document.getElementById('profileBalance').textContent = balance;

    accountSidebar.render(profile);
}

function formatProfileBalance(balanceVnd) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })
        .format(balanceVnd || 0);
}

function parseRole(role) {
    if (!role) {
        return '-';
    }

    try {
        return JSON.parse(role).role || role;
    } catch {
        return role;
    }
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
