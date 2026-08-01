/**
 * Giao diện điều khiển dành cho Quản trị viên (Admin console)
 * File này chỉ xử lý phần frontend (hiển thị UI) cho các trang cấu hình (config) và báo cáo (reports).
 * Các chức năng Dashboard (thống kê) & Tài khoản được lấy trực tiếp thông qua API.
 */
(function () {
    const ENDPOINT = '/admin/user-management';
    const MOCK_KEY = 'mmo_admin_mock';

    const VIEWS = [
        'dashboard', 'audit-logs', 'revenue', 'add-staff', 'account-detail', 'system-config',
        'commissions', 'accounts', 'permissions', 'notifications', 'audit-log-detail'
    ];

    const ALL_PERMISSIONS = [
        { id: 'APPROVE_KYC', label: 'Duyệt hồ sơ định danh KYC', group: 'Kiểm duyệt', desc: 'Cho phép xem, duyệt hoặc từ chối thông tin định danh cá nhân của người dùng.' },
        { id: 'FLAG_SELLER', label: 'Cắm cờ & Đánh gạch Seller', group: 'Kiểm duyệt', desc: 'Cho phép gắn cờ vi phạm (gạch phạt) đối với người bán vi phạm chính sách.' },
        { id: 'APPROVE_WITHDRAWALS', label: 'Phê duyệt yêu cầu rút tiền', group: 'Tài chính', desc: 'Cho phép duyệt lệnh chuyển tiền/rút tiền của Seller từ ví hệ thống về tài khoản ngân hàng.' },
        { id: 'HANDLE_DISPUTES', label: 'Phân xử tranh chấp & Hoàn tiền', group: 'Vận hành', desc: 'Cho phép làm trung gian giải quyết khiếu nại giữa người mua và người bán, hoàn trả hoặc giải ngân tiền Escrow.' },
        { id: 'MANAGE_SUPPORT', label: 'Tiếp nhận & Hỗ trợ khách hàng', group: 'Vận hành', desc: 'Cho phép tiếp nhận, phản hồi và hỗ trợ giải đáp các thắc mắc (ticketing/live chat) của khách hàng.' },
        { id: 'MANAGE_SHOPS', label: 'Quản lý cửa hàng (Shop)', group: 'Vận hành', desc: 'Cho phép xem, phê duyệt yêu cầu mở gian hàng, khóa hoặc mở khóa hoạt động của các Shop.' },
        { id: 'MANAGE_CATEGORIES', label: 'Quản lý danh mục sản phẩm', group: 'Danh mục', desc: 'Cho phép xem, tạo mới, chỉnh sửa và ẩn/hiện các danh mục sản phẩm số trên hệ thống.' }
    ];

    const MOCK_DEFAULT = {
        auditLogs: [
            { id: 101, timestamp: '2026-06-04T10:15:00Z', operator: 'tran.van.b@mmomarket.com', action: 'KYC_Approve', ipAddress: '192.168.1.50', desc: 'Duyệt KYC cho pham.duc.d@gmail.com', diff: '{"kycStatus": "pending -> verified"}' },
            { id: 102, timestamp: '2026-06-04T09:40:00Z', operator: 'le.thi.c@mmomarket.com', action: 'Fund_Withdraw', ipAddress: '192.168.1.62', desc: 'Duyệt rút 5.000.000 VNĐ', diff: '{"status": "pending -> completed"}' },
            { id: 103, timestamp: '2026-06-03T16:20:00Z', operator: 'admin@mmomarket.com', action: 'Config_Update', ipAddress: '113.161.40.85', desc: 'Cập nhật hạn mức rút tối thiểu', diff: '{"minWithdrawal": "50000 -> 100000"}' },
            { id: 104, timestamp: '2026-06-03T11:00:00Z', operator: 'tran.van.b@mmomarket.com', action: 'Lock_User', ipAddress: '192.168.1.50', desc: 'Khóa hoang.thi.h@gmail.com', diff: '{"isLocked": "false -> true"}' },
            { id: 105, timestamp: '2026-06-02T14:30:00Z', operator: 'admin@mmomarket.com', action: 'Maintenance_Toggle', ipAddress: '113.161.40.85', desc: 'Lên lịch bảo trì hệ thống', diff: '{"scheduled": "false -> true"}' }
        ],
        cashFlow: [
            { id: 'TX1001', timestamp: '2026-06-04T10:00:00Z', email: 'pham.duc.d@gmail.com', type: 'Deposit', amount: 20000000, fee: 0, status: 'Completed' },
            { id: 'TX1002', timestamp: '2026-06-04T09:40:00Z', email: 'nguyen.hoang.e@gmail.com', type: 'Withdrawal', amount: 5000000, fee: 75000, status: 'Completed' },
            { id: 'TX1003', timestamp: '2026-06-03T15:25:00Z', email: 'doan.van.g@gmail.com', type: 'Deposit', amount: 1500000, fee: 0, status: 'Completed' },
            { id: 'TX1004', timestamp: '2026-06-03T08:10:00Z', email: 'dang.thi.k@gmail.com', type: 'C2C_Purchase', amount: 4500000, fee: 226000, status: 'Completed' },
            { id: 'TX1005', timestamp: '2026-06-02T11:45:00Z', email: 'pham.duc.d@gmail.com', type: 'Withdrawal', amount: 50000000, fee: 750000, status: 'Completed' }
        ],
        permissions: {
            2: ['APPROVE_KYC', 'REVIEW_SUSPICIOUS', 'AUDIT_ACCOUNTS', 'HANDLE_DISPUTES', 'MANAGE_REQUESTS', 'MANAGE_SHOPS'],
            3: ['APPROVE_WITHDRAWALS', 'MANAGE_ESCROW', 'HANDLE_DISPUTES', 'MANAGE_REQUESTS', 'MANAGE_SHOPS']
        },
        systemConfig: {
            appName: 'MMO Market System',
            sessionTimeout: 15,
            otpTimeout: 5,
            maxLoginRetries: 5,
            escrowHoldHours: 72,
            allowGoogleLogin: true,
            allowRegister: true,
            requireWithdraw2FA: true
        },
        commissions: {
            basePercent: 5.0,
            withdrawalPercent: 1.5,
            shopOpeningFee: 50000,
            minWithdrawLimit: 50000,
            maxWithdrawLimit: 50000000,
            minDepositLimit: 10000,
            maxDepositLimit: 50000000
        },
        maintenance: {
            active: false,
            message: 'Hệ thống MMO Market đang bảo trì nâng cấp định kỳ. Xin lỗi vì sự bất tiện.',
            whitelist: '127.0.0.1',
            startTime: '2026-06-05T01:00',
            endTime: '2026-06-05T04:00'
        },
        notifications: [
            {
                timestamp: new Date(Date.now() - 3600000).toISOString(),
                title: 'Bảo trì hệ thống định kỳ tháng 6/2026',
                type: 'maintenance',
                content: 'Chúng tôi sẽ tiến hành bảo trì nâng cấp hệ thống định kỳ từ 01:00 đến 04:00 ngày 15/06/2026. Trong thời gian này, một số tính năng nạp tiền và giao dịch có thể bị chậm trễ hoặc tạm ngưng để đảm bảo an toàn dữ liệu. Xin chân thành cảm ơn sự thông cảm của quý khách.',
                author: 'Hệ thống'
            },
            {
                timestamp: new Date(Date.now() - 86400000).toISOString(),
                title: 'Cập nhật chính sách phí giao dịch MMO Market',
                type: 'policy',
                content: 'Kể từ ngày 12/06/2026, MMO Market sẽ cập nhật biểu phí dịch vụ bảo trợ C2C đối với người mua ở mức 2% (đã bao gồm phí thanh toán tự động). Mức phí này giúp chúng tôi duy trì hệ thống bảo chứng Escrow Hold 3 ngày (72 giờ) tối ưu nhất và mở rộng bộ phận CSKH hỗ trợ 24/7.',
                author: 'Ban Quản Trị'
            },
            {
                timestamp: new Date(Date.now() - 2 * 86400000).toISOString(),
                title: 'Cảnh báo bảo mật: Các hình thức lừa đảo giả mạo Staff',
                type: 'warning',
                content: 'Khách hàng đặc biệt chú ý: Gần đây có một số đối tượng giả mạo Staff hoặc Moderator của MMO Market để nhắn tin riêng yêu cầu hỗ trợ giao dịch, yêu cầu cung cấp OTP hoặc chuyển tiền trực tiếp ngoài hệ thống.\n\nStaff của hệ thống KHÔNG BAO GIỜ chủ động nhắn tin riêng yêu cầu bạn chuyển khoản ngoài hoặc cung cấp thông tin đăng nhập/OTP. Mọi hoạt động trung gian đều phải được thực hiện thông qua hệ thống website MMO Market.',
                author: 'Phòng Bảo Mật'
            }
        ]
    };

    let mock = {};
    let users = [];
    let currentPage = 0;
    let currentPageSize = 10;
    let totalPages = 1;
    let totalElements = 0;
    let selectedStaffId = null;
    let selectedPermId = null;
    let selectedGroupId = 'ALL';
    let isSearchActive = false;
    let activeFilterPermIds = [];
    let activeFilterGroupId = 'ALL';
    let permissionsPage = 0;
    let permissionsPageSize = 10;
    let permissionsTotalPages = 1;
    let permissionsTotalElements = 0;
    let auditPage = 0;
    let auditPageSize = 10;
    let auditFiltered = [];
    let revPage = 0;
    let revPageSize = 10;
    let revFiltered = [];
    let notifPage = 0;
    let notifPageSize = 10;
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
        Withdrawal_Reject: 'Từ chối rút tiền',
        Complaint_Resolve: 'Giải quyết khiếu nại',
        Dispute_Start: 'Mở đối chất khiếu nại',
        Shop_Approve: 'Duyệt mở Shop',
        Shop_Reject: 'Từ chối mở Shop',
        Support_Resolve: 'Phản hồi hỗ trợ',
        Lock_User: 'Khóa tài khoản',
        Unlock_User: 'Mở khóa tài khoản',
        Role_Update: 'Phân quyền tài khoản',
        Perm_Update: 'Cập nhật phân quyền',
        Config_Update: 'Cập nhật cấu hình',
        Maintenance_Toggle: 'Bảo trì hệ thống',
        Notification_Create: 'Tạo thông báo',
        Notification_Delete: 'Xóa thông báo'
    };

    const ACTION_GROUPS = {
        '': [
            { value: '', label: 'Tất cả hành động cụ thể' },
            { value: 'Fund_Withdraw', label: 'Duyệt rút tiền' },
            { value: 'Withdrawal_Reject', label: 'Từ chối rút tiền' },
            { value: 'Complaint_Resolve', label: 'Giải quyết khiếu nại' },
            { value: 'Dispute_Start', label: 'Mở đối chất khiếu nại' },
            { value: 'Shop_Approve', label: 'Phê duyệt mở Shop' },
            { value: 'Shop_Reject', label: 'Từ chối mở Shop' },
            { value: 'KYC_Approve', label: 'Duyệt KYC' },
            { value: 'Lock_User', label: 'Khóa tài khoản' },
            { value: 'Unlock_User', label: 'Mở khóa tài khoản' },
            { value: 'Role_Update', label: 'Thay đổi vai trò / Cấp quyền' },
            { value: 'Support_Resolve', label: 'Phản hồi / Đóng phiếu hỗ trợ' },
            { value: 'Config_Update', label: 'Cập nhật cấu hình phí & sàn' },
            { value: 'Maintenance_Toggle', label: 'Bật / Tắt bảo trì hệ thống' },
            { value: 'Notification_Create', label: 'Phát thông báo hệ thống' },
            { value: 'Notification_Delete', label: 'Xóa thông báo hệ thống' }
        ],
        FINANCE: [
            { value: '', label: 'Tất cả hành động Tài chính' },
            { value: 'Fund_Withdraw', label: 'Duyệt rút tiền' },
            { value: 'Withdrawal_Reject', label: 'Từ chối rút tiền' }
        ],
        COMPLAINT: [
            { value: '', label: 'Tất cả hành động Khiếu nại' },
            { value: 'Complaint_Resolve', label: 'Giải quyết khiếu nại' },
            { value: 'Dispute_Start', label: 'Mở đối chất khiếu nại' }
        ],
        SHOP: [
            { value: '', label: 'Tất cả hành động về Shop' },
            { value: 'Shop_Approve', label: 'Phê duyệt mở Shop' },
            { value: 'Shop_Reject', label: 'Từ chối mở Shop' }
        ],
        USER_MGMT: [
            { value: '', label: 'Tất cả hành động Tài khoản & KYC' },
            { value: 'KYC_Approve', label: 'Duyệt KYC' },
            { value: 'Lock_User', label: 'Khóa tài khoản' },
            { value: 'Unlock_User', label: 'Mở khóa tài khoản' },
            { value: 'Role_Update', label: 'Thay đổi vai trò / Cấp quyền' }
        ],
        SUPPORT: [
            { value: '', label: 'Tất cả hành động Hỗ trợ' },
            { value: 'Support_Resolve', label: 'Phản hồi / Đóng phiếu hỗ trợ' }
        ],
        SYSTEM: [
            { value: '', label: 'Tất cả hành động Cấu hình & Bảo trì' },
            { value: 'Config_Update', label: 'Cập nhật cấu hình phí & sàn' },
            { value: 'Maintenance_Toggle', label: 'Bật / Tắt bảo trì hệ thống' },
            { value: 'Notification_Create', label: 'Phát thông báo hệ thống' },
            { value: 'Notification_Delete', label: 'Xóa thông báo hệ thống' }
        ]
    };

    function updateActionDropdown(categoryKey) {
        const actionEl = document.getElementById('logActionFilter');
        if (!actionEl) return;
        const actions = ACTION_GROUPS[categoryKey] || ACTION_GROUPS[''];
        actionEl.innerHTML = actions.map(a => `<option value="${a.value}">${escapeHtml(a.label)}</option>`).join('');
    }

    const TX_TYPE_LABELS = {
        C2C_Purchase: 'Giao dịch C2C',
        Shop_Opening: 'Phí mở shop',
        Withdrawal: 'Rút tiền'
    };

    const TX_STATUS_LABELS = {
        Completed: 'Hoàn thành',
        Pending: 'Đang chờ',
        Failed: 'Thất bại',
        Held: 'Tạm giữ'
    };

    const ICON_VIEW = `<svg class="ds-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M2.25 12C3.73 8.12 7.49 5.25 12 5.25C16.51 5.25 20.27 8.12 21.75 12C20.27 15.88 16.51 18.75 12 18.75C7.49 18.75 3.73 15.88 2.25 12Z" stroke="currentColor" stroke-width="2"/><path d="M12 15.25C13.79 15.25 15.25 13.79 15.25 12C15.25 10.21 13.79 8.75 12 8.75C10.21 8.75 8.75 10.21 8.75 12C8.75 13.79 10.21 15.25 12 15.25Z" stroke="currentColor" stroke-width="2"/></svg>`;

    const ICON_EDIT = `<svg class="ds-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>`;

    const ICON_PUBLISH = `<svg class="ds-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M6 12L3 21l18-9L3 3l3 9zm0 0h12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>`;

    const ICON_DELETE = `<svg class="ds-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 7H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M10 11V17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M14 11V17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M6 7L7 20C7.08 21.1 7.9 22 9 22H15C16.1 22 16.92 21.1 17 20L18 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M9 7V4C9 3.45 9.45 3 10 3H14C14.55 3 15 3.45 15 4V7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>`;

    // Biến lưu trữ sự kiện khởi tạo trang khi DOM đã load xong
    document.addEventListener('DOMContentLoaded', () => {
        // Kiểm tra quyền truy cập của Admin, nếu không phải Admin sẽ chặn lại
        if (!guardAdminAccess()) return;
        
        initMock(); // Khởi tạo dữ liệu giả lập (mock data) nếu cần
        initSystemConfig(); // Khởi tạo cấu hình hệ thống từ API
        bindEvents(); // Gán sự kiện click/input cho các thành phần UI
        navigateFromHash(); // Xử lý điều hướng trang dựa trên URL Hash (#)
    });

    /**
     * Hàm gọi API để lấy cấu hình hệ thống và hoa hồng hiện tại từ DB
     * Nếu lỗi sẽ giữ lại dữ liệu mock mặc định.
     */
    async function initSystemConfig() {
        try {
            const response = await authFetch('/admin/system-config');
            if (response.ok) {
                const data = await response.json();
                mock.systemConfig = data.systemConfig;
                mock.commissions = data.commissions;
                saveMock();
            }
        } catch (e) {
            // Keep local mock defaults if server connection fails
        }
    }

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
            const status = document.getElementById('accountStatusFilter');
            const startDate = document.getElementById('accountStartDate');
            const endDate = document.getElementById('accountEndDate');
            const sort = document.getElementById('accountSortOrder');

            if (search) search.value = '';
            if (role) role.value = '';
            if (status) status.value = '';
            if (startDate) startDate.value = '';
            if (endDate) endDate.value = '';
            if (sort) sort.value = 'DESC';

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
        document.getElementById('revKeywordFilter')?.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') { e.preventDefault(); revPage = 0; renderRevenueView(); }
        });

        document.getElementById('auditSearchBtn')?.addEventListener('click', () => {
            auditPage = 0;
            filterAuditLogs();
        });
        document.getElementById('auditResetFilter')?.addEventListener('click', () => {
            const s = document.getElementById('logSearch');
            const cat = document.getElementById('logCategoryFilter');
            const sd = document.getElementById('logStartDate');
            const ed = document.getElementById('logEndDate');
            const sort = document.getElementById('logSortOrder');
            if (s) s.value = '';
            if (cat) cat.value = '';
            updateActionDropdown('');
            if (sd) sd.value = '';
            if (ed) ed.value = '';
            if (sort) sort.value = 'DESC';
            auditPage = 0;
            filterAuditLogs();
        });
        document.getElementById('logSearch')?.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') { e.preventDefault(); auditPage = 0; filterAuditLogs(); }
        });
        document.getElementById('logCategoryFilter')?.addEventListener('change', function () {
            updateActionDropdown(this.value);
        });

        // Revenue filters
        document.getElementById('revenueSearchBtn')?.addEventListener('click', () => {
            revPage = 0;
            renderRevenueView();
        });
        document.getElementById('revenueResetFilter')?.addEventListener('click', () => {
            const startDate = document.getElementById('revStartDate');
            const endDate = document.getElementById('revEndDate');
            const k = document.getElementById('revKeywordFilter');
            const t = document.getElementById('revTypeFilter');
            const s = document.getElementById('revStatusFilter');
            const sort = document.getElementById('revSortOrder');
            if (startDate) startDate.value = '';
            if (endDate) endDate.value = '';
            if (k) k.value = '';
            if (t) t.value = '';
            if (s) s.value = '';
            if (sort) sort.value = 'DESC';
            revPage = 0;
            renderRevenueView();
        });

        // Notification filters
        document.getElementById('notifSearchBtn')?.addEventListener('click', () => {
            notifPage = 0;
            loadNotificationsView();
        });
        document.getElementById('notifResetFilter')?.addEventListener('click', () => {
            const s = document.getElementById('notifSearch');
            const t = document.getElementById('notifTypeFilter');
            const sd = document.getElementById('notifStartDate');
            const ed = document.getElementById('notifEndDate');
            const sort = document.getElementById('notifSortOrder');
            if (s) s.value = '';
            if (t) t.value = '';
            if (sd) sd.value = '';
            if (ed) ed.value = '';
            if (sort) sort.value = 'DESC';
            notifPage = 0;
            loadNotificationsView();
        });
        document.getElementById('notifSearch')?.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') { e.preventDefault(); notifPage = 0; loadNotificationsView(); }
        });

        document.getElementById('accountFormStatusToggle')?.addEventListener('click', () => {
            if (accountFormMode === 'detail-readonly') return;
            setAccountFormActive(!accountFormActive);
        });
        document.querySelectorAll('.ds-toggle-system, #maintActive').forEach(btn => {
            btn.addEventListener('click', () => {
                const isActive = btn.getAttribute('aria-pressed') === 'true';
                btn.setAttribute('aria-pressed', String(!isActive));
                btn.classList.toggle('ds-toggle-inactive', isActive);
            });
        });
        document.getElementById('accountFormSubmitBtn')?.addEventListener('click', () => submitAccountForm());
        document.getElementById('accountFormEmail')?.addEventListener('input', function() {
            syncAccountFormProfile();
            const val = this.value.trim();
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (val === '') {
                showFieldError('accountFormEmail', 'Địa chỉ email không được để trống.');
            } else if (!emailRegex.test(val)) {
                showFieldError('accountFormEmail', 'Định dạng email không hợp lệ (Ví dụ: abc@gmail.com).');
            } else {
                clearFieldError('accountFormEmail');
            }
        });

        document.getElementById('accountFormFullName')?.addEventListener('input', function() {
            syncAccountFormProfile();
            const val = this.value.trim();
            if (val === '') {
                showFieldError('accountFormFullName', 'Họ và tên không được để trống.');
            } else if (val.length < 3) {
                showFieldError('accountFormFullName', 'Họ và tên phải có ít nhất 3 ký tự.');
            } else {
                clearFieldError('accountFormFullName');
            }
        });

        document.getElementById('accountFormPassword')?.addEventListener('input', function() {
            const val = this.value;
            const passwordRegex = /^(?=.*[A-Z])(?=.*[\W_]).{6,}$/;
            if (accountFormMode === 'create') {
                if (val === '') {
                    showFieldError('accountFormPassword', 'Mật khẩu không được để trống.');
                } else if (!passwordRegex.test(val)) {
                    showFieldError('accountFormPassword', 'Mật khẩu phải có ít nhất 6 ký tự, gồm 1 chữ viết hoa và 1 ký tự đặc biệt.');
                } else {
                    clearFieldError('accountFormPassword');
                }
            } else if (accountFormMode === 'detail-edit') {
                if (val !== '' && !passwordRegex.test(val)) {
                    showFieldError('accountFormPassword', 'Mật khẩu phải có ít nhất 6 ký tự, gồm 1 chữ viết hoa và 1 ký tự đặc biệt.');
                } else {
                    clearFieldError('accountFormPassword');
                }
            }
        });

        document.getElementById('accountFormPhone')?.addEventListener('input', function() {
            const val = this.value.trim();
            if (val !== '') {
                const phoneRegex = /^0\d{9}$/;
                if (!phoneRegex.test(val)) {
                    showFieldError('accountFormPhone', 'Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0.');
                } else {
                    clearFieldError('accountFormPhone');
                }
            } else {
                clearFieldError('accountFormPhone');
            }
        });

        document.getElementById('accountFormNationalId')?.addEventListener('input', function() {
            const val = this.value.trim();
            if (val !== '') {
                const nationalIdRegex = /^\d{12}$/;
                if (!nationalIdRegex.test(val)) {
                    showFieldError('accountFormNationalId', 'Số CCCD không hợp lệ. Phải đúng 12 chữ số.');
                } else {
                    clearFieldError('accountFormNationalId');
                }
            } else {
                clearFieldError('accountFormNationalId');
            }
        });

        document.getElementById('accountFormBirthDate')?.addEventListener('change', function() {
            const val = this.value;
            if (val !== '') {
                const birthDate = new Date(val);
                const today = new Date();
                today.setHours(0, 0, 0, 0);
                if (birthDate > today) {
                    showFieldError('accountFormBirthDate', 'Ngày sinh không được ở tương lai.');
                } else {
                    clearFieldError('accountFormBirthDate');
                }
            } else {
                clearFieldError('accountFormBirthDate');
            }
        });

        document.getElementById('accountFormAddress')?.addEventListener('input', function() {
            const val = this.value.trim();
            if (val !== '') {
                if (val.length < 5) {
                    showFieldError('accountFormAddress', 'Địa chỉ phải có ít nhất 5 ký tự.');
                } else {
                    clearFieldError('accountFormAddress');
                }
            } else {
                clearFieldError('accountFormAddress');
            }
        });


        document.getElementById('notifType')?.addEventListener('change', (e) => {
            const wrap = document.getElementById('notifMaintToggleWrap');
            if (wrap) {
                if (e.target.value === 'maintenance') {
                    wrap.classList.remove('ds-hidden');
                } else {
                    wrap.classList.add('ds-hidden');
                    const toggle = document.getElementById('notifMaintToggle');
                    if (toggle) {
                        toggle.setAttribute('aria-pressed', 'false');
                        toggle.classList.add('ds-toggle-inactive');
                    }
                }
            }
        });

        document.getElementById('notifSearchBtn')?.addEventListener('click', () => {
            notifPage = 0;
            loadNotificationsView();
        });
        document.getElementById('notifResetFilter')?.addEventListener('click', () => {
            const s = document.getElementById('notifSearch');
            const f = document.getElementById('notifTypeFilter');
            if (s) s.value = '';
            if (f) f.value = '';
            notifPage = 0;
            loadNotificationsView();
        });
        document.getElementById('notifSearch')?.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                notifPage = 0;
                loadNotificationsView();
            }
        });

        // Click outside listener for custom multiselect dropdowns
        document.addEventListener('click', (e) => {
            const dropdown = document.getElementById('multiselectDropdown');
            const trigger = document.getElementById('multiselectTrigger');
            if (dropdown && !dropdown.classList.contains('ds-hidden')) {
                if (trigger && !trigger.contains(e.target) && !dropdown.contains(e.target)) {
                    dropdown.classList.add('ds-hidden');
                }
            }

            const permDropdown = document.getElementById('permMultiselectDropdown');
            const permTrigger = document.getElementById('permMultiselectTrigger');
            if (permDropdown && !permDropdown.classList.contains('ds-hidden')) {
                if (permTrigger && !permTrigger.contains(e.target) && !permDropdown.contains(e.target)) {
                    permDropdown.classList.add('ds-hidden');
                }
            }
        });

        const currencyInputIds = [
            'commShopOpeningFee',
            'commMinWithdrawLimit',
            'commMaxWithdrawLimit',
            'commMinDepositLimit',
            'commMaxDepositLimit'
        ];
        currencyInputIds.forEach(id => {
            document.getElementById(id)?.addEventListener('input', function() {
                this.value = formatNumberWithDots(this.value);
            });
        });
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
        const stats = document.getElementById('accountFormStats');
        if (stats) stats.style.display = isDetail ? 'grid' : 'none';
        // Note: Title and kicker changes are handled in prepareAccountFormCreate/fillAccountForm
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

    function tableActionsEdit(onclick, title) {
        return `<button type="button" class="ds-icon-btn" style="color: var(--ds-primary);" title="${escapeHtml(title)}" aria-label="${escapeHtml(title)}" onclick="${onclick}">${ICON_EDIT}</button>`;
    }

    function tableActionsPublish(onclick, title) {
        return `<button type="button" class="ds-icon-btn" style="color: var(--ds-success);" title="${escapeHtml(title)}" aria-label="${escapeHtml(title)}" onclick="${onclick}">${ICON_PUBLISH}</button>`;
    }

    function tableActionsDelete(onclick, title) {
        return `<button type="button" class="ds-icon-btn ds-icon-btn-delete" title="${escapeHtml(title)}" aria-label="${escapeHtml(title)}" onclick="${onclick}">${ICON_DELETE}</button>`;
    }

    function resolveViewElementId(target) {
        if (target === 'accounts') return 'accountsView';
        if (target === 'add-staff' || target === 'account-detail') return 'accountFormView';
        if (target === 'audit-log-detail') return 'auditLogDetailView';
        return `${target}View`;
    }

    window.switchAdminView = function (viewName, statusParam) {
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

        if (target === 'notifications' && statusParam) {
            const filterEl = document.getElementById('notifStatusFilter');
            if (filterEl) filterEl.value = statusParam;
        }

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
            case 'notifications': loadNotificationsView(); break;
            case 'accounts': loadUsers(); break;
            case 'permissions':
                resetSearchFilters();
                loadPermissionsView();
                break;
        }
    }

    function resetSearchFilters() {
        isSearchActive = false;
        activeFilterPermIds = [];
        activeFilterGroupId = 'ALL';
        selectedGroupId = 'ALL';
        permissionsPage = 0;
        permissionsPageSize = 10;

        // Clear permission checkboxes
        const permSelectAll = document.getElementById('permSelectAllCheckbox');
        if (permSelectAll) permSelectAll.checked = false;
        const permSearch = document.getElementById('permMultiselectSearch');
        if (permSearch) permSearch.value = '';
        window.AdminConsole.updateSelectedPermsCount();

        // Clear staff checkboxes
        const selectAllCheckbox = document.getElementById('selectAllCheckbox');
        if (selectAllCheckbox) selectAllCheckbox.checked = false;
        const multiselectSearch = document.getElementById('multiselectSearch');
        if (multiselectSearch) multiselectSearch.value = '';
        window.AdminConsole.updateSelectedStaffCount();
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
            await renderDashboardChart();
            await renderDashboardRecentLogs();
        } catch (error) {
            showToast(error.message, true);
        }
    }

    async function renderDashboardChart() {
        let feesByDay = [0, 0, 0, 0, 0, 0, 0];
        let labels = [];
        const daysOfWeek = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];
        const dateKeys = [];
        
        const d = new Date();
        const day = d.getDay();
        // Số ngày cần lùi từ hôm nay để về Thứ 2 tuần trước
        const daysToSubtract = day === 0 ? 13 : day + 6;
        
        for (let i = 0; i < 7; i++) {
            const temp = new Date();
            temp.setDate(d.getDate() - daysToSubtract + i);
            labels.push(daysOfWeek[temp.getDay()]);
            const key = temp.getFullYear() + '-' + String(temp.getMonth() + 1).padStart(2, '0') + '-' + String(temp.getDate()).padStart(2, '0');
            dateKeys.push(key);
        }

        try {
            const listRes = await authFetch(`/admin/revenue/transactions?startDate=${dateKeys[0]}&endDate=${dateKeys[6]}&size=1000`);
            if (listRes.ok) {
                const listData = await listRes.json();
                const txs = listData.content || [];
                txs.forEach(tx => {
                    if (tx.status === 'Completed' || tx.status === 'Held') {
                        const txDate = tx.timestamp.split('T')[0];
                        const idx = dateKeys.indexOf(txDate);
                        if (idx !== -1) {
                            feesByDay[idx] += Number(tx.fee) || 0;
                        }
                    }
                });
            }
        } catch (e) {
            console.error('Lỗi khi tải transactions thật cho dashboard chart:', e);
            const mockFees = mock.cashFlow.reduce((s, t) => s + (t.fee || 0), 0);
            feesByDay = [1200000, 2450000, 1800000, 3100000, 2900000, 4200000, mockFees];
            labels = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ nhật'];
        }

        drawRevenueChart(feesByDay, labels);

        const summary = document.getElementById('chartSummary');
        if (summary) {
            const total = feesByDay.reduce((a, b) => a + b, 0);
            const peak = Math.max(...feesByDay);
            const peakIdx = feesByDay.indexOf(peak);
            summary.innerHTML = `
                <span class="ds-caption">Tổng tuần trước: <strong class="ds-money">${formatVnd(total)}</strong></span>
                <span class="ds-caption">Cao nhất: <strong class="ds-money">${formatVnd(peak)}</strong> (${labels[peakIdx]})</span>
                <span class="ds-caption">Trung bình/ngày: <strong class="ds-money">${formatVnd(Math.round(total / feesByDay.length))}</strong></span>
            `;
        }
    }

    async function renderDashboardRecentLogs() {
        const body = document.getElementById('dashLogsBody');
        if (!body) return;
        try {
            const response = await authFetch('/admin/audit-logs?page=0&size=4');
            if (response.ok) {
                const data = await response.json();
                const logs = data.content || [];
                body.innerHTML = logs.map((l, i) => `
                    <tr>
                        <td class="ds-table-center">${i + 1}</td>
                        <td>${formatDateTime(l.timestamp)}</td>
                        <td><strong>${escapeHtml(l.operator)}</strong></td>
                        <td><span class="ds-badge ${auditBadgeClass(l.action)}">${escapeHtml(actionLabel(l.action))}</span></td>
                        <td class="muted">${escapeHtml(l.desc)}</td>
                    </tr>
                `).join('') || '<tr><td colspan="5" class="ds-empty-state">Chưa có nhật ký.</td></tr>';
                return;
            }
        } catch (e) {
            console.error('Lỗi khi tải audit logs thật cho dashboard:', e);
        }

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

    let CHART_DAY_LABELS = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ nhật'];

    function formatVndShort(value) {
        const n = Number(value) || 0;
        if (n >= 1_000_000_000) return `${(n / 1_000_000_000).toFixed(1)} tỷ`;
        if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)} tr`;
        if (n >= 1_000) return `${Math.round(n / 1_000)}K`;
        return String(n);
    }

    function drawRevenueChart(dataPoints, dayLabels = CHART_DAY_LABELS) {
        CHART_DAY_LABELS = dayLabels;
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
        const keyword = document.getElementById('searchInput')?.value.trim() || '';
        if (keyword) {
            if (keyword.includes('@')) {
                params.set('email', keyword);
            } else if (/^\d+$/.test(keyword)) {
                params.set('phone', keyword);
            } else {
                params.set('name', keyword);
            }
        }
        const roleVal = document.getElementById('roleFilter')?.value;
        if (roleVal) params.set('role', roleVal);

        const statusVal = document.getElementById('accountStatusFilter')?.value;
        if (statusVal) params.set('status', statusVal);

        const startDate = document.getElementById('accountStartDate')?.value;
        if (startDate) params.set('startDate', startDate);

        const endDate = document.getElementById('accountEndDate')?.value;
        if (endDate) params.set('endDate', endDate);

        const sortOrder = document.getElementById('accountSortOrder')?.value || 'DESC';
        params.set('sort', sortOrder);

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
                `<tr><td colspan="8" class="ds-empty-state">${escapeHtml(error.message)}</td></tr>`;
            showToast(error.message, true);
        }
    }

    function renderUsers(list) {
        const tbody = document.getElementById('usersBody');
        if (!list.length) {
            tbody.innerHTML = '<tr><td colspan="8" class="ds-empty-state">Không tìm thấy tài khoản phù hợp.</td></tr>';
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
        clearAllErrors();
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
        const emailEl = document.getElementById('accountFormEmail');
        if (emailEl) emailEl.disabled = false;
        const pwdWrap = document.getElementById('accountFormPasswordWrap');
        if (pwdWrap) pwdWrap.style.display = '';
        const pwdInput = document.getElementById('accountFormPassword');
        if (pwdInput) pwdInput.required = true;
        const pwdReq = document.getElementById('accountFormPasswordRequired');
        if (pwdReq) pwdReq.style.display = '';
        
        const permWrap = document.getElementById('accountFormPermissionsWrap');
        if (permWrap) permWrap.style.display = 'none';
        
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
        clearAllErrors();
        const role = user.role || 'Customer';
        const isStaff = role === 'Staff';
        accountFormMode = isStaff ? 'detail-edit' : 'detail-readonly';

        setAccountFormLayoutMode('detail');
        setText('accountFormTitle', user.fullName || 'Chi tiết tài khoản');
        setText('accountFormCaption', `Mã #${user.id} · ${user.email || ''}`);
        setText('accountFormSubmitLabel', 'Lưu thay đổi');

        const setVal = (id, val) => { const el = document.getElementById(id); if (el) el.value = val; };
        setVal('accountFormId', user.id || '');
        setVal('accountFormEmail', user.email || '');
        setVal('accountFormFullName', user.fullName || '');
        setVal('accountFormPhone', user.phone || '');
        setVal('accountFormAddress', user.address || '');
        setVal('accountFormNationalId', user.nationalId || '');
        setVal('accountFormBirthDate', user.dateOfBirth || '');
        const gender = user.gender || 'Nam';
        document.querySelectorAll('input[name="accountFormGender"]').forEach(r => {
            r.checked = r.value.toLowerCase() === gender.toLowerCase();
        });
        setAccountFormActive(!user.isLocked);
        syncAccountFormProfile();

        setText('accountFormIdDisplay', user.id ? `#${user.id}` : '-');
        const roleEl = document.getElementById('accountFormRoleBadge');
        if (roleEl) {
            roleEl.textContent = roleLabel(role);
            roleEl.className = `ds-badge ${roleBadgeClass(role)}`;
        }
        const profileRole = document.getElementById('accountFormProfileRole');
        if (profileRole) {
            profileRole.textContent = roleLabel(role);
            profileRole.className = `ds-badge ${roleBadgeClass(role)}`;
            profileRole.style.display = 'inline-flex';
        }
        const verifyBadge = document.getElementById('accountFormVerifyBadge');
        if (verifyBadge) {
            verifyBadge.innerHTML = user.isVerified
                ? '<span class="ds-badge ds-badge-success">Đã xác thực</span>'
                : '<span class="ds-badge ds-badge-warning">Chưa xác thực</span>';
        }
        const balanceEl = document.getElementById('accountFormBalance');
        if (balanceEl) balanceEl.textContent = formatVnd(user.balanceVnd || 0);
        const createdAtEl = document.getElementById('accountFormCreatedAt');
        if (createdAtEl) createdAtEl.textContent = user.createdAt ? new Date(user.createdAt).toLocaleString('vi-VN') : '-';

        const submitBtn = document.getElementById('accountFormSubmitBtn');
        const actionsBar = document.querySelector('.account-form-actions');
        
        // Render staff permissions list
        const permWrap = document.getElementById('accountFormPermissionsWrap');
        if (permWrap) {
            if (isStaff) {
                permWrap.style.display = '';
                const permListEl = document.getElementById('accountFormPermissionsList');
                if (permListEl) {
                    if (!mock.permissions) mock.permissions = {};
                    const userPerms = mock.permissions[user.id] || [];
                    if (userPerms.length > 0) {
                        permListEl.innerHTML = userPerms.map(pid => {
                            const p = ALL_PERMISSIONS.find(ap => ap.id === pid);
                            if (p) {
                                return `<span class="ds-badge ds-badge-warning" style="margin-right: 8px; margin-bottom: 8px;" title="${escapeHtml(p.desc)}"><i class="fa fa-shield" style="margin-right: 4px;"></i>${escapeHtml(p.label)}</span>`;
                            }
                            return '';
                        }).filter(Boolean).join('');
                    } else {
                        permListEl.innerHTML = '<span class="ds-text-muted" style="font-size: 13px; font-style: italic;">Chưa được cấp quyền nào</span>';
                    }
                }
            } else {
                permWrap.style.display = 'none';
            }
        }

        if (isStaff) {
            document.getElementById('accountFormEmail').disabled = true;
            document.getElementById('accountFormPasswordWrap').style.display = '';
            document.getElementById('accountFormPassword').value = '';
            document.getElementById('accountFormPassword').required = false;
            document.getElementById('accountFormPasswordRequired').style.display = 'none';
            if (submitBtn) submitBtn.style.display = '';
            if (actionsBar) actionsBar.style.display = '';
            setAccountFormEditable(false);
            const toggle = document.getElementById('accountFormStatusToggle');
            if (toggle) toggle.disabled = false;
        } else {
            document.getElementById('accountFormEmail').disabled = true;
            document.getElementById('accountFormPasswordWrap').style.display = 'none';
            if (submitBtn) submitBtn.style.display = 'none';
            if (actionsBar) actionsBar.style.display = 'none';
            setAccountFormEditable(false);
        }
    }

    function resetAccountFormFields() {
        const form = document.getElementById('accountForm');
        if (form) form.reset();
        const idField = document.getElementById('accountFormId');
        if (idField) idField.value = '';
        const defaultGender = document.querySelector('input[name="accountFormGender"][value="Nam"]');
        if (defaultGender) defaultGender.checked = true;
    }

    function showFieldError(fieldId, message) {
        const inputEl = document.getElementById(fieldId);
        const errorEl = document.getElementById(fieldId + '-error');
        if (inputEl) inputEl.classList.add('ds-input-error');
        if (errorEl) errorEl.textContent = message;
    }

    function clearFieldError(fieldId) {
        const inputEl = document.getElementById(fieldId);
        const errorEl = document.getElementById(fieldId + '-error');
        if (inputEl) inputEl.classList.remove('ds-input-error');
        if (errorEl) errorEl.textContent = '';
    }

    function clearAllErrors() {
        ['accountFormEmail', 'accountFormPassword', 'accountFormFullName', 'accountFormPhone', 'accountFormNationalId', 'accountFormBirthDate', 'accountFormAddress'].forEach(id => {
            clearFieldError(id);
        });
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

        let isValid = true;
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        const passwordRegex = /^(?=.*[A-Z])(?=.*[\W_]).{6,}$/;

        // 1. Email (only for create)
        if (accountFormMode === 'create') {
            if (!payload.email) {
                showFieldError('accountFormEmail', 'Địa chỉ email không được để trống.');
                isValid = false;
            } else if (!emailRegex.test(payload.email)) {
                showFieldError('accountFormEmail', 'Định dạng email không hợp lệ (Ví dụ: abc@gmail.com).');
                isValid = false;
            } else {
                clearFieldError('accountFormEmail');
            }
        }

        // 2. Full Name
        if (!payload.fullName) {
            showFieldError('accountFormFullName', 'Họ và tên không được để trống.');
            isValid = false;
        } else if (payload.fullName.length < 3) {
            showFieldError('accountFormFullName', 'Họ và tên phải có ít nhất 3 ký tự.');
            isValid = false;
        } else {
            clearFieldError('accountFormFullName');
        }

        // 3. Password
        if (accountFormMode === 'create') {
            if (!payload.password) {
                showFieldError('accountFormPassword', 'Mật khẩu không được để trống.');
                isValid = false;
            } else if (!passwordRegex.test(payload.password)) {
                showFieldError('accountFormPassword', 'Mật khẩu phải có ít nhất 6 ký tự, gồm 1 chữ viết hoa và 1 ký tự đặc biệt.');
                isValid = false;
            } else {
                clearFieldError('accountFormPassword');
            }
        } else if (accountFormMode === 'detail-edit') {
            if (payload.password && !passwordRegex.test(payload.password)) {
                showFieldError('accountFormPassword', 'Mật khẩu phải có ít nhất 6 ký tự, gồm 1 chữ viết hoa và 1 ký tự đặc biệt.');
                isValid = false;
            } else {
                clearFieldError('accountFormPassword');
            }
        }

        // 4. Phone (optional)
        if (payload.phone) {
            const phoneRegex = /^0\d{9}$/;
            if (!phoneRegex.test(payload.phone)) {
                showFieldError('accountFormPhone', 'Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0.');
                isValid = false;
            } else {
                clearFieldError('accountFormPhone');
            }
        } else {
            clearFieldError('accountFormPhone');
        }

        // 5. CCCD (optional)
        if (payload.nationalId) {
            const nationalIdRegex = /^\d{12}$/;
            if (!nationalIdRegex.test(payload.nationalId)) {
                showFieldError('accountFormNationalId', 'Số CCCD không hợp lệ. Phải đúng 12 chữ số.');
                isValid = false;
            } else {
                clearFieldError('accountFormNationalId');
            }
        } else {
            clearFieldError('accountFormNationalId');
        }

        // 6. Birth Date (optional)
        if (payload.dateOfBirth) {
            const birthDate = new Date(payload.dateOfBirth);
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            if (birthDate > today) {
                showFieldError('accountFormBirthDate', 'Ngày sinh không được ở tương lai.');
                isValid = false;
            } else {
                clearFieldError('accountFormBirthDate');
            }
        } else {
            clearFieldError('accountFormBirthDate');
        }

        // 7. Address (optional)
        if (payload.address) {
            if (payload.address.length < 5) {
                showFieldError('accountFormAddress', 'Địa chỉ phải có ít nhất 5 ký tự.');
                isValid = false;
            } else {
                clearFieldError('accountFormAddress');
            }
        } else {
            clearFieldError('accountFormAddress');
        }

        if (!isValid) {
            showToast('Vui lòng kiểm tra lại thông tin nhập vào.', true);
            return;
        }

        if (accountFormMode === 'create') {
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

    /* ---------- API: Audit logs ---------- */
    async function filterAuditLogs() {
        const q = document.getElementById('logSearch')?.value || '';
        const category = document.getElementById('logCategoryFilter')?.value || '';
        const action = document.getElementById('logActionFilter')?.value || '';
        const startDate = document.getElementById('logStartDate')?.value || '';
        const endDate = document.getElementById('logEndDate')?.value || '';
        const sort = document.getElementById('logSortOrder')?.value || 'DESC';

        try {
            const url = `/admin/audit-logs?search=${encodeURIComponent(q)}&category=${encodeURIComponent(category)}&action=${encodeURIComponent(action)}&startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}&sort=${encodeURIComponent(sort)}&page=${auditPage}&size=${auditPageSize}`;
            const response = await authFetch(url);
            if (!response.ok) {
                throw new Error('Không thể tải nhật ký từ máy chủ.');
            }
            const data = await response.json();
            auditFiltered = data.content || [];

            const body = document.getElementById('auditLogsBody');
            if (!body) return;
            body.innerHTML = auditFiltered.length ? auditFiltered.map((l, i) => `
                <tr>
                    <td class="ds-table-center">${sttNumber(auditPage, auditPageSize, i)}</td>
                    <td class="ds-table-center">${l.id}</td>
                    <td>${formatDateTime(l.timestamp)}</td>
                    <td>${escapeHtml(l.operator)}</td>
                    <td class="ds-table-center"><span class="ds-badge ${auditBadgeClass(l.action)}">${escapeHtml(actionLabel(l.action))}</span></td>
                    <td class="ds-table-center"><span class="ds-badge ds-badge-success">Thành công</span></td>
                    <td>
                        <div class="ds-table-actions">
                            ${tableActionsView(`AdminConsole.openLogDetail(${l.id})`, 'Xem chi tiết')}
                        </div>
                    </td>
                </tr>
            `).join('') : '<tr><td colspan="7" class="ds-empty-state">Không có nhật ký phù hợp.</td></tr>';

            mountPagination('auditPagination', {
                page: data.page,
                totalPages: data.totalPages,
                totalElements: data.totalElements,
                pageSize: data.size
            }, {
                onPage: (p) => { auditPage = p; filterAuditLogs(); },
                onSize: (s) => { auditPageSize = s; auditPage = 0; filterAuditLogs(); }
            });
        } catch (e) {
            console.error(e);
            const body = document.getElementById('auditLogsBody');
            if (body) {
                body.innerHTML = `<tr><td colspan="7" class="ds-empty-state" style="color: var(--ds-color-danger)">Lỗi: ${escapeHtml(e.message)}</td></tr>`;
            }
        }
    }

    window.AdminConsole.exportAuditLogs = async function () {
        const q = document.getElementById('logSearch')?.value || '';
        const category = document.getElementById('logCategoryFilter')?.value || '';
        const action = document.getElementById('logActionFilter')?.value || '';
        const startDate = document.getElementById('logStartDate')?.value || '';
        const endDate = document.getElementById('logEndDate')?.value || '';
        const sort = document.getElementById('logSortOrder')?.value || 'DESC';

        try {
            const url = `/admin/audit-logs/export?search=${encodeURIComponent(q)}&category=${encodeURIComponent(category)}&action=${encodeURIComponent(action)}&startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}&sort=${encodeURIComponent(sort)}`;
            const response = await authFetch(url);
            if (!response.ok) {
                throw new Error('Không thể tải file xuất nhật ký từ máy chủ.');
            }
            const blob = await response.blob();
            const downloadUrl = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = downloadUrl;
            a.download = `nhat-ky-he-thong-${new Date().toISOString().slice(0, 10)}.csv`;
            document.body.appendChild(a);
            a.click();
            a.remove();
            window.URL.revokeObjectURL(downloadUrl);
        } catch (e) {
            console.error(e);
            if (typeof showToast === 'function') {
                showToast(e.message || 'Lỗi khi xuất file Excel', true);
            }
        }
    };
 
    window.AdminConsole.openLogDetail = function (id) {
        const log = auditFiltered.find(l => l.id === id);
        if (!log) return;
        
        const titleEl = document.getElementById('auditDetTitle');
        if (titleEl) titleEl.textContent = `Chi tiết nhật ký hoạt động #${log.id}`;
        
        const subEl = document.getElementById('auditDetSubtitle');
        if (subEl) subEl.textContent = `Thời gian khởi tạo: ${formatDateTime(log.timestamp)}`;
        
        const opEl = document.getElementById('auditDetOperator');
        if (opEl) opEl.textContent = log.operator || 'Hệ thống';
        
        const actEl = document.getElementById('auditDetAction');
        if (actEl) {
            actEl.textContent = actionLabel(log.action);
            actEl.className = `ds-badge ${auditBadgeClass(log.action)}`;
        }
        
        const descEl = document.getElementById('auditDetDesc');
        if (descEl) descEl.textContent = log.desc;

        // Populate Category and Severity
        const categoryEl = document.getElementById('auditDetCategory');
        const severityEl = document.getElementById('auditDetSeverity');
        
        let categoryText = 'Vận hành hệ thống';
        let severityText = 'Thấp';
        let severityClass = 'ds-badge-muted';
        
        if (log.action === 'KYC_Approve') {
            categoryText = 'Xác thực người dùng';
            severityText = 'Trung bình';
            severityClass = 'ds-badge-success';
        } else if (log.action === 'Lock_User' || log.action === 'Unlock_User' || log.action === 'Role_Update' || log.action === 'Perm_Update') {
            categoryText = 'Quản trị tài khoản & Phân quyền';
            severityText = 'Cao';
            severityClass = 'ds-badge-danger';
        } else if (log.action === 'Fund_Withdraw' || log.action === 'Withdrawal_Reject') {
            categoryText = 'Giao dịch tài chính';
            severityText = 'Cao';
            severityClass = 'ds-badge-danger';
        } else if (log.action === 'Complaint_Resolve' || log.action === 'Dispute_Start') {
            categoryText = 'Xử lý khiếu nại';
            severityText = 'Cao';
            severityClass = 'ds-badge-warning';
        } else if (log.action === 'Shop_Approve' || log.action === 'Shop_Reject') {
            categoryText = 'Quản lý Shop';
            severityText = 'Trung bình';
            severityClass = 'ds-badge-success';
        } else if (log.action === 'Config_Update') {
            categoryText = 'Cấu hình hệ thống';
            severityText = 'Trung bình';
            severityClass = 'ds-badge-warning';
        } else if (log.action === 'Maintenance_Toggle') {
            categoryText = 'Bảo trì hệ thống';
            severityText = 'Cao';
            severityClass = 'ds-badge-danger';
        } else if (log.action === 'Notification_Create' || log.action === 'Notification_Delete') {
            categoryText = 'Thông báo hệ thống';
            severityText = 'Thấp';
            severityClass = 'ds-badge-success';
        }
        
        if (categoryEl) categoryEl.textContent = categoryText;
        if (severityEl) {
            severityEl.textContent = severityText;
            severityEl.className = `ds-badge ${severityClass}`;
        }
        
        // Extract target of the action
        const targetValue = document.getElementById('auditDetTargetValue');
        let valText = 'Hệ thống';

        if (log.action === 'KYC_Approve' || log.action === 'Lock_User' || log.action === 'Unlock_User') {
            const match = log.desc ? log.desc.match(/(cho|Khóa|Mở\s+khóa)\s+([^\s]+)/i) : null;
            if (match) {
                valText = match[2] || match[1];
            } else {
                valText = 'Tài khoản người dùng';
            }
        } else if (log.action === 'Complaint_Resolve' || log.action === 'Dispute_Start') {
            valText = 'Đơn khiếu nại';
        } else if (log.action === 'Shop_Approve' || log.action === 'Shop_Reject') {
            valText = 'Cửa hàng (Shop)';
        } else if (log.action === 'Fund_Withdraw' || log.action === 'Withdrawal_Reject') {
            valText = 'Lệnh rút tiền';
        } else if (log.action === 'Config_Update') {
            valText = 'Cấu hình hệ thống';
        } else if (log.action === 'Maintenance_Toggle') {
            valText = 'Bảo trì hệ thống';
        } else if (log.action === 'Notification_Create' || log.action === 'Notification_Delete') {
            valText = 'Thông báo';
        }

        if (targetValue) {
            targetValue.textContent = valText;
        }
        
        const diffWrap = document.getElementById('auditDetDiffWrap');
        if (diffWrap) {
            diffWrap.innerHTML = renderLogDiff(log.diff, log);
        }
        
        switchAdminView('audit-log-detail');
        window.scrollTo(0, 0);
    };

    window.AdminConsole.closeLogDetail = function () {
        switchAdminView('audit-logs');
    };

    function renderLogDiff(diffJson) {
        if (!diffJson) return '<div style="color: var(--ds-text-muted); font-size: 13px; padding: 12px; background: var(--ds-surface-muted); border-radius: var(--ds-radius-sm); border: 1px dashed var(--ds-border); text-align: center;"><i class="fa fa-info-circle"></i> Không có chi tiết thay đổi dữ liệu.</div>';
        
        let data = {};
        let parsed = false;

        // Try standard JSON.parse first
        try {
            data = typeof diffJson === 'object' ? diffJson : JSON.parse(diffJson);
            parsed = true;
        } catch (e) {
            // Regex fallback to parse malformed JSON strings in mock data (e.g. unquoted arrows)
            try {
                const cleaned = String(diffJson).trim();
                if (cleaned.startsWith('{') && cleaned.endsWith('}')) {
                    const content = cleaned.substring(1, cleaned.length - 1);
                    const regex = /"([^"]+)"\s*:\s*(.+?)(?=\s*,\s*"|\s*$)/g;
                    let match;
                    while ((match = regex.exec(content)) !== null) {
                        const key = match[1].trim();
                        let val = match[2].trim();
                        if (val.startsWith('"') && val.endsWith('"')) {
                            val = val.substring(1, val.length - 1);
                        } else if (val.startsWith("'") && val.endsWith("'")) {
                            val = val.substring(1, val.length - 1);
                        }
                        data[key] = val;
                    }
                    parsed = Object.keys(data).length > 0;
                }
            } catch (err) {
                parsed = false;
            }
        }

        if (!parsed) {
            return `<pre style="background:var(--ds-surface-muted);padding:12px;border-radius:var(--ds-radius-sm);font-size:12px;overflow:auto;margin:0;white-space:pre-wrap;word-break:break-all;border:1px solid var(--ds-border);">${escapeHtml(diffJson || '—')}</pre>`;
        }

        const keyMap = {
            'sessionTimeout': 'Thời gian phiên làm việc',
            'otpTimeout': 'Thời hạn mã OTP',
            'maxLoginRetries': 'Số lần thử đăng nhập tối đa',
            'lockDurationMins': 'Thời gian khóa tài khoản tạm thời',
            'escrowHoldHours': 'Thời gian giam tiền bảo lãnh Escrow',
            'allowGoogleLogin': 'Đăng nhập bằng Google',
            'allowRegister': 'Cho phép đăng ký tài khoản mới',
            'requireWithdraw2FA': 'Yêu cầu xác thực 2 bước (2FA) khi rút tiền',
            'basePercent': 'Phí hoa hồng C2C cơ bản',
            'flatBuyerFee': 'Phí giao dịch cố định người mua',
            'withdrawalPercent': 'Phí rút tiền',
            'minWithdrawFee': 'Phí rút tiền tối thiểu',
            'minWithdrawLimit': 'Hạn mức rút tiền tối thiểu / lần',
            'maxWithdrawLimit': 'Hạn mức rút tiền tối đa / lần',
            'autoWithdrawLimit': 'Hạn mức duyệt rút tiền tự động',
            'minDepositLimit': 'Hạn mức nạp tiền tối thiểu',
            'maxDepositLimit': 'Hạn mức nạp tiền tối đa',
            'isLocked': 'Trạng thái khóa tài khoản',
            'kycStatus': 'Trạng thái xác thực KYC',
            'status': 'Trạng thái giao dịch / yêu cầu',
            'role': 'Vai trò người dùng',
            'scheduled': 'Trạng thái lên lịch bảo trì',
            'active': 'Trạng thái hoạt động',
            'isDelete': 'Trạng thái xóa',
            'shopStatus': 'Trạng thái cửa hàng (Shop)',
            'fullName': 'Họ và tên',
            'phone': 'Số điện thoại',
            'address': 'Địa chỉ',
            'nationalId': 'Số CCCD',
            'dateOfBirth': 'Ngày sinh',
            'title': 'Tiêu đề thông báo',
            'type': 'Loại thông báo',
            'content': 'Nội dung thông báo'
        };

        function translateVal(val, key) {
            if (val === null || val === undefined || String(val).trim() === '' || String(val).trim() === '—' || String(val).trim() === 'none') {
                return 'Trống / Không có';
            }
            const str = String(val).trim();
            const lowerStr = str.toLowerCase();

            // 1. Vai trò người dùng
            if (key === 'role') {
                if (lowerStr.includes('admin')) return 'Quản trị viên';
                if (lowerStr.includes('staff')) return 'Nhân viên';
                if (lowerStr.includes('seller')) return 'Người bán';
                if (lowerStr.includes('customer')) return 'Khách hàng';
                return str;
            }

            // 2. Trạng thái khóa tài khoản
            if (key === 'isLocked') {
                if (lowerStr === 'true') return 'Đã khóa';
                if (lowerStr === 'false') return 'Đang hoạt động (Không khóa)';
            }

            // 3. Trạng thái xóa
            if (key === 'isDelete') {
                if (lowerStr === 'true') return 'Đã xóa';
                if (lowerStr === 'false') return 'Hoạt động (Chưa xóa)';
            }

            // 4. Trạng thái hoạt động
            if (key === 'active') {
                if (lowerStr === 'true') return 'Đang hoạt động';
                if (lowerStr === 'false') return 'Tạm dừng';
            }

            // 5. KYC Status
            if (key === 'kycStatus') {
                if (lowerStr === 'pending') return 'Đang chờ duyệt';
                if (lowerStr === 'verified') return 'Đã xác thực';
                if (lowerStr === 'rejected') return 'Đã từ chối';
            }

            // 6. Trạng thái yêu cầu & Thông báo
            if (key === 'status') {
                if (lowerStr === 'published') return 'Đã phát hành';
                if (lowerStr === 'draft') return 'Bản nháp';
                if (lowerStr === 'pending') return 'Đang chờ';
                if (lowerStr === 'completed') return 'Đã hoàn thành';
                if (lowerStr === 'failed') return 'Thất bại';
                if (lowerStr === 'held') return 'Tạm giữ (Escrow)';
            }

            // 6b. Loại thông báo
            if (key === 'type') {
                if (lowerStr === 'info') return 'Thông tin chung';
                if (lowerStr === 'warning') return 'Cảnh báo bảo mật';
                if (lowerStr === 'maintenance') return 'Bảo trì hệ thống';
                if (lowerStr === 'policy') return 'Chính sách & Điều khoản';
                if (lowerStr === 'c2c_purchase') return 'Giao dịch C2C';
                if (lowerStr === 'shop_opening') return 'Phí mở shop';
                if (lowerStr === 'withdrawal') return 'Rút tiền';
            }

            // 7. Trạng thái cửa hàng
            if (key === 'shopStatus') {
                if (lowerStr === 'pending') return 'Chờ duyệt';
                if (lowerStr === 'active') return 'Đang hoạt động';
                if (lowerStr === 'banned') return 'Đã bị khóa';
            }

            // 8. Đơn vị thời gian
            if (key === 'sessionTimeout' || key === 'otpTimeout' || key === 'lockDurationMins') {
                return str + ' phút';
            }
            if (key === 'escrowHoldHours') {
                return str + ' giờ';
            }

            // 9. Phần trăm
            if (key === 'basePercent' || key === 'withdrawalPercent') {
                return str + '%';
            }

            // 10. Đơn vị tiền tệ VNĐ
            if (key && (key.toLowerCase().includes('fee') || key.toLowerCase().includes('limit') || key.toLowerCase().includes('vnd') || key.toLowerCase().includes('balance'))) {
                const num = Number(str);
                if (!isNaN(num)) {
                    return formatVnd(num);
                }
            }

            // 11. Các giá trị Đúng/Sai Boolean
            if (lowerStr === 'true') {
                if (key && (key.startsWith('allow') || key.startsWith('require') || key.endsWith('2FA'))) {
                    return 'Bật (Cho phép)';
                }
                return 'Bật';
            }
            if (lowerStr === 'false') {
                if (key && (key.startsWith('allow') || key.startsWith('require') || key.endsWith('2FA'))) {
                    return 'Tắt (Chặn)';
                }
                return 'Tắt';
            }

            return val;
        }

        let html = '';
        let count = 0;
        for (const [key, val] of Object.entries(data)) {
            count++;
            const parts = String(val).split(' -> ');
            const oldVal = parts[0] !== undefined ? parts[0] : '—';
            const newVal = parts[1] !== undefined ? parts[1] : '—';
            
            const translatedKey = keyMap[key] || key;
            const formattedOld = translateVal(oldVal, key);
            const formattedNew = translateVal(newVal, key);

            if (parts.length > 1) {
                html += `
                    <div style="border: 1px solid var(--ds-border); border-radius: var(--ds-radius-md); margin-bottom: 12px; background: var(--ds-surface); overflow: hidden;">
                        <div style="background: var(--ds-table-head); padding: 8px 12px; border-bottom: 1px solid var(--ds-border); font-size: 13px; font-weight: 700; color: var(--ds-text); display: flex; align-items: center; justify-content: space-between;">
                            <span>${escapeHtml(translatedKey)}</span>
                            <span style="font-family: monospace; font-size: 11px; color: var(--ds-text-muted); font-weight: normal;">${escapeHtml(key)}</span>
                        </div>
                        <div style="padding: 12px; display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
                            <div style="flex: 1; min-width: 120px; background: var(--ds-danger-soft); border-radius: var(--ds-radius-sm); padding: 8px 10px; display: flex; align-items: center; gap: 8px;">
                                <i class="fa fa-minus-circle" style="color: var(--ds-danger);"></i>
                                <div>
                                    <div style="font-size: 10px; color: var(--ds-text-muted); text-transform: uppercase;">Trước</div>
                                    <div style="font-size: 13px; color: var(--ds-danger); font-weight: 500; text-decoration: line-through; word-break: break-all;">${escapeHtml(formattedOld)}</div>
                                </div>
                            </div>
                            <div style="color: var(--ds-text-subtle); display: flex; align-items: center;">
                                <i class="fa fa-arrow-right"></i>
                            </div>
                            <div style="flex: 1; min-width: 120px; background: var(--ds-success-soft); border-radius: var(--ds-radius-sm); padding: 8px 10px; display: flex; align-items: center; gap: 8px;">
                                <i class="fa fa-plus-circle" style="color: var(--ds-success);"></i>
                                <div>
                                    <div style="font-size: 10px; color: var(--ds-text-muted); text-transform: uppercase;">Sau</div>
                                    <div style="font-size: 13px; color: var(--ds-success); font-weight: 700; word-break: break-all;">${escapeHtml(formattedNew)}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
            } else {
                html += `
                    <div style="border: 1px solid var(--ds-border); border-radius: var(--ds-radius-md); margin-bottom: 12px; background: var(--ds-surface); overflow: hidden;">
                        <div style="background: var(--ds-table-head); padding: 8px 12px; border-bottom: 1px solid var(--ds-border); font-size: 13px; font-weight: 700; color: var(--ds-text); display: flex; align-items: center; justify-content: space-between;">
                            <span>${escapeHtml(translatedKey)}</span>
                            <span style="font-family: monospace; font-size: 11px; color: var(--ds-text-muted); font-weight: normal;">${escapeHtml(key)}</span>
                        </div>
                        <div style="padding: 12px;">
                            <div style="background: var(--ds-surface-muted); border-radius: var(--ds-radius-sm); padding: 8px 10px; display: flex; align-items: center; gap: 8px;">
                                <i class="fa fa-info-circle" style="color: var(--ds-primary);"></i>
                                <div>
                                    <div style="font-size: 10px; color: var(--ds-text-muted); text-transform: uppercase;">Chi tiết</div>
                                    <div style="font-size: 13px; color: var(--ds-text); font-weight: 600; word-break: break-all;">${escapeHtml(formattedOld)}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
            }
        }
        return count > 0 ? html : '<div style="color: var(--ds-text-muted); font-size: 13px; padding: 12px; background: var(--ds-surface-muted); border-radius: var(--ds-radius-sm); border: 1px dashed var(--ds-border); text-align: center;"><i class="fa fa-info-circle"></i> Không có chi tiết thay đổi dữ liệu.</div>';
    }

    /* ---------- Mock: Revenue ---------- */
    async function renderRevenueView() {
        const startDate = document.getElementById('revStartDate')?.value || '';
        const endDate = document.getElementById('revEndDate')?.value || '';
        const typeFilter = document.getElementById('revTypeFilter')?.value || '';
        const statusFilter = document.getElementById('revStatusFilter')?.value || '';
        const sortOrder = document.getElementById('revSortOrder')?.value || 'DESC';
        const keyword = (document.getElementById('revKeywordFilter')?.value || '').trim();

        try {
            // 1. Tải thông tin tóm tắt doanh thu
            const summaryRes = await authFetch('/admin/revenue/summary');
            if (summaryRes.ok) {
                const summary = await summaryRes.json();
                setText('revCommissions', formatVnd(summary.commissions));
                // Cột 'revBuyerFees' hiển thị phí mở shop
                setText('revBuyerFees', formatVnd(summary.shopOpeningFees || 0));
                setText('revWithdrawalFees', formatVnd(summary.withdrawalFees));
                setText('revNetTotal', formatVnd(summary.netTotal));
            }

            // 2. Tải danh sách giao dịch dòng tiền phân trang
            const params = new URLSearchParams({
                keyword: keyword,
                type: typeFilter,
                status: statusFilter,
                startDate: startDate,
                endDate: endDate,
                sort: sortOrder,
                page: String(revPage),
                size: String(revPageSize)
            });

            const listRes = await authFetch(`/admin/revenue/transactions?${params.toString()}`);
            const body = document.getElementById('revTransactionsBody');
            if (!body) return;

            if (listRes.ok) {
                const data = await listRes.json();
                const slice = data.content || [];
                const total = data.totalElements || 0;
                const totalPg = Math.max(data.totalPages || 1, 1);
                
                body.innerHTML = slice.length ? slice.map((t, i) => {
                    const typeClass = t.type === 'Shop_Opening' ? 'ds-badge-info' : (t.type === 'Withdrawal' ? 'ds-badge-warning' : 'ds-badge-success');
                    const statusClass = t.status === 'Completed' ? 'ds-badge-success' : (t.status === 'Held' ? 'ds-badge-secondary' : (t.status === 'Pending' ? 'ds-badge-warning' : 'ds-badge-danger'));
                    return `
                        <tr>
                            <td class="ds-table-center">${sttNumber(revPage, revPageSize, i)}</td>
                            <td class="ds-table-center"><code>${escapeHtml(t.id)}</code></td>
                            <td>${formatDateTime(t.timestamp)}</td>
                            <td>${escapeHtml(t.email)}</td>
                            <td class="ds-table-center"><span class="ds-badge ${typeClass}">${escapeHtml(txTypeLabel(t.type))}</span></td>
                            <td class="ds-money">${formatVnd(t.amount)}</td>
                            <td class="ds-money">${formatVnd(t.fee)}</td>
                            <td class="ds-table-center"><span class="ds-badge ${statusClass}">${escapeHtml(txStatusLabel(t.status))}</span></td>
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
            } else {
                body.innerHTML = '<tr><td colspan="8" class="ds-empty-state" style="color: var(--ds-color-danger)">Không thể tải danh sách giao dịch từ máy chủ.</td></tr>';
            }
        } catch (error) {
            console.error(error);
            const body = document.getElementById('revTransactionsBody');
            if (body) {
                body.innerHTML = `<tr><td colspan="8" class="ds-empty-state" style="color: var(--ds-color-danger)">Lỗi: ${escapeHtml(error.message)}</td></tr>`;
            }
        }
    }

    window.AdminConsole.exportRevenue = async function () {
        const startDate = document.getElementById('revStartDate')?.value || '';
        const endDate = document.getElementById('revEndDate')?.value || '';
        const typeFilter = document.getElementById('revTypeFilter')?.value || '';
        const statusFilter = document.getElementById('revStatusFilter')?.value || '';
        const sortOrder = document.getElementById('revSortOrder')?.value || 'DESC';
        const keyword = (document.getElementById('revKeywordFilter')?.value || '').trim();

        const loadingToast = showToast('Đang xuất báo cáo doanh thu...', 'info');
        try {
            const params = new URLSearchParams({
                keyword: keyword,
                type: typeFilter,
                status: statusFilter,
                startDate: startDate,
                endDate: endDate,
                sort: sortOrder
            });
            const response = await authFetch(`/admin/revenue/export?${params.toString()}`);
            if (loadingToast) loadingToast.remove();
            if (response.ok) {
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `bao-cao-doanh-thu-${new Date().toISOString().slice(0, 10)}.csv`;
                document.body.appendChild(a);
                a.click();
                a.remove();
                window.URL.revokeObjectURL(url);
                showToast('Đã tải báo cáo doanh thu thành công.');
            } else {
                showToast('Không thể xuất báo cáo từ máy chủ.', true);
            }
        } catch (e) {
            if (loadingToast) loadingToast.remove();
            showToast('Lỗi kết nối khi xuất báo cáo.', true);
        }
    };

    window.AdminConsole.mockExport = async function (type) {
        if (type === 'revenue') {
            return window.AdminConsole.exportRevenue();
        } else if (type === 'audit') {
            return window.AdminConsole.exportAuditLogs();
        }
    };

    async function loadSystemConfigForm() {
        try {
            const response = await authFetch('/admin/system-config');
            if (!response.ok) {
                showToast('Không thể tải cấu hình hệ thống từ máy chủ.', true);
                return;
            }
            const data = await response.json();
            const c = data.systemConfig;
            
            // Sync local mock data
            mock.systemConfig = c;
            mock.commissions = data.commissions;
            saveMock();
            
            document.getElementById('cfgSessionTimeout').value = c.sessionTimeout;
            document.getElementById('cfgOtpTimeout').value = c.otpTimeout;
            document.getElementById('cfgMaxLoginRetries').value = c.maxLoginRetries;
            document.getElementById('cfgLockDurationMins').value = c.lockDurationMins || 15;
            document.getElementById('cfgEscrowHoldHours').value = c.escrowHoldHours || 72;
            
            const toggleBtn = (id, active) => {
                const el = document.getElementById(id);
                if (el) {
                    el.setAttribute('aria-pressed', String(active));
                    el.classList.toggle('ds-toggle-inactive', !active);
                }
            };
            toggleBtn('cfgAllowGoogle', c.allowGoogleLogin);
            toggleBtn('cfgAllowRegister', c.allowRegister);
            toggleBtn('cfgWithdraw2FA', c.requireWithdraw2FA);
        } catch (error) {
            showToast('Lỗi kết nối khi tải cấu hình.', true);
        }
    }

    window.AdminConsole.saveSystemConfig = async function () {
        const payload = {
            sessionTimeout: Number(document.getElementById('cfgSessionTimeout').value),
            otpTimeout: Number(document.getElementById('cfgOtpTimeout').value),
            maxLoginRetries: Number(document.getElementById('cfgMaxLoginRetries').value),
            lockDurationMins: Number(document.getElementById('cfgLockDurationMins').value),
            escrowHoldHours: Number(document.getElementById('cfgEscrowHoldHours').value),
            allowGoogleLogin: document.getElementById('cfgAllowGoogle')?.getAttribute('aria-pressed') === 'true',
            allowRegister: document.getElementById('cfgAllowRegister')?.getAttribute('aria-pressed') === 'true',
            requireWithdraw2FA: document.getElementById('cfgWithdraw2FA')?.getAttribute('aria-pressed') === 'true'
        };
        try {
            const response = await authFetch('/admin/system-config/general', {
                method: 'PUT',
                body: JSON.stringify(payload)
            });
            const data = await response.json();
            if (response.ok && data.success) {
                mock.systemConfig = {
                    ...mock.systemConfig,
                    ...payload
                };
                saveMock();
                showToast(data.message || 'Đã lưu cấu hình hệ thống.');
            } else {
                showToast(data.message || 'Không thể lưu cấu hình hệ thống.', true);
            }
        } catch (error) {
            showToast('Lỗi kết nối khi lưu cấu hình.', true);
        }
    };

    async function loadCommissionsForm() {
        try {
            const response = await authFetch('/admin/system-config');
            if (!response.ok) {
                showToast('Không thể tải cấu hình phí & hoa hồng từ máy chủ.', true);
                return;
            }
            const data = await response.json();
            const cm = data.commissions;

            mock.systemConfig = data.systemConfig;
            mock.commissions = cm;
            saveMock();

            const setVal = (id, val) => {
                const el = document.getElementById(id);
                if (el) el.value = val;
            };
            setVal('commBasePercent', cm.basePercent ?? 5.0);
            setVal('commWithdrawPercent', cm.withdrawalPercent ?? 1.5);
            setVal('commShopOpeningFee', formatNumberWithDots(cm.shopOpeningFee ?? 50000));
            setVal('commMinWithdrawLimit', formatNumberWithDots(cm.minWithdrawLimit ?? 50000));
            setVal('commMaxWithdrawLimit', formatNumberWithDots(cm.maxWithdrawLimit ?? 50000000));
            setVal('commMinDepositLimit', formatNumberWithDots(cm.minDepositLimit ?? 10000));
            setVal('commMaxDepositLimit', formatNumberWithDots(cm.maxDepositLimit ?? 50000000));
        } catch (error) {
            showToast('Lỗi kết nối khi tải cấu hình hoa hồng.', true);
        }
    }

    window.AdminConsole.saveCommissions = async function () {
        const payload = {
            basePercent: Number(document.getElementById('commBasePercent')?.value || 0),
            withdrawalPercent: Number(document.getElementById('commWithdrawPercent')?.value || 0),
            shopOpeningFee: stripDots(document.getElementById('commShopOpeningFee')?.value),
            minWithdrawLimit: stripDots(document.getElementById('commMinWithdrawLimit')?.value),
            maxWithdrawLimit: stripDots(document.getElementById('commMaxWithdrawLimit')?.value),
            minDepositLimit: stripDots(document.getElementById('commMinDepositLimit')?.value),
            maxDepositLimit: stripDots(document.getElementById('commMaxDepositLimit')?.value)
        };
        try {
            const response = await authFetch('/admin/system-config/commissions', {
                method: 'PUT',
                body: JSON.stringify(payload)
            });
            const data = await response.json();
            if (response.ok && data.success) {
                mock.commissions = {
                    ...mock.commissions,
                    ...payload
                };
                saveMock();
                showToast(data.message || 'Đã lưu cấu hình phí & hoa hồng.');
            } else {
                showToast(data.message || 'Không thể lưu cấu hình phí & hoa hồng.', true);
            }
        } catch (error) {
            showToast('Lỗi kết nối khi lưu cấu hình hoa hồng.', true);
        }
    };

    /* ---------- Notifications Management & Drafts ---------- */
    let notifCache = [];

    window.AdminConsole.openCreateNotification = function () {
        const editingIdEl = document.getElementById('notifEditingId');
        if (editingIdEl) editingIdEl.value = '';

        const titleEl = document.getElementById('notifTitle');
        const contentEl = document.getElementById('notifContent');
        const typeEl = document.getElementById('notifType');

        if (titleEl) { titleEl.value = ''; titleEl.disabled = false; }
        if (contentEl) { contentEl.value = ''; contentEl.disabled = false; }
        if (typeEl) {
            typeEl.value = 'info';
            typeEl.disabled = false;
            typeEl.onchange = function () {
                const wrap = document.getElementById('notifMaintToggleWrap');
                if (wrap) {
                    if (typeEl.value === 'maintenance') wrap.classList.remove('ds-hidden');
                    else wrap.classList.add('ds-hidden');
                }
            };
        }

        const titleHeader = document.getElementById('notifModalTitle');
        if (titleHeader) titleHeader.textContent = 'Soạn thông báo mới';

        const saveDraftBtn = document.getElementById('saveNotifDraftBtn');
        const publishBtn = document.getElementById('publishNotifBtn');
        const closeBtn = document.getElementById('closeNotifModalBtn');

        if (saveDraftBtn) saveDraftBtn.style.display = '';
        if (publishBtn) publishBtn.style.display = '';
        if (closeBtn) closeBtn.textContent = 'Hủy';

        const wrap = document.getElementById('notifMaintToggleWrap');
        if (wrap) wrap.classList.add('ds-hidden');

        const toggle = document.getElementById('notifMaintToggle');
        if (toggle) {
            toggle.setAttribute('aria-pressed', 'false');
            toggle.className = 'ds-toggle ds-toggle-system ds-toggle-inactive';
            toggle.disabled = false;
        }

        const modal = document.getElementById('createNotifModal');
        if (modal) modal.classList.remove('ds-hidden');
    };

    window.AdminConsole.openNotificationModal = function (id) {
        const item = notifCache.find(n => n.id === id);
        if (!item) return;

        const editingIdEl = document.getElementById('notifEditingId');
        if (editingIdEl) editingIdEl.value = id;

        const titleEl = document.getElementById('notifTitle');
        const contentEl = document.getElementById('notifContent');
        const typeEl = document.getElementById('notifType');

        if (titleEl) titleEl.value = item.title || '';
        if (contentEl) contentEl.value = item.content || '';
        if (typeEl) typeEl.value = item.type || 'info';

        const isPublished = item.status === 'PUBLISHED';
        const titleHeader = document.getElementById('notifModalTitle');
        if (titleHeader) {
            titleHeader.textContent = isPublished ? 'Chi tiết thông báo (Đã phát hành)' : 'Chi tiết & Chỉnh sửa bản nháp';
        }

        if (titleEl) titleEl.disabled = isPublished;
        if (contentEl) contentEl.disabled = isPublished;
        if (typeEl) typeEl.disabled = isPublished;

        const saveDraftBtn = document.getElementById('saveNotifDraftBtn');
        const publishBtn = document.getElementById('publishNotifBtn');
        const closeBtn = document.getElementById('closeNotifModalBtn');

        if (isPublished) {
            if (saveDraftBtn) saveDraftBtn.style.display = 'none';
            if (publishBtn) publishBtn.style.display = 'none';
            if (closeBtn) closeBtn.textContent = 'Đóng';
        } else {
            if (saveDraftBtn) saveDraftBtn.style.display = '';
            if (publishBtn) publishBtn.style.display = '';
            if (closeBtn) closeBtn.textContent = 'Hủy';
        }

        const wrap = document.getElementById('notifMaintToggleWrap');
        const toggle = document.getElementById('notifMaintToggle');
        if (wrap) {
            // For published notifications, hide the maintenance toggle completely to avoid confusion
            if (!isPublished && item.type === 'maintenance') {
                wrap.classList.remove('ds-hidden');
                const isActive = item.activateMaintenance !== undefined && item.activateMaintenance !== null
                    ? Boolean(item.activateMaintenance)
                    : false;
                if (toggle) {
                    toggle.setAttribute('aria-pressed', isActive ? 'true' : 'false');
                    toggle.className = 'ds-toggle ds-toggle-system ' + (isActive ? 'ds-toggle-active' : 'ds-toggle-inactive');
                }
            } else {
                wrap.classList.add('ds-hidden');
            }
        }

        if (typeEl) {
            typeEl.onchange = function () {
                const isMaint = typeEl.value === 'maintenance';
                if (!isPublished && isMaint) {
                    if (wrap) wrap.classList.remove('ds-hidden');
                } else {
                    if (wrap) wrap.classList.add('ds-hidden');
                }
            };
        }

        const modal = document.getElementById('createNotifModal');
        if (modal) modal.classList.remove('ds-hidden');
    };

    window.AdminConsole.closeCreateNotification = function () {
        const modal = document.getElementById('createNotifModal');
        if (modal) modal.classList.add('ds-hidden');
    };

    async function loadNotificationsView() {
        // Toggle Active Maintenance Banner
        const maintBanner = document.getElementById('maintActiveBanner');
        const maintMsg = document.getElementById('maintActiveMessage');
        try {
            const maintRes = await authFetch('/admin/notifications/maintenance-status');
            if (maintRes.ok) {
                const status = await maintRes.json();
                if (maintBanner) {
                    if (status.active) {
                        maintBanner.classList.remove('ds-hidden');
                        if (maintMsg) maintMsg.textContent = status.message || 'Hệ thống đang bảo trì nâng cấp.';
                    } else {
                        maintBanner.classList.add('ds-hidden');
                    }
                }
            }
        } catch (e) {
            console.error('Failed to load maintenance status', e);
        }

        const body = document.getElementById('notifHistoryBody');
        if (!body) return;

        // Apply filters
        const keyword = (document.getElementById('notifSearch')?.value || '').trim();
        const typeFilter = document.getElementById('notifTypeFilter')?.value || '';
        const statusFilter = document.getElementById('notifStatusFilter')?.value || 'ALL';
        const startDate = document.getElementById('notifStartDate')?.value || '';
        const endDate = document.getElementById('notifEndDate')?.value || '';
        const sortOrder = document.getElementById('notifSortOrder')?.value || 'DESC';

        const params = new URLSearchParams();
        params.append('page', notifPage);
        params.append('size', notifPageSize);
        if (keyword) params.append('search', keyword);
        if (typeFilter) params.append('type', typeFilter);
        if (statusFilter) params.append('status', statusFilter);
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);
        if (sortOrder) params.append('sort', sortOrder);

        try {
            const response = await authFetch('/notifications?' + params.toString());
            if (!response.ok) {
                body.innerHTML = '<tr><td colspan="7" class="ds-empty-state">Không thể tải danh sách thông báo. Vui lòng kiểm tra lại kết nối.</td></tr>';
                return;
            }
            const data = await response.json();
            const list = data.content || [];
            notifCache = list;
            const total = data.totalElements || 0;
            const totalPg = data.totalPages || 1;

            if (list.length === 0) {
                body.innerHTML = '<tr><td colspan="7" class="ds-empty-state">Chưa có thông báo nào phù hợp.</td></tr>';
                const pag = document.getElementById('notifPagination');
                if (pag) pag.innerHTML = '';
                return;
            }

            body.innerHTML = list.map((n, idx) => {
                let typeLabel = 'Thông tin';
                let typeBadge = 'ds-badge-info';
                if (n.type === 'warning') {
                    typeLabel = 'Cảnh báo';
                    typeBadge = 'ds-badge-warning';
                } else if (n.type === 'maintenance') {
                    typeLabel = 'Bảo trì';
                    typeBadge = 'ds-badge-danger';
                } else if (n.type === 'policy') {
                    typeLabel = 'Chính sách';
                    typeBadge = 'ds-badge-muted';
                }

                const isPublished = n.status === 'PUBLISHED';
                const statusBadge = isPublished
                    ? '<span class="ds-badge ds-badge-success">🟢 Đã phát hành</span>'
                    : '<span class="ds-badge ds-badge-warning">🟡 Bản nháp</span>';

                let actionsHtml = '';
                if (isPublished) {
                    // Read-only for Published: View (Eye icon)
                    actionsHtml = tableActionsView(`AdminConsole.openNotificationModal(${n.id})`, 'Xem chi tiết thông báo');
                } else {
                    // Full CRUD for Drafts: View & Edit (Eye icon), Publish (Paper plane icon), Delete (Trash icon)
                    actionsHtml = `
                        ${tableActionsView(`AdminConsole.openNotificationModal(${n.id})`, 'Xem & Chỉnh sửa bản nháp')}
                        ${tableActionsPublish(`AdminConsole.publishDraftDirect(${n.id})`, 'Phát hành ngay')}
                        ${tableActionsDelete(`AdminConsole.deleteNotification(${n.id})`, 'Xóa bản nháp')}
                    `;
                }

                return `
                    <tr>
                        <td class="ds-table-center">${sttNumber(notifPage, notifPageSize, idx)}</td>
                        <td class="ds-table-center">${formatDateTime(n.timestamp)}</td>
                        <td><strong>${escapeHtml(n.title)}</strong></td>
                        <td class="ds-table-center"><span class="ds-badge ${typeBadge}">${typeLabel}</span></td>
                        <td class="ds-table-center">${statusBadge}</td>
                        <td>${escapeHtml(n.author)}</td>
                        <td class="ds-table-center">
                            <div class="ds-table-actions" style="display: flex; gap: 4px; justify-content: center;">
                                ${actionsHtml}
                            </div>
                        </td>
                    </tr>
                `;
            }).join('');

            mountPagination('notifPagination', {
                page: notifPage,
                totalPages: totalPg,
                totalElements: total,
                pageSize: notifPageSize
            }, {
                onPage: (p) => { notifPage = p; loadNotificationsView(); },
                onSize: (s) => { notifPageSize = s; notifPage = 0; loadNotificationsView(); }
            });
        } catch (err) {
            body.innerHTML = '<tr><td colspan="7" class="ds-empty-state">Lỗi kết nối khi tải danh sách thông báo. Vui lòng thử lại sau.</td></tr>';
        }
    }

    window.AdminConsole.submitNotification = async function (targetStatus) {
        const editingId = document.getElementById('notifEditingId')?.value;
        const title = document.getElementById('notifTitle').value.trim();
        const type = document.getElementById('notifType').value;
        const content = document.getElementById('notifContent').value.trim();
        if (!title || !content) {
            showToast('Vui lòng nhập đầy đủ tiêu đề và nội dung thông báo.', true);
            return;
        }

        const isMaint = (type === 'maintenance');
        const maintToggle = document.getElementById('notifMaintToggle');
        const shouldMaint = isMaint && maintToggle && (maintToggle.getAttribute('aria-pressed') === 'true');

        const payload = {
            title: title,
            content: content,
            type: type,
            activateMaintenance: shouldMaint
        };

        try {
            let response;
            if (editingId) {
                if (targetStatus === 'DRAFT') {
                    response = await authFetch(`/admin/notifications/drafts/${editingId}`, {
                        method: 'PUT',
                        body: JSON.stringify(payload)
                    });
                } else {
                    response = await authFetch(`/admin/notifications/drafts/${editingId}/publish`, {
                        method: 'POST',
                        body: JSON.stringify({ activateMaintenance: shouldMaint })
                    });
                }
            } else {
                if (targetStatus === 'DRAFT') {
                    response = await authFetch('/admin/notifications/drafts', {
                        method: 'POST',
                        body: JSON.stringify(payload)
                    });
                } else {
                    response = await authFetch('/admin/notifications', {
                        method: 'POST',
                        body: JSON.stringify(payload)
                    });
                }
            }

            const data = await response.json();
            if (response.ok && data.success) {
                document.getElementById('notifTitle').value = '';
                document.getElementById('notifContent').value = '';
                AdminConsole.closeCreateNotification();
                const msg = targetStatus === 'DRAFT'
                    ? 'Đã lưu bản nháp thông báo thành công.'
                    : (shouldMaint ? 'Đã phát thông báo & kích hoạt bảo trì hệ thống thành công.' : 'Đã phát thông báo thành công tới toàn bộ người dùng.');
                showToast(msg);
                await loadNotificationsView();
            } else {
                showToast(data.message || 'Thao tác không thành công. Vui lòng thử lại.', true);
            }
        } catch (e) {
            showToast('Lỗi kết nối máy chủ khi xử lý thông báo.', true);
        }
    };

    window.AdminConsole.publishDraftDirect = async function (id) {
        if (!confirm('Bạn có chắc chắn muốn phát hành bản nháp thông báo này cho toàn bộ người dùng?')) {
            return;
        }
        try {
            const response = await authFetch(`/admin/notifications/drafts/${id}/publish`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ activateMaintenance: false })
            });
            const data = await response.json();
            if (response.ok && data.success) {
                showToast('Đã phát hành bản nháp thành công tới toàn bộ người dùng.');
                await loadNotificationsView();
            } else {
                showToast(data.message || 'Không thể phát hành bản nháp.', true);
            }
        } catch (e) {
            showToast('Lỗi kết nối máy chủ khi phát hành bản nháp.', true);
        }
    };

    window.AdminConsole.deleteNotification = async function (id) {
        if (!confirm('Bạn có chắc chắn muốn xóa bản nháp này?')) {
            return;
        }
        try {
            const response = await authFetch('/admin/notifications/' + id, {
                method: 'DELETE'
            });
            const data = await response.json();
            if (response.ok && data.success) {
                showToast('Đã xóa bản nháp thành công.');
                await loadNotificationsView();
            } else {
                showToast(data.message || 'Không thể xóa bản nháp.', true);
            }
        } catch (e) {
            showToast('Lỗi kết nối máy chủ khi xóa bản nháp.', true);
        }
    };

    window.AdminConsole.disableMaintenance = async function () {
        try {
            const response = await authFetch('/admin/notifications/toggle-maintenance', {
                method: 'POST',
                body: JSON.stringify({ active: false })
            });
            const data = await response.json();
            if (response.ok && data.success) {
                showToast('Đã tắt chế độ bảo trì hệ thống.');
                await loadNotificationsView();
            } else {
                showToast(data.message || 'Không thể tắt chế độ bảo trì.', true);
            }
        } catch (e) {
            showToast('Lỗi kết nối khi tắt chế độ bảo trì.', true);
        }
    };

    /* ---------- Mock: Commissions ---------- */
    async function loadCommissionsForm() {
        try {
            const response = await authFetch('/admin/system-config');
            if (!response.ok) {
                showToast('Không thể tải cấu hình phí từ máy chủ.', true);
                return;
            }
            const data = await response.json();
            const c = data.commissions;
            
            // Sync local mock data
            mock.systemConfig = data.systemConfig;
            mock.commissions = c;
            saveMock();
            
            document.getElementById('commBasePercent').value = c.basePercent;
            document.getElementById('commWithdrawPercent').value = c.withdrawalPercent;
            document.getElementById('commShopOpeningFee').value = formatNumberWithDots(c.shopOpeningFee);
            document.getElementById('commMinWithdrawLimit').value = formatNumberWithDots(c.minWithdrawLimit);
            document.getElementById('commMaxWithdrawLimit').value = formatNumberWithDots(c.maxWithdrawLimit);
            document.getElementById('commMinDepositLimit').value = formatNumberWithDots(c.minDepositLimit);
            document.getElementById('commMaxDepositLimit').value = formatNumberWithDots(c.maxDepositLimit);
        } catch (error) {
            showToast('Lỗi kết nối khi tải cấu hình.', true);
        }
    }

    window.AdminConsole.saveCommissions = async function () {
        const payload = {
            basePercent: Number(document.getElementById('commBasePercent').value),
            withdrawalPercent: Number(document.getElementById('commWithdrawPercent').value),
            shopOpeningFee: stripDots(document.getElementById('commShopOpeningFee').value),
            minWithdrawLimit: stripDots(document.getElementById('commMinWithdrawLimit').value),
            maxWithdrawLimit: stripDots(document.getElementById('commMaxWithdrawLimit').value),
            minDepositLimit: stripDots(document.getElementById('commMinDepositLimit').value),
            maxDepositLimit: stripDots(document.getElementById('commMaxDepositLimit').value)
        };
        try {
            const response = await authFetch('/admin/system-config/commissions', {
                method: 'PUT',
                body: JSON.stringify(payload)
            });
            const data = await response.json();
            if (response.ok && data.success) {
                mock.commissions = payload;
                saveMock();
                showToast(data.message || 'Đã lưu biểu phí và hạn mức hệ thống.');
            } else {
                showToast(data.message || 'Không thể lưu cấu hình.', true);
            }
        } catch (error) {
            showToast('Lỗi kết nối khi lưu cấu hình.', true);
        }
    };

    /* ---------- Mock: Permissions ---------- */
    let staffListCache = [];

    function populatePermDropdown(filteredPermissions) {
        const permMultiselectList = document.getElementById('permMultiselectList');
        if (!permMultiselectList) return;

        // Get currently checked permission IDs
        const checked = Array.from(document.querySelectorAll('input[name="filterPermCheckbox"]:checked')).map(cb => cb.value);

        if (filteredPermissions.length > 0) {
            permMultiselectList.innerHTML = filteredPermissions.map(p => {
                const isChecked = checked.includes(p.id) ? 'checked' : '';
                return `
                    <label class="perm-multiselect-item ds-dropdown-option" data-search="${escapeHtml(p.label.toLowerCase())} ${escapeHtml(p.desc.toLowerCase())}" style="display: flex; align-items: center; gap: 10px; cursor: pointer; margin: 0; padding: 8px 12px;" onclick="event.stopPropagation()">
                        <input type="checkbox" name="filterPermCheckbox" value="${p.id}" ${isChecked} style="cursor: pointer; width: 14px; height: 14px; margin: 0;" onchange="window.AdminConsole.updateSelectedPermsCount()">
                        <span style="font-size: 13px; color: var(--ds-text);">
                            <strong>${escapeHtml(p.label)}</strong>
                        </span>
                    </label>
                `;
            }).join('');
        } else {
            permMultiselectList.innerHTML = `
                <div style="padding: 10px; color: var(--ds-text-muted); font-size: 12.5px; text-align: center; font-style: italic;">
                    Không có quyền nào.
                </div>
            `;
        }
        window.AdminConsole.updateSelectedPermsCount();
    }

    function populateStaffDropdown(staffList) {
        const multiselectList = document.getElementById('multiselectList');
        if (!multiselectList) return;

        // Get currently checked staff IDs
        const checked = Array.from(document.querySelectorAll('input[name="assignStaffCheckbox"]:checked')).map(cb => cb.value);

        if (staffList.length > 0) {
            multiselectList.innerHTML = staffList.map(s => {
                const isChecked = checked.includes(String(s.id)) ? 'checked' : '';
                return `
                    <label class="multiselect-item ds-dropdown-option" data-search="${escapeHtml(s.fullName.toLowerCase())} ${escapeHtml(s.email.toLowerCase())}" style="display: flex; align-items: center; gap: 10px; cursor: pointer; margin: 0; padding: 8px 12px;" onclick="event.stopPropagation()">
                        <input type="checkbox" name="assignStaffCheckbox" value="${s.id}" ${isChecked} style="cursor: pointer; width: 14px; height: 14px; margin: 0;" onchange="window.AdminConsole.updateSelectedStaffCount()">
                        <span style="font-size: 13px; color: var(--ds-text);">
                            <strong>${escapeHtml(s.fullName)}</strong>
                        </span>
                    </label>
                `;
            }).join('');
        } else {
            multiselectList.innerHTML = `
                <div style="padding: 10px; color: var(--ds-text-muted); font-size: 12.5px; text-align: center; font-style: italic;">
                    Không có nhân viên nào.
                </div>
            `;
        }
        window.AdminConsole.updateSelectedStaffCount();
    }

    async function loadPermissionsView() {
        let staffList = staffListCache;
        if (!staffList.length) {
            staffList = users.filter(u => normalizeRole(u.role) === 'Staff');
            try {
                const response = await authFetch(`${ENDPOINT}/users?size=50&role=Staff`);
                const data = await response.json();
                if (response.ok && data.content?.length) {
                    staffList = data.content;
                    staffListCache = data.content;
                }
            } catch (_) { /* giữ cache */ }
        }

        // Tải các quyền đã gán từ Backend thật
        try {
            const response = await authFetch('/admin/staff-permissions/all-assigned');
            if (response.ok) {
                mock.permissions = await response.json();
                saveMock();
            }
        } catch (e) {
            console.error('Không thể tải danh sách quyền đã gán:', e);
        }

        // Get filtered permissions list based on selected group
        const filteredPermissions = selectedGroupId === 'ALL'
            ? ALL_PERMISSIONS
            : ALL_PERMISSIONS.filter(p => p.group === selectedGroupId);

        // Populate group select dropdown (#permGroupFilter)
        const permGroupFilter = document.getElementById('permGroupFilter');
        if (permGroupFilter) {
            permGroupFilter.value = selectedGroupId;
        }

        // Populate permissions select checklist dropdown
        populatePermDropdown(filteredPermissions);

        // Build mock.permissions if undefined
        if (!mock.permissions) {
            mock.permissions = {};
        }

        // Filter the staff according to search criteria
        let displayedStaff = [];
        if (!isSearchActive) {
            if (selectedGroupId === 'ALL') {
                displayedStaff = staffList.filter(s => {
                    const userPerms = mock.permissions[s.id] || [];
                    const groups = new Set();
                    userPerms.forEach(pid => {
                        const p = ALL_PERMISSIONS.find(ap => ap.id === pid);
                        if (p) groups.add(p.group);
                    });
                    return userPerms.length > 0;
                });
            } else {
                const groupPerms = ALL_PERMISSIONS.filter(p => p.group === selectedGroupId).map(p => p.id);
                displayedStaff = staffList.filter(s => {
                    const userPerms = mock.permissions[s.id] || [];
                    return groupPerms.every(pid => userPerms.includes(pid));
                });
            }
        } else {
            if (activeFilterPermIds.length > 0) {
                // Filter staff who have ALL checked permissions (AND logic)
                displayedStaff = staffList.filter(s => {
                    const userPerms = mock.permissions[s.id] || [];
                    return activeFilterPermIds.every(pid => userPerms.includes(pid));
                });
            } else if (activeFilterGroupId !== 'ALL') {
                // Filter staff who have ALL permissions in the selected group (AND logic)
                const groupPerms = ALL_PERMISSIONS.filter(p => p.group === activeFilterGroupId).map(p => p.id);
                displayedStaff = staffList.filter(s => {
                    const userPerms = mock.permissions[s.id] || [];
                    return groupPerms.every(pid => userPerms.includes(pid));
                });
            } else {
                // Filter staff who have permissions in all groups (ALL group selected, no details checked)
                displayedStaff = staffList.filter(s => {
                    const userPerms = mock.permissions[s.id] || [];
                    const groups = new Set();
                    userPerms.forEach(pid => {
                        const p = ALL_PERMISSIONS.find(ap => ap.id === pid);
                        if (p) groups.add(p.group);
                    });
                    return userPerms.length > 0;
                });
            }
        }

        // Calculate pagination metadata
        permissionsTotalElements = displayedStaff.length;
        permissionsTotalPages = Math.ceil(permissionsTotalElements / permissionsPageSize) || 1;
        if (permissionsPage >= permissionsTotalPages) {
            permissionsPage = 0;
        }

        const startIndex = permissionsPage * permissionsPageSize;
        const paginatedStaff = displayedStaff.slice(startIndex, startIndex + permissionsPageSize);

        // Render assigned staff in table
        const assignedStaffBody = document.getElementById('assignedStaffTableBody');
        if (assignedStaffBody) {
            if (paginatedStaff.length > 0) {
                assignedStaffBody.innerHTML = paginatedStaff.map((s, index) => {
                    const rowNum = startIndex + index + 1;
                    return `
                        <tr>
                            <td class="ds-table-center">${rowNum}</td>
                            <td class="ds-table-center">${s.id}</td>
                            <td>
                                <div class="ds-entity">
                                    <span class="ds-avatar ds-avatar-sm ds-avatar-primary">${escapeHtml(s.fullName.charAt(0).toUpperCase())}</span>
                                    <div>
                                        <div class="ds-entity-title">${escapeHtml(s.fullName)}</div>
                                    </div>
                                </div>
                            </td>
                            <td>${escapeHtml(s.email)}</td>
                            <td class="ds-table-center">
                                <div class="ds-table-actions">
                                    ${tableActionsDelete(`window.AdminConsole.removeStaffPermissions(${s.id})`, 'Thu hồi quyền')}
                                </div>
                            </td>
                        </tr>
                    `;
                }).join('');
            } else {
                assignedStaffBody.innerHTML = `
                    <tr>
                        <td colspan="5" class="ds-empty-state" style="text-align: center; padding: 24px; color: var(--ds-text-muted); font-size: 13px;">
                            Không tìm thấy nhân viên nào phù hợp bộ lọc.
                        </td>
                    </tr>
                `;
            }
        }

        // Render pagination controls
        mountPagination('permissionsPagination', {
            page: permissionsPage,
            totalPages: permissionsTotalPages,
            totalElements: permissionsTotalElements,
            pageSize: permissionsPageSize
        }, {
            onPage: (p) => {
                permissionsPage = p;
                loadPermissionsView();
            },
            onSize: (s) => {
                permissionsPageSize = s;
                permissionsPage = 0;
                loadPermissionsView();
            }
        });

        // Render staff list inside custom dropdown
        populateStaffDropdown(staffList);
    }

    window.AdminConsole.changePermGroup = function (groupId) {
        selectedGroupId = groupId;
        permissionsPage = 0;
        
        // Repopulate without preserving checked items to reset filter
        const filteredPermissions = selectedGroupId === 'ALL'
            ? ALL_PERMISSIONS
            : ALL_PERMISSIONS.filter(p => p.group === selectedGroupId);

        const permMultiselectList = document.getElementById('permMultiselectList');
        if (permMultiselectList) {
            if (filteredPermissions.length > 0) {
                permMultiselectList.innerHTML = filteredPermissions.map(p => `
                    <label class="perm-multiselect-item ds-dropdown-option" data-search="${escapeHtml(p.label.toLowerCase())} ${escapeHtml(p.desc.toLowerCase())}" style="display: flex; align-items: center; gap: 10px; cursor: pointer; margin: 0; padding: 8px 12px;" onclick="event.stopPropagation()">
                        <input type="checkbox" name="filterPermCheckbox" value="${p.id}" style="cursor: pointer; width: 14px; height: 14px; margin: 0;" onchange="window.AdminConsole.updateSelectedPermsCount()">
                        <span style="font-size: 13px; color: var(--ds-text);">
                            <strong>${escapeHtml(p.label)}</strong>
                        </span>
                    </label>
                `).join('');
            } else {
                permMultiselectList.innerHTML = `
                    <div style="padding: 10px; color: var(--ds-text-muted); font-size: 12.5px; text-align: center; font-style: italic;">
                        Không có quyền nào.
                    </div>
                `;
            }
        }

        const permSelectAll = document.getElementById('permSelectAllCheckbox');
        if (permSelectAll) permSelectAll.checked = false;

        const permSearch = document.getElementById('permMultiselectSearch');
        if (permSearch) permSearch.value = '';

        window.AdminConsole.updateSelectedPermsCount();

        // Clear permission description box since no permission is active
        const descBox = document.getElementById('selectedPermDescBox');
        if (descBox) {
            descBox.style.display = 'none';
            descBox.innerHTML = '';
        }
    };

    window.AdminConsole.togglePermMultiselectDropdown = function (event) {
        if (event) event.stopPropagation();
        // Close employee dropdown
        const empDropdown = document.getElementById('multiselectDropdown');
        if (empDropdown) empDropdown.classList.add('ds-hidden');

        const dropdown = document.getElementById('permMultiselectDropdown');
        if (dropdown) {
            dropdown.classList.toggle('ds-hidden');
            const searchInput = document.getElementById('permMultiselectSearch');
            if (searchInput) {
                searchInput.value = '';
                window.AdminConsole.filterPermMultiselectList('');
            }
        }
    };

    window.AdminConsole.filterPermMultiselectList = function (query) {
        const q = query.toLowerCase().trim();
        const items = document.querySelectorAll('.perm-multiselect-item');
        items.forEach(item => {
            const searchText = item.getAttribute('data-search') || '';
            const isMatch = searchText.includes(q);
            item.style.display = isMatch ? 'flex' : 'none';
        });
        const selectAll = document.getElementById('permSelectAllCheckbox');
        if (selectAll) selectAll.checked = false;
    };

    window.AdminConsole.togglePermSelectAll = function (checked) {
        const items = document.querySelectorAll('.perm-multiselect-item');
        items.forEach(item => {
            if (item.style.display !== 'none') {
                const cb = item.querySelector('input[name="filterPermCheckbox"]');
                if (cb) cb.checked = checked;
            }
        });
        window.AdminConsole.updateSelectedPermsCount();
    };

    window.AdminConsole.updateSelectedPermsCount = function () {
        const checked = document.querySelectorAll('input[name="filterPermCheckbox"]:checked');
        const count = checked.length;
        const triggerText = document.getElementById('permMultiselectTriggerText');
        if (triggerText) {
            if (count > 0) {
                triggerText.textContent = `Đã chọn: ${count} quyền`;
                triggerText.style.color = 'var(--ds-text)';
            } else {
                triggerText.textContent = '-- Chọn quyền --';
                triggerText.style.color = 'var(--ds-text-muted)';
            }
        }

        // Also update description box dynamically
        const descBox = document.getElementById('selectedPermDescBox');
        if (descBox) {
            if (count > 0) {
                descBox.style.display = 'block';
                const descHtml = Array.from(checked).map(cb => {
                    const pInfo = ALL_PERMISSIONS.find(ap => ap.id === cb.value);
                    if (!pInfo) return '';
                    return `<div><strong>${escapeHtml(pInfo.label)}:</strong> ${escapeHtml(pInfo.desc)}</div>`;
                }).filter(html => html !== '').join('<hr style="margin: 6px 0; border: none; border-top: 1px dashed rgba(0,0,0,0.1);" />');

                descBox.innerHTML = `<strong>Mô tả các quyền đã chọn:</strong><div style="margin-top: 6px; display: flex; flex-direction: column; gap: 4px;">${descHtml}</div>`;
            } else {
                descBox.style.display = 'none';
                descBox.innerHTML = '';
            }
        }
    };

    window.AdminConsole.toggleMultiselectDropdown = function (event) {
        if (event) event.stopPropagation();
        // Close permissions dropdown
        const permDropdown = document.getElementById('permMultiselectDropdown');
        if (permDropdown) permDropdown.classList.add('ds-hidden');

        const dropdown = document.getElementById('multiselectDropdown');
        if (dropdown) {
            dropdown.classList.toggle('ds-hidden');
            const searchInput = document.getElementById('multiselectSearch');
            if (searchInput) {
                searchInput.value = '';
                window.AdminConsole.filterMultiselectList('');
            }
        }
    };

    window.AdminConsole.filterMultiselectList = function (query) {
        const q = query.toLowerCase().trim();
        const items = document.querySelectorAll('.multiselect-item');
        items.forEach(item => {
            const searchText = item.getAttribute('data-search') || '';
            const isMatch = searchText.includes(q);
            item.style.display = isMatch ? 'flex' : 'none';
        });
        const selectAll = document.getElementById('selectAllCheckbox');
        if (selectAll) selectAll.checked = false;
    };

    window.AdminConsole.toggleSelectAll = function (checked) {
        const items = document.querySelectorAll('.multiselect-item');
        items.forEach(item => {
            if (item.style.display !== 'none') {
                const cb = item.querySelector('input[name="assignStaffCheckbox"]');
                if (cb) cb.checked = checked;
            }
        });
        window.AdminConsole.updateSelectedStaffCount();
    };

    window.AdminConsole.updateSelectedStaffCount = function () {
        const checked = document.querySelectorAll('input[name="assignStaffCheckbox"]:checked');
        const count = checked.length;
        const triggerText = document.getElementById('multiselectTriggerText');
        if (triggerText) {
            if (count > 0) {
                triggerText.textContent = `Đã chọn: ${count} nhân viên`;
                triggerText.style.color = 'var(--ds-text)';
            } else {
                triggerText.textContent = '-- Chọn nhân viên --';
                triggerText.style.color = 'var(--ds-text-muted)';
            }
        }
    };

    window.AdminConsole.addStaffToPermission = async function () {
        const checkedPerms = Array.from(document.querySelectorAll('input[name="filterPermCheckbox"]:checked')).map(cb => cb.value);
        if (checkedPerms.length === 0) {
            showToast('Vui lòng chọn ít nhất một quyền cụ thể để gán.', true);
            return;
        }
        const checkboxes = document.querySelectorAll('input[name="assignStaffCheckbox"]:checked');
        if (checkboxes.length === 0) {
            showToast('Vui lòng chọn ít nhất một nhân viên để gán quyền.', true);
            return;
        }

        const staffIds = Array.from(checkboxes).map(cb => Number(cb.value));

        try {
            const response = await authFetch('/admin/staff-permissions/assign', {
                method: 'POST',
                body: JSON.stringify({
                    userIds: staffIds,
                    permissionNames: checkedPerms
                })
            });
            const data = await response.json();
            if (response.ok && data.success) {
                showToast(`Đã gán thành công ${checkedPerms.length} quyền cho ${checkboxes.length} nhân viên.`);
                
                // Clear staff dropdown checkboxes
                const staffCbs = document.querySelectorAll('input[name="assignStaffCheckbox"]');
                staffCbs.forEach(cb => cb.checked = false);
                const selectAllCheckbox = document.getElementById('selectAllCheckbox');
                if (selectAllCheckbox) selectAllCheckbox.checked = false;
                window.AdminConsole.updateSelectedStaffCount();

                // Close dropdown
                const dropdown = document.getElementById('multiselectDropdown');
                if (dropdown) dropdown.classList.add('ds-hidden');

                await loadPermissionsView();
            } else {
                showToast(data.message || 'Không thể gán quyền.', true);
            }
        } catch (err) {
            showToast('Lỗi kết nối máy chủ.', true);
        }
    };

    window.AdminConsole.removeStaffPermissions = async function (staffId) {
        if (!mock.permissions || !mock.permissions[staffId]) return;

        let permsToRevoke = [];
        if (isSearchActive) {
            if (activeFilterPermIds.length > 0) {
                permsToRevoke = activeFilterPermIds;
            } else if (activeFilterGroupId !== 'ALL') {
                permsToRevoke = ALL_PERMISSIONS.filter(p => p.group === activeFilterGroupId).map(p => p.id);
            } else {
                const groups = ['Kiểm duyệt', 'Tài chính', 'Vận hành'];
                permsToRevoke = mock.permissions[staffId].filter(pid => {
                    const p = ALL_PERMISSIONS.find(ap => ap.id === pid);
                    return p && groups.includes(p.group);
                });
            }
        } else {
            if (selectedGroupId === 'ALL') {
                const groups = ['Kiểm duyệt', 'Tài chính', 'Vận hành'];
                permsToRevoke = mock.permissions[staffId].filter(pid => {
                    const p = ALL_PERMISSIONS.find(ap => ap.id === pid);
                    return p && groups.includes(p.group);
                });
            } else {
                permsToRevoke = ALL_PERMISSIONS.filter(p => p.group === selectedGroupId).map(p => p.id);
            }
        }

        if (permsToRevoke.length === 0) {
            showToast('Không tìm thấy quyền phù hợp để thu hồi.', true);
            return;
        }

        const permNames = permsToRevoke.map(pid => {
            const p = ALL_PERMISSIONS.find(ap => ap.id === pid);
            return p ? p.label : pid;
        }).join(', ');

        if (confirm(`Bạn có chắc chắn muốn thu hồi các quyền sau của nhân viên: ${permNames}?`)) {
            try {
                const response = await authFetch('/admin/staff-permissions/revoke', {
                    method: 'POST',
                    body: JSON.stringify({
                        userId: staffId,
                        permissionNames: permsToRevoke
                    })
                });
                const data = await response.json();
                if (response.ok && data.success) {
                    showToast('Đã thu hồi quyền thành công.');
                    await loadPermissionsView();
                } else {
                    showToast(data.message || 'Không thể thu hồi quyền.', true);
                }
            } catch (err) {
                showToast('Lỗi kết nối máy chủ.', true);
            }
        }
    };

    window.AdminConsole.searchStaffByPermissions = function () {
        isSearchActive = true;
        permissionsPage = 0;

        const checkedPerms = Array.from(document.querySelectorAll('input[name="filterPermCheckbox"]:checked')).map(cb => cb.value);
        activeFilterPermIds = checkedPerms;
        activeFilterGroupId = selectedGroupId;

        loadPermissionsView();
        showToast('Đã cập nhật danh sách tìm kiếm.');
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

    function actionLabel(action) {
        if (!action) return '—';
        const str = String(action).trim();
        const lower = str.toLowerCase();

        // Staff & User Management
        if (lower === 'create_staff' || lower === 'user_create' || lower === 'staff_create') return 'Thêm nhân viên mới';
        if (lower === 'update_staff' || lower === 'user_status_update' || lower === 'account_update') return 'Cập nhật nhân viên';
        if (lower === 'delete_staff' || lower === 'soft_delete_user' || lower === 'user_delete') return 'Xóa tài khoản nhân viên';
        if (lower === 'lock_user' || lower === 'user_lock' || lower === 'account_lock') return 'Khóa tài khoản';
        if (lower === 'unlock_user' || lower === 'user_unlock' || lower === 'account_unlock') return 'Mở khóa tài khoản';
        if (lower === 'change_user_role' || lower === 'role_update' || lower === 'user_role_change') return 'Thay đổi vai trò';

        // Permissions
        if (lower === 'perm_update' || lower === 'permission_update') return 'Cập nhật quyền';
        if (lower === 'permission_grant' || lower === 'perm_grant') return 'Gán quyền nhân viên';
        if (lower === 'permission_revoke' || lower === 'perm_revoke') return 'Thu hồi quyền nhân viên';

        // Notifications & Maintenance
        if (lower === 'notification_create') return 'Tạo thông báo';
        if (lower === 'notification_draft_save') return 'Lưu bản nháp thông báo';
        if (lower === 'notification_draft_update') return 'Cập nhật bản nháp thông báo';
        if (lower === 'notification_publish' || lower === 'notification_draft_publish') return 'Phát hành thông báo';
        if (lower === 'notification_delete') return 'Xóa thông báo';
        if (lower === 'maintenance_toggle' || lower === 'toggle_maintenance') return 'Bật/Tắt bảo trì hệ thống';

        // System config & Commissions
        if (lower === 'config_update' || lower === 'system_config_update') return 'Cập nhật cấu hình hệ thống';
        if (lower === 'commission_update') return 'Cập nhật biểu phí';

        // KYC & Shop
        if (lower === 'kyc_approve') return 'Duyệt xác thực KYC';
        if (lower === 'kyc_reject') return 'Từ chối xác thực KYC';
        if (lower === 'shop_approve') return 'Duyệt mở cửa hàng';
        if (lower === 'shop_reject') return 'Từ chối mở cửa hàng';

        // Financial & Complaints
        if (lower === 'fund_withdraw' || lower === 'withdrawal_approve') return 'Duyệt lệnh rút tiền';
        if (lower === 'withdrawal_reject') return 'Từ chối lệnh rút tiền';
        if (lower === 'complaint_resolve') return 'Giải quyết khiếu nại';
        if (lower === 'dispute_start') return 'Khởi tạo tranh chấp';

        // Auth
        if (lower === 'login') return 'Đăng nhập';
        if (lower === 'logout') return 'Đăng xuất';

        let result = str.replace(/_/g, ' ');
        if (result === result.toUpperCase()) {
            result = result.split(' ').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join(' ');
        }
        return result;
    }

    function auditBadgeClass(action) {
        if (!action) return 'ds-badge-warning';
        const str = String(action);
        if (str.includes('Approve') || str.includes('Grant') || str.includes('Unlock') || str.includes('Publish')) return 'ds-badge-success';
        if (str.includes('Reject') || str.includes('Lock') || str.includes('Delete') || str.includes('Revoke')) return 'ds-badge-danger';
        if (str.includes('Config') || str.includes('Maintenance')) return 'ds-badge-info';
        return 'ds-badge-warning';
    }

    function formatVnd(n) {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(n || 0);
    }

    function txTypeLabel(type) {
        if (!type) return '—';
        const str = String(type).trim();
        if (str === 'C2C_Purchase' || str === 'Purchase') return 'Giao dịch C2C';
        if (str === 'Shop_Opening' || str === 'Shop_Fee') return 'Phí mở shop';
        if (str === 'Withdrawal' || str === 'Withdraw') return 'Rút tiền';
        if (str === 'Deposit') return 'Nạp tiền';
        return str.replace(/_/g, ' ');
    }

    function txStatusLabel(status) {
        if (!status) return '—';
        const str = String(status).trim();
        if (str === 'Completed' || str === 'COMPLETED' || str === 'SUCCESS') return 'Hoàn thành';
        if (str === 'Pending' || str === 'PENDING') return 'Đang xử lý';
        if (str === 'Held' || str === 'HELD' || str === 'ESCROW') return 'Tạm giữ (Escrow)';
        if (str === 'Failed' || str === 'FAILED' || str === 'REJECTED') return 'Thất bại';
        return str;
    }

    function formatNumberWithDots(val) {
        if (val === null || val === undefined || val === '') return '';
        const digits = String(val).replace(/\D/g, '');
        if (!digits) return '';
        return digits.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    }

    function stripDots(val) {
        if (!val) return 0;
        return Number(String(val).replace(/\./g, ''));
    }

    function formatDateTime(iso) {
        try { return new Date(iso).toLocaleString('vi-VN'); } catch { return iso; }
    }

    function showToast(message, isError = false) {
        const container = document.getElementById('toastContainer');
        if (!container) return null;
        const toast = document.createElement('div');
        
        let type = 'success';
        let title = 'Thành công';
        if (isError === true) {
            type = 'error';
            title = 'Thất bại';
        } else if (isError === 'info') {
            type = 'info';
            title = 'Thông báo';
        }
        
        toast.className = `ds-toast ds-toast-${type}`;
        toast.innerHTML = `
            <div>
                <p class="ds-toast-title">${title}</p>
                <p class="ds-toast-message">${escapeHtml(message)}</p>
            </div>
            <button class="ds-toast-close" onclick="this.parentElement.remove()">×</button>
        `;
        container.appendChild(toast);
        setTimeout(() => {
            if (toast.parentElement) toast.remove();
        }, 3000);
        return toast;
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
