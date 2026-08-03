// Khóa LocalStorage dùng để giả lập lưu trữ trạng thái đóng/mở của cửa hàng
const CLOSE_SHOP_STATUS_STORAGE_KEY = 'mmoMarketShopStatusMock';

// Lắng nghe sự kiện DOMContentLoaded để tiến hành khởi tạo trang
document.addEventListener('DOMContentLoaded', initializeCloseShopPage);

/**
 * Khởi tạo trang Đóng cửa hàng, cập nhật giao diện trạng thái và đăng ký sự kiện cho các thành phần UI.
 */
function initializeCloseShopPage() {
    renderCloseShopSidebarStatus();
    document.getElementById('closeShopReason').addEventListener('change', toggleOtherReasonField);
    document.getElementById('toggleCloseShopPassword').addEventListener('click', toggleCloseShopPassword);
    document.getElementById('closeShopForm').addEventListener('submit', submitCloseShopRequest);
}

/**
 * Ẩn/Hiện trường nhập lý do khác (textarea) khi người dùng chọn tùy chọn "Lý do khác" trong dropdown.
 */
function toggleOtherReasonField() {
    const isOther = document.getElementById('closeShopReason').value === 'OTHER';
    document.getElementById('otherReasonField').hidden = !isOther;
    if (!isOther) {
        document.getElementById('closeShopOtherReason').value = '';
        document.getElementById('closeShopOtherReasonError').textContent = '';
    }
}

/**
 * Bật/Tắt chế độ hiển thị mật khẩu (Ẩn/Hiện mật khẩu nhập vào).
 */
function toggleCloseShopPassword() {
    const input = document.getElementById('closeShopPassword');
    const button = document.getElementById('toggleCloseShopPassword');
    const shouldShow = input.type === 'password';
    input.type = shouldShow ? 'text' : 'password';
    button.innerHTML = `<i class="fa ${shouldShow ? 'fa-eye-slash' : 'fa-eye'}" aria-hidden="true"></i>`;
    button.setAttribute('aria-label', shouldShow ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
}

/**
 * Xử lý việc gửi yêu cầu đóng Shop.
 * Kiểm tra tính hợp lệ của lý do đóng cửa hàng và mật khẩu xác nhận.
 *
 * @param {Event} event Sự kiện submit form
 */
function submitCloseShopRequest(event) {
    event.preventDefault();
    clearCloseShopErrors();

    const reason = document.getElementById('closeShopReason').value;
    const otherReason = document.getElementById('closeShopOtherReason').value.trim();
    const password = document.getElementById('closeShopPassword').value;
    let valid = true;

    // Kiểm tra tính hợp lệ của form
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

    // Giả lập lưu trạng thái đóng Shop vào LocalStorage
    localStorage.setItem(CLOSE_SHOP_STATUS_STORAGE_KEY, 'CLOSED');
    renderCloseShopSidebarStatus();
    showCloseShopToast('Yêu cầu đóng Shop đã được ghi nhận.');
    document.querySelector('#closeShopForm button[type="submit"]').disabled = true;
}

/**
 * Xóa tất cả các thông báo lỗi hiển thị trên form.
 */
function clearCloseShopErrors() {
    document.querySelectorAll('#closeShopForm .ds-error-text').forEach(element => element.textContent = '');
}

/**
 * Hiển thị trạng thái hoạt động hiện tại của Shop trên thanh Sidebar bên cạnh.
 */
function renderCloseShopSidebarStatus() {
    const sidebarStatus = document.querySelector('[data-seller-shop-status]');
    if (!sidebarStatus) return;
    const status = localStorage.getItem(CLOSE_SHOP_STATUS_STORAGE_KEY) || 'ACTIVE';
    sidebarStatus.textContent = status === 'CLOSED' ? 'Trạng thái: Đã đóng' : 'Trạng thái: Hoạt động';
}

/**
 * Hiển thị Toast thông báo trạng thái gửi yêu cầu thành công trên giao diện.
 *
 * @param {string} message Nội dung thông báo
 */
function showCloseShopToast(message) {
    const toast = document.createElement('div');
    toast.className = 'ds-toast ds-toast-success';
    toast.innerHTML = `<div><p class="ds-toast-title">Đã gửi yêu cầu</p><p class="ds-toast-message">${message}</p></div>`;
    document.getElementById('shopToastContainer').appendChild(toast);
    window.setTimeout(() => toast.remove(), 3500);
}
