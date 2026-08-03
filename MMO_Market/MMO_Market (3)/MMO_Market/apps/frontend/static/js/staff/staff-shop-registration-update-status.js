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
            let valToSet = data.shopStatus;
            for (let i = 0; i < selectEl.options.length; i++) {
                if (selectEl.options[i].value.toLowerCase() === valToSet.toLowerCase()) {
                    valToSet = selectEl.options[i].value;
                    break;
                }
            }
            selectEl.value = valToSet;
        }

    } catch (e) {
        console.error("Lỗi", e);
        showToast(e.message, "danger");
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

    try {
        const response = await authFetch(`/v1/shop-registrations/${currentShopId}/update-status?status=${selectedStatus}`, {
            method: 'PUT'
        });

        if (response.ok) {
            showToast("Cập nhật trạng thái Shop thành công", "success");
            setTimeout(() => {
                window.location.href = '/staff/shop-registrations';
            }, 1500);
        } else {
            const res = await response.json();
            showToast(res.description || 'Lỗi khi cập nhật trạng thái Shop.', "danger");
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
    
    // 4 new statuses mapping
    if (stUpper === 'WITHDRAWN' || stUpper === 'DELETED') return '<span class="ds-badge ds-badge-danger">Đã đóng Shop (Hoàn phí)</span>';
    if (stUpper === 'SUSPENDED' || stUpper === 'TEMP_LOCKED') return '<span class="ds-badge ds-badge-warning">Tạm ngưng</span>';
    if (stUpper === 'LOCKED' || stUpper === 'INDEFINITE_LOCKED') return '<span class="ds-badge ds-badge-warning">Tạm khóa</span>';
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
