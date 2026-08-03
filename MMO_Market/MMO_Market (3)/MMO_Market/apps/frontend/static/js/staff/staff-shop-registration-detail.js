document.addEventListener('DOMContentLoaded', () => {
    loadShopDetail();
});

let currentShopId = new URLSearchParams(window.location.search).get('id');

async function loadShopDetail() {
    if (!currentShopId) {
        showToast("Không tìm thấy mã đăng ký Shop", "danger");
        return;
    }

    try {
        const response = await authFetch(`/v1/shop-registrations/${currentShopId}`);
        if (!response.ok) throw new Error("Lỗi tải chi tiết đăng ký Shop");
        const data = await response.json();

        document.getElementById('shopRequestCodeDisplay').textContent = `#${data.code || data.id}`;
        document.getElementById('shopRequestCodeSubtitle').textContent = 'Mã Shop: ' + (data.code || data.id);
        document.getElementById('shopCode').textContent = data.code || data.id;
        document.getElementById('shopName').textContent = data.shopName || '-';
        document.getElementById('shopDescription').textContent = data.description || '-';
        document.getElementById('shopOwnerName').textContent = data.ownerName || '-';
        document.getElementById('shopEmail').textContent = data.supportEmail || '-';
        document.getElementById('shopPhone').textContent = data.supportPhone || '-';

        document.getElementById('shopDeposit').textContent = formatVnd(data.depositVnd) + ' VNĐ';
        document.getElementById('shopBalance').textContent = formatVnd(data.balanceVnd) + ' VNĐ';
        document.getElementById('shopBankAccountNumber').textContent = data.bankAccountNumber || '-';
        document.getElementById('shopBankName').textContent = data.bankName || '-';
        document.getElementById('shopBankBranch').textContent = data.bankBranch || '-';

        const badge = document.getElementById('shopStatusBadge');
        const pendingSec = document.getElementById('pendingReviewSection');
        const stUpper = (data.status || '').toUpperCase();
        const shopStUpper = (data.shopStatus || '').toUpperCase();

        if (stUpper === 'PENDING') {
            badge.className = 'ds-badge ds-badge-warning';
            badge.textContent = 'Chờ kích hoạt';
            if (pendingSec) pendingSec.style.display = 'block';
        } else {
            if (pendingSec) pendingSec.style.display = 'none';

            if (stUpper === 'REJECTED') {
                badge.className = 'ds-badge ds-badge-danger';
                badge.textContent = 'Bị từ chối';
                document.getElementById('shopRejectionReasonRow').style.display = 'flex';
                document.getElementById('shopRejectionReason').textContent = data.rejectionReason || 'Không ghi rõ lý do.';
            } else if (shopStUpper === 'WITHDRAWN' || shopStUpper === 'DELETED') {
                badge.className = 'ds-badge ds-badge-danger';
                badge.textContent = 'Đã đóng Shop (Hoàn phí)';
            } else if (shopStUpper === 'SUSPENDED' || shopStUpper === 'TEMP_LOCKED') {
                badge.className = 'ds-badge ds-badge-warning';
                badge.textContent = 'Tạm ngưng';
            } else if (shopStUpper === 'LOCKED' || shopStUpper === 'INDEFINITE_LOCKED') {
                badge.className = 'ds-badge ds-badge-warning';
                badge.textContent = 'Tạm khóa';
            } else if (shopStUpper === 'BANNED' || shopStUpper === 'PERMANENT_BANNED') {
                badge.className = 'ds-badge ds-badge-danger';
                badge.textContent = 'Khóa vĩnh viễn';
            } else {
                badge.className = 'ds-badge ds-badge-success';
                badge.textContent = 'Hoạt động';
            }
        }

        const statusSelect = document.getElementById('shopStatusSelect');
        if (statusSelect) {
            if (data.shopStatus) {
                statusSelect.value = data.shopStatus;
            } else if (stUpper === 'APPROVED' || stUpper === 'ACTIVE') {
                statusSelect.value = 'Active';
            }
        }

        if (data.suspendedUntil) {
            startCountdown(data.suspendedUntil);
        } else {
            const cardAlert = document.getElementById('cardSuspendedAlert');
            if (cardAlert) cardAlert.style.display = 'none';
        }
    } catch (e) {
        console.error("Lỗi", e);
        showToast(e.message, "danger");
    }
}

let countdownInterval = null;

function startCountdown(suspendedUntilStr) {
    if (countdownInterval) clearInterval(countdownInterval);
    const cardAlert = document.getElementById('cardSuspendedAlert');
    const cardUntilText = document.getElementById('cardSuspendedUntilText');
    const cardCountDisplay = document.getElementById('cardSuspendedCountdown');

    if (!suspendedUntilStr) return;

    const targetTime = new Date(suspendedUntilStr).getTime();
    if (isNaN(targetTime)) return;

    if (cardAlert) cardAlert.style.display = 'flex';

    try {
        const dt = new Date(suspendedUntilStr);
        const formatted = dt.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' });
        if (cardUntilText) cardUntilText.textContent = 'Thời hạn: Đến ' + formatted;
    } catch (ex) {}

    function update() {
        const now = new Date().getTime();
        const diff = targetTime - now;

        if (diff <= 0) {
            clearInterval(countdownInterval);
            if (cardCountDisplay) cardCountDisplay.textContent = 'Tự động mở lại: Đang kích hoạt...';
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

        if (cardCountDisplay) cardCountDisplay.textContent = 'Tự động mở lại sau: ' + str;
    }

    update();
    countdownInterval = setInterval(update, 1000);
}

let pendingStatusToUpdate = 'Suspended';

async function submitShopStatusUpdate() {
    if (!currentShopId) return;
    const select = document.getElementById('shopStatusSelect');
    const newStatus = select ? select.value : '';
    if (!newStatus) return;

    if (newStatus === 'Suspended' || newStatus === 'Locked' || newStatus === 'TEMP_LOCKED' || newStatus === 'INDEFINITE_LOCKED') {
        openSuspendShopModal(newStatus);
        return;
    }

    executeStatusUpdate(newStatus, null);
}

function openSuspendShopModal(status) {
    pendingStatusToUpdate = status || 'Locked';
    const modal = document.getElementById('suspendShopModal');
    const input = document.getElementById('modalSuspendUntilInput');
    const err = document.getElementById('modalSuspendUntilError');
    if (err) err.textContent = '';

    const modalTitle = modal ? modal.querySelector('h3') : null;
    if (modalTitle) {
        modalTitle.innerHTML = `<i class="fa fa-clock-o"></i> ${pendingStatusToUpdate === 'Locked' ? 'Tạm khóa' : 'Tạm ngưng'} hoạt động Shop`;
    }
    
    // Set default datetime-local to 1 day from now
    const now = new Date();
    now.setDate(now.getDate() + 1);
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    if (input) input.value = `${year}-${month}-${day}T${hours}:${minutes}`;

    if (modal) modal.style.display = 'flex';
}

function closeSuspendShopModal() {
    const modal = document.getElementById('suspendShopModal');
    if (modal) modal.style.display = 'none';
}

function confirmSuspendShop() {
    const input = document.getElementById('modalSuspendUntilInput');
    const err = document.getElementById('modalSuspendUntilError');
    const val = input ? input.value : '';

    if (!val) {
        if (err) err.textContent = 'Vui lòng chọn ngày, giờ, phút hết hạn.';
        return;
    }

    const selectedTime = new Date(val);
    if (isNaN(selectedTime.getTime()) || selectedTime <= new Date()) {
        if (err) err.textContent = 'Thời điểm hết hạn phải lớn hơn thời điểm hiện tại.';
        return;
    }

    closeSuspendShopModal();
    executeStatusUpdate(pendingStatusToUpdate, val);
}

async function executeStatusUpdate(status, suspendedUntil) {
    try {
        let url = `/v1/shop-registrations/${currentShopId}/update-status?status=${encodeURIComponent(status)}`;
        if (suspendedUntil) {
            url += `&suspendedUntil=${encodeURIComponent(suspendedUntil)}`;
        }
        const response = await authFetch(url, { method: 'PUT' });
        const res = await response.json();
        if (response.ok) {
            showToast(res.description || 'Cập nhật trạng thái Shop thành công!', "success");
            setTimeout(() => {
                window.location.reload();
            }, 1000);
        } else {
            showToast(res.description || 'Lỗi khi cập nhật trạng thái Shop.', "danger");
        }
    } catch (error) {
        showToast('Lỗi kết nối máy chủ.', "danger");
    }
}

function openApproveShopModal() {
    const modal = document.getElementById('approveShopModal');
    if (modal) modal.style.display = 'flex';
}

function closeApproveShopModal() {
    const modal = document.getElementById('approveShopModal');
    if (modal) modal.style.display = 'none';
}

function submitApproveShop() {
    closeApproveShopModal();
    executeShopReview(true, '');
}

function openRejectShopModal() {
    const modal = document.getElementById('rejectShopModal');
    const input = document.getElementById('modalShopRejectReason');
    const err = document.getElementById('modalShopRejectReasonError');
    if (input) input.value = '';
    if (err) err.textContent = '';
    if (modal) modal.style.display = 'flex';
}

function closeRejectShopModal() {
    const modal = document.getElementById('rejectShopModal');
    if (modal) modal.style.display = 'none';
}

function submitRejectShop() {
    const input = document.getElementById('modalShopRejectReason');
    const err = document.getElementById('modalShopRejectReasonError');
    const reason = input ? input.value.trim() : '';
    if (!reason) {
        if (err) err.textContent = 'Vui lòng nhập lý do từ chối đăng ký Shop.';
        return;
    }
    closeRejectShopModal();
    executeShopReview(false, reason);
}

async function executeShopReview(isApproved, reason) {
    if (!currentShopId) return;

    try {
        const response = await authFetch(`/v1/shop-registrations/${currentShopId}/review`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ approved: isApproved, reason: reason })
        });

        if (response.ok) {
            showToast(isApproved ? "Phê duyệt đăng ký Shop thành công" : "Từ chối đăng ký Shop thành công", "success");
            setTimeout(() => {
                window.location.href = '/staff/shop-registrations';
            }, 1500);
        } else {
            const res = await response.json();
            showToast(res.description || 'Lỗi xử lý yêu cầu.', "danger");
        }
    } catch (error) {
        showToast('Lỗi kết nối máy chủ.', "danger");
    }
}

function formatVnd(value) {
    if (value === null || value === undefined) return '0';
    return value.toLocaleString('vi-VN');
}

function showToast(message, type) {
    if (type === 'danger' && typeof window.showErrorToast === 'function') {
        window.showErrorToast(message);
        return;
    }
    if (type === 'success' && typeof window.showSuccessToast === 'function') {
        window.showSuccessToast(message);
        return;
    }

    let container = document.getElementById('staffToastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'staffToastContainer';
        container.className = 'ds-toast-container';
        document.body.appendChild(container);
    }

    const toastClass = type === 'danger' ? 'ds-toast-error' : 'ds-toast-success';
    const title = type === 'danger' ? 'Thông báo' : 'Thành công';

    const toast = document.createElement('div');
    toast.className = 'ds-toast ' + toastClass;
    toast.innerHTML =
        '<div><p class="ds-toast-title">' + title + '</p>' +
        '<p class="ds-toast-message">' + message + '</p></div>' +
        '<button class="ds-toast-close" type="button" aria-label="Đóng">×</button>';

    toast.querySelector('.ds-toast-close').addEventListener('click', function () {
        toast.remove();
    });

    container.appendChild(toast);

    setTimeout(function () {
        toast.remove();
        if (!container.children.length) {
            container.remove();
        }
    }, 3200);
}
