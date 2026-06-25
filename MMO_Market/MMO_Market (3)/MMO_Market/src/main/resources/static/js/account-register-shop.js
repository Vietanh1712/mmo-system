const SHOP_REGISTRATION_STORAGE_KEY = 'mmoMarketShopRegistrationMock';
const KYC_STORAGE_KEY_FOR_SHOP = 'mmoMarketKycMock';

let shopAccountSidebar = null;
let shopRegistrationState = { status: 'NOT_SUBMITTED' };

document.addEventListener('DOMContentLoaded', initializeShopRegistrationPage);

async function initializeShopRegistrationPage() {
    const token = sessionStorage.getItem('accessToken');
    if (!token || token === 'null' || token === 'undefined') {
        window.location.href = '/login';
        return;
    }

    shopAccountSidebar = new AccountSidebar();
    document.getElementById('shopRegistrationForm').addEventListener('submit', submitShopRegistration);
    document.getElementById('shopEditRequestButton').addEventListener('click', editShopRegistration);

    try {
        const response = await authFetch('/v1/profile');
        if (!response.ok) throw new Error('Không thể tải thông tin tài khoản.');
        const profile = await response.json();
        shopAccountSidebar.render(profile);
        prefillShopContact(profile);
    } catch (error) {
        showShopFormMessage(error.message || 'Không thể tải thông tin tài khoản.');
    }

    shopRegistrationState = readShopRegistrationState();
    renderShopRegistrationState();
}

function readShopRegistrationState() {
    try {
        return JSON.parse(localStorage.getItem(SHOP_REGISTRATION_STORAGE_KEY)) || { status: 'NOT_SUBMITTED' };
    } catch {
        return { status: 'NOT_SUBMITTED' };
    }
}

function saveShopRegistrationState(state) {
    shopRegistrationState = state;
    localStorage.setItem(SHOP_REGISTRATION_STORAGE_KEY, JSON.stringify(state));
}

function readKycApproved() {
    try {
        return JSON.parse(sessionStorage.getItem(KYC_STORAGE_KEY_FOR_SHOP) || '{}').status === 'APPROVED';
    } catch {
        return false;
    }
}

function renderShopRegistrationState() {
    const approvedKyc = readKycApproved();
    const badge = document.getElementById('shopRegistrationStatus');
    const form = document.getElementById('shopRegistrationForm');
    const summary = document.getElementById('shopRequestSummary');
    const requirement = document.getElementById('kycRequirement');

    requirement.classList.toggle('shop-requirement--done', approvedKyc);
    requirement.querySelector('i').className = approvedKyc ? 'fa fa-check-circle' : 'fa fa-clock-o';
    document.getElementById('kycRequirementText').textContent =
        approvedKyc ? 'Danh tính đã được xác minh.' : 'Chưa xác minh danh tính.';
    document.getElementById('shopRequirementProgress').textContent = approvedKyc ? '3/3 hoàn tất' : '2/3 hoàn tất';

    const hasRequest = shopRegistrationState.status !== 'NOT_SUBMITTED';
    summary.hidden = !hasRequest;
    form.hidden = hasRequest;

    if (!hasRequest) {
        badge.textContent = 'Chưa đăng ký';
        badge.className = 'ds-badge ds-badge-muted';
        document.getElementById('shopSubmitButton').disabled = !approvedKyc;
        return;
    }

    const config = getShopStatusConfig(shopRegistrationState.status);
    badge.textContent = config.label;
    badge.className = `ds-badge ${config.badge}`;
    document.getElementById('shopSummaryTitle').textContent = config.title;
    document.getElementById('shopSummaryDescription').textContent = config.description;
    document.getElementById('shopRequestCode').textContent = shopRegistrationState.code || '-';
    document.getElementById('shopRequestDate').textContent = shopRegistrationState.submittedAt || '-';
    document.getElementById('shopRequestName').textContent = shopRegistrationState.shopName || '-';
    document.getElementById('shopRequestCategory').textContent = shopRegistrationState.category || '-';
    document.getElementById('shopEditRequestButton').hidden = shopRegistrationState.status === 'APPROVED';
    document.getElementById('openSellerPortalButton').hidden = shopRegistrationState.status !== 'APPROVED';
}

function getShopStatusConfig(status) {
    if (status === 'APPROVED') {
        return { label: 'Đã duyệt', badge: 'ds-badge-success', title: 'Shop đã được phê duyệt', description: 'Bạn có thể chuyển sang Seller Portal để bắt đầu bán hàng.' };
    }
    if (status === 'REJECTED') {
        return { label: 'Cần bổ sung', badge: 'ds-badge-danger', title: 'Yêu cầu cần được cập nhật', description: 'Kiểm tra lại thông tin Shop và gửi lại yêu cầu.' };
    }
    return { label: 'Chờ duyệt', badge: 'ds-badge-warning', title: 'Yêu cầu đang chờ xét duyệt', description: 'Staff sẽ phản hồi yêu cầu trong vòng 1-3 ngày làm việc.' };
}

function prefillShopContact(profile) {
    document.getElementById('shopSupportEmail').value = profile.email || '';
    document.getElementById('shopSupportPhone').value = profile.phone || '';
}

function submitShopRegistration(event) {
    event.preventDefault();
    clearShopErrors();

    const data = {
        shopName: document.getElementById('shopName').value.trim(),
        category: document.getElementById('shopCategory').value,
        description: document.getElementById('shopDescription').value.trim(),
        supportEmail: document.getElementById('shopSupportEmail').value.trim(),
        supportPhone: document.getElementById('shopSupportPhone').value.trim()
    };

    let valid = true;
    valid = requireShopField('shopName', data.shopName, 'Vui lòng nhập tên Shop.') && valid;
    valid = requireShopField('shopCategory', data.category, 'Vui lòng chọn danh mục kinh doanh.') && valid;
    valid = requireShopField('shopDescription', data.description, 'Vui lòng mô tả Shop.') && valid;
    valid = requireShopField('shopSupportEmail', data.supportEmail, 'Vui lòng nhập email hỗ trợ.') && valid;

    if (!document.getElementById('shopPolicyConfirm').checked) {
        document.getElementById('shopPolicyConfirmError').textContent = 'Bạn cần xác nhận cam kết người bán.';
        valid = false;
    }
    if (!readKycApproved()) {
        showShopFormMessage('Bạn cần hoàn tất KYC trước khi gửi yêu cầu mở Shop.');
        valid = false;
    }
    if (!valid) return;

    saveShopRegistrationState({
        status: 'PENDING',
        code: `SHOP-${Date.now().toString().slice(-6)}`,
        submittedAt: new Date().toLocaleDateString('vi-VN'),
        ...data
    });
    renderShopRegistrationState();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function editShopRegistration() {
    document.getElementById('shopName').value = shopRegistrationState.shopName || '';
    document.getElementById('shopCategory').value = shopRegistrationState.category || '';
    document.getElementById('shopDescription').value = shopRegistrationState.description || '';
    document.getElementById('shopSupportEmail').value = shopRegistrationState.supportEmail || '';
    document.getElementById('shopSupportPhone').value = shopRegistrationState.supportPhone || '';
    saveShopRegistrationState({ status: 'NOT_SUBMITTED' });
    renderShopRegistrationState();
}

function requireShopField(fieldId, value, message) {
    if (value) return true;
    document.getElementById(`${fieldId}Error`).textContent = message;
    return false;
}

function clearShopErrors() {
    document.querySelectorAll('.shop-registration .ds-error-text').forEach(element => element.textContent = '');
    document.getElementById('shopFormMessage').hidden = true;
}

function showShopFormMessage(message) {
    const element = document.getElementById('shopFormMessage');
    element.textContent = message;
    element.hidden = false;
}
