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
        document.getElementById('shopOwnerName').textContent = data.ownerName || '-';

        const badge = document.getElementById('currentShopStatusBadge');
        if (badge) {
            badge.innerHTML = getShopStatusBadge(data.shopStatus);
        }

        // Set values in select dropdown
        const selectEl = document.getElementById('shopStatusSelect');
        if (selectEl && data.shopStatus) {
            let stUpper = String(data.shopStatus).toUpperCase();
            let valToSet = 'Active';
            if (stUpper === 'LOCKED' || stUpper === 'TEMP_LOCKED' || stUpper === 'INDEFINITE_LOCKED') {
                valToSet = 'Locked';
            } else if (stUpper === 'SUSPENDED' || stUpper === 'TEMP_SUSPENDED') {
                valToSet = 'Suspended';
            } else if (stUpper === 'BANNED' || stUpper === 'PERMANENT_BANNED') {
                valToSet = 'Banned';
            } else if (stUpper === 'WITHDRAWN' || stUpper === 'DELETED') {
                valToSet = 'Withdrawn';
            } else if (stUpper === 'ACTIVE' || stUpper === 'APPROVED') {
                valToSet = 'Active';
            }
            selectEl.value = valToSet;
            onShopStatusChange();
        }

        if (data.suspendedUntil) {
            const untilInput = document.getElementById('suspendedUntilInput');
            if (untilInput) {
                let raw = String(data.suspendedUntil).replace(' ', 'T');
                if (raw.length > 16) raw = raw.substring(0, 16);
                untilInput.value = raw;
                const presetEl = document.getElementById('lockPresetsSelect');
                if (presetEl) presetEl.value = 'custom';
            }
        }

    } catch (e) {
        console.error("Lỗi", e);
        showToast(e.message, "danger");
    }
}

function onShopStatusChange() {
    const selectEl = document.getElementById('shopStatusSelect');
    const container = document.getElementById('lockDurationContainer');
    if (!selectEl || !container) return;

    const val = selectEl.value;
    if (val === 'Locked' || val === 'Suspended') {
        container.style.display = 'block';
        const untilInput = document.getElementById('suspendedUntilInput');
        if (untilInput && !untilInput.value) {
            applyPresetDays(7);
        }
    } else {
        container.style.display = 'none';
    }
}

function handleLockPresetChange() {
    const presetEl = document.getElementById('lockPresetsSelect');
    if (!presetEl) return;
    const val = presetEl.value;
    if (val !== 'custom') {
        applyPresetDays(parseInt(val, 10));
    }
}

function applyPresetDays(days) {
    const now = new Date();
    now.setDate(now.getDate() + days);
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');

    const formatted = `${year}-${month}-${day}T${hours}:${minutes}`;
    const untilInput = document.getElementById('suspendedUntilInput');
    if (untilInput) {
        untilInput.value = formatted;
    }
}

async function submitShopStatusUpdate() {
    if (!currentShopId) return;

    const selectEl = document.getElementById('shopStatusSelect');
    const selectedStatus = selectEl ? selectEl.value : '';
    if (!selectedStatus) {
        showToast("Trạng thái không hợp lệ.", "danger");
        return;
    }

    let url = `/v1/shop-registrations/${currentShopId}/update-status?status=${selectedStatus}`;

    if (selectedStatus === 'Locked' || selectedStatus === 'Suspended') {
        const untilInput = document.getElementById('suspendedUntilInput');
        const untilVal = untilInput ? untilInput.value : '';
        if (!untilVal) {
            showToast("Vui lòng chọn ngày và giờ hết hạn tạm khóa.", "danger");
            return;
        }
        url += `&suspendedUntil=${encodeURIComponent(untilVal)}`;
    }

    try {
        const response = await authFetch(url, {
            method: 'PUT'
        });

        if (response.ok) {
            showToast("Cập nhật trạng thái Shop thành công", "success");
            setTimeout(() => {
                window.location.href = '/staff/shop-registrations';
            }, 1500);
        } else {
            const res = await response.json();
            showToast(res.description || res.message || 'Lỗi khi cập nhật trạng thái Shop.', "danger");
        }
    } catch (error) {
        showToast('Lỗi kết nối máy chủ.', "danger");
    }
}

function getShopStatusBadge(shopStatus) {
    if (!shopStatus) return '<span class="ds-badge ds-badge-warning">Chờ kích hoạt</span>';
    const stUpper = shopStatus.toUpperCase();
    if (stUpper === 'PENDING') return '<span class="ds-badge ds-badge-warning">Chờ kích hoạt</span>';
    if (stUpper === 'ACTIVE' || stUpper === 'APPROVED') return '<span class="ds-badge ds-badge-success">Hoạt động</span>';
    if (stUpper === 'REJECTED') return '<span class="ds-badge ds-badge-danger">Bị từ chối</span>';
    
    if (stUpper === 'WITHDRAWN' || stUpper === 'DELETED') return '<span class="ds-badge ds-badge-danger">Đã đóng Shop (Hoàn phí)</span>';
    if (stUpper === 'SUSPENDED' || stUpper === 'TEMP_SUSPENDED') return '<span class="ds-badge ds-badge-warning">Tạm ngưng</span>';
    if (stUpper === 'LOCKED' || stUpper === 'TEMP_LOCKED' || stUpper === 'INDEFINITE_LOCKED') return '<span class="ds-badge ds-badge-warning">Tạm khóa</span>';
    if (stUpper === 'BANNED' || stUpper === 'PERMANENT_BANNED') return '<span class="ds-badge ds-badge-danger">Khóa vĩnh viễn</span>';
    
    return `<span class="ds-badge ds-badge-info">${shopStatus}</span>`;
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
