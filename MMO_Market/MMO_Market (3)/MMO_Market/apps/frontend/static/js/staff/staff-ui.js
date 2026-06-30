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
            const res = await authFetch('/staff/my-permissions');
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

    function mountStaffPagination(containerId, state, handlers) {
        const root = document.getElementById(containerId);
        if (!root) return;

        const page = state.page;
        const totalPages = Math.max(state.totalPages, 1);
        const totalElements = state.totalElements;
        const pageSize = state.pageSize;
        const options = state.pageSizeOptions || [10, 20, 50, 100];

        let pagesHtml = '';
        const addPage = (p) => {
            pagesHtml += `<a href="#" role="button" class="ds-page-link${p === page ? ' ds-page-link-active' : ''}" data-page="${p}">${p + 1}</a>`;
        };
        if (totalPages <= 7) {
            for (let p = 0; p < totalPages; p++) addPage(p);
        } else {
            addPage(0);
            if (page > 2) pagesHtml += '<span class="ds-caption" style="padding:0 4px">…</span>';
            for (let p = Math.max(1, page - 1); p <= Math.min(totalPages - 2, page + 1); p++) addPage(p);
            if (page < totalPages - 3) pagesHtml += '<span class="ds-caption" style="padding:0 4px">…</span>';
            addPage(totalPages - 1);
        }

        const sizeOptions = options.map(o =>
            `<option value="${o}"${o === pageSize ? ' selected' : ''}>${o}</option>`
        ).join('');

        root.innerHTML = `
            <div class="ds-pagination">
                <div class="ds-pagination-pages">
                    <a href="#" role="button" class="ds-page-link${page <= 0 ? ' ds-page-link-disabled' : ''}" data-nav="first" aria-label="Trang đầu">«</a>
                    <a href="#" role="button" class="ds-page-link${page <= 0 ? ' ds-page-link-disabled' : ''}" data-nav="prev" aria-label="Trang trước">‹</a>
                    ${pagesHtml}
                    <a href="#" role="button" class="ds-page-link${page >= totalPages - 1 ? ' ds-page-link-disabled' : ''}" data-nav="next" aria-label="Trang sau">›</a>
                    <a href="#" role="button" class="ds-page-link${page >= totalPages - 1 ? ' ds-page-link-disabled' : ''}" data-nav="last" aria-label="Trang cuối">»</a>
                </div>
                <div class="ds-pagination-meta">
                    <span>Tổng số: ${totalElements} bản ghi</span>
                    <select class="ds-page-size" aria-label="Số dòng mỗi trang">${sizeOptions}</select>
                </div>
            </div>
        `;

        root.querySelectorAll('[data-nav]').forEach(el => {
            el.addEventListener('click', (e) => {
                e.preventDefault();
                if (el.classList.contains('ds-page-link-disabled')) return;
                const nav = el.getAttribute('data-nav');
                let next = page;
                if (nav === 'first') next = 0;
                else if (nav === 'prev') next = page - 1;
                else if (nav === 'next') next = page + 1;
                else if (nav === 'last') next = totalPages - 1;
                handlers.onPage(next);
            });
        });

        root.querySelectorAll('[data-page]').forEach(el => {
            el.addEventListener('click', (e) => {
                e.preventDefault();
                handlers.onPage(Number(el.getAttribute('data-page')));
            });
        });

        const sizeSelect = root.querySelector('.ds-page-size');
        if (sizeSelect) {
            sizeSelect.addEventListener('change', () => {
                handlers.onSize(Number(sizeSelect.value));
            });
        }
    }

    function changePageSize(newSize) {
        const url = new URL(window.location.href);
        url.searchParams.set('size', newSize);
        url.searchParams.set('page', '0');
        window.location.href = url.toString();
    }

    // Expose helpers globally
    window.mountStaffPagination = mountStaffPagination;
    window.changePageSize = changePageSize;

    document.addEventListener('DOMContentLoaded', function () {
        loadMyPermissions();
        bindActionButtons();
    });
})();
