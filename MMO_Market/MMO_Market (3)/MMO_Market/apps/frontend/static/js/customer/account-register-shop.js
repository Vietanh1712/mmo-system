(function () {
let shopAccountSidebar = null;
let shopRegistrationState = { status: 'NOT_SUBMITTED' };
let kycApproved = false;
let currentShopProfile = null;

registerAccountPage('/js/account-register-shop.js', initializeShopRegistrationPage);
registerAccountPage('/js/customer/account-register-shop.js', initializeShopRegistrationPage);

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
        currentShopProfile = profile;
        if (!allowShopRegistration(profile.role)) {
            return;
        }
        shopAccountSidebar.render(profile);
        prefillShopContact(profile);
    } catch (error) {
        showShopFormMessage(error.message || 'Không thể tải thông tin tài khoản.');
    }

    await loadKycStatus();
    await loadShopRegistrationState();
    renderShopRegistrationState();
}

function allowShopRegistration(roleValue) {
    const role = shopAccountSidebar.normalizeRole(roleValue);
    if (role === 'Customer' || role === 'Seller') return true;

    const targetByRole = {
        Staff: '/staff/dashboard',
        Admin: '/admin/users'
    };
    if (targetByRole[role]) {
        window.location.replace(targetByRole[role]);
        return false;
    }
    return true;
}

async function loadKycStatus() {
    if (isApprovedKycStatus(currentShopProfile?.kycStatus)) {
        kycApproved = true;
        return;
    }

    try {
        const response = await authFetch('/v1/kyc/me');
        if (response.ok) {
            const data = await response.json();
            // data is an array of KYC history
            if (Array.isArray(data)) {
                kycApproved = data.some(item => isApprovedKycStatus(item.status || item.kycStatus));
            } else {
                kycApproved = isApprovedKycStatus(data.status || data.kycStatus);
            }
        } else {
            kycApproved = false;
        }
    } catch {
        kycApproved = false;
    }
}

function isApprovedKycStatus(status) {
    const normalized = String(status || '').trim().toUpperCase();
    return normalized === 'APPROVED' || normalized === 'VERIFIED';
}

async function loadShopRegistrationState() {
    try {
        const response = await authFetch('/v1/shop-registrations/me');
        if (response.ok) {
            const data = await response.json();
            if (data && data.status !== 'NOT_SUBMITTED') {
                shopRegistrationState = data;
                return;
            }
        }
        shopRegistrationState = { status: 'NOT_SUBMITTED' };
    } catch {
        shopRegistrationState = { status: 'NOT_SUBMITTED' };
    }
}

function renderShopRegistrationState() {
    const badge = document.getElementById('shopRegistrationStatus');
    const form = document.getElementById('shopRegistrationForm');
    const summary = document.getElementById('shopRequestSummary');
    const requirement = document.getElementById('kycRequirement');

    requirement.classList.toggle('shop-requirement--done', kycApproved);
    requirement.querySelector('i').className = kycApproved ? 'fa fa-check-circle' : 'fa fa-clock-o';
    document.getElementById('kycRequirementText').textContent =
        kycApproved ? 'Danh tính đã được xác minh.' : 'Chưa xác minh danh tính.';
    document.getElementById('shopRequirementProgress').textContent = kycApproved ? '3/3 hoàn tất' : '2/3 hoàn tất';

    const hasRequest = shopRegistrationState.status !== 'NOT_SUBMITTED';
    summary.hidden = !hasRequest;
    form.hidden = hasRequest;

    if (!hasRequest) {
        badge.textContent = 'Chưa đăng ký';
        badge.className = 'ds-badge ds-badge-muted';
        document.getElementById('shopSubmitButton').disabled = !kycApproved;
        return;
    }

    const config = getShopStatusConfig(shopRegistrationState.status);
    badge.textContent = config.label;
    badge.className = `ds-badge ${config.badge}`;
    document.getElementById('shopSummaryTitle').textContent = config.title;
    document.getElementById('shopSummaryDescription').textContent = config.description;
    document.getElementById('shopRequestCode').textContent = shopRegistrationState.code || '-';
    
    let formattedDate = '-';
    if (shopRegistrationState.submittedAt) {
        try {
             formattedDate = new Date(shopRegistrationState.submittedAt).toLocaleDateString('vi-VN');
        } catch(e) {}
    }
    document.getElementById('shopRequestDate').textContent = formattedDate;
    
    document.getElementById('shopRequestName').textContent = shopRegistrationState.shopName || '-';
    document.getElementById('shopRequestCategory').textContent = shopRegistrationState.category || '-';
    document.getElementById('shopEditRequestButton').style.display = shopRegistrationState.status === 'APPROVED' ? 'none' : 'inline-flex';
    document.getElementById('openSellerPortalButton').style.display = shopRegistrationState.status === 'APPROVED' ? 'inline-flex' : 'none';

    const rejectionAlert = document.getElementById('shopRejectionReasonAlert');
    if (shopRegistrationState.status === 'REJECTED' && shopRegistrationState.rejectionReason) {
        document.getElementById('shopRejectionReasonText').textContent = shopRegistrationState.rejectionReason;
        rejectionAlert.hidden = false;
    } else {
        rejectionAlert.hidden = true;
    }
}

function getShopStatusConfig(status) {
    if (status === 'APPROVED') {
        return { label: 'Đã duyệt', badge: 'ds-badge-success', title: 'Shop đã được phê duyệt', description: 'Bạn vui lòng đăng nhập lại để vào Seller Portal.' };
    }
    if (status === 'REJECTED') {
        return { label: 'Bị từ chối', badge: 'ds-badge-danger', title: 'Yêu cầu bị từ chối', description: 'Kiểm tra lại thông tin Shop và gửi lại yêu cầu mới.' };
    }
    return { label: 'Chờ duyệt', badge: 'ds-badge-warning', title: 'Yêu cầu đang chờ xét duyệt', description: 'Staff sẽ phản hồi yêu cầu trong vòng 1-3 ngày làm việc.' };
}

function prefillShopContact(profile) {
    document.getElementById('shopSupportEmail').value = profile.email || '';
    document.getElementById('shopSupportPhone').value = profile.phone || '';
}

async function submitShopRegistration(event) {
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
    if (!kycApproved) {
        showShopFormMessage('Bạn cần hoàn tất KYC trước khi gửi yêu cầu mở Shop.');
        valid = false;
    }
    if (!valid) return;

    const submitBtn = document.getElementById('shopSubmitButton');
    const originalText = submitBtn.innerHTML;
    submitBtn.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang gửi...';
    submitBtn.disabled = true;

    try {
        const response = await authFetch('/v1/profile/register-shop', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        const result = await response.json();
        
        if (response.ok) {
            // Đăng ký thành công và đã được trừ tiền
            shopRegistrationState = {
                status: 'APPROVED',
                code: `SHOP-${Date.now().toString().slice(-6)}`,
                submittedAt: new Date().toLocaleDateString('vi-VN'),
                ...data
            };
            
            // Cập nhật lại số dư ví hiển thị ở sidebar và session
            if (result.balanceVnd !== undefined) {
                const sidebarBalance = document.getElementById('sidebarBalance');
                if (sidebarBalance) {
                    sidebarBalance.textContent = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(result.balanceVnd);
                }
                // Cập nhật user info lưu trong session
                const user = JSON.parse(sessionStorage.getItem('userInfo') || sessionStorage.getItem('user') || '{}');
                user.balanceVnd = result.balanceVnd;
                user.role = result.role;
                user.shopStatus = result.shopStatus;
                sessionStorage.setItem('userInfo', JSON.stringify(user));
                
                // Cập nhật lại sidebar
                if (shopAccountSidebar) {
                    shopAccountSidebar.render(result);
                }
            }

            renderShopRegistrationState();
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } else {
            showShopFormMessage(result.message || 'Không thể gửi yêu cầu đăng ký shop.');
        }
    } catch (error) {
        showShopFormMessage('Lỗi kết nối khi gửi yêu cầu đăng ký shop.');
    } finally {
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
    }
}

function editShopRegistration() {
    document.getElementById('shopName').value = shopRegistrationState.shopName || '';
    document.getElementById('shopCategory').value = shopRegistrationState.category || '';
    document.getElementById('shopDescription').value = shopRegistrationState.description || '';
    document.getElementById('shopSupportEmail').value = shopRegistrationState.supportEmail || '';
    document.getElementById('shopSupportPhone').value = shopRegistrationState.supportPhone || '';
    shopRegistrationState = { status: 'NOT_SUBMITTED' };
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

function registerAccountPage(scriptPath, initializer) {
    window.AccountPageInitializers = window.AccountPageInitializers || {};
    window.AccountPageInitializers[scriptPath] = initializer;
    if (document.currentScript?.dataset.accountPartial !== 'true' && !initializer.__shopRegistrationDomReadyBound) {
        initializer.__shopRegistrationDomReadyBound = true;
        document.addEventListener('DOMContentLoaded', initializer);
    }
}
})();
