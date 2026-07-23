(function () {
let shopAccountSidebar = null;
let shopRegistrationState = { status: 'NOT_SUBMITTED' };
let kycApproved = false;
let currentShopProfile = null;
let shopOpeningFee = 50000;


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

    await loadMainCategories();

    try {
        const feeResponse = await fetch('/api/public/config/shop-fee');
        if (feeResponse.ok) {
            const feeData = await feeResponse.json();
            if (feeData.shopOpeningFee) {
                shopOpeningFee = feeData.shopOpeningFee;
            }
        }
    } catch (e) {
        console.error('Không thể tải phí mở shop', e);
    }

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

    const formattedFee = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(shopOpeningFee);
    showCustomConfirmModal(
        `Phí đăng ký mở Shop là ${formattedFee}. Số tiền này sẽ được trừ vào ví của bạn. Bạn có đồng ý tiếp tục không?`, 
        () => {
            executeShopRegistrationSubmit(data);
        }
    );
}

async function executeShopRegistrationSubmit(data) {
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
            if (result.message && (result.message.startsWith('INSUFFICIENT_FUNDS') || result.message.includes('Số dư tài khoản không đủ'))) {
                const msg = result.message.startsWith('INSUFFICIENT_FUNDS') 
                    ? 'Tài khoản của bạn không đủ số dư để thanh toán phí mở Shop.' 
                    : result.message;
                    
                showShopFormMessage(`
                    <div>${msg}</div>
                    <div style="margin-top: 10px;">
                        <a href="/wallet/topup" style="display: inline-block; padding: 6px 16px; background: #dc3545; color: white; text-decoration: none; border-radius: 4px; font-weight: 500; font-size: 14px;"><i class="fa fa-plus-circle"></i> Nạp thêm tiền</a>
                    </div>
                `);
            } else {
                showShopFormMessage(result.message || 'Không thể gửi yêu cầu đăng ký shop.');
            }
        }
    } catch (error) {
        showShopFormMessage('Lỗi kết nối khi gửi yêu cầu đăng ký shop.');
    } finally {
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
    }
}

let allCategoriesList = [];

async function loadMainCategories() {
    try {
        const response = await fetch('/api/search/categories?parentOnly=true');
        if (response.ok) {
            const categories = await response.json();
            allCategoriesList = categories.map(cat => cat.name);
            
            const listEl = document.getElementById('shopCategoryDropdownList');
            const inputEl = document.getElementById('shopCategory');
            const btnEl = document.getElementById('shopCategoryDropdownBtn');
            
            if (listEl && inputEl && btnEl) {
                const renderDropdownItems = (items) => {
                    listEl.innerHTML = '';
                    if (items.length === 0) {
                        const li = document.createElement('li');
                        li.style.padding = '10px 16px';
                        li.style.fontSize = '13px';
                        li.style.color = '#94a3b8';
                        li.textContent = 'Không có kết quả';
                        listEl.appendChild(li);
                        return;
                    }
                    items.forEach(name => {
                        const li = document.createElement('li');
                        li.className = 'category-dropdown-item';
                        li.style.padding = '10px 16px';
                        li.style.fontSize = '13.5px';
                        li.style.color = '#1e293b';
                        li.style.cursor = 'pointer';
                        li.style.transition = 'background 0.15s ease';
                        li.textContent = name;
                        
                        li.addEventListener('mouseover', () => { li.style.background = '#f1f5f9'; });
                        li.addEventListener('mouseout', () => { li.style.background = 'transparent'; });
                        
                        li.addEventListener('mousedown', (e) => {
                            e.preventDefault();
                            inputEl.value = name;
                            listEl.style.display = 'none';
                        });
                        listEl.appendChild(li);
                    });
                };
                
                renderDropdownItems(allCategoriesList);
                
                btnEl.addEventListener('click', (e) => {
                    e.stopPropagation();
                    const isOpen = listEl.style.display === 'block';
                    listEl.style.display = isOpen ? 'none' : 'block';
                    if (!isOpen) {
                        renderDropdownItems(allCategoriesList);
                    }
                });
                
                inputEl.addEventListener('input', () => {
                    const text = inputEl.value.trim().toLowerCase();
                    if (text === '') {
                        listEl.style.display = 'none';
                        return;
                    }
                    const filtered = allCategoriesList.filter(name => name.toLowerCase().includes(text));
                    listEl.style.display = 'block';
                    renderDropdownItems(filtered);
                });
                
                document.addEventListener('click', (e) => {
                    if (!e.target.closest('.custom-select-search')) {
                        listEl.style.display = 'none';
                    }
                });
            }
        }
    } catch (e) {
        console.error('Không thể tải danh mục kinh doanh chính', e);
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
    element.innerHTML = message;
    element.hidden = false;
}

function showCustomConfirmModal(message, onConfirm) {
    let overlay = document.getElementById('customConfirmOverlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'customConfirmOverlay';
        overlay.style = 'position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 99999;';
        
        const modalBox = document.createElement('div');
        modalBox.style = 'background: white; padding: 24px; border-radius: 12px; max-width: 400px; width: 90%; text-align: center; box-shadow: 0 4px 12px rgba(0,0,0,0.15);';
        
        const icon = document.createElement('div');
        icon.innerHTML = '<i class="fa fa-info-circle" style="font-size: 40px; color: #007bff; margin-bottom: 16px;"></i>';
        
        const title = document.createElement('h4');
        title.textContent = 'Xác nhận mở Shop';
        title.style = 'margin-top: 0; margin-bottom: 12px; font-weight: 600; color: #333;';
        
        const text = document.createElement('p');
        text.id = 'customConfirmText';
        text.style = 'color: #555; margin-bottom: 24px; font-size: 15px; line-height: 1.5;';
        
        const btnContainer = document.createElement('div');
        btnContainer.style = 'display: flex; gap: 12px; justify-content: center;';
        
        const btnCancel = document.createElement('button');
        btnCancel.textContent = 'Huỷ';
        btnCancel.style = 'padding: 8px 20px; border-radius: 6px; border: 1px solid #ddd; background: #f8f9fa; cursor: pointer; font-weight: 500; color: #333; font-size: 14px;';
        btnCancel.onclick = () => { overlay.style.display = 'none'; };
        
        const btnOk = document.createElement('button');
        btnOk.id = 'customConfirmOkBtn';
        btnOk.textContent = 'Đồng ý';
        btnOk.style = 'padding: 8px 20px; border-radius: 6px; border: none; background: #007bff; color: white; cursor: pointer; font-weight: 500; box-shadow: 0 2px 4px rgba(0,123,255,0.2); font-size: 14px;';
        
        btnContainer.appendChild(btnCancel);
        btnContainer.appendChild(btnOk);
        
        modalBox.appendChild(icon);
        modalBox.appendChild(title);
        modalBox.appendChild(text);
        modalBox.appendChild(btnContainer);
        overlay.appendChild(modalBox);
        document.body.appendChild(overlay);
    }
    
    document.getElementById('customConfirmText').textContent = message;
    document.getElementById('customConfirmOkBtn').onclick = () => {
        overlay.style.display = 'none';
        onConfirm();
    };
    overlay.style.display = 'flex';
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
