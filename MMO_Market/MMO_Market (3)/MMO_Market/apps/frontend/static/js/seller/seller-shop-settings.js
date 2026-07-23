const SHOP_STATUS_STORAGE_KEY = 'mmoMarketShopStatusMock';

document.addEventListener('DOMContentLoaded', initializeShopSettings);

async function initializeShopSettings() {
    const form = document.getElementById('shopInfoForm');
    if (!form) return;

    form.addEventListener('submit', saveShopInfo);
    const toggleBtn = document.getElementById('toggleShopStatusButton');
    if (toggleBtn) toggleBtn.addEventListener('click', toggleShopStatus);
    await loadShopInfo();
}

async function loadShopInfo() {
    try {
        const response = await sellerFetch('/shop-info');
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || 'Không thể tải thông tin cửa hàng.');

        document.getElementById('shopName').value = data.shopName || '';
        document.getElementById('shopDesc').value = data.description || '';
        
        document.getElementById('bankName').value = data.bankName || '';

        document.getElementById('accountNumber').value = data.accountNumber || '';
        document.getElementById('accountHolder').value = data.accountHolder || '';
        document.getElementById('branch').value = data.branch || '';

        const lvlBadge = document.getElementById('shop-info-level-badge');
        if (lvlBadge) {
            const lvl = data.shopLevel !== undefined ? data.shopLevel : 1;
            if (lvl === 0) {
                lvlBadge.innerHTML = `<span style="color: #dc2626; background: #fee2e2; padding: 4px 10px; border-radius: 6px; display: inline-block; font-weight: 700; font-size: 13px;"><i class="fa fa-exclamation-triangle"></i> Shop Cảnh Cáo (Level 0)</span>`;
            } else if (lvl === 2) {
                lvlBadge.innerHTML = `<span style="color: #16a34a; background: #dcfce7; padding: 4px 10px; border-radius: 6px; display: inline-block; font-weight: 700; font-size: 13px;"><i class="fa fa-check-circle"></i> Shop Uy Tín (Level 2)</span>`;
            } else {
                lvlBadge.innerHTML = `<span style="color: #0284c7; background: #e0f2fe; padding: 4px 10px; border-radius: 6px; display: inline-block; font-weight: 700; font-size: 13px;"><i class="fa fa-star-o"></i> Shop Mới (Level 1)</span>`;
            }
        }

        renderShopStatus(readEffectiveShopStatus(data.shopStatus), data.suspendedUntil);
    } catch (error) {
        showShopError(error.message || 'Không thể tải thông tin cửa hàng.');
    }
}

function readEffectiveShopStatus(apiStatus) {
    const normalized = String(apiStatus || 'Active').toUpperCase();
    if (normalized === 'BANNED' || normalized === 'PERMANENT_BANNED') return 'BANNED';
    if (normalized === 'LOCKED' || normalized === 'INDEFINITE_LOCKED') return 'CLOSED';
    if (normalized === 'PENDING') return 'PENDING';
    if (normalized === 'SUSPENDED' || normalized === 'TEMP_LOCKED' || normalized === 'TEMPORARILY_CLOSED') return 'TEMPORARILY_CLOSED';
    return 'ACTIVE';
}

function renderShopStatus(status, suspendedUntilStr) {
    const badge = document.getElementById('shopStatusBadge');
    const panel = document.querySelector('.shop-status-panel');
    const icon = document.getElementById('shopStatusIcon');
    const title = document.getElementById('shopStatusTitle');
    const description = document.getElementById('shopStatusDescription');
    const toggleButton = document.getElementById('toggleShopStatusButton');
    const sidebarStatus = document.querySelector('[data-seller-shop-status]');

    panel.className = 'shop-status-panel';

    const statusConfig = {
        ACTIVE: ['ds-badge ds-badge-success', 'Đang hoạt động', 'fa-check-circle', 'Shop đang hoạt động', 'Sản phẩm đang hiển thị và khách hàng có thể tạo đơn mới.', '', 'Hoạt động'],
        TEMPORARILY_CLOSED: ['ds-badge ds-badge-warning', 'Tạm ngưng', 'fa-pause-circle', 'Shop đang tạm ngưng', 'Shop tạm ngưng nhận đơn mới. Sản phẩm sẽ tự động mở lại sau khi hết thời hạn tạm ngưng.', '', 'Tạm ngưng'],
        PENDING: ['ds-badge ds-badge-warning', 'Chờ duyệt', 'fa-clock-o', 'Shop đang chờ duyệt', 'Staff đang xét duyệt trạng thái hoạt động của Shop.', '', 'Chờ duyệt'],
        BANNED: ['ds-badge ds-badge-danger', 'Khóa vĩnh viễn', 'fa-ban', 'Shop đang bị hạn chế', 'Liên hệ Staff để được hỗ trợ về trạng thái Shop.', '', 'Khóa vĩnh viễn'],
        CLOSED: ['ds-badge ds-badge-danger', 'Tạm khóa', 'fa-lock', 'Shop đã bị tạm khóa', 'Liên hệ Staff nếu bạn cần hỗ trợ mở lại Shop.', '', 'Tạm khóa']
    };
    const config = statusConfig[status] || statusConfig.ACTIVE;

    badge.className = config[0];
    badge.textContent = config[1];
    icon.innerHTML = `<i class="fa ${config[2]}" aria-hidden="true"></i>`;
    title.textContent = config[3];
    description.textContent = config[4];
    if (toggleButton) {
        toggleButton.textContent = config[5];
        toggleButton.hidden = !config[5];
    }
    if (sidebarStatus) sidebarStatus.textContent = `Trạng thái: ${config[6]}`;

    if (status === 'TEMPORARILY_CLOSED' || status === 'PENDING') panel.classList.add('is-paused');
    if (status === 'BANNED' || status === 'CLOSED') panel.classList.add('is-closed');

    startSellerCountdown(suspendedUntilStr);
}

let sellerCountdownInterval = null;

function startSellerCountdown(suspendedUntilStr) {
    if (sellerCountdownInterval) clearInterval(sellerCountdownInterval);
    const alertBox = document.getElementById('sellerSuspendedAlert');
    const untilText = document.getElementById('sellerSuspendedUntilText');
    const countDisplay = document.getElementById('sellerSuspendedCountdown');

    if (!suspendedUntilStr) {
        if (alertBox) alertBox.style.display = 'none';
        return;
    }

    const targetTime = new Date(suspendedUntilStr).getTime();
    if (isNaN(targetTime)) {
        if (alertBox) alertBox.style.display = 'none';
        return;
    }

    if (alertBox) alertBox.style.display = 'flex';

    try {
        const dt = new Date(suspendedUntilStr);
        const formatted = dt.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' });
        if (untilText) untilText.textContent = 'Thời hạn tạm ngưng: Đến ' + formatted;
    } catch (ex) {}

    function update() {
        const now = new Date().getTime();
        const diff = targetTime - now;

        if (diff <= 0) {
            clearInterval(sellerCountdownInterval);
            if (countDisplay) countDisplay.textContent = 'Tự động mở lại: Đang kích hoạt...';
            setTimeout(() => {
                window.location.reload();
            }, 1500);
            return;
        }

        const days = Math.floor(diff / (1000 * 60 * 60 * 24));
        const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((diff % (1000 * 60)) / 1000);

        let str = '';
        if (days > 0) {
            str += `${days} ngày `;
        }
        str += `${String(hours).padStart(2, '0')} giờ ${String(minutes).padStart(2, '0')} phút ${String(seconds).padStart(2, '0')} giây`;

        if (countDisplay) countDisplay.textContent = 'Tự động mở lại sau: ' + str;
    }

    update();
    sellerCountdownInterval = setInterval(update, 1000);
}

async function toggleShopStatus() {
    const toggleButton = document.getElementById('toggleShopStatusButton');
    toggleButton.disabled = true;
    try {
        const response = await sellerFetch('/shop-status', {
            method: 'PUT'
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || 'Không thể thay đổi trạng thái cửa hàng.');
        
        showShopToast(data.message || 'Cập nhật trạng thái cửa hàng thành công.');
        await loadShopInfo();
    } catch (error) {
        showShopError(error.message || 'Không thể thay đổi trạng thái cửa hàng.');
    } finally {
        toggleButton.disabled = false;
    }
}

async function saveShopInfo(event) {
    event.preventDefault();
    clearShopMessages();

    const shopName = document.getElementById('shopName').value.trim();
    if (!shopName) {
        document.getElementById('shopNameError').textContent = 'Tên cửa hàng không được để trống.';
        return;
    }

    const button = document.getElementById('saveShopInfoButton');
    button.disabled = true;
    try {
        const response = await sellerFetch('/shop-info', {
            method: 'PUT',
            body: JSON.stringify({
                shopName,
                description: document.getElementById('shopDesc').value.trim(),
                bankName: document.getElementById('bankName').value.trim(),
                accountNumber: document.getElementById('accountNumber').value.trim(),
                branch: document.getElementById('branch').value.trim()
            })
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || 'Không thể lưu thông tin cửa hàng.');
        showShopToast(data.message || 'Đã lưu thông tin cửa hàng.');
        await initSellerLayout();
    } catch (error) {
        showShopError(error.message || 'Không thể lưu thông tin cửa hàng.');
    } finally {
        button.disabled = false;
    }
}

function clearShopMessages() {
    document.getElementById('shopNameError').textContent = '';
    document.getElementById('shopSettingsError').hidden = true;
}

function showShopError(message) {
    const error = document.getElementById('shopSettingsError');
    error.textContent = message;
    error.hidden = false;
}

function showShopToast(message) {
    const toast = document.createElement('div');
    toast.className = 'ds-toast ds-toast-success';
    toast.innerHTML = `<div><p class="ds-toast-title">Thành công</p><p class="ds-toast-message">${message}</p></div>`;
    document.getElementById('shopToastContainer').appendChild(toast);
    window.setTimeout(() => toast.remove(), 3500);
}
