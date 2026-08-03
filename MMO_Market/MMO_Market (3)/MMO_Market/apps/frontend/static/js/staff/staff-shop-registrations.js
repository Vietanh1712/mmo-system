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
                const shopStatusSelect = document.getElementById('shopAccountStatusFilter');
                if (shopStatusSelect) shopStatusSelect.value = '';
                loadRegistrations(0);
                loadRegistrationStats();
            });
        }


    });

    async function loadRegistrationStats() {
        try {
            const response = await authFetch('/v1/shop-registrations/stats');
            if (response.ok) {
                const stats = await response.json();
                const totalEl = document.getElementById('stat-total-shops');
                const activeEl = document.getElementById('stat-active-shops');
                const depositEl = document.getElementById('stat-total-deposit');
                const bannedEl = document.getElementById('stat-banned-shops');
                const lockedEl = document.getElementById('stat-locked-shops');
                const suspendedEl = document.getElementById('stat-suspended-shops');
                const withdrawnEl = document.getElementById('stat-withdrawn-shops');

                if (totalEl) totalEl.textContent = stats.totalShops || 0;
                if (activeEl) activeEl.textContent = stats.activeShops || 0;
                if (depositEl) depositEl.textContent = formatVnd(stats.totalDeposit || 0) + ' đ';
                if (bannedEl) bannedEl.textContent = stats.permanentBannedShops || 0;
                if (lockedEl) lockedEl.textContent = stats.indefiniteLockedShops || 0;
                if (suspendedEl) suspendedEl.textContent = stats.temporarySuspendedShops || 0;
                if (withdrawnEl) withdrawnEl.textContent = stats.withdrawnShops || 0;
            }
        } catch (error) {
            console.error("Lỗi tải thống kê đăng ký Shop", error);
        }
    }

    async function loadRegistrations(page = 0) {
        const tbody = document.getElementById('shopRegistrationsTableBody');
        tbody.innerHTML = '<tr><td colspan="7" class="ds-table-center">Đang tải...</td></tr>';

        const shopStatusSelect = document.getElementById('shopAccountStatusFilter');
        const shopStatus = shopStatusSelect ? shopStatusSelect.value : '';
        const keywordInput = document.getElementById('shopKeywordFilter');
        const keyword = keywordInput ? keywordInput.value.trim() : '';

        let url = `/v1/shop-registrations?page=${page}&size=${shopPageSize}`;
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
                tbody.innerHTML = '<tr><td colspan="7" class="ds-table-center">Lỗi khi tải dữ liệu</td></tr>';
            }
        } catch (error) {
            tbody.innerHTML = '<tr><td colspan="7" class="ds-table-center">Lỗi kết nối</td></tr>';
        }
    }

    function renderTable(content, page, size) {
        const tbody = document.getElementById('shopRegistrationsTableBody');
        tbody.innerHTML = '';

        if (!content || content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="ds-table-center">Không có yêu cầu nào.</td></tr>';
            return;
        }

        content.forEach((item, index) => {
            const stt = page * size + index + 1;

            const shopStatusBadge = getShopStatusBadge(item.shopStatus);
            const depositFormatted = formatVnd(item.depositVnd);
            const balanceFormatted = formatVnd(item.balanceVnd);

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="ds-table-center">${stt}</td>
                <td>${item.code || item.id}</td>
                <td>
                    <div class="ds-entity-title">${item.shopName || ''}</div>
                </td>
                <td class="ds-table-center">${shopStatusBadge}</td>
                <td class="ds-table-right">${depositFormatted}</td>
                <td class="ds-table-right">${balanceFormatted}</td>

                <td class="ds-table-center">
                    <div class="ds-table-actions" style="justify-content: center;">
                        <a class="ds-icon-btn ds-icon-btn-view" href="/staff/shop-registrations/detail?id=${item.id}" title="Xem chi tiết" aria-label="Xem chi tiết">
                            <svg class="ds-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true" style="width: 16px; height: 16px;">
                                <path d="M2.25 12C3.73 8.12 7.49 5.25 12 5.25C16.51 5.25 20.27 8.12 21.75 12C20.27 15.88 16.51 18.75 12 18.75C7.49 18.75 3.73 15.88 2.25 12Z" stroke="currentColor" stroke-width="2"/>
                                <path d="M12 15.25C13.79 15.25 15.25 13.79 15.25 12C15.25 10.21 13.79 8.75 12 8.75C10.21 8.75 8.75 10.21 8.75 12C8.75 13.79 10.21 15.25 12 15.25Z" stroke="currentColor" stroke-width="2"/>
                            </svg>
                        </a>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });
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

    function formatVnd(value) {
        if (value === null || value === undefined) return '0';
        return value.toLocaleString('vi-VN');
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
                    } else if (stUpper === 'REJECTED') {
                        label = 'Bị từ chối';
                    } else if (stUpper === 'WITHDRAWN' || stUpper === 'DELETED') {
                        label = 'Đã đóng Shop (Hoàn phí)';
                    } else if (stUpper === 'SUSPENDED' || stUpper === 'TEMP_LOCKED') {
                        label = 'Tạm ngưng';
                    } else if (stUpper === 'LOCKED' || stUpper === 'INDEFINITE_LOCKED') {
                        label = 'Tạm khóa';
                    } else if (stUpper === 'BANNED' || stUpper === 'PERMANENT_BANNED') {
                        label = 'Khóa vĩnh viễn';
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


})();
