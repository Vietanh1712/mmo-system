const SHOP_STATUS_STORAGE_KEY = 'mmoMarketShopStatusMock';

document.addEventListener('DOMContentLoaded', initializeShopSettings);

async function initializeShopSettings() {
    const form = document.getElementById('shopInfoForm');
    if (!form) return;

    form.addEventListener('submit', saveShopInfo);
    document.getElementById('toggleShopStatusButton').addEventListener('click', toggleShopStatus);
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

        renderShopStatus(readEffectiveShopStatus(data.shopStatus));
    } catch (error) {
        showShopError(error.message || 'Không thể tải thông tin cửa hàng.');
    }
}

function readEffectiveShopStatus(apiStatus) {
    const localStatus = localStorage.getItem(SHOP_STATUS_STORAGE_KEY);
    if (localStatus === 'TEMPORARILY_CLOSED' || localStatus === 'CLOSED') return localStatus;

    const normalized = String(apiStatus || 'Active').toUpperCase();
    if (normalized === 'BANNED') return 'BANNED';
    if (normalized === 'PENDING') return 'PENDING';
    return 'ACTIVE';
}

function renderShopStatus(status) {
    const badge = document.getElementById('shopStatusBadge');
    const panel = document.querySelector('.shop-status-panel');
    const icon = document.getElementById('shopStatusIcon');
    const title = document.getElementById('shopStatusTitle');
    const description = document.getElementById('shopStatusDescription');
    const toggleButton = document.getElementById('toggleShopStatusButton');
    const sidebarStatus = document.querySelector('[data-seller-shop-status]');

    panel.className = 'shop-status-panel';
    toggleButton.hidden = false;

    const statusConfig = {
        ACTIVE: ['ds-badge ds-badge-success', 'Đang hoạt động', 'fa-check-circle', 'Shop đang hoạt động', 'Sản phẩm đang hiển thị và khách hàng có thể tạo đơn mới.', 'Tạm đóng cửa hàng', 'Active'],
        TEMPORARILY_CLOSED: ['ds-badge ds-badge-warning', 'Tạm đóng', 'fa-pause-circle', 'Shop đang tạm đóng', 'Sản phẩm vẫn được lưu nhưng khách hàng không thể tạo đơn mới.', 'Mở lại cửa hàng', 'Tạm đóng'],
        PENDING: ['ds-badge ds-badge-warning', 'Chờ duyệt', 'fa-clock-o', 'Shop đang chờ duyệt', 'Staff đang xét duyệt trạng thái hoạt động của Shop.', '', 'Pending'],
        BANNED: ['ds-badge ds-badge-danger', 'Bị hạn chế', 'fa-ban', 'Shop đang bị hạn chế', 'Liên hệ Staff để được hỗ trợ về trạng thái Shop.', '', 'Banned'],
        CLOSED: ['ds-badge ds-badge-danger', 'Đã đóng', 'fa-lock', 'Shop đã đóng', 'Liên hệ Staff nếu bạn cần hỗ trợ mở lại Shop.', '', 'Đã đóng']
    };
    const config = statusConfig[status] || statusConfig.ACTIVE;

    badge.className = config[0];
    badge.textContent = config[1];
    icon.innerHTML = `<i class="fa ${config[2]}" aria-hidden="true"></i>`;
    title.textContent = config[3];
    description.textContent = config[4];
    toggleButton.textContent = config[5];
    toggleButton.hidden = !config[5];
    if (sidebarStatus) sidebarStatus.textContent = `Trạng thái: ${config[6]}`;

    if (status === 'TEMPORARILY_CLOSED' || status === 'PENDING') panel.classList.add('is-paused');
    if (status === 'BANNED' || status === 'CLOSED') panel.classList.add('is-closed');
}

function toggleShopStatus() {
    const current = localStorage.getItem(SHOP_STATUS_STORAGE_KEY);
    const nextStatus = current === 'TEMPORARILY_CLOSED' ? 'ACTIVE' : 'TEMPORARILY_CLOSED';
    if (nextStatus === 'ACTIVE') {
        localStorage.removeItem(SHOP_STATUS_STORAGE_KEY);
    } else {
        localStorage.setItem(SHOP_STATUS_STORAGE_KEY, nextStatus);
    }
    renderShopStatus(nextStatus);
    showShopToast(nextStatus === 'ACTIVE' ? 'Đã mở lại cửa hàng trên giao diện.' : 'Cửa hàng đã tạm đóng trên giao diện.');
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
