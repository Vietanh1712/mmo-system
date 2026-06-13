const SHOP_STATUS_STORAGE_KEY = 'mmoMarketShopStatusMock';
const SHOP_INFO_STORAGE_KEY = 'mmoMarketShopInfoMock';

document.addEventListener('DOMContentLoaded', initializeShopSettings);

function initializeShopSettings() {
    restoreShopInfo();
    renderShopStatus(readShopStatus());

    document.getElementById('shopInfoForm').addEventListener('submit', saveShopInfo);
    document.getElementById('toggleShopStatusButton').addEventListener('click', toggleShopStatus);
}

function readShopStatus() {
    return localStorage.getItem(SHOP_STATUS_STORAGE_KEY) || 'ACTIVE';
}

function saveShopStatus(status) {
    localStorage.setItem(SHOP_STATUS_STORAGE_KEY, status);
    renderShopStatus(status);
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

    if (status === 'TEMPORARILY_CLOSED') {
        badge.className = 'ds-badge ds-badge-warning';
        badge.textContent = 'Tạm đóng';
        panel.classList.add('is-paused');
        icon.innerHTML = '<i class="fa fa-pause-circle" aria-hidden="true"></i>';
        title.textContent = 'Shop đang tạm đóng';
        description.textContent = 'Sản phẩm vẫn được lưu nhưng khách hàng không thể tạo đơn mới.';
        toggleButton.textContent = 'Mở lại cửa hàng';
        sidebarStatus.textContent = 'Trạng thái: Tạm đóng';
        return;
    }

    if (status === 'CLOSED') {
        badge.className = 'ds-badge ds-badge-danger';
        badge.textContent = 'Đã đóng';
        panel.classList.add('is-closed');
        icon.innerHTML = '<i class="fa fa-lock" aria-hidden="true"></i>';
        title.textContent = 'Shop đã đóng vĩnh viễn';
        description.textContent = 'Liên hệ Staff nếu bạn cần được hỗ trợ về trạng thái Shop.';
        toggleButton.hidden = true;
        sidebarStatus.textContent = 'Trạng thái: Đã đóng';
        return;
    }

    badge.className = 'ds-badge ds-badge-success';
    badge.textContent = 'Đang hoạt động';
    icon.innerHTML = '<i class="fa fa-check-circle" aria-hidden="true"></i>';
    title.textContent = 'Shop đang hoạt động';
    description.textContent = 'Sản phẩm đang hiển thị và khách hàng có thể tạo đơn mới.';
    toggleButton.textContent = 'Tạm đóng cửa hàng';
    sidebarStatus.textContent = 'Trạng thái: Active';
}

function toggleShopStatus() {
    const nextStatus = readShopStatus() === 'ACTIVE' ? 'TEMPORARILY_CLOSED' : 'ACTIVE';
    saveShopStatus(nextStatus);
    showShopToast(nextStatus === 'ACTIVE' ? 'Đã mở lại cửa hàng.' : 'Cửa hàng đã tạm đóng.');
}

function saveShopInfo(event) {
    event.preventDefault();
    const shopName = document.getElementById('shopName').value.trim();
    const error = document.getElementById('shopNameError');
    error.textContent = '';

    if (!shopName) {
        error.textContent = 'Tên cửa hàng không được để trống.';
        return;
    }

    const data = {
        shopName,
        supportEmail: document.getElementById('supportEmail').value.trim(),
        description: document.getElementById('shopDesc').value.trim(),
        bankName: document.getElementById('bankName').value,
        accountNumber: document.getElementById('accountNumber').value.trim(),
        accountHolder: document.getElementById('accountHolder').value.trim()
    };
    localStorage.setItem(SHOP_INFO_STORAGE_KEY, JSON.stringify(data));
    showShopToast('Đã lưu thông tin cửa hàng.');
}

function restoreShopInfo() {
    try {
        const data = JSON.parse(localStorage.getItem(SHOP_INFO_STORAGE_KEY) || 'null');
        if (!data) return;
        document.getElementById('shopName').value = data.shopName || '';
        document.getElementById('supportEmail').value = data.supportEmail || '';
        document.getElementById('shopDesc').value = data.description || '';
        document.getElementById('bankName').value = data.bankName || 'Vietcombank';
        document.getElementById('accountNumber').value = data.accountNumber || '';
        document.getElementById('accountHolder').value = data.accountHolder || '';
    } catch {
        localStorage.removeItem(SHOP_INFO_STORAGE_KEY);
    }
}

function showShopToast(message, isError = false) {
    const toast = document.createElement('div');
    toast.className = `ds-toast ${isError ? 'ds-toast-error' : 'ds-toast-success'}`;
    toast.innerHTML = `<div><p class="ds-toast-title">${isError ? 'Đã đóng cửa hàng' : 'Thành công'}</p><p class="ds-toast-message">${message}</p></div>`;
    document.getElementById('shopToastContainer').appendChild(toast);
    window.setTimeout(() => toast.remove(), 3500);
}
