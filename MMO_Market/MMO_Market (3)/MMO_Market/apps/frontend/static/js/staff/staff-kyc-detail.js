document.addEventListener('DOMContentLoaded', () => {
    loadKycDetail();
});

let currentKycId = new URLSearchParams(window.location.search).get('id');
let currentVersion = 0;

async function loadKycDetail() {
    if (!currentKycId) {
        showToast("Không tìm thấy mã hồ sơ", "danger");
        return;
    }

    try {
        const response = await authFetch(`/v1/staff/kyc/${currentKycId}`);
        if (!response.ok) throw new Error("Lỗi tải chi tiết KYC");
        const kyc = await response.json();
        
        currentVersion = kyc.version;
        
        document.getElementById('kycRequestCodeDisplay').textContent = `#${kyc.requestCode}`;
        document.getElementById('kycFullName').textContent = kyc.fullName || '-';
        document.getElementById('kycEmail').textContent = kyc.email || '-';
        document.getElementById('kycDateOfBirth').textContent = kyc.dateOfBirth || '-';
        document.getElementById('kycAddress').textContent = kyc.address || '-';
        document.getElementById('kycIdNumber').textContent = kyc.idNumber;
        document.getElementById('kycIdType').textContent = kyc.idType;
        document.getElementById('kycCreatedAt').textContent = new Date(kyc.createdAt).toLocaleString('vi-VN');

        document.getElementById('kycRejectionReason').textContent = kyc.rejectionReason || '-';
        
        const badge = document.getElementById('kycStatusBadge');
        if (kyc.status === 'PENDING') {
            badge.className = 'ds-badge ds-badge-warning';
            badge.textContent = 'Chờ duyệt';
        } else if (kyc.status === 'APPROVED') {
            badge.className = 'ds-badge ds-badge-success';
            badge.textContent = 'Đã duyệt';
            hideActionPanel();
        } else {
            badge.className = 'ds-badge ds-badge-danger';
            badge.textContent = 'Từ chối';
            hideActionPanel();
        }

        // Set images
        const baseUrl = `/api/v1/kyc/${currentKycId}/documents`;
        document.getElementById('frontImage').src = `${baseUrl}/front?token=${sessionStorage.getItem('accessToken')}`;
        document.getElementById('backImage').src = `${baseUrl}/back?token=${sessionStorage.getItem('accessToken')}`;
        document.getElementById('selfieImage').src = `${baseUrl}/selfie?token=${sessionStorage.getItem('accessToken')}`;

    } catch (e) {
        console.error("Lỗi", e);
    }
}

function hideActionPanel() {
    const actionCard = document.querySelector('.staff-action-card');
    if (actionCard) {
        actionCard.style.display = 'none';
    }
}

async function reviewKyc(status) {
    const note = document.getElementById('kycNote').value.trim();
    if (status === 'REJECTED' && !note) {
        showToast("Vui lòng nhập lý do từ chối", "danger");
        return;
    }

    const payload = {
        version: currentVersion,
        status: status,
        rejectionReason: note
    };

    try {
        const response = await fetch(`/api/v1/staff/kyc/${currentKycId}/review`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${sessionStorage.getItem('accessToken')}`
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || 'Lỗi khi duyệt');
        }

        showToast("Duyệt hồ sơ thành công", "success");
        setTimeout(() => {
            window.location.href = '/staff/kyc';
        }, 1500);

    } catch (e) {
        showToast(e.message, "danger");
        if (e.message.includes('thay đổi bởi một người dùng khác')) {
            // Optimistic locking failure, reload to get new version
            setTimeout(() => location.reload(), 2000);
        }
    }
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
