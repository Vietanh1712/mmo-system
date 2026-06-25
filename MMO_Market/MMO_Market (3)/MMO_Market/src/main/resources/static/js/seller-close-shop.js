const CLOSE_SHOP_STATUS_STORAGE_KEY = 'mmoMarketShopStatusMock';

document.addEventListener('DOMContentLoaded', initializeCloseShopPage);

function initializeCloseShopPage() {
    renderCloseShopSidebarStatus();
    document.getElementById('closeShopReason').addEventListener('change', toggleOtherReasonField);
    document.getElementById('toggleCloseShopPassword').addEventListener('click', toggleCloseShopPassword);
    document.getElementById('closeShopForm').addEventListener('submit', submitCloseShopRequest);
}

function toggleOtherReasonField() {
    const isOther = document.getElementById('closeShopReason').value === 'OTHER';
    document.getElementById('otherReasonField').hidden = !isOther;
    if (!isOther) {
        document.getElementById('closeShopOtherReason').value = '';
        document.getElementById('closeShopOtherReasonError').textContent = '';
    }
}

function toggleCloseShopPassword() {
    const input = document.getElementById('closeShopPassword');
    const button = document.getElementById('toggleCloseShopPassword');
    const shouldShow = input.type === 'password';
    input.type = shouldShow ? 'text' : 'password';
    button.innerHTML = `<i class="fa ${shouldShow ? 'fa-eye-slash' : 'fa-eye'}" aria-hidden="true"></i>`;
    button.setAttribute('aria-label', shouldShow ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
}

function submitCloseShopRequest(event) {
    event.preventDefault();
    clearCloseShopErrors();

    const reason = document.getElementById('closeShopReason').value;
    const otherReason = document.getElementById('closeShopOtherReason').value.trim();
    const password = document.getElementById('closeShopPassword').value;
    let valid = true;

    if (!reason) {
        document.getElementById('closeShopReasonError').textContent = 'Vui lòng chọn lý do đóng Shop.';
        valid = false;
    }
    if (reason === 'OTHER' && !otherReason) {
        document.getElementById('closeShopOtherReasonError').textContent = 'Vui lòng nhập lý do cụ thể.';
        valid = false;
    }
    if (!password) {
        document.getElementById('closeShopPasswordError').textContent = 'Vui lòng nhập mật khẩu để xác nhận.';
        valid = false;
    }
    if (!valid) return;

    localStorage.setItem(CLOSE_SHOP_STATUS_STORAGE_KEY, 'CLOSED');
    renderCloseShopSidebarStatus();
    showCloseShopToast('Yêu cầu đóng Shop đã được ghi nhận.');
    document.querySelector('#closeShopForm button[type="submit"]').disabled = true;
}

function clearCloseShopErrors() {
    document.querySelectorAll('#closeShopForm .ds-error-text').forEach(element => element.textContent = '');
}

function renderCloseShopSidebarStatus() {
    const sidebarStatus = document.querySelector('[data-seller-shop-status]');
    if (!sidebarStatus) return;
    const status = localStorage.getItem(CLOSE_SHOP_STATUS_STORAGE_KEY) || 'ACTIVE';
    sidebarStatus.textContent = status === 'CLOSED' ? 'Trạng thái: Đã đóng' : 'Trạng thái: Active';
}

function showCloseShopToast(message) {
    const toast = document.createElement('div');
    toast.className = 'ds-toast ds-toast-success';
    toast.innerHTML = `<div><p class="ds-toast-title">Đã gửi yêu cầu</p><p class="ds-toast-message">${message}</p></div>`;
    document.getElementById('shopToastContainer').appendChild(toast);
    window.setTimeout(() => toast.remove(), 3500);
}
