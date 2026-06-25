(function () {
    let myPermissions = [];

    function showToast(message, type) {
        let container = document.getElementById('staffToastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'staffToastContainer';
            container.className = 'ds-toast-container';
            document.body.appendChild(container);
        }

        const toastClass = type === 'danger' ? 'ds-toast-error' : 'ds-toast-success';
        const title = type === 'danger' ? 'Thất bại' : 'Thành công';

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
            if (toast.parentElement) {
                toast.remove();
            }
            if (container.parentElement && !container.children.length) {
                container.remove();
            }
        }, 3500);
    }

    async function loadMyPermissions() {
        try {
            // authFetch tự động đính kèm Authorization header Bearer token
            const res = await authFetch('/api/staff/my-permissions');
            if (res.ok) {
                myPermissions = await res.json();
                applyMenuPermissions();
            }
        } catch (e) {
            console.error('Không thể load danh sách quyền của Staff:', e);
        }
    }

    function applyMenuPermissions() {
        const links = document.querySelectorAll('.staff-sidebar__link[data-required-permission]');
        links.forEach(function (link) {
            const reqPerm = link.getAttribute('data-required-permission');
            if (!myPermissions.includes(reqPerm)) {
                // Vô hiệu hóa liên kết
                link.classList.add('is-disabled');
                link.addEventListener('click', function (event) {
                    event.preventDefault();
                    event.stopPropagation();
                    
                    // Lấy nhãn mô tả quyền để hiển thị cho thân thiện
                    let permLabel = reqPerm;
                    if (reqPerm === 'APPROVE_KYC') permLabel = 'Duyệt hồ sơ định danh KYC';
                    else if (reqPerm === 'FLAG_SELLER') permLabel = 'Cắm cờ & Đánh gạch Seller';
                    else if (reqPerm === 'APPROVE_WITHDRAWALS') permLabel = 'Phê duyệt yêu cầu rút tiền';
                    else if (reqPerm === 'HANDLE_DISPUTES') permLabel = 'Phân xử tranh chấp & Hoàn tiền';
                    else if (reqPerm === 'MANAGE_SUPPORT') permLabel = 'Tiếp nhận & Hỗ trợ khách hàng';

                    showToast('Bạn không có quyền truy cập chức năng này. Quyền yêu cầu: ' + permLabel, 'danger');
                });
            }
        });
    }

    function bindActionButtons() {
        document.querySelectorAll('[data-staff-action]').forEach(function (button) {
            button.addEventListener('click', function () {
                const action = button.getAttribute('data-staff-action');
                
                // Trích xuất kycId động từ giao diện nếu có (tiêu đề trang chi tiết)
                let kycId = "1198";
                const subtitleEl = document.querySelector('.staff-page-header .ds-page-subtitle');
                if (subtitleEl && subtitleEl.textContent.includes('#KYC-')) {
                    const match = subtitleEl.textContent.match(/#KYC-(\d+)/);
                    if (match) kycId = match[1];
                }

                const kycNote = document.getElementById('kycNote')?.value || '';
                if (action === 'reject' && !kycNote.trim()) {
                    showToast('Vui lòng nhập lý do từ chối vào ô Ghi chú.', 'danger');
                    return;
                }

                // Thực hiện gọi API Backend thật
                button.disabled = true;
                const originalText = button.textContent;
                button.textContent = 'Đang xử lý...';

                authFetch(`/staff/kyc/${action}/${kycId}`, {
                    method: 'POST',
                    body: JSON.stringify({ note: kycNote })
                })
                .then(async function (response) {
                    const data = await response.json();
                    button.disabled = false;
                    button.textContent = originalText;

                    if (response.status === 403) {
                        showToast('Bạn không có quyền thực hiện hành động này. Quyền yêu cầu: Duyệt hồ sơ định danh KYC', 'danger');
                    } else if (response.ok) {
                        showToast(data.message || 'Thao tác thành công!', 'success');
                        setTimeout(function () {
                            window.location.href = '/staff/kyc';
                        }, 2000);
                    } else {
                        showToast(data.message || 'Thao tác thất bại.', 'danger');
                    }
                })
                .catch(function (err) {
                    button.disabled = false;
                    button.textContent = originalText;
                    showToast('Lỗi hệ thống hoặc bị từ chối truy cập (HTTP 403 Forbidden).', 'danger');
                });
            });
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        loadMyPermissions();
        bindActionButtons();
    });
})();
