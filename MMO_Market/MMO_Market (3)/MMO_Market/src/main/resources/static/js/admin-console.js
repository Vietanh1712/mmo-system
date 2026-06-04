/**
 * Admin console — frontend only for config/reports; dashboard & accounts use API.
 */
(function () {
    const ENDPOINT = '/admin/user-management';
    const MOCK_KEY = 'mmo_admin_mock';

    const VIEWS = [
        'dashboard', 'audit-logs', 'revenue', 'add-staff', 'account-detail', 'system-config',
        'commissions', 'maintenance', 'accounts', 'permissions'
    ];

    const ALL_PERMISSIONS = [
        { id: 'APPROVE_KYC', label: 'Duyệt hồ sơ KYC', group: 'Kiểm duyệt' },
        { id: 'REVIEW_SUSPICIOUS', label: 'Rà soát giao dịch bất thường', group: 'Kiểm duyệt' },
        { id: 'AUDIT_ACCOUNTS', label: 'Kiểm tra tài khoản', group: 'Kiểm duyệt' },
        { id: 'APPROVE_WITHDRAWALS', label: 'Duyệt lệnh rút tiền', group: 'Tài chính' },
        { id: 'MANAGE_ESCROW', label: 'Quản lý giam tiền Escrow', group: 'Tài chính' },
        { id: 'VIEW_REVENUE_REPORTS', label: 'Xem báo cáo doanh thu', group: 'Tài chính' },
        { id: 'HANDLE_DISPUTES', label: 'Xử lý tranh chấp', group: 'Vận hành' },
        { id: 'MANAGE_REQUESTS', label: 'Quản lý yêu cầu hỗ trợ', group: 'Vận hành' }
    ];

    const MOCK_DEFAULT = {
        auditLogs: [
            { id: 101, timestamp: '2026-06-04T10:15:00Z', operator: 'tran.van.b@mmomarket.com', action: 'KYC_Approve', ipAddress: '192.168.1.50', desc: 'Duyệt KYC cho pham.duc.d@gmail.com', diff: '{ "kycStatus": "pending -> verified" }' },
            { id: 102, timestamp: '2026-06-04T09:40:00Z', operator: 'le.thi.c@mmomarket.com', action: 'Fund_Withdraw', ipAddress: '192.168.1.62', desc: 'Duyệt rút 5.000.000 VNĐ', diff: '{ "status": "pending -> completed" }' },
            { id: 103, timestamp: '2026-06-03T16:20:00Z', operator: 'admin@mmomarket.com', action: 'Config_Update', ipAddress: '113.161.40.85', desc: 'Cập nhật hạn mức rút tối thiểu', diff: '{ "minWithdrawal": 50000 -> 100000 }' },
            { id: 104, timestamp: '2026-06-03T11:00:00Z', operator: 'tran.van.b@mmomarket.com', action: 'Lock_User', ipAddress: '192.168.1.50', desc: 'Khóa hoang.thi.h@gmail.com', diff: '{ "isLocked": false -> true }' },
            { id: 105, timestamp: '2026-06-02T14:30:00Z', operator: 'admin@mmomarket.com', action: 'Maintenance_Toggle', ipAddress: '113.161.40.85', desc: 'Lên lịch bảo trì hệ thống', diff: '{ "scheduled": true }' }
        ],
        cashFlow: [
            { id: 'TX1001', timestamp: '2026-06-04T10:00:00Z', email: 'pham.duc.d@gmail.com', type: 'Deposit', amount: 20000000, fee: 0, status: 'Completed' },
            { id: 'TX1002', timestamp: '2026-06-04T09:40:00Z', email: 'nguyen.hoang.e@gmail.com', type: 'Withdrawal', amount: 5000000, fee: 75000, status: 'Completed' },
            { id: 'TX1003', timestamp: '2026-06-03T15:25:00Z', email: 'doan.van.g@gmail.com', type: 'Deposit', amount: 1500000, fee: 0, status: 'Completed' },
            { id: 'TX1004', timestamp: '2026-06-03T08:10:00Z', email: 'dang.thi.k@gmail.com', type: 'C2C_Purchase', amount: 4500000, fee: 226000, status: 'Completed' },
            { id: 'TX1005', timestamp: '2026-06-02T11:45:00Z', email: 'pham.duc.d@gmail.com', type: 'Withdrawal', amount: 50000000, fee: 750000, status: 'Completed' }
        ],
        permissions: {
            2: ['APPROVE_KYC', 'REVIEW_SUSPICIOUS', 'AUDIT_ACCOUNTS', 'HANDLE_DISPUTES', 'MANAGE_REQUESTS'],
            3: ['APPROVE_WITHDRAWALS', 'MANAGE_ESCROW', 'VIEW_REVENUE_REPORTS', 'HANDLE_DISPUTES', 'MANAGE_REQUESTS']
        },
        systemConfig: {
            appName: 'MMO Market System',
            adminEmail: 'alerts@mmomarket.com',
            sessionTimeout: 15,
            otpTimeout: 5,
            maxLoginRetries: 5,
            minWithdrawal: 50000,
            allowGoogleLogin: true,
            allowRegister: true,
            requireWithdraw2FA: true
        },
        commissions: {
            basePercent: 5.0,
            flatBuyerFee: 1000,
            withdrawalPercent: 1.5,
            minWithdrawFee: 10000
        },
        maintenance: {
            active: false,
            message: 'Hệ thống MMO Market đang bảo trì nâng cấp định kỳ. Xin lỗi vì sự bất tiện.',
            whitelist: '127.0.0.1',
            startTime: '2026-06-05T01:00',
            endTime: '2026-06-05T04:00'
        }
    };

    let mock = {};
    let users = [];
    let currentPage = 0;
    let currentPageSize = 10;
    let totalPages = 1;
    let totalElements = 0;
    let selectedStaffId = null;
    let auditPage = 0;
    let auditPageSize = 10;
    let auditFiltered = [];
    let revPage = 0;
    let revPageSize = 10;
    let revFiltered = [];
    let accountFormMode = 'create';
    let accountFormUserId = null;
    let accountFormReturnView = 'accounts';
    let accountFormActive = true;

    const ROLE_LABELS = {
        Admin: 'Quản trị viên',
        Staff: 'Nhân viên',
        Seller: 'Người bán',
        Customer: 'Khách hàng'
    };

    const ACTION_LABELS = {
        KYC_Approve: 'Duyệt KYC',
        Fund_Withdraw: 'Duyệt rút tiền',
        Lock_User: 'Khóa tài khoản',
        Config_Update: 'Cập nhật cấu hình',
        Maintenance_Toggle: 'Bảo trì hệ thống'
    };

    const TX_TYPE_LABELS = {
        Deposit: 'Nạp tiền',
        Withdrawal: 'Rút tiền',
        C2C_Purchase: 'Giao dịch C2C'
    };

    const TX_STATUS_LABELS = {
        Completed: 'Hoàn tất',
        Pending: 'Đang chờ',
        Failed: 'Thất bại'
    };

    const ICON_VIEW = `<svg class="ds-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M2.25 12C3.73 8.12 7.49 5.25 12 5.25C16.51 5.25 20.27 8.12 21.75 12C20.27 15.88 16.51 18.75 12 18.75C7.49 18.75 3.73 15.88 2.25 12Z" stroke="currentColor" stroke-width="2"/><path d="M12 15.25C13.79 15.25 15.25 13.79 15.25 12C15.25 10.21 13.79 8.75 12 8.75C10.21 8.75 8.75 10.21 8.75 12C8.75 13.79 10.21 15.25 12 15.25Z" stroke="currentColor" stroke-width="2"/></svg>`;

    const ICON_DELETE = `<svg class="ds-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 7H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M10 11V17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M14 11V17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M6 7L7 20C7.08 21.1 7.9 22 9 22H15C16.1 22 16.92 21.1 17 20L18 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M9 7V4C9 3.45 9.45 3 10 3H14C14.55 3 15 3.45 15 4V7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>`;

    document.addEventListener('DOMContentLoaded', () => {
        if (!guardAdminAccess()) return;
        initMock();
        bindEvents();
        navigateFromHash();
    });

    function navigateFromHash() {
        const raw = (window.location.hash || '#dashboard').slice(1);
        if (raw.startsWith('account-detail')) {
            const qIndex = raw.indexOf('?');
            const params = new URLSearchParams(qIndex >= 0 ? raw.slice(qIndex + 1) : '');
            const id = params.get('id');
            if (id) {
                accountFormUserId = Number(id);
                accountFormMode = 'detail';
                switchAdminView('account-detail');
                return;
            }
        }
        switchAdminView(raw.split('?')[0] || 'dashboard');
    }

    function initMock() {
        try {
            const stored = sessionStorage.getItem(MOCK_KEY);
            mock = stored ? JSON.parse(stored) : JSON.parse(JSON.stringify(MOCK_DEFAULT));
        } catch (e) {
            mock = JSON.parse(JSON.stringify(MOCK_DEFAULT));
        }
        saveMock();
    }

    function saveMock() {
        sessionStorage.setItem(MOCK_KEY, JSON.stringify(mock));
    }

    function bindEvents() {
        window.addEventListener('hashchange', navigateFromHash);

        document.getElementById('accountsSearchBtn')?.addEventListener('click', () => {
            currentPage = 0;
            loadUsers();
        });
        document.getElementById('accountsResetFilter')?.addEventListener('click', () => {
            const search = document.getElementById('searchInput');
            const role = document.getElementById('roleFilter');
            if (search) search.value = '';
            if (role) role.value = '';
            currentPage = 0;
            loadUsers();
        });
        document.getElementById('searchInput')?.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                currentPage = 0;
                loadUsers();
            }
        });

        document.getElementById('auditSearchBtn')?.addEventListener('click', () => {
            auditPage = 0;
            filterAuditLogs();
        });
        document.getElementById('auditResetFilter')?.addEventListener('click', () => {
            const s = document.getElementById('logSearch');
            const f = document.getElementById('logActionFilter');
            if (s) s.value = '';
            if (f) f.value = '';
            auditPage = 0;
            filterAuditLogs();
        });
        document.getElementById('logSearch')?.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') { e.preventDefault(); auditPage = 0; filterAuditLogs(); }
        });

        document.getElementById('revenueSearchBtn')?.addEventListener('click', () => {
            revPage = 0;
            renderRevenueView();
        });
        document.getElementById('revenueResetFilter')?.addEventListener('click', () => {
            const f = document.getElementById('revTimeFilter');
            if (f) f.value = '7days';
            revPage = 0;
            renderRevenueView();
        });

        document.getElementById('accountFormStatusToggle')?.addEventListener('click', () => {
            if (accountFormMode === 'detail-readonly') return;
            setAccountFormActive(!accountFormActive);
        });
        document.getElementById('accountFormSubmitBtn')?.addEventListener('click', () => submitAccountForm());
        document.getElementById('accountFormEmail')?.addEventListener('input', syncAccountFormProfile);
        document.getElementById('accountFormFullName')?.addEventListener('input', syncAccountFormProfile);
    }

    function syncAccountFormProfile() {
        const name = document.getElementById('accountFormFullName')?.value.trim();
        const email = document.getElementById('accountFormEmail')?.value.trim();
        const avatar = document.getElementById('accountFormAvatar');
        const nameEl = document.getElementById('accountFormProfileName');
        const emailEl = document.getElementById('accountFormProfileEmail');
        const displayName = name || (accountFormMode === 'create' ? 'Nhân viên mới' : '—');
        const displayEmail = email || '—';
        if (nameEl) nameEl.textContent = displayName;
        if (emailEl) emailEl.textContent = displayEmail;
        if (avatar) avatar.textContent = String(displayName).charAt(0).toUpperCase();
    }

    function setAccountFormLayoutMode(mode) {
        const isDetail = mode === 'detail';
        const createAside = document.getElementById('accountFormAsideCreate');
        const statsAside = document.getElementById('accountFormAsideStats');
        const profileRole = document.getElementById('accountFormProfileRole');
        const modeBadge = document.getElementById('accountFormModeBadge');
        if (createAside) createAside.style.display = isDetail ? 'none' : 'block';
        if (statsAside) statsAside.style.display = isDetail ? 'grid' : 'none';
        if (profileRole) profileRole.style.display = isDetail ? 'inline-flex' : 'none';
        if (modeBadge) {
            modeBadge.textContent = isDetail ? 'Chi tiết' : 'Tạo mới';
            modeBadge.className = `ds-badge account-form-mode-badge ${isDetail ? 'ds-badge-muted' : 'ds-badge-info'}`;
        }
        setText('accountFormKicker', isDetail ? 'Hồ sơ thành viên' : 'Quản lý tài khoản');
    }

    function sttNumber(page, pageSize, index) {
        return page * pageSize + index + 1;
    }

    function roleLabel(role) {
        return ROLE_LABELS[role] || role;
    }

    function actionLabel(action) {
        return ACTION_LABELS[action] || action;
    }

    function txTypeLabel(type) {
        return TX_TYPE_LABELS[type] || type;
    }

    function txStatusLabel(status) {
        return TX_STATUS_LABELS[status] || status;
    }

    function mountPagination(containerId, state, handlers) {
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

    function tableActionsView(onclick, title) {
        return `<button type="button" class="ds-icon-btn ds-icon-btn-view" title="${escapeHtml(title)}" aria-label="${escapeHtml(title)}" onclick="${onclick}">${ICON_VIEW}</button>`;
    }

    function tableActionsDelete(onclick, title) {
        return `<button type="button" class="ds-icon-btn ds-icon-btn-delete" title="${escapeHtml(title)}" aria-label="${escapeHtml(title)}" onclick="${onclick}">${ICON_DELETE}</button>`;
    }

    function resolveViewElementId(target) {
        if (target === 'accounts') return 'accountsView';
        if (target === 'add-staff' || target === 'account-detail') return 'accountFormView';
        return `${target}View`;
    }

    window.switchAdminView = function (viewName) {
        const target = VIEWS.includes(viewName) ? viewName : 'dashboard';
        const viewId = resolveViewElementId(target);

        if (target !== 'account-detail' && window.location.hash !== `#${target}`) {
            window.history.replaceState(null, '', `#${target}`);
        }

        document.querySelectorAll('.admin-view').forEach(el => {
            el.classList.toggle('active', el.id === viewId);
        });
        document.querySelectorAll('.sidebar-item').forEach(el => {
            el.classList.toggle('active', el.getAttribute('data-target') === target);
        });

        loadViewData(target);
    };

    function loadViewData(view) {
        switch (view) {
            case 'dashboard': loadDashboard(); break;
            case 'audit-logs': filterAuditLogs(); break;
            case 'revenue': renderRevenueView(); break;
            case 'add-staff': prepareAccountFormCreate(); break;
            case 'account-detail': loadAccountFormDetail(); break;
            case 'system-config': loadSystemConfigForm(); break;
            case 'commissions': loadCommissionsForm(); break;
            case 'maintenance': loadMaintenanceForm(); break;
            case 'accounts': loadUsers(); break;
            case 'permissions': loadPermissionsView(); break;
        }
    }

    function guardAdminAccess() {
        const token = sessionStorage.getItem('accessToken');
        const user = readCurrentUser();
        if (!token || !user) {
            window.location.href = '/login';
            return false;
        }
        if (normalizeRole(user.role) !== 'Admin') {
            showToast('Bạn không có quyền truy cập trang quản trị.', true);
            setTimeout(() => window.location.href = '/', 900);
            return false;
        }
        return true;
    }

    /* ---------- API: Dashboard ---------- */
    async function loadDashboard() {
        try {
            const response = await authFetch(`${ENDPOINT}/summary`);
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || 'Không thể tải dashboard.');
            setText('statTotal', data.totalAccounts ?? 0);
            setText('statActive', data.activeAccounts ?? 0);
            setText('statLocked', data.lockedAccounts ?? 0);
            setText('statStaff', data.staffAccounts ?? 0);
            setText('statVerified', data.verifiedAccounts ?? 0);
            setText('statSeller', data.sellerAccounts ?? 0);
            renderDashboardChart();
            renderDashboardRecentLogs();
        } catch (error) {
            showToast(error.message, true);
        }
    }

    function renderDashboardChart() {
        const fees = mock.cashFlow.reduce((s, t) => s + (t.fee || 0), 0);
        const dataPoints = [1200000, 2450000, 1800000, 3100000, 2900000, 4200000, fees];
        const labels = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ nhật'];
        drawRevenueChart(dataPoints, labels);

        const summary = document.getElementById('chartSummary');
        if (summary) {
            const total = dataPoints.reduce((a, b) => a + b, 0);
            const peak = Math.max(...dataPoints);
            const peakIdx = dataPoints.indexOf(peak);
            summary.innerHTML = `
                <span class="ds-caption">Tổng tuần: <strong class="ds-money">${formatVnd(total)}</strong></span>
                <span class="ds-caption">Cao nhất: <strong class="ds-money">${formatVnd(peak)}</strong> (${labels[peakIdx]})</span>
                <span class="ds-caption">Trung bình/ngày: <strong class="ds-money">${formatVnd(Math.round(total / dataPoints.length))}</strong></span>
            `;
        }
    }

    function renderDashboardRecentLogs() {
        const body = document.getElementById('dashLogsBody');
        if (!body) return;
        const rows = mock.auditLogs.slice(0, 4);
        body.innerHTML = rows.map((l, i) => `
            <tr>
                <td class="ds-table-center">${i + 1}</td>
                <td>${formatDateTime(l.timestamp)}</td>
                <td><strong>${escapeHtml(l.operator)}</strong></td>
                <td><span class="ds-badge ${auditBadgeClass(l.action)}">${escapeHtml(actionLabel(l.action))}</span></td>
                <td class="muted">${escapeHtml(l.desc)}</td>
            </tr>
        `).join('') || '<tr><td colspan="5" class="ds-empty-state">Chưa có nhật ký.</td></tr>';
    }

    const CHART_DAY_LABELS = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ nhật'];

    function formatVndShort(value) {
        const n = Number(value) || 0;
        if (n >= 1_000_000_000) return `${(n / 1_000_000_000).toFixed(1)} tỷ`;
        if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)} tr`;
        if (n >= 1_000) return `${Math.round(n / 1_000)}K`;
        return String(n);
    }

    function drawRevenueChart(dataPoints, dayLabels = CHART_DAY_LABELS) {
        const pathEl = document.getElementById('revenueChartPath');
        const areaEl = document.getElementById('revenueChartArea');
        const dotsEl = document.getElementById('revenueChartDots');
        const gridEl = document.getElementById('revenueChartGrid');
        const yAxisEl = document.getElementById('revenueChartYAxis');
        const xAxisEl = document.getElementById('revenueChartXAxis');
        const valueLabelsEl = document.getElementById('revenueChartValueLabels');
        if (!pathEl || !dataPoints.length) return;

        const width = 800;
        const height = 280;
        const paddingLeft = 72;
        const paddingRight = 28;
        const paddingTop = 28;
        const paddingBottom = 52;
        const chartWidth = width - paddingLeft - paddingRight;
        const chartHeight = height - paddingTop - paddingBottom;
        const rawMax = Math.max(...dataPoints, 1);
        const scaleMax = Math.ceil((rawMax * 1.12) / 500_000) * 500_000 || rawMax * 1.15;
        const step = chartWidth / Math.max(dataPoints.length - 1, 1);
        const yTickCount = 4;

        let gridHtml = '';
        let yAxisHtml = '';
        for (let i = 0; i <= yTickCount; i++) {
            const ratio = i / yTickCount;
            const y = paddingTop + ratio * chartHeight;
            const tickValue = scaleMax * (1 - ratio);
            gridHtml += `<line x1="${paddingLeft}" y1="${y}" x2="${width - paddingRight}" y2="${y}" stroke="#e5e7eb" stroke-width="1" stroke-dasharray="${i === yTickCount ? '0' : '4 4'}"/>`;
            yAxisHtml += `<text x="${paddingLeft - 10}" y="${y + 4}" class="chart-axis-text" fill="#6b7280" font-size="10" font-weight="600" text-anchor="end">${formatVndShort(tickValue)}</text>`;
        }
        gridHtml += `<line x1="${paddingLeft}" y1="${paddingTop}" x2="${paddingLeft}" y2="${height - paddingBottom}" stroke="#cbd5e1" stroke-width="1.5"/>`;
        gridHtml += `<line x1="${paddingLeft}" y1="${height - paddingBottom}" x2="${width - paddingRight}" y2="${height - paddingBottom}" stroke="#cbd5e1" stroke-width="1.5"/>`;

        let pathD = '';
        let areaD = `M ${paddingLeft} ${height - paddingBottom}`;
        let dotsHtml = '';
        let xAxisHtml = '';
        let valueLabelsHtml = '';

        dataPoints.forEach((val, idx) => {
            const x = paddingLeft + idx * step;
            const y = height - paddingBottom - (val / scaleMax) * chartHeight;
            pathD += idx === 0 ? `M ${x} ${y}` : ` L ${x} ${y}`;
            areaD += ` L ${x} ${y}`;
            const shortLabel = (dayLabels[idx] || '').replace('Thứ ', 'T').replace('Chủ nhật', 'CN');
            xAxisHtml += `<text x="${x}" y="${height - paddingBottom + 22}" fill="#6b7280" font-size="11" font-weight="700" text-anchor="middle">${escapeHtml(shortLabel)}</text>`;
            valueLabelsHtml += `<text x="${x}" y="${y - 10}" class="chart-value-label">${formatVndShort(val)}</text>`;
            dotsHtml += `<circle cx="${x}" cy="${y}" r="5" class="chart-dot"
                data-day="${idx}"
                onmouseover="AdminConsole.showChartTooltip(event, ${val}, ${idx})"
                onmouseout="AdminConsole.hideChartTooltip()"/>`;
        });

        areaD += ` L ${paddingLeft + (dataPoints.length - 1) * step} ${height - paddingBottom} Z`;

        if (gridEl) gridEl.innerHTML = gridHtml;
        if (yAxisEl) yAxisEl.innerHTML = yAxisHtml;
        if (xAxisEl) xAxisEl.innerHTML = xAxisHtml;
        if (valueLabelsEl) valueLabelsEl.innerHTML = valueLabelsHtml;
        pathEl.setAttribute('d', pathD);
        areaEl.setAttribute('d', areaD);
        dotsEl.innerHTML = dotsHtml;
    }

    window.AdminConsole = {
        showChartTooltip(e, value, dayIdx) {
            const tooltip = document.getElementById('chartTooltip');
            if (!tooltip) return;
            tooltip.innerHTML = `<strong>${CHART_DAY_LABELS[dayIdx] || ''}</strong><br>Doanh thu: <span class="ds-money">${formatVnd(value)}</span>`;
            tooltip.style.display = 'block';
            const container = e.target.closest('.chart-svg-container');
            if (!container) return;
            const rect = container.getBoundingClientRect();
            const dot = e.target.getBoundingClientRect();
            tooltip.style.left = `${dot.left - rect.left + 12}px`;
            tooltip.style.top = `${dot.top - rect.top - 48}px`;
        },
        hideChartTooltip() {
            const t = document.getElementById('chartTooltip');
            if (t) t.style.display = 'none';
        }
    };

    /* ---------- API: Accounts ---------- */
    async function loadUsers() {
        setLoading(true);
        const params = new URLSearchParams({
            page: String(currentPage),
            size: String(currentPageSize)
        });
        const keyword = document.getElementById('searchInput').value.trim();
        if (keyword) params.set('name', keyword);
        const roleVal = document.getElementById('roleFilter').value;
        if (roleVal) params.set('role', roleVal);
        try {
            const response = await authFetch(`${ENDPOINT}/users?${params.toString()}`);
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || 'Không thể tải danh sách.');
            users = data.content || [];
            currentPage = data.page ?? 0;
            totalPages = Math.max(data.totalPages ?? 1, 1);
            totalElements = data.totalElements ?? 0;
            renderUsers(users);
            renderPagination();
            loadDashboard();
        } catch (error) {
            document.getElementById('usersBody').innerHTML =
                `<tr><td colspan="9" class="ds-empty-state">${escapeHtml(error.message)}</td></tr>`;
            showToast(error.message, true);
        }
    }

    function renderUsers(list) {
        const tbody = document.getElementById('usersBody');
        if (!list.length) {
            tbody.innerHTML = '<tr><td colspan="9" class="ds-empty-state">Không tìm thấy tài khoản.</td></tr>';
            return;
        }
        tbody.innerHTML = list.map((user, index) => {
            const initial = String(user.fullName || user.email || '?').charAt(0).toUpperCase();
            const locked = Boolean(user.isLocked);
            const role = user.role || 'Customer';
            const balance = formatVnd(user.balanceVnd || 0);
            const createdAt = user.createdAt ? new Date(user.createdAt).toLocaleDateString('vi-VN') : '—';
            const self = user.id === readCurrentUser()?.id;
            const isAdmin = role === 'Admin';
            const canDelete = !self && !isAdmin;

            return `
                <tr>
                    <td class="ds-table-center">${sttNumber(currentPage, currentPageSize, index)}</td>
                    <td class="ds-table-center">${user.id}</td>
                    <td>
                        <div class="ds-entity">
                            <span class="ds-avatar ds-avatar-sm ds-avatar-primary">${escapeHtml(initial)}</span>
                            <div>
                                <div class="ds-entity-title">${escapeHtml(user.fullName || 'Chưa cập nhật')}</div>
                                <div class="ds-entity-subtitle">${escapeHtml(user.email || '')}</div>
                            </div>
                        </div>
                    </td>
                    <td class="ds-table-center"><span class="ds-badge ${roleBadgeClass(role)}">${escapeHtml(roleLabel(role))}</span></td>
                    <td class="ds-table-center"><span class="ds-badge ${user.isVerified ? 'ds-badge-success' : 'ds-badge-warning'}">${user.isVerified ? 'Đã xác thực' : 'Chưa xác thực'}</span></td>
                    <td><span class="ds-money">${balance}</span></td>
                    <td>${createdAt}</td>
                    <td class="ds-table-center">${statusToggleCell(user, self)}</td>
                    <td>
                        <div class="ds-table-actions">
                            ${tableActionsView(`AdminConsole.openAccountDetail(${user.id})`, 'Xem chi tiết')}
                            ${canDelete ? tableActionsDelete(`AdminConsole.softDeleteUser(${user.id})`, 'Xóa tài khoản') : ''}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    function renderPagination() {
        mountPagination('accountsPagination', {
            page: currentPage,
            totalPages,
            totalElements,
            pageSize: currentPageSize
        }, {
            onPage: (p) => {
                currentPage = p;
                loadUsers();
            },
            onSize: (s) => {
                currentPageSize = s;
                currentPage = 0;
                loadUsers();
            }
        });
    }

    function statusToggleCell(user, disabled) {
        const locked = Boolean(user.isLocked);
        const inactive = locked ? ' ds-toggle-inactive' : '';
        const label = locked ? 'Tạm dừng' : 'Đang hoạt động';
        return `
            <button type="button" class="ds-toggle${inactive}" aria-pressed="${!locked}" ${disabled ? 'disabled' : ''}
                onclick="AdminConsole.toggleStatus(${user.id}, this)">
                <span class="ds-toggle-track"><span class="ds-toggle-knob"></span></span>
                <span class="ds-toggle-label">${label}</span>
            </button>
        `;
    }

    window.AdminConsole.toggleStatus = async function (userId, btn) {
        const user = users.find(u => u.id === userId);
        if (!user || userId === readCurrentUser()?.id) return;
        try {
            const response = await authFetch(`${ENDPOINT}/users/${userId}/toggle-lock`, { method: 'POST' });
            const data = await response.json();
            if (!response.ok || !data.success) throw new Error(data.message || 'Không thể đổi trạng thái.');
            user.isLocked = data.isLocked;
            const locked = Boolean(data.isLocked);
            btn.classList.toggle('ds-toggle-inactive', locked);
            btn.setAttribute('aria-pressed', String(!locked));
            const labelEl = btn.querySelector('.ds-toggle-label');
            if (labelEl) labelEl.textContent = locked ? 'Tạm dừng' : 'Đang hoạt động';
            showToast(data.message || 'Đã cập nhật trạng thái.');
        } catch (error) {
            showToast(error.message, true);
        }
    };

    window.AdminConsole.softDeleteUser = async function (userId) {
        const user = users.find(u => u.id === userId);
        if (!user) return;
        if (!confirm(`Xác nhận xóa (mềm) tài khoản ${user.email}?`)) return;
        try {
            const response = await authFetch(`${ENDPOINT}/users/${userId}`, { method: 'DELETE' });
            const data = await response.json();
            if (!response.ok || !data.success) throw new Error(data.message || 'Không thể xóa tài khoản.');
            showToast(data.message || 'Đã xóa tài khoản.');
            loadUsers();
        } catch (error) {
            showToast(error.message, true);
        }
    };

    /* ---------- Form thêm / chi tiết tài khoản ---------- */
    window.AdminConsole.openCreateStaff = function () {
        accountFormMode = 'create';
        accountFormUserId = null;
        accountFormReturnView = 'accounts';
        switchAdminView('add-staff');
    };

    window.AdminConsole.openAccountDetail = function (userId) {
        accountFormMode = 'detail';
        accountFormUserId = userId;
        accountFormReturnView = 'accounts';
        window.history.replaceState(null, '', `#account-detail?id=${userId}`);
        switchAdminView('account-detail');
    };

    window.AdminConsole.backFromAccountForm = function () {
        switchAdminView(accountFormReturnView || 'accounts');
    };

    function prepareAccountFormCreate() {
        accountFormMode = 'create';
        accountFormUserId = null;
        setAccountFormLayoutMode('create');
        setText('accountFormTitle', 'Thêm tài khoản nhân viên');
        setText('accountFormCaption', 'Điền thông tin để tạo tài khoản nhân viên mới.');
        setText('accountFormSubmitLabel', 'Tạo nhân viên');
        const submitBtn = document.getElementById('accountFormSubmitBtn');
        if (submitBtn) submitBtn.style.display = '';
        const actionsBar = document.querySelector('.account-form-actions');
        if (actionsBar) actionsBar.style.display = '';
        resetAccountFormFields();
        setAccountFormEditable(true);
        setAccountFormActive(true);
        document.getElementById('accountFormEmail').disabled = false;
        document.getElementById('accountFormPasswordWrap').style.display = '';
        document.getElementById('accountFormPassword').required = true;
        document.getElementById('accountFormPasswordRequired').style.display = '';
        syncAccountFormProfile();
    }

    async function loadAccountFormDetail() {
        if (!accountFormUserId) {
            AdminConsole.backFromAccountForm();
            return;
        }
        try {
            const response = await authFetch(`${ENDPOINT}/users/${accountFormUserId}`);
            const user = await response.json();
            if (!response.ok) throw new Error(user.message || 'Không tải được tài khoản.');
            fillAccountForm(user);
        } catch (error) {
            showToast(error.message, true);
            AdminConsole.backFromAccountForm();
        }
    }

    function fillAccountForm(user) {
        const role = user.role || 'Customer';
        const isStaff = role === 'Staff';
        accountFormMode = isStaff ? 'detail-edit' : 'detail-readonly';

        setAccountFormLayoutMode('detail');
        setText('accountFormTitle', user.fullName || 'Chi tiết tài khoản');
        setText('accountFormCaption', `Mã #${user.id} · ${user.email || ''}`);
        setText('accountFormSubmitLabel', 'Lưu thay đổi');

        document.getElementById('accountFormId').value = user.id || '';
        document.getElementById('accountFormEmail').value = user.email || '';
        document.getElementById('accountFormFullName').value = user.fullName || '';
        document.getElementById('accountFormPhone').value = user.phone || '';
        document.getElementById('accountFormAddress').value = user.address || '';
        document.getElementById('accountFormNationalId').value = user.nationalId || '';
        document.getElementById('accountFormBirthDate').value = user.dateOfBirth || '';
        const gender = (user.gender || 'Nam').toLowerCase();
        document.querySelectorAll('input[name="accountFormGender"]').forEach(r => {
            r.checked = (gender.startsWith('n') && r.value === 'Nữ') || (!gender.startsWith('n') && r.value === 'Nam');
        });
        setAccountFormActive(!user.isLocked);
        syncAccountFormProfile();

        setText('accountFormIdDisplay', user.id ? `#${user.id}` : '—');
        const roleEl = document.getElementById('accountFormRoleBadge');
        roleEl.textContent = roleLabel(role);
        roleEl.className = `ds-badge ${roleBadgeClass(role)}`;
        const profileRole = document.getElementById('accountFormProfileRole');
        if (profileRole) {
            profileRole.textContent = roleLabel(role);
            profileRole.className = `ds-badge ${roleBadgeClass(role)}`;
            profileRole.style.display = 'inline-flex';
        }
        document.getElementById('accountFormVerifyBadge').innerHTML = user.isVerified
            ? '<span class="ds-badge ds-badge-success">Đã xác thực</span>'
            : '<span class="ds-badge ds-badge-warning">Chưa xác thực</span>';
        document.getElementById('accountFormBalance').textContent = formatVnd(user.balanceVnd || 0);
        document.getElementById('accountFormCreatedAt').textContent = user.createdAt
            ? new Date(user.createdAt).toLocaleString('vi-VN') : '—';

        const submitBtn = document.getElementById('accountFormSubmitBtn');
        const actionsBar = document.querySelector('.account-form-actions');
        if (isStaff) {
            document.getElementById('accountFormEmail').disabled = true;
            document.getElementById('accountFormPasswordWrap').style.display = '';
            document.getElementById('accountFormPassword').value = '';
            document.getElementById('accountFormPassword').required = false;
            document.getElementById('accountFormPasswordRequired').style.display = 'none';
            if (submitBtn) submitBtn.style.display = '';
            if (actionsBar) actionsBar.style.display = '';
            setAccountFormEditable(true);
        } else {
            document.getElementById('accountFormEmail').disabled = true;
            document.getElementById('accountFormPasswordWrap').style.display = 'none';
            if (submitBtn) submitBtn.style.display = 'none';
            if (actionsBar) actionsBar.style.display = 'none';
            setAccountFormEditable(false);
        }
    }

    function resetAccountFormFields() {
        document.getElementById('accountForm').reset();
        document.getElementById('accountFormId').value = '';
        document.querySelector('input[name="accountFormGender"][value="Nam"]').checked = true;
    }

    function setAccountFormEditable(editable) {
        ['accountFormFullName', 'accountFormPhone', 'accountFormAddress', 'accountFormNationalId', 'accountFormBirthDate'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.disabled = !editable;
        });
        document.querySelectorAll('input[name="accountFormGender"]').forEach(r => { r.disabled = !editable; });
        const toggle = document.getElementById('accountFormStatusToggle');
        if (toggle) toggle.disabled = !editable;
    }

    function setAccountFormActive(active) {
        accountFormActive = active;
        const btn = document.getElementById('accountFormStatusToggle');
        const label = document.getElementById('accountFormStatusLabel');
        if (!btn || !label) return;
        btn.classList.toggle('ds-toggle-inactive', !active);
        btn.setAttribute('aria-pressed', String(active));
        label.textContent = active ? 'Đang hoạt động' : 'Tạm dừng';
    }

    function readAccountFormPayload() {
        const gender = document.querySelector('input[name="accountFormGender"]:checked')?.value || 'Nam';
        const birth = document.getElementById('accountFormBirthDate').value;
        return {
            email: document.getElementById('accountFormEmail').value.trim(),
            fullName: document.getElementById('accountFormFullName').value.trim(),
            phone: document.getElementById('accountFormPhone').value.trim(),
            password: document.getElementById('accountFormPassword').value,
            gender,
            address: document.getElementById('accountFormAddress').value.trim(),
            nationalId: document.getElementById('accountFormNationalId').value.trim(),
            dateOfBirth: birth || null,
            active: accountFormActive
        };
    }

    async function submitAccountForm() {
        const id = document.getElementById('accountFormId').value;
        const payload = readAccountFormPayload();

        if (accountFormMode === 'create') {
            if (!payload.email || !payload.fullName || !payload.password) {
                showToast('Vui lòng nhập email, họ tên và mật khẩu.', true);
                return;
            }
            try {
                const response = await authFetch(`${ENDPOINT}/staff`, { method: 'POST', body: JSON.stringify(payload) });
                const data = await response.json();
                if (!response.ok) throw new Error(data.message || 'Không thể tạo nhân viên.');
                showToast('Đã tạo tài khoản nhân viên.');
                switchAdminView('accounts');
                loadUsers();
            } catch (error) {
                showToast(error.message, true);
            }
            return;
        }

        if (accountFormMode === 'detail-edit' && id) {
            if (!payload.fullName) {
                showToast('Họ tên không được để trống.', true);
                return;
            }
            const body = { ...payload };
            delete body.email;
            if (!body.password) delete body.password;
            try {
                const response = await authFetch(`${ENDPOINT}/staff/${id}`, { method: 'PUT', body: JSON.stringify(body) });
                const data = await response.json();
                if (!response.ok) throw new Error(data.message || 'Không thể lưu.');
                showToast('Đã cập nhật nhân viên.');
                loadUsers();
                AdminConsole.openAccountDetail(Number(id));
            } catch (error) {
                showToast(error.message, true);
            }
        }
    }

    /* ---------- Mock: Audit logs ---------- */
    function filterAuditLogs() {
        const q = (document.getElementById('logSearch')?.value || '').toLowerCase();
        const action = document.getElementById('logActionFilter')?.value || '';
        let list = [...mock.auditLogs];
        if (q) {
            list = list.filter(l =>
                l.operator.toLowerCase().includes(q) ||
                l.desc.toLowerCase().includes(q) ||
                l.ipAddress.includes(q)
            );
        }
        if (action) list = list.filter(l => l.action === action);

        auditFiltered = list;
        const total = list.length;
        const totalPg = Math.max(Math.ceil(total / auditPageSize), 1);
        if (auditPage >= totalPg) auditPage = totalPg - 1;
        const slice = list.slice(auditPage * auditPageSize, auditPage * auditPageSize + auditPageSize);

        const body = document.getElementById('auditLogsBody');
        if (!body) return;
        body.innerHTML = slice.length ? slice.map((l, i) => `
            <tr>
                <td class="ds-table-center">${sttNumber(auditPage, auditPageSize, i)}</td>
                <td class="ds-table-center">${l.id}</td>
                <td>${formatDateTime(l.timestamp)}</td>
                <td>${escapeHtml(l.operator)}</td>
                <td class="ds-table-center"><span class="ds-badge ${auditBadgeClass(l.action)}">${escapeHtml(actionLabel(l.action))}</span></td>
                <td class="muted">${escapeHtml(l.desc)}</td>
                <td>${escapeHtml(l.ipAddress)}</td>
                <td class="ds-table-center"><span class="ds-badge ds-badge-success">Thành công</span></td>
                <td>
                    <div class="ds-table-actions">
                        ${tableActionsView(`AdminConsole.openLogDetail(${l.id})`, 'Xem chi tiết')}
                    </div>
                </td>
            </tr>
        `).join('') : '<tr><td colspan="9" class="ds-empty-state">Không có nhật ký phù hợp.</td></tr>';

        mountPagination('auditPagination', {
            page: auditPage,
            totalPages: totalPg,
            totalElements: total,
            pageSize: auditPageSize
        }, {
            onPage: (p) => { auditPage = p; filterAuditLogs(); },
            onSize: (s) => { auditPageSize = s; auditPage = 0; filterAuditLogs(); }
        });
    }

    window.AdminConsole.openLogDetail = function (id) {
        const log = mock.auditLogs.find(l => l.id === id);
        if (!log) return;
        document.getElementById('logDetTime').textContent = formatDateTime(log.timestamp);
        document.getElementById('logDetOperator').textContent = log.operator;
        const actEl = document.getElementById('logDetAction');
        actEl.textContent = actionLabel(log.action);
        actEl.className = `ds-badge ${auditBadgeClass(log.action)}`;
        document.getElementById('logDetIp').textContent = log.ipAddress;
        document.getElementById('logDetDesc').textContent = log.desc;
        document.getElementById('logDetDiff').textContent = log.diff || '—';
        document.getElementById('logDetailModal').classList.add('active');
    };

    window.AdminConsole.closeLogDetail = function () {
        document.getElementById('logDetailModal').classList.remove('active');
    };

    /* ---------- Mock: Revenue ---------- */
    function renderRevenueView() {
        const filter = document.getElementById('revTimeFilter')?.value || '7days';
        let txs = [...mock.cashFlow];
        if (filter === 'today') txs = txs.slice(0, 2);
        else if (filter === '30days') { /* giữ nguyên demo */ }

        revFiltered = txs;
        const commissions = txs.filter(t => t.type === 'C2C_Purchase').reduce((s, t) => s + t.fee * 0.8, 0);
        const buyerFees = txs.filter(t => t.type === 'C2C_Purchase').length * (mock.commissions.flatBuyerFee || 1000);
        const withdrawFees = txs.filter(t => t.type === 'Withdrawal').reduce((s, t) => s + (t.fee || 0), 0);
        const net = commissions + buyerFees + withdrawFees;

        setText('revCommissions', formatVnd(commissions));
        setText('revBuyerFees', formatVnd(buyerFees));
        setText('revWithdrawalFees', formatVnd(withdrawFees));
        setText('revNetTotal', formatVnd(net));

        const total = txs.length;
        const totalPg = Math.max(Math.ceil(total / revPageSize), 1);
        if (revPage >= totalPg) revPage = totalPg - 1;
        const slice = txs.slice(revPage * revPageSize, revPage * revPageSize + revPageSize);

        const body = document.getElementById('revTransactionsBody');
        if (!body) return;
        body.innerHTML = slice.length ? slice.map((t, i) => {
            const typeClass = t.type === 'Deposit' ? 'ds-badge-success' : (t.type === 'Withdrawal' ? 'ds-badge-warning' : 'ds-badge-info');
            return `
                <tr>
                    <td class="ds-table-center">${sttNumber(revPage, revPageSize, i)}</td>
                    <td class="ds-table-center"><code>${escapeHtml(t.id)}</code></td>
                    <td>${formatDateTime(t.timestamp)}</td>
                    <td>${escapeHtml(t.email)}</td>
                    <td class="ds-table-center"><span class="ds-badge ${typeClass}">${escapeHtml(txTypeLabel(t.type))}</span></td>
                    <td class="ds-money">${formatVnd(t.amount)}</td>
                    <td class="ds-money">${formatVnd(t.fee)}</td>
                    <td class="ds-table-center"><span class="ds-badge ds-badge-success">${escapeHtml(txStatusLabel(t.status))}</span></td>
                </tr>
            `;
        }).join('') : '<tr><td colspan="8" class="ds-empty-state">Không có giao dịch phù hợp.</td></tr>';

        mountPagination('revenuePagination', {
            page: revPage,
            totalPages: totalPg,
            totalElements: total,
            pageSize: revPageSize
        }, {
            onPage: (p) => { revPage = p; renderRevenueView(); },
            onSize: (s) => { revPageSize = s; revPage = 0; renderRevenueView(); }
        });
    }

    window.AdminConsole.mockExport = function (type) {
        const label = type === 'audit' ? 'nhật ký' : 'doanh thu';
        showToast(`Đang xuất báo cáo ${label}...`);
        setTimeout(() => showToast(`Đã tải báo cáo ${label} (demo).`), 1200);
    };

    /* ---------- Mock: System config ---------- */
    function loadSystemConfigForm() {
        const c = mock.systemConfig;
        document.getElementById('cfgAppName').value = c.appName;
        document.getElementById('cfgAdminEmail').value = c.adminEmail;
        document.getElementById('cfgSessionTimeout').value = c.sessionTimeout;
        document.getElementById('cfgOtpTimeout').value = c.otpTimeout;
        document.getElementById('cfgMaxLoginRetries').value = c.maxLoginRetries;
        document.getElementById('cfgMinWithdrawal').value = c.minWithdrawal;
        document.getElementById('cfgAllowGoogle').checked = c.allowGoogleLogin;
        document.getElementById('cfgAllowRegister').checked = c.allowRegister;
        document.getElementById('cfgWithdraw2FA').checked = c.requireWithdraw2FA;
    }

    window.AdminConsole.saveSystemConfig = function () {
        mock.systemConfig = {
            appName: document.getElementById('cfgAppName').value.trim(),
            adminEmail: document.getElementById('cfgAdminEmail').value.trim(),
            sessionTimeout: Number(document.getElementById('cfgSessionTimeout').value),
            otpTimeout: Number(document.getElementById('cfgOtpTimeout').value),
            maxLoginRetries: Number(document.getElementById('cfgMaxLoginRetries').value),
            minWithdrawal: Number(document.getElementById('cfgMinWithdrawal').value),
            allowGoogleLogin: document.getElementById('cfgAllowGoogle').checked,
            allowRegister: document.getElementById('cfgAllowRegister').checked,
            requireWithdraw2FA: document.getElementById('cfgWithdraw2FA').checked
        };
        saveMock();
        showToast('Đã lưu cấu hình hệ thống (demo frontend).');
    };

    /* ---------- Mock: Commissions ---------- */
    function loadCommissionsForm() {
        const c = mock.commissions;
        document.getElementById('commBasePercent').value = c.basePercent;
        document.getElementById('commFlatBuyer').value = c.flatBuyerFee;
        document.getElementById('commWithdrawPercent').value = c.withdrawalPercent;
        document.getElementById('commMinWithdrawFee').value = c.minWithdrawFee;
    }

    window.AdminConsole.saveCommissions = function () {
        mock.commissions = {
            basePercent: Number(document.getElementById('commBasePercent').value),
            flatBuyerFee: Number(document.getElementById('commFlatBuyer').value),
            withdrawalPercent: Number(document.getElementById('commWithdrawPercent').value),
            minWithdrawFee: Number(document.getElementById('commMinWithdrawFee').value)
        };
        saveMock();
        showToast('Đã cập nhật biểu phí (demo frontend).');
    };

    /* ---------- Mock: Maintenance ---------- */
    function loadMaintenanceForm() {
        const m = mock.maintenance;
        document.getElementById('maintActive').checked = m.active;
        document.getElementById('maintMessage').value = m.message;
        document.getElementById('maintWhitelist').value = m.whitelist;
        document.getElementById('maintStart').value = m.startTime;
        document.getElementById('maintEnd').value = m.endTime;
        updateMaintenancePreview();
    }

    window.AdminConsole.saveMaintenance = function () {
        mock.maintenance = {
            active: document.getElementById('maintActive').checked,
            message: document.getElementById('maintMessage').value.trim(),
            whitelist: document.getElementById('maintWhitelist').value.trim(),
            startTime: document.getElementById('maintStart').value,
            endTime: document.getElementById('maintEnd').value
        };
        saveMock();
        updateMaintenancePreview();
        showToast('Đã lưu cấu hình bảo trì (demo frontend).');
    };

    function updateMaintenancePreview() {
        const banner = document.getElementById('maintPreview');
        if (!banner) return;
        banner.classList.toggle('active', mock.maintenance.active);
        banner.innerHTML = mock.maintenance.active
            ? `<i class="fa fa-exclamation-triangle"></i><div><strong>Bảo trì đang BẬT</strong><p class="ds-caption" style="margin:4px 0 0">${escapeHtml(mock.maintenance.message)}</p></div>`
            : `<i class="fa fa-check-circle"></i><div><strong>Hệ thống đang vận hành bình thường</strong></div>`;
    }

    /* ---------- Mock: Permissions ---------- */
    async function loadPermissionsView() {
        let staffList = users.filter(u => u.role === 'Staff');
        try {
            const response = await authFetch(`${ENDPOINT}/users?size=50&role=Staff`);
            const data = await response.json();
            if (response.ok && data.content?.length) staffList = data.content;
        } catch (_) { /* giữ cache */ }
        const box = document.getElementById('staffPermList');
        if (!staffList.length) {
            box.innerHTML = '<p class="ds-empty-state">Chưa có nhân viên. Hãy tạo tài khoản nhân viên trước.</p>';
            document.getElementById('permPanel').innerHTML = '<p class="ds-caption">Chọn nhân viên ở cột trái.</p>';
            return;
        }
        if (!selectedStaffId || !staffList.find(s => s.id === selectedStaffId)) {
            selectedStaffId = staffList[0].id;
        }
        box.innerHTML = staffList.map(s => `
            <div class="staff-item-option ${s.id === selectedStaffId ? 'selected' : ''}" onclick="AdminConsole.selectStaffPerm(${s.id})">
                <strong>${escapeHtml(s.fullName)}</strong>
                <div class="muted">${escapeHtml(s.email)}</div>
            </div>
        `).join('');
        renderPermissionCheckboxes(staffList);
    }

    window.AdminConsole.selectStaffPerm = function (id) {
        selectedStaffId = id;
        loadPermissionsView();
    };

    function renderPermissionCheckboxes(staffList) {
        const staff = (staffList || users).find(u => u.id === selectedStaffId);
        const granted = mock.permissions[selectedStaffId] || [];
        const groups = [...new Set(ALL_PERMISSIONS.map(p => p.group))];
        const panel = document.getElementById('permPanel');
        panel.innerHTML = `
            <div class="view-header" style="margin-bottom:12px">
                <div>
                    <h3 class="ds-heading-md" style="margin:0">${escapeHtml(staff?.fullName || '')}</h3>
                    <p class="ds-caption">${escapeHtml(staff?.email || '')}</p>
                </div>
                <button type="button" class="ds-btn ds-btn-primary" onclick="AdminConsole.savePermissions()"><i class="fa fa-save"></i> Lưu quyền</button>
            </div>
            ${groups.map(g => `
                <div class="perm-group">
                    <div class="perm-group-title">${escapeHtml(g)}</div>
                    <div class="perm-checkboxes">
                        ${ALL_PERMISSIONS.filter(p => p.group === g).map(p => `
                            <label class="perm-label-checkbox">
                                <input type="checkbox" value="${p.id}" ${granted.includes(p.id) ? 'checked' : ''}>
                                ${escapeHtml(p.label)}
                            </label>
                        `).join('')}
                    </div>
                </div>
            `).join('')}
        `;
    }

    window.AdminConsole.savePermissions = function () {
        if (!selectedStaffId) {
            showToast('Chọn nhân viên trước.', true);
            return;
        }
        const checked = [...document.querySelectorAll('#permPanel input[type=checkbox]:checked')].map(el => el.value);
        mock.permissions[selectedStaffId] = checked;
        saveMock();
        showToast('Đã lưu phân quyền nhân viên (dữ liệu mẫu).');
    };

    /* ---------- Helpers ---------- */
    function setText(id, val) {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
    }

    function setLoading(on) {
        if (!on) return;
        const body = document.getElementById('usersBody');
        if (body) body.innerHTML = '<tr><td colspan="9" class="ds-empty-state">Đang tải dữ liệu...</td></tr>';
    }

    function roleBadgeClass(role) {
        return { Admin: 'ds-badge-danger', Staff: 'ds-badge-warning', Seller: 'ds-badge-info', Customer: 'ds-badge-muted' }[role] || 'ds-badge-muted';
    }

    function statusBadgeClass(locked, online) {
        if (locked) return 'ds-badge-danger';
        if (online) return 'ds-badge-success';
        return 'ds-badge-warning';
    }

    function auditBadgeClass(action) {
        if (action.includes('Approve')) return 'ds-badge-success';
        if (action.includes('Reject') || action.includes('Lock')) return 'ds-badge-danger';
        if (action.includes('Config') || action.includes('Maintenance')) return 'ds-badge-info';
        return 'ds-badge-warning';
    }

    function formatVnd(n) {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(n || 0);
    }

    function formatDateTime(iso) {
        try { return new Date(iso).toLocaleString('vi-VN'); } catch { return iso; }
    }

    function showToast(message, isError = false) {
        const toast = document.getElementById('toast');
        toast.textContent = message;
        toast.className = `admin-toast${isError ? ' error' : ''}`;
        toast.style.display = 'block';
        clearTimeout(window.__adminToastTimer);
        window.__adminToastTimer = setTimeout(() => { toast.style.display = 'none'; }, 2800);
    }

    function readCurrentUser() {
        try { return JSON.parse(sessionStorage.getItem('userInfo') || sessionStorage.getItem('user') || 'null'); }
        catch { return null; }
    }

    function normalizeRole(roleValue) {
        if (!roleValue) return 'Customer';
        try {
            const p = JSON.parse(roleValue);
            return p.role || 'Customer';
        } catch {
            const r = String(roleValue).replaceAll('"', '').trim();
            if (r.toLowerCase().includes('admin')) return 'Admin';
            if (r.toLowerCase().includes('staff')) return 'Staff';
            if (r.toLowerCase().includes('seller')) return 'Seller';
            return 'Customer';
        }
    }

    function escapeHtml(v) {
        return String(v ?? '').replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;').replaceAll("'", '&#039;');
    }

    function debounce(fn, wait) {
        let t;
        return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), wait); };
    }
})();
