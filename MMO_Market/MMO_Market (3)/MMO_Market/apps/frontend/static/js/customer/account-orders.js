(function () {

const ACCOUNT_ORDERS_MOCK_KEY = 'mmoMarketMyOrdersMock';

let accountSidebar = null;
let myOrders = [];
let currentPage = 1;
let pageSize = 5;
let appliedFilters = createEmptyFilters();

// THAY ĐỔI QUAN TRỌNG: Gọi registerAccountPage ĐÚNG CÁCH.
registerAccountPage('/js/customer/account-orders.js', initializeOrdersPage);

function initializeOrdersPage() {
    try {
        accountSidebar = new AccountSidebar();
        bindOrderEvents();
        loadOrdersPage();
    } catch (e) {
        alert("Lỗi khởi tạo JS: " + e.message);
    }
}

function bindOrderEvents() {
    document.getElementById('ordersFilterForm').addEventListener('submit', handleFilterSubmit);
    document.getElementById('ordersResetButton').addEventListener('click', resetFilters);
    document.getElementById('ordersPageSize').addEventListener('change', handlePageSizeChange);
}

async function loadOrdersPage() {
    try {
        const response = await authFetch('/v1/profile');
        if (!response.ok) {
            throw new Error('Không thể tải thông tin tài khoản.');
        }

        const profile = await response.json();
        accountSidebar.render(profile);
        
        // GỌI THẬT ĐẾN BACKEND (đã sửa API)
        const ordersResp = await authFetch('/transactions/me');
        if (!ordersResp.ok) {
            let errorText = 'Không thể tải danh sách đơn hàng.';
            try {
                const errJson = await ordersResp.json();
                if (errJson && errJson.message) errorText = errJson.message;
            } catch(e) {}
            throw new Error(errorText);
        }
        
        myOrders = await ordersResp.json();
        
        renderSummary(myOrders);
        renderOrders();
        
        const msgEl = document.getElementById('ordersMessage');
        if (msgEl) {
            msgEl.hidden = true;
            msgEl.classList.add('ds-hidden');
        }
    } catch (error) {
        console.error(error);
        alert("Lỗi tải trang đơn hàng: " + error.message);
        showOrdersMessage(error.message || 'Không thể tải danh sách đơn hàng.', 'danger');
    }
}


function getUserSpecificKey(baseKey) {
    try {
        const userStr = sessionStorage.getItem('userInfo') || sessionStorage.getItem('user');
        if (userStr) {
            const user = JSON.parse(userStr);
            if (user && user.email) {
                return `${baseKey}_${user.email}`;
            }
        }
    } catch (e) {
        console.error('Lỗi khi lấy user-specific key:', e);
    }
    return baseKey;
}

function readOrders() {
    const key = getUserSpecificKey(ACCOUNT_ORDERS_MOCK_KEY);
    try {
        // Ưu tiên đọc localStorage (persist qua tab/session), fallback sang sessionStorage
        const saved = localStorage.getItem(key) || sessionStorage.getItem(key);
        if (saved !== null) {
            const parsed = JSON.parse(saved);
            // Sync lên sessionStorage để các thư viện khác đọc được
            sessionStorage.setItem(key, saved);
            return parsed;
        }
    } catch {
        // fallback to seeded data below
    }

    let isDemo = false;
    try {
        const userStr = sessionStorage.getItem('userInfo') || sessionStorage.getItem('user');
        if (userStr) {
            const user = JSON.parse(userStr);
            if (user && user.email) {
                const demoEmails = ['customer01@gmail.com', 'customer02@gmail.com', 'customer03@gmail.com', 'customer04@gmail.com', 'customer05@gmail.com'];
                if (demoEmails.includes(user.email.toLowerCase())) {
                    isDemo = true;
                }
            }
        }
    } catch (e) {
        // ignore
    }

    const seeded = isDemo ? createSeedOrders() : [];
    // Seed vào cả hai storage
    localStorage.setItem(key, JSON.stringify(seeded));
    sessionStorage.setItem(key, JSON.stringify(seeded));
    return seeded;
}

function createSeedOrders() {
    const now = new Date();
    return [
        createOrder('MMO-ORD-1001', 7, 'Tài khoản Canva Pro 1 năm', 'Digital Store VN', 129000, 'COMPLETED', 'PAID', addDays(now, -1), '12 Tháng (1 Năm)'),
        createOrder('MMO-ORD-1002', 3, 'Gói proxy dân cư 5GB', 'ProxyHub', 240000, 'DELIVERED', 'PAID', addDays(now, -2), '5GB'),
        createOrder('MMO-ORD-1003', 13, 'Template landing page MMO', 'Design Market', 99000, 'PAID', 'PAID', addDays(now, -3), '1 Thiết kế'),
        createOrder('MMO-ORD-1004', 1, 'Tài khoản Netflix Premium', 'Account247', 75000, 'DISPUTED', 'PAID', addDays(now, -4), '1 Tháng'),
        createOrder('MMO-ORD-1005', 9, 'Tool automation social', 'ToolBox Seller', 450000, 'PENDING', 'PENDING', addDays(now, -5), 'Vĩnh viễn'),
        createOrder('MMO-ORD-1006', 5, 'Key Windows 11 Pro', 'Key Mall', 180000, 'REFUNDED', 'REFUNDED', addDays(now, -6), 'Vĩnh viễn'),
        createOrder('MMO-ORD-1007', 13, 'Khóa học chạy quảng cáo cơ bản', 'Ads Academy', 299000, 'COMPLETED', 'PAID', addDays(now, -7), 'Trọn đời'),
        createOrder('MMO-ORD-1008', 4, 'Tài khoản Spotify Family', 'Sub Store', 65000, 'CANCELLED', 'FAILED', addDays(now, -8), '12 Tháng (1 Năm)'),
        createOrder('MMO-ORD-1009', 8, 'Data email marketing B2B', 'DataX', 350000, 'DELIVERED', 'PAID', addDays(now, -9), '1 Danh sách')
    ];
}

function createOrder(orderCode, productId, productName, sellerName, amount, status, paymentStatus, createdDate, variantLabel = '') {
    return {
        orderCode,
        productId,
        productName,
        variantLabel,
        sellerName,
        amount,
        status,
        paymentStatus,
        createdAt: formatDateTime(createdDate),
        escrowReleaseDate: formatDate(addDays(createdDate, 3)),
        isReviewed: false
    };
}


function renderSummary(orders) {
    const completedCount = orders.filter(order => order.status === 'COMPLETED' || order.status === 'DELIVERED').length;
    const processingCount = orders.filter(order => order.status === 'PENDING' || order.status === 'PAID').length;
    const disputedCount = orders.filter(order => order.status === 'DISPUTED' || order.status === 'CANCELLED').length;

    const totalCountEl = document.getElementById('ordersTotalCount');
    const completedCountEl = document.getElementById('ordersCompletedCount');
    const processingCountEl = document.getElementById('ordersProcessingCount');
    const disputedCountEl = document.getElementById('ordersDisputedCount');

    if (totalCountEl) totalCountEl.textContent = `${orders.length} đơn`;
    if (completedCountEl) completedCountEl.textContent = `${completedCount} đơn`;
    if (processingCountEl) processingCountEl.textContent = `${processingCount} đơn`;
    if (disputedCountEl) disputedCountEl.textContent = `${disputedCount} đơn`;
}

function renderOrders() {
    const filtered = getFilteredOrders();
    const tableWrap = document.getElementById('ordersTableWrap');
    const emptyState = document.getElementById('ordersEmptyState');
    const tableBody = document.getElementById('ordersTableBody');
    const summary = document.getElementById('ordersResultSummary');
    const pagination = document.getElementById('ordersPagination');
    const totalPages = Math.max(Math.ceil(filtered.length / pageSize), 1);

    currentPage = Math.min(currentPage, totalPages);
    const startIndex = (currentPage - 1) * pageSize;
    const pagedOrders = filtered.slice(startIndex, startIndex + pageSize);

    if (!filtered.length) {
        tableWrap.hidden = true;
        emptyState.hidden = false;
        pagination.hidden = true;
        tableBody.innerHTML = '';
        summary.textContent = `Hiển thị 0/${myOrders.length} đơn hàng.`;
        return;
    }

    tableWrap.hidden = false;
    emptyState.hidden = true;
    pagination.hidden = totalPages <= 1;
    summary.textContent = `Hiển thị ${startIndex + 1}-${startIndex + pagedOrders.length}/${filtered.length} đơn hàng.`;

    tableBody.innerHTML = pagedOrders.map(order => {
        const orderDate = parseVietnameseDateTime(order.createdAt);
        const sellerLink = order.sellerId 
            ? `<a href="/shop/${order.sellerId}" style="text-decoration: underline; color: inherit;">${escapeHtml(order.sellerName)}</a>`
            : escapeHtml(order.sellerName);
        return `
            <tr>
                <td>${escapeHtml(order.orderCode)}</td>
                <td>
                    <div class="ds-flex ds-align-center ds-gap-sm">
                        <span>${escapeHtml(order.productName)}</span>
                    </div>
                    <div class="ds-text-sm ds-text-muted ds-mt-sm">
                        ${sellerLink}
                    </div>
                </td>
                <td class="ds-text-right">${formatMoney(order.amount)}</td>
                <td>
                    <span class="ds-badge ${getOrderStatusBadgeClass(order.status)}">
                        ${formatOrderStatus(order.status)}
                    </span>
                </td>
                <td>
                    <span class="ds-badge ${getPaymentBadgeClass(order.paymentStatus)}">
                        ${formatPaymentStatus(order.paymentStatus)}
                    </span>
                </td>
                <td>${orderDate ? formatDateTime(orderDate) : escapeHtml(order.createdAt)}</td>
                <td>
                    <div class="ds-flex ds-gap-sm">
                        <button type="button" class="ds-icon-btn ds-icon-btn-view" data-order-code="${escapeHtml(order.orderCode)}" title="Xem chi tiết">
                            <svg class="ds-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true" style="width: 16px; height: 16px;">
                                <path d="M2.25 12C3.73 8.12 7.49 5.25 12 5.25C16.51 5.25 20.27 8.12 21.75 12C20.27 15.88 16.51 18.75 12 18.75C7.49 18.75 3.73 15.88 2.25 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                <path d="M12 15.25C13.79 15.25 15.25 13.79 15.25 12C15.25 10.21 13.79 8.75 12 8.75C10.21 8.75 8.75 10.21 8.75 12C8.75 13.79 10.21 15.25 12 15.25Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            </svg>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
    bindOrderDetailButtons();
    renderPagination(filtered.length, totalPages);
}

function bindOrderDetailButtons() {
    document.querySelectorAll('[data-order-code]').forEach(button => {
        button.addEventListener('click', () => {
            window.location.href = `/account/orders/${encodeURIComponent(button.dataset.orderCode)}`;
        });
    });
}

function getFilteredOrders() {
    const keyword = appliedFilters.keyword;
    const status = appliedFilters.status;
    const paymentStatus = appliedFilters.paymentStatus;
    const fromDate = parseIsoDate(appliedFilters.fromDate);
    const toDate = parseIsoDate(appliedFilters.toDate);

    return myOrders.filter(order => {
        const text = `${order.orderCode || ''} ${order.productName || ''} ${order.sellerName || ''}`.toLowerCase();
        const orderDate = parseVietnameseDateTime(order.createdAt);

        if (keyword && !text.includes(keyword)) return false;
        if (status && (order.status || '').toUpperCase() !== status.toUpperCase()) return false;
        if (paymentStatus && order.paymentStatus !== paymentStatus) return false;
        if (fromDate && orderDate && orderDate < fromDate) return false;
        if (toDate && orderDate && orderDate > endOfDay(toDate)) return false;
        return true;
    });
}

function handleFilterSubmit(event) {
    event.preventDefault();
    appliedFilters = readCurrentFilters();
    currentPage = 1;
    renderOrders();
}

function readCurrentFilters() {
    const dateRange = readDateRangeFilter();

    return {
        keyword: document.getElementById('ordersSearchInput').value.trim().toLowerCase(),
        status: document.getElementById('ordersStatusFilter').value,
        paymentStatus: document.getElementById('ordersPaymentFilter').value,
        fromDate: dateRange.fromDate,
        toDate: dateRange.toDate
    };
}

function resetFilters() {
    document.getElementById('ordersSearchInput').value = '';
    document.getElementById('ordersStatusFilter').value = '';
    document.getElementById('ordersPaymentFilter').value = '';
    clearDatePicker('ordersDateRange', 'ordersDateRangeDisplay');
    appliedFilters = createEmptyFilters();
    currentPage = 1;
    renderOrders();
}

function readDateRangeFilter() {
    const range = document.getElementById('ordersDateRange')?.value || '';
    const [fromDate = '', toDate = ''] = range.split(',');
    return { fromDate, toDate };
}

function createEmptyFilters() {
    return {
        keyword: '',
        status: '',
        paymentStatus: '',
        fromDate: '',
        toDate: ''
    };
}

function handlePageSizeChange(event) {
    pageSize = Number(event.target.value) || 5;
    currentPage = 1;
    renderOrders();
}

function renderPagination(totalItems, totalPages) {
    const pages = document.getElementById('ordersPaginationPages');
    const info = document.getElementById('ordersPaginationInfo');

    info.textContent = `Tổng số: ${totalItems} đơn`;
    pages.innerHTML = [
        createPageButton('«', 1, currentPage === 1),
        createPageButton('‹', currentPage - 1, currentPage === 1),
        ...createPageNumbers(totalPages).map(page => createPageButton(page, page, false, page === currentPage)),
        createPageButton('›', currentPage + 1, currentPage === totalPages),
        createPageButton('»', totalPages, currentPage === totalPages)
    ].join('');

    pages.querySelectorAll('[data-page]').forEach(button => {
        button.addEventListener('click', () => {
            currentPage = Number(button.dataset.page);
            renderOrders();
        });
    });
}

function createPageNumbers(totalPages) {
    const pages = [];
    const start = Math.max(currentPage - 2, 1);
    const end = Math.min(start + 4, totalPages);

    for (let page = start; page <= end; page += 1) {
        pages.push(page);
    }

    return pages;
}

function createPageButton(label, page, disabled, active = false) {
    const classes = [
        'ds-page-link',
        active ? 'ds-page-link-active' : '',
        disabled ? 'ds-page-link-disabled' : ''
    ].filter(Boolean).join(' ');

    return `<button class="${classes}" type="button" data-page="${page}" ${disabled ? 'disabled' : ''}>${label}</button>`;
}

function clearDatePicker(hiddenId, displayId) {
    const hidden = document.getElementById(hiddenId);
    const display = document.getElementById(displayId);
    if (hidden) hidden.value = '';
    if (display) display.value = '';
}

function parseIsoDate(value) {
    if (!value) return null;
    const date = new Date(`${value}T00:00:00`);
    return Number.isNaN(date.getTime()) ? null : date;
}

function parseVietnameseDateTime(value) {
    const match = String(value || '').match(/^(\d{1,2}):(\d{2}):(\d{2})\s+(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
    if (!match) return null;
    return new Date(Number(match[6]), Number(match[5]) - 1, Number(match[4]), Number(match[1]), Number(match[2]), Number(match[3]));
}

function endOfDay(date) {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59, 999);
}

function addDays(date, days) {
    const next = new Date(date);
    next.setDate(next.getDate() + days);
    return next;
}

function formatDateTime(date) {
    return date.toLocaleString('vi-VN');
}

function formatDate(date) {
    return date.toLocaleDateString('vi-VN');
}

function formatMoney(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(amount) || 0);
}

function formatOrderStatus(status) {
    if (!status) return '-';
    const upperStatus = status.toUpperCase().trim();
    const map = {
        PENDING: 'Chờ xử lý',
        HELD: 'Đã giao',
        PAID: 'Đã thanh toán',
        DELIVERED: 'Đã giao',
        COMPLETED: 'Hoàn tất',
        CANCELLED: 'Đã hủy',
        DISPUTED: 'Tranh chấp',
        REFUNDED: 'Đã hoàn tiền'
    };
    return map[upperStatus] || status || '-';
}

function getOrderStatusBadgeClass(status) {
    if (!status) return 'ds-badge-muted';
    const upperStatus = status.toUpperCase().trim();
    if (upperStatus === 'COMPLETED' || upperStatus === 'DELIVERED' || upperStatus === 'HELD') return 'ds-badge-success';
    if (upperStatus === 'DISPUTED' || upperStatus === 'CANCELLED') return 'ds-badge-danger';
    if (upperStatus === 'PENDING' || upperStatus === 'PAID') return 'ds-badge-warning';
    if (upperStatus === 'REFUNDED') return 'ds-badge-info';
    return 'ds-badge-muted';
}

function formatPaymentStatus(status) {
    if (!status) return '-';
    const upperStatus = status.toUpperCase().trim();
    const map = {
        PAID: 'Đã thanh toán',
        PENDING: 'Chờ thanh toán',
        FAILED: 'Thất bại',
        REFUNDED: 'Đã hoàn tiền'
    };
    return map[upperStatus] || status || '-';
}

function getPaymentBadgeClass(status) {
    if (!status) return 'ds-badge-muted';
    const upperStatus = status.toUpperCase().trim();
    if (upperStatus === 'PAID') return 'ds-badge-success';
    if (upperStatus === 'PENDING') return 'ds-badge-warning';
    if (upperStatus === 'FAILED') return 'ds-badge-danger';
    if (upperStatus === 'REFUNDED') return 'ds-badge-info';
    return 'ds-badge-muted';
}

function showOrdersMessage(message, type) {
    const messageElement = document.getElementById('ordersMessage');
    messageElement.textContent = message;
    messageElement.hidden = false;
    messageElement.style.display = '';
    messageElement.classList.remove('ds-hidden');
    messageElement.classList.remove('ds-alert-info', 'ds-alert-warning', 'ds-alert-danger', 'ds-alert-success');
    messageElement.classList.add(`ds-alert-${type}`);
}

function escapeHtml(value) {
    return String(value || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function registerAccountPage(scriptPath, initializer) {
    window.AccountPageInitializers = window.AccountPageInitializers || {};
    window.AccountPageInitializers[scriptPath] = initializer;
    if (document.currentScript?.dataset.accountPartial !== 'true') {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', initializer);
        } else {
            initializer();
        }
    }
}

})();
