(function() {
    let currentRegistrationId = null;
    let shopPageSize = 10;
    let registrationsList = [];

    document.addEventListener('DOMContentLoaded', () => {
        const token = sessionStorage.getItem('accessToken');
        if (!token) {
            window.location.href = '/login';
            return;
        }

        loadRegistrations();
        loadRegistrationStats();
        loadShopAccountStatuses();
        loadRegistrationStatuses();

        // Search and filter events
        const searchBtn = document.getElementById('shopSearchBtn');
        if (searchBtn) {
            searchBtn.addEventListener('click', () => {
                loadRegistrations(0);
                loadRegistrationStats();
            });
        }

        const resetBtn = document.getElementById('shopResetBtn');
        if (resetBtn) {
            resetBtn.addEventListener('click', () => {
                const keywordInput = document.getElementById('shopKeywordFilter');
                if (keywordInput) keywordInput.value = '';
                const statusSelect = document.getElementById('regStatusFilter');
                if (statusSelect) statusSelect.value = '';
                const shopStatusSelect = document.getElementById('shopAccountStatusFilter');
                if (shopStatusSelect) shopStatusSelect.value = '';
                loadRegistrations(0);
                loadRegistrationStats();
            });
        }

        const modal = document.getElementById('reviewModal');
        document.getElementById('closeReviewModal').addEventListener('click', () => modal.close());
        document.getElementById('approveBtn').addEventListener('click', () => submitReview(true));
        document.getElementById('rejectBtn').addEventListener('click', () => submitReview(false));
    });

    async function loadRegistrationStats() {
        try {
            const response = await authFetch('/v1/shop-registrations/stats');
            if (response.ok) {
                const stats = await response.json();
                const totalEl = document.getElementById('stat-total-shops');
                const pendingEl = document.getElementById('stat-pending-shops');
                const approvedEl = document.getElementById('stat-approved-shops');
                const rejectedEl = document.getElementById('stat-rejected-shops');

                if (totalEl) totalEl.textContent = stats.total || 0;
                if (pendingEl) pendingEl.textContent = stats.pending || 0;
                if (approvedEl) approvedEl.textContent = stats.approved || 0;
                if (rejectedEl) rejectedEl.textContent = stats.rejected || 0;
            }
        } catch (error) {
            console.error("Lỗi tải thống kê đăng ký Shop", error);
        }
    }

    async function loadRegistrations(page = 0) {
        const tbody = document.getElementById('shopRegistrationsTableBody');
        tbody.innerHTML = '<tr><td colspan="9" class="ds-table-center">Đang tải...</td></tr>';

        const statusSelect = document.getElementById('regStatusFilter');
        const status = statusSelect ? statusSelect.value : '';
        const shopStatusSelect = document.getElementById('shopAccountStatusFilter');
        const shopStatus = shopStatusSelect ? shopStatusSelect.value : '';
        const keywordInput = document.getElementById('shopKeywordFilter');
        const keyword = keywordInput ? keywordInput.value.trim() : '';

        let url = `/v1/shop-registrations?page=${page}&size=${shopPageSize}`;
        if (status) {
            url += `&status=${status}`;
        }
        if (shopStatus) {
            url += `&shopStatus=${shopStatus}`;
        }
        if (keyword) {
            url += `&keyword=${encodeURIComponent(keyword)}`;
        }

        try {
            const response = await authFetch(url);
            if (response.ok) {
                const data = await response.json();
                registrationsList = data.content || [];
                renderTable(registrationsList, data.number, data.size);
                renderPagination(data);
            } else {
                tbody.innerHTML = '<tr><td colspan="9" class="ds-table-center">Lỗi khi tải dữ liệu</td></tr>';
            }
        } catch (error) {
            tbody.innerHTML = '<tr><td colspan="9" class="ds-table-center">Lỗi kết nối</td></tr>';
        }
    }

    function renderTable(content, page, size) {
        const tbody = document.getElementById('shopRegistrationsTableBody');
        tbody.innerHTML = '';

        if (!content || content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="ds-table-center">Không có yêu cầu nào.</td></tr>';
            return;
        }

        content.forEach((item, index) => {
            const stt = page * size + index + 1;
            let formattedDate = '-';
            try {
                if (item.submittedAt) {
                    formattedDate = new Date(item.submittedAt).toLocaleDateString('vi-VN');
                }
            } catch(e) {}

            const statusBadge = getStatusBadge(item.status);
            const shopStatusBadge = getShopStatusBadge(item.shopStatus);
            const depositFormatted = formatVnd(item.depositVnd);
            const balanceFormatted = formatVnd(item.balanceVnd);

            const emailPhone = [item.supportEmail, item.supportPhone].filter(val => val && val !== 'undefined').join(' - ');
            const emailPhoneHtml = emailPhone ? `<div class="ds-entity-subtitle">${emailPhone}</div>` : '';

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="ds-table-center">${stt}</td>
                <td>${item.code}</td>
                <td>
                    <div class="ds-entity-title">${item.shopName || ''}</div>
                    ${emailPhoneHtml}
                </td>
                <td class="ds-table-center">${shopStatusBadge}</td>
                <td class="ds-table-right">${depositFormatted}</td>
                <td class="ds-table-right">${balanceFormatted}</td>
                <td class="ds-table-center">${statusBadge}</td>
                <td>${formattedDate}</td>
                <td class="ds-table-center">
                    <label class="ds-switch" title="Bật/Tắt trạng thái hoạt động">
                        <input type="checkbox" class="shop-status-toggle" data-id="${item.id}" ${item.shopStatus && (item.shopStatus.toUpperCase() === 'ACTIVE' || item.shopStatus.toUpperCase() === 'APPROVED') ? 'checked' : ''}>
                        <span class="ds-slider"></span>
                    </label>
                </td>
            `;
            tbody.appendChild(tr);
        });

        document.querySelectorAll('.review-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const btnTarget = e.target.closest('.review-btn');
                currentRegistrationId = btnTarget.getAttribute('data-id');
                const item = registrationsList.find(x => x.id == currentRegistrationId);
                if (item) {
                    openReviewModal(item);
                }
            });
        });

        document.querySelectorAll('.shop-status-toggle').forEach(toggle => {
            toggle.addEventListener('change', async (e) => {
                const checkbox = e.target;
                const regId = checkbox.getAttribute('data-id');
                const isActive = checkbox.checked;
                
                try {
                    const response = await authFetch(`/v1/shop-registrations/${regId}/toggle-status?active=${isActive}`, {
                        method: 'PUT'
                    });
                    
                    if (response.ok) {
                        loadRegistrations(page); // reload current page
                    } else {
                        const res = await response.json();
                        alert(res.description || 'Lỗi khi cập nhật trạng thái Shop.');
                        checkbox.checked = !isActive;
                    }
                } catch (err) {
                    alert('Lỗi kết nối máy chủ.');
                    checkbox.checked = !isActive;
                }
            });
        });
    }

    function getShopStatusBadge(shopStatus) {
        if (!shopStatus) return '<span class="ds-badge ds-badge-warning">Chờ kích hoạt</span>';
        const stUpper = shopStatus.toUpperCase();
        if (stUpper === 'PENDING') return '<span class="ds-badge ds-badge-warning">Chờ kích hoạt</span>';
        if (stUpper === 'ACTIVE' || stUpper === 'APPROVED') return '<span class="ds-badge ds-badge-success">Hoạt động</span>';
        if (stUpper === 'BANNED' || stUpper === 'LOCKED') return '<span class="ds-badge ds-badge-danger">Đang bị khóa</span>';
        if (stUpper === 'REJECTED') return '<span class="ds-badge ds-badge-danger">Bị từ chối</span>';
        return `<span class="ds-badge ds-badge-info">${shopStatus}</span>`;
    }

    function formatVnd(value) {
        if (value === null || value === undefined) return '0';
        return value.toLocaleString('vi-VN');
    }

    function getStatusBadge(status) {
        if (!status) return '<span class="ds-badge ds-badge-warning">Chờ duyệt</span>';
        const stUpper = status.toUpperCase();
        if (stUpper === 'PENDING') return '<span class="ds-badge ds-badge-warning">Chờ duyệt</span>';
        if (stUpper === 'APPROVED') return '<span class="ds-badge ds-badge-success">Chấp thuận</span>';
        if (stUpper === 'REJECTED') return '<span class="ds-badge ds-badge-danger">Từ chối</span>';
        return `<span class="ds-badge ds-badge-info">${status}</span>`;
    }

    function openReviewModal(item) {
        document.getElementById('modalShopCode').value = item.code || '';
        document.getElementById('modalShopName').value = item.shopName || '';
        document.getElementById('modalShopCategory').value = item.category || '';
        document.getElementById('modalShopEmail').value = item.supportEmail || '';
        document.getElementById('modalShopPhone').value = item.supportPhone || '';
        document.getElementById('modalShopDesc').value = item.description || '';

        const stUpper = (item.status || '').toUpperCase();
        const footerActions = document.getElementById('modalFooterActions');
        const reasonGroup = document.getElementById('rejectionReasonFieldGroup');
        const reasonInput = document.getElementById('reviewReason');
        const reasonLabel = document.getElementById('rejectionReasonLabel');

        if (stUpper === 'PENDING') {
            if (footerActions) footerActions.style.display = 'flex';
            if (reasonGroup) reasonGroup.style.display = 'block';
            if (reasonInput) {
                reasonInput.value = '';
                reasonInput.removeAttribute('readonly');
            }
            if (reasonLabel) reasonLabel.textContent = 'Lý do từ chối (nếu từ chối)';
        } else if (stUpper === 'APPROVED') {
            if (footerActions) footerActions.style.display = 'none';
            if (reasonGroup) reasonGroup.style.display = 'none';
        } else if (stUpper === 'REJECTED') {
            if (footerActions) footerActions.style.display = 'none';
            if (reasonGroup) reasonGroup.style.display = 'block';
            if (reasonInput) {
                reasonInput.value = item.rejectionReason || 'Không ghi rõ lý do.';
                reasonInput.setAttribute('readonly', 'true');
            }
            if (reasonLabel) reasonLabel.textContent = 'Lý do đã từ chối';
        }

        document.getElementById('reviewModal').showModal();
    }

    function renderPagination(data) {
        if (typeof window.mountStaffPagination === 'function') {
            window.mountStaffPagination('shopPagination', {
                page: data.number,
                totalPages: data.totalPages,
                totalElements: data.totalElements,
                pageSize: data.size
            }, {
                onPage: (p) => { loadRegistrations(p); },
                onSize: (s) => { shopPageSize = s; loadRegistrations(0); }
            });
        }
    }

    async function submitReview(isApproved) {
        if (!currentRegistrationId) return;

        const reasonInput = document.getElementById('reviewReason');
        const reason = reasonInput ? reasonInput.value.trim() : '';
        if (!isApproved && !reason) {
            alert('Vui lòng nhập lý do từ chối.');
            return;
        }

        try {
            const response = await authFetch(`/v1/shop-registrations/${currentRegistrationId}/review`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ approved: isApproved, reason: reason })
            });

            if (response.ok) {
                document.getElementById('reviewModal').close();
                loadRegistrations(0);
                loadRegistrationStats();
            } else {
                const res = await response.json();
                alert(res.description || 'Lỗi xử lý yêu cầu.');
            }
        } catch (error) {
            alert('Lỗi kết nối máy chủ.');
        }
    }

    async function loadShopAccountStatuses() {
        const select = document.getElementById('shopAccountStatusFilter');
        if (!select) return;
        
        try {
            const response = await authFetch('/v1/shop-registrations/shop-statuses');
            if (response.ok) {
                const statuses = await response.json();
                select.innerHTML = '<option value="">Trạng thái Shop (Tất cả)</option>';
                
                const addedValues = new Set();
                statuses.forEach(s => {
                    if (!s) return;
                    const stUpper = s.toUpperCase();
                    
                    let label = s;
                    let val = s;
                    
                    if (stUpper === 'PENDING') {
                        label = 'Chờ kích hoạt';
                    } else if (stUpper === 'ACTIVE' || stUpper === 'APPROVED') {
                        label = 'Hoạt động';
                    } else if (stUpper === 'BANNED' || stUpper === 'LOCKED') {
                        label = 'Đang bị khóa';
                    } else if (stUpper === 'REJECTED') {
                        label = 'Bị từ chối';
                    }
                    
                    if (!addedValues.has(label)) {
                        addedValues.add(label);
                        const opt = document.createElement('option');
                        opt.value = val;
                        opt.textContent = label;
                        select.appendChild(opt);
                    }
                });
            }
        } catch (error) {
            console.error("Lỗi tải danh sách trạng thái tài khoản shop từ database:", error);
        }
    }

    async function loadRegistrationStatuses() {
        const select = document.getElementById('regStatusFilter');
        if (!select) return;
        
        try {
            const response = await authFetch('/v1/shop-registrations/statuses');
            if (response.ok) {
                const statuses = await response.json();
                select.innerHTML = '<option value="">Trạng thái yêu cầu (Tất cả)</option>';
                
                const addedValues = new Set();
                statuses.forEach(s => {
                    if (!s) return;
                    const stUpper = s.toUpperCase();
                    
                    let label = s;
                    let val = s;
                    
                    if (stUpper === 'PENDING') {
                        label = 'Chờ duyệt';
                    } else if (stUpper === 'APPROVED') {
                        label = 'Chấp thuận (Đã duyệt)';
                    } else if (stUpper === 'REJECTED') {
                        label = 'Từ chối';
                    }
                    
                    if (!addedValues.has(label)) {
                        addedValues.add(label);
                        const opt = document.createElement('option');
                        opt.value = val;
                        opt.textContent = label;
                        select.appendChild(opt);
                    }
                });
            }
        } catch (error) {
            console.error("Lỗi tải danh sách trạng thái yêu cầu từ database:", error);
        }
    }
})();
