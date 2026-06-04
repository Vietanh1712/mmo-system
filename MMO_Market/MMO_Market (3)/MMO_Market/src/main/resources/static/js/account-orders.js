const ACCOUNT_ORDERS_MOCK_KEY = 'mmoMarketMyOrdersMock';

let accountSidebar = null;
let myOrders = [];
let currentPage = 1;
let pageSize = 5;
let appliedFilters = createEmptyFilters();

document.addEventListener('DOMContentLoaded', initializeOrdersPage);

function initializeOrdersPage() {
    accountSidebar = new AccountSidebar();
    bindOrderEvents();
    loadOrdersPage();
}

function bindOrderEvents() {
    document.getElementById('ordersFilterForm').addEventListener('submit', handleFilterSubmit);
    document.getElementById('ordersResetButton').addEventListener('click', resetFilters);
    document.getElementById('ordersPageSize').addEventListener('change', handlePageSizeChange);
}

async function loadOrdersPage() {
    const token = sessionStorage.getItem('accessToken');
    if (!token || token === 'null' || token === 'undefined') {
        window.location.href = '/login';
        return;
    }

    try {
        const response = await authFetch('/v1/profile');
        if (!response.ok) {
            throw new Error('Không thể tải thông tin tài khoản.');
        }

        const profile = await response.json();
        accountSidebar.render(profile);
        myOrders = readOrders();
        renderSummary(myOrders);
        renderOrders();
        showOrdersMessage('Danh sách đơn hàng hiện dùng dữ liệu mock frontend.', 'info');
    } catch (error) {
        showOrdersMessage(error.message || 'Không thể tải danh sách đơn hàng.', 'danger');
    }
}

function readOrders() {
    try {
        const saved = JSON.parse(sessionStorage.getItem(ACCOUNT_ORDERS_MOCK_KEY));
        if (Array.isArray(saved) && saved.length) {
            return saved;
        }
    } catch {
        // fallback to seeded data below
    }

    const seeded = createSeedOrders();
    sessionStorage.setItem(ACCOUNT_ORDERS_MOCK_KEY, JSON.stringify(seeded));
    return seeded;
}

function createSeedOrders() {
    const now = new Date();
    return [
        createOrder('MMO-ORD-1001', 'Tài khoản Canva Pro 1 năm', 'Digital Store VN', 129000, 'COMPLETED', 'PAID', addDays(now, -1)),
        createOrder('MMO-ORD-1002', 'Gói proxy dân cư 5GB', 'ProxyHub', 240000, 'DELIVERED', 'PAID', addDays(now, -2)),
        createOrder('MMO-ORD-1003', 'Template landing page MMO', 'Design Market', 99000, 'PAID', 'PAID', addDays(now, -3)),
        createOrder('MMO-ORD-1004', 'Tài khoản Netflix Premium', 'Account247', 75000, 'DISPUTED', 'PAID', addDays(now, -4)),
        createOrder('MMO-ORD-1005', 'Tool automation social', 'ToolBox Seller', 450000, 'PENDING', 'PENDING', addDays(now, -5)),
        createOrder('MMO-ORD-1006', 'Key Windows 11 Pro', 'Key Mall', 180000, 'REFUNDED', 'REFUNDED', addDays(now, -6)),
        createOrder('MMO-ORD-1007', 'Khóa học chạy quảng cáo cơ bản', 'Ads Academy', 299000, 'COMPLETED', 'PAID', addDays(now, -7)),
        createOrder('MMO-ORD-1008', 'Tài khoản Spotify Family', 'Sub Store', 65000, 'CANCELLED', 'FAILED', addDays(now, -8)),
        createOrder('MMO-ORD-1009', 'Data email marketing B2B', 'DataX', 350000, 'DELIVERED', 'PAID', addDays(now, -9))
    ];
}

function createOrder(orderCode, productName, sellerName, amount, status, paymentStatus, createdDate) {
    return {
        orderCode,
        productName,
        sellerName,
        amount,
        status,
        paymentStatus,
        createdAt: formatDateTime(createdDate),
        escrowReleaseDate: formatDate(addDays(createdDate, 3))
    };
}

function renderSummary(orders) {
    const summary = orders.reduce((result, order) => {
        result.total += 1;
        if (order.status === 'COMPLETED') result.completed += 1;
        if (['PENDING', 'PAID', 'DELIVERED'].includes(order.status)) result.processing += 1;
        if (order.status === 'DISPUTED') result.disputed += 1;
        return result;
    }, { total: 0, completed: 0, processing: 0, disputed: 0 });

    document.getElementById('ordersTotalCount').textContent = `${summary.total} đơn`;
    document.getElementById('ordersCompletedCount').textContent = `${summary.completed} đơn`;
    document.getElementById('ordersProcessingCount').textContent = `${summary.processing} đơn`;
    document.getElementById('ordersDisputedCount').textContent = `${summary.disputed} đơn`;
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

    emptyState.hidden = true;
    tableWrap.hidden = false;
    pagination.hidden = false;
    summary.textContent = `Hiển thị ${startIndex + 1}-${startIndex + pagedOrders.length}/${filtered.length} đơn hàng.`;
    tableBody.innerHTML = pagedOrders.map((order, index) => `
        <tr>
            <td class="ds-table-center">${startIndex + index + 1}</td>
            <td><span class="orders-code">${escapeHtml(order.orderCode)}</span></td>
            <td>
                <div class="orders-product">
                    <div class="orders-product-title">${escapeHtml(order.productName)}</div>
                    <div class="orders-product-subtitle">Escrow đến: ${escapeHtml(order.escrowReleaseDate)}</div>
                </div>
            </td>
            <td>${escapeHtml(order.sellerName)}</td>
            <td><span class="orders-amount">${formatMoney(order.amount)}</span></td>
            <td class="ds-table-center"><span class="ds-badge ${getOrderStatusBadgeClass(order.status)}">${formatOrderStatus(order.status)}</span></td>
            <td class="ds-table-center"><span class="ds-badge ${getPaymentBadgeClass(order.paymentStatus)}">${formatPaymentStatus(order.paymentStatus)}</span></td>
            <td>${escapeHtml(order.createdAt)}</td>
            <td>
                <div class="ds-table-actions">
                    <button class="ds-icon-btn ds-icon-btn-view" type="button" data-order-code="${escapeHtml(order.orderCode)}" aria-label="Xem chi tiết đơn hàng">
                        <svg class="ds-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                            <path d="M2.25 12C3.73 8.12 7.49 5.25 12 5.25C16.51 5.25 20.27 8.12 21.75 12C20.27 15.88 16.51 18.75 12 18.75C7.49 18.75 3.73 15.88 2.25 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            <path d="M12 15.25C13.79 15.25 15.25 13.79 15.25 12C15.25 10.21 13.79 8.75 12 8.75C10.21 8.75 8.75 10.21 8.75 12C8.75 13.79 10.21 15.25 12 15.25Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        </svg>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
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
        if (status && order.status !== status) return false;
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
    return {
        keyword: document.getElementById('ordersSearchInput').value.trim().toLowerCase(),
        status: document.getElementById('ordersStatusFilter').value,
        paymentStatus: document.getElementById('ordersPaymentFilter').value,
        fromDate: document.getElementById('ordersFromDate').value,
        toDate: document.getElementById('ordersToDate').value
    };
}

function resetFilters() {
    document.getElementById('ordersSearchInput').value = '';
    document.getElementById('ordersStatusFilter').value = '';
    document.getElementById('ordersPaymentFilter').value = '';
    clearDatePicker('ordersFromDate', 'ordersFromDateDisplay');
    clearDatePicker('ordersToDate', 'ordersToDateDisplay');
    appliedFilters = createEmptyFilters();
    currentPage = 1;
    renderOrders();
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
    document.getElementById(hiddenId).value = '';
    document.getElementById(displayId).value = '';
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
    const map = {
        PENDING: 'Chờ xử lý',
        PAID: 'Đã thanh toán',
        DELIVERED: 'Đã giao',
        COMPLETED: 'Hoàn tất',
        CANCELLED: 'Đã hủy',
        DISPUTED: 'Tranh chấp',
        REFUNDED: 'Đã hoàn tiền'
    };
    return map[status] || status || '-';
}

function getOrderStatusBadgeClass(status) {
    if (status === 'COMPLETED' || status === 'DELIVERED') return 'ds-badge-success';
    if (status === 'DISPUTED' || status === 'CANCELLED') return 'ds-badge-danger';
    if (status === 'PENDING' || status === 'PAID') return 'ds-badge-warning';
    if (status === 'REFUNDED') return 'ds-badge-info';
    return 'ds-badge-muted';
}

function formatPaymentStatus(status) {
    const map = {
        PAID: 'Đã thanh toán',
        PENDING: 'Chờ thanh toán',
        FAILED: 'Thất bại',
        REFUNDED: 'Đã hoàn tiền'
    };
    return map[status] || status || '-';
}

function getPaymentBadgeClass(status) {
    if (status === 'PAID') return 'ds-badge-success';
    if (status === 'PENDING') return 'ds-badge-warning';
    if (status === 'FAILED') return 'ds-badge-danger';
    if (status === 'REFUNDED') return 'ds-badge-info';
    return 'ds-badge-muted';
}

function showOrdersMessage(message, type) {
    const messageElement = document.getElementById('ordersMessage');
    messageElement.textContent = message;
    messageElement.hidden = false;
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
