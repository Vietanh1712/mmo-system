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
        const stUpper = (data.status || '').toUpperCase();
        if (stUpper === 'PENDING') {
            badge.className = 'ds-badge ds-badge-warning';
            badge.textContent = 'Chờ duyệt';
        } else if (stUpper === 'APPROVED') {
            badge.className = 'ds-badge ds-badge-success';
            badge.textContent = 'Đã duyệt';
            hideActionPanel();
        } else if (stUpper === 'REJECTED') {
            badge.className = 'ds-badge ds-badge-danger';
            badge.textContent = 'Từ chối';
            hideActionPanel();
            
            document.getElementById('shopRejectionReasonRow').style.display = 'flex';
            document.getElementById('shopRejectionReason').textContent = data.rejectionReason || 'Không ghi rõ lý do.';
        } else {
            badge.className = 'ds-badge ds-badge-info';
            badge.textContent = data.status || '-';
            hideActionPanel();
        }
    } catch (e) {
        console.error("Lỗi", e);
        showToast(e.message, "danger");
    }
}

function hideActionPanel() {
    const actionCard = document.querySelector('.staff-action-card');
    if (actionCard) {
        actionCard.style.display = 'none';
    }
}

async function reviewShop(isApproved) {
    if (!currentShopId) return;

    const reasonInput = document.getElementById('reviewReason');
    const reason = reasonInput ? reasonInput.value.trim() : '';
    if (!isApproved && !reason) {
        showToast("Vui lòng nhập lý do từ chối.", "danger");
        return;
    }

    try {
        const response = await authFetch(`/v1/shop-registrations/${currentShopId}/review`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ approved: isApproved, reason: reason })
        });

        if (response.ok) {
            showToast("Xử lý yêu cầu thành công", "success");
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
