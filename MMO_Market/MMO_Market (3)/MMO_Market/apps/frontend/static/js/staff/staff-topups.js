(function() {
    let topupPageSize = 10;
    let currentTopupsList = [];
    let activeRetryItem = null;

    document.addEventListener('DOMContentLoaded', () => {
        const token = sessionStorage.getItem('accessToken');
        if (!token) {
            window.location.href = '/login';
            return;
        }

        loadTopups(0);
        loadTopupStats();

        // Search & Filter Events
        const searchBtn = document.getElementById('topupSearchBtn');
        if (searchBtn) {
            searchBtn.addEventListener('click', () => {
                loadTopups(0);
            });
        }

        const kwInput = document.getElementById('topupKeywordFilter');
        if (kwInput) {
            kwInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    loadTopups(0);
                }
            });
        }

        const statusSelect = document.getElementById('topupStatusFilter');
        if (statusSelect) {
            statusSelect.addEventListener('change', () => {
                loadTopups(0);
            });
        }

        const resetBtn = document.getElementById('topupResetBtn');
        if (resetBtn) {
            resetBtn.addEventListener('click', () => {
                if (kwInput) kwInput.value = '';
                if (statusSelect) statusSelect.value = '';
                loadTopups(0);
            });
        }

        const confirmRetryBtn = document.getElementById('confirmRetryBtn');
        if (confirmRetryBtn) {
            confirmRetryBtn.addEventListener('click', confirmRetryTopup);
        }

        // Check if retryId parameter exists from detail page redirect
        const urlParams = new URLSearchParams(window.location.search);
        const retryId = urlParams.get('retryId');
        if (retryId) {
            fetchSingleTopupAndOpenModal(retryId);
        }
    });

    async function loadTopupStats() {
        try {
            const response = await authFetch('/v1/staff/topups/stats');
            if (response.ok) {
                const stats = await response.json();
                const totalEl = document.getElementById('stat-total-topups');
                const successEl = document.getElementById('stat-success-topups');
                const failedEl = document.getElementById('stat-failed-topups');
                const pendingEl = document.getElementById('stat-pending-topups');

                if (totalEl) totalEl.textContent = stats.totalTopups || 0;
                if (successEl) successEl.textContent = stats.successTopups || 0;
                if (failedEl) failedEl.textContent = stats.failedTopups || 0;
                if (pendingEl) pendingEl.textContent = stats.pendingTopups || 0;
            }
        } catch (error) {
            console.error("Lỗi tải thống kê nạp tiền:", error);
        }
    }

    async function loadTopups(page = 0) {
        const tbody = document.getElementById('topupsTableBody');
        if (!tbody) return;
        tbody.innerHTML = '<tr><td colspan="9" class="ds-table-center">Đang tải dữ liệu...</td></tr>';

        const status = document.getElementById('topupStatusFilter') ? document.getElementById('topupStatusFilter').value : '';
        const keyword = document.getElementById('topupKeywordFilter') ? document.getElementById('topupKeywordFilter').value.trim() : '';

        const params = new URLSearchParams();
        params.append('page', page);
        params.append('size', topupPageSize);
        if (status) params.append('status', status);
        if (keyword) params.append('keyword', keyword);

        try {
            const response = await authFetch('/v1/staff/topups?' + params.toString());
            if (response.ok) {
                const data = await response.json();
                currentTopupsList = data.content || [];
                renderTopupsTable(currentTopupsList, page, data.size);
                renderPagination(data);
            } else {
                tbody.innerHTML = '<tr><td colspan="9" class="ds-table-center ds-text-danger">Lỗi tải danh sách giao dịch nạp tiền.</td></tr>';
            }
        } catch (error) {
            console.error("Lỗi tải danh sách nạp tiền:", error);
            tbody.innerHTML = '<tr><td colspan="9" class="ds-table-center ds-text-danger">Không thể kết nối máy chủ.</td></tr>';
        }
    }

    function renderTopupsTable(items, currentPage, pageSize) {
        const tbody = document.getElementById('topupsTableBody');
        if (!tbody) return;
        tbody.innerHTML = '';

        if (!items || items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="ds-table-center" style="padding: 24px; color: #64748b;">Không tìm thấy giao dịch nạp tiền nào.</td></tr>';
            return;
        }

        items.forEach((item, index) => {
            const stt = currentPage * pageSize + index + 1;
            const tr = document.createElement('tr');

            const statusBadge = getStatusBadgeHtml(item.status);
            const amountStr = formatVnd(item.amountVnd) + ' đ';
            const userStr = item.userFullName 
                ? `<strong>${escapeHtml(item.userFullName)}</strong><br/><small style="color: #64748b;">${escapeHtml(item.userEmail || '')} (ID: ${item.userId})</small>`
                : `<span style="color: #ef4444; font-weight: 500;">Chưa nhận diện User</span>${item.userId ? `<br/><small style="color: #64748b;">ID: ${item.userId}</small>` : ''}`;

            const stUpper = item.status ? item.status.toUpperCase() : '';
            const canRetry = (stUpper === 'FAILED' || stUpper === 'PENDING');

            tr.innerHTML = `
                <td class="ds-table-center">${stt}</td>
                <td><strong>#TOPUP-${item.id}</strong></td>
                <td><span class="ds-badge ds-badge-info" style="font-family: monospace; font-weight: 600;">${escapeHtml(item.sepayCode || '-')}</span></td>
                <td>${userStr}</td>
                <td class="ds-table-right"><strong style="color: #059669; font-size: 14.5px;">${amountStr}</strong></td>
                <td><code style="background: #f1f5f9; padding: 2px 6px; border-radius: 4px; font-size: 12.5px; word-break: break-all;">${escapeHtml(item.transferContent || '-')}</code></td>
                <td class="ds-table-center">${statusBadge}</td>
                <td>${item.createdAt ? item.createdAt.replace('T', ' ').substring(0, 19) : '-'}</td>
                <td class="ds-table-center">
                    <div class="ds-table-actions" style="justify-content: center; gap: 6px;">
                        <a class="ds-icon-btn ds-icon-btn-view" href="/staff/topups/detail?id=${item.id}" title="Xem chi tiết" aria-label="Xem chi tiết">
                            <svg class="ds-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true" style="width: 16px; height: 16px;">
                                <path d="M2.25 12C3.73 8.12 7.49 5.25 12 5.25C16.51 5.25 20.27 8.12 21.75 12C20.27 15.88 16.51 18.75 12 18.75C7.49 18.75 3.73 15.88 2.25 12Z" stroke="currentColor" stroke-width="2"/>
                                <path d="M12 15.25C13.79 15.25 15.25 13.79 15.25 12C15.25 10.21 13.79 8.75 12 8.75C10.21 8.75 8.75 10.21 8.75 12C8.75 13.79 10.21 15.25 12 15.25Z" stroke="currentColor" stroke-width="2"/>
                            </svg>
                        </a>
                        ${canRetry ? `
                        <button class="ds-icon-btn" type="button" onclick="window.openTopupRetryModal(${item.id})" title="Thử lại / Kích hoạt lại" style="color: #d97706; background: #fffbeb; border: 1px solid #fde68a;">
                            <i class="fa fa-refresh"></i>
                        </button>
                        ` : ''}
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    function getStatusBadgeHtml(status) {
        if (!status) return '<span class="ds-badge ds-badge-warning">Chờ xử lý</span>';
        const stUpper = status.toUpperCase();
        if (stUpper === 'SUCCESS') return '<span class="ds-badge ds-badge-success">Thành công</span>';
        if (stUpper === 'FAILED') return '<span class="ds-badge ds-badge-danger">Thất bại</span>';
        return '<span class="ds-badge ds-badge-warning">Chờ xử lý</span>';
    }

    function renderPagination(data) {
        if (typeof window.mountStaffPagination === 'function') {
            window.mountStaffPagination('topupsPagination', {
                page: data.number,
                totalPages: data.totalPages,
                totalElements: data.totalElements,
                pageSize: data.size
            }, {
                onPage: (p) => { loadTopups(p); },
                onSize: (s) => { topupPageSize = s; loadTopups(0); }
            });
        }
    }

    window.openTopupRetryModal = function(topupId) {
        const item = currentTopupsList.find(t => t.id === topupId);
        if (item) {
            openModalWithItem(item);
        } else {
            fetchSingleTopupAndOpenModal(topupId);
        }
    };

    async function fetchSingleTopupAndOpenModal(topupId) {
        try {
            const response = await authFetch('/v1/staff/topups/' + topupId);
            if (response.ok) {
                const item = await response.json();
                openModalWithItem(item);
            }
        } catch (e) {
            console.error("Lỗi lấy thông tin topup:", e);
        }
    }

    function openModalWithItem(item) {
        activeRetryItem = item;
        const modal = document.getElementById('retryTopupModal');
        if (!modal) return;

        document.getElementById('modalTopupId').textContent = '#' + item.id;
        document.getElementById('modalSepayCode').textContent = item.sepayCode || '-';
        document.getElementById('modalAmount').textContent = formatVnd(item.amountVnd) + ' đ';
        document.getElementById('modalRawContent').textContent = item.transferContent || '(Nội dung rỗng)';
        
        const failureBox = document.getElementById('modalFailureReasonBox');
        if (item.failureReason) {
            failureBox.style.display = 'block';
            document.getElementById('modalFailureReason').textContent = item.failureReason;
        } else {
            failureBox.style.display = 'none';
        }

        const targetUserInput = document.getElementById('modalTargetUserId');
        if (targetUserInput) {
            targetUserInput.value = item.userId || '';
        }

        const skipMinCb = document.getElementById('modalSkipMinCheck');
        if (skipMinCb) skipMinCb.checked = false;

        const noteInput = document.getElementById('modalStaffNote');
        if (noteInput) {
            noteInput.value = item.failureReason ? `Khách gõ nhầm cú pháp (${item.failureReason}). Đã kiểm tra bill chuyển khoản khớp.` : 'Duyệt nạp tiền thủ công bởi Staff.';
        }

        modal.classList.add('is-open');
    }

    window.closeRetryModal = function() {
        const modal = document.getElementById('retryTopupModal');
        if (modal) modal.classList.remove('is-open');
        activeRetryItem = null;
    };

    async function confirmRetryTopup() {
        if (!activeRetryItem) return;

        const targetUserIdInput = document.getElementById('modalTargetUserId');
        const targetUserId = targetUserIdInput ? parseInt(targetUserIdInput.value) : null;
        if (!targetUserId || isNaN(targetUserId) || targetUserId <= 0) {
            alert("Vui lòng nhập ID người dùng hợp lệ để cộng tiền!");
            if (targetUserIdInput) targetUserIdInput.focus();
            return;
        }

        const skipMinCheck = document.getElementById('modalSkipMinCheck') ? document.getElementById('modalSkipMinCheck').checked : false;
        const staffNote = document.getElementById('modalStaffNote') ? document.getElementById('modalStaffNote').value.trim() : '';

        if (!staffNote) {
            alert("Vui lòng nhập lý do/ghi chú xử lý của Staff!");
            return;
        }

        const btn = document.getElementById('confirmRetryBtn');
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang xử lý...';
        }

        try {
            const response = await authFetch('/v1/staff/topups/' + activeRetryItem.id + '/retry', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    targetUserId: targetUserId,
                    skipMinCheck: skipMinCheck,
                    staffNote: staffNote
                })
            });

            const result = await response.json();
            if (response.ok) {
                alert(`Kích hoạt nạp tiền thành công! Số tiền ${formatVnd(activeRetryItem.amountVnd)}đ đã được cộng vào tài khoản người dùng ID ${targetUserId}.`);
                closeRetryModal();
                loadTopups(0);
                loadTopupStats();
            } else {
                alert('Lỗi duyệt nạp tiền: ' + (result.message || 'Thao tác không thành công.'));
            }
        } catch (error) {
            console.error("Lỗi kích hoạt nạp tiền thủ công:", error);
            alert('Không thể kết nối máy chủ để duyệt nạp tiền.');
        } finally {
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = '<i class="fa fa-check-circle"></i> Xác nhận kích hoạt';
            }
        }
    }

    function formatVnd(value) {
        if (value === null || value === undefined) return '0';
        return value.toLocaleString('vi-VN');
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
})();
