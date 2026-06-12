const WALLET_MOCK_TRANSACTIONS_KEY = 'mmoMarketWalletTransactionsMock';

let accountSidebar = null;
let walletTransactions = [];
let currentPage = 1;
let pageSize = 5;
let appliedFilters = createEmptyFilters();

document.addEventListener('DOMContentLoaded', initializeTransactionsPage);

function initializeTransactionsPage() {
    accountSidebar = new AccountSidebar();
    bindTransactionEvents();
    loadTransactionsPage();
}

function bindTransactionEvents() {
    document.getElementById('transactionsFilterForm').addEventListener('submit', handleFilterSubmit);
    document.getElementById('transactionsPageSize').addEventListener('change', handlePageSizeChange);
    document.getElementById('transactionsResetButton').addEventListener('click', resetFilters);
}

async function loadTransactionsPage() {
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
        walletTransactions = readWalletTransactions();
        renderSummary(walletTransactions);
        renderTransactions();
        showTransactionsMessage('Lịch sử giao dịch hiện dùng dữ liệu mock frontend.', 'info');
    } catch (error) {
        showTransactionsMessage(error.message || 'Không thể tải lịch sử giao dịch.', 'danger');
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

function readWalletTransactions() {
    const key = getUserSpecificKey(WALLET_MOCK_TRANSACTIONS_KEY);
    try {
        const saved = sessionStorage.getItem(key);
        if (saved !== null) {
            return JSON.parse(saved);
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

    const seeded = isDemo ? createSeedTransactions() : [];
    sessionStorage.setItem(key, JSON.stringify(seeded));
    return seeded;
}

function createSeedTransactions() {
    const now = new Date();
    return [
        {
            code: 'MMO-TOPUP-DEMO-001',
            type: 'TOPUP',
            amount: 100000,
            status: 'SUCCESS',
            description: 'Nạp tiền demo qua chuyển khoản',
            createdAt: formatDateTime(addDays(now, -1))
        },
        {
            code: 'MMO-PAY-DEMO-002',
            type: 'PAYMENT',
            amount: -45000,
            status: 'SUCCESS',
            description: 'Thanh toán đơn hàng demo',
            createdAt: formatDateTime(addDays(now, -2))
        },
        {
            code: 'MMO-TOPUP-DEMO-003',
            type: 'TOPUP',
            amount: 200000,
            status: 'PENDING',
            description: 'Yêu cầu nạp tiền đang chờ thanh toán',
            createdAt: formatDateTime(now)
        },
        {
            code: 'MMO-REFUND-DEMO-004',
            type: 'REFUND',
            amount: 25000,
            status: 'SUCCESS',
            description: 'Hoàn tiền đơn hàng demo',
            createdAt: formatDateTime(addDays(now, -3))
        },
        {
            code: 'MMO-ESCROW-DEMO-005',
            type: 'ESCROW',
            amount: -75000,
            status: 'PENDING',
            description: 'Tiền đang giữ escrow',
            createdAt: formatDateTime(addDays(now, -4))
        },
        {
            code: 'MMO-TOPUP-DEMO-006',
            type: 'TOPUP',
            amount: 500000,
            status: 'FAILED',
            description: 'Yêu cầu nạp tiền thất bại',
            createdAt: formatDateTime(addDays(now, -5))
        },
        {
            code: 'MMO-PAY-DEMO-007',
            type: 'PAYMENT',
            amount: -120000,
            status: 'SUCCESS',
            description: 'Thanh toán đơn hàng sản phẩm số',
            createdAt: formatDateTime(addDays(now, -6))
        },
        {
            code: 'MMO-TOPUP-DEMO-008',
            type: 'TOPUP',
            amount: 300000,
            status: 'SUCCESS',
            description: 'Nạp tiền tự động demo',
            createdAt: formatDateTime(addDays(now, -7))
        },
        {
            code: 'MMO-WITHDRAW-DEMO-009',
            type: 'WITHDRAWAL',
            amount: -150000,
            status: 'PENDING',
            description: 'Yêu cầu rút tiền demo',
            createdAt: formatDateTime(addDays(now, -8))
        }
    ];
}

function renderSummary(transactions) {
    const summary = transactions.reduce((result, item) => {
        if (item.type === 'TOPUP' && item.status === 'SUCCESS') {
            result.topup += Math.abs(Number(item.amount) || 0);
        }

        if (['PAYMENT', 'WITHDRAWAL', 'ESCROW'].includes(item.type) && item.status === 'SUCCESS') {
            result.spent += Math.abs(Number(item.amount) || 0);
        }

        if (item.status === 'PENDING') {
            result.pending += 1;
        }

        result.total += 1;
        return result;
    }, { topup: 0, spent: 0, pending: 0, total: 0 });

    document.getElementById('transactionsTopupTotal').textContent = formatMoney(summary.topup);
    document.getElementById('transactionsSpentTotal').textContent = formatMoney(summary.spent);
    document.getElementById('transactionsPendingCount').textContent = `${summary.pending} giao dịch`;
    document.getElementById('transactionsTotalCount').textContent = `${summary.total} giao dịch`;
}

function renderTransactions() {
    const filtered = getFilteredTransactions();
    const tableWrap = document.getElementById('transactionsTableWrap');
    const emptyState = document.getElementById('transactionsEmptyState');
    const tableBody = document.getElementById('transactionsTableBody');
    const summary = document.getElementById('transactionsResultSummary');
    const pagination = document.getElementById('transactionsPagination');
    const totalPages = Math.max(Math.ceil(filtered.length / pageSize), 1);

    currentPage = Math.min(currentPage, totalPages);
    const startIndex = (currentPage - 1) * pageSize;
    const pagedTransactions = filtered.slice(startIndex, startIndex + pageSize);

    if (!filtered.length) {
        tableWrap.hidden = true;
        emptyState.hidden = false;
        pagination.hidden = true;
        tableBody.innerHTML = '';
        summary.textContent = `Hiển thị 0/${walletTransactions.length} giao dịch.`;
        return;
    }

    emptyState.hidden = true;
    tableWrap.hidden = false;
    pagination.hidden = false;
    summary.textContent = `Hiển thị ${startIndex + 1}-${startIndex + pagedTransactions.length}/${filtered.length} giao dịch.`;
    tableBody.innerHTML = pagedTransactions.map((transaction, index) => `
        <tr>
            <td class="ds-table-center">${startIndex + index + 1}</td>
            <td><span class="transactions-code">${escapeHtml(transaction.code)}</span></td>
            <td>${formatTransactionType(transaction.type)}</td>
            <td class="transactions-description">${escapeHtml(transaction.description || getDefaultDescription(transaction.type))}</td>
            <td><span class="transactions-amount ${getAmountClass(transaction)}">${formatSignedMoney(transaction)}</span></td>
            <td class="ds-table-center">
                <span class="ds-badge ${getStatusBadgeClass(transaction.status)}">${formatStatus(transaction.status)}</span>
            </td>
            <td>${escapeHtml(transaction.createdAt || '-')}</td>
        </tr>
    `).join('');
    renderPagination(filtered.length, totalPages);
}

function getFilteredTransactions() {
    const keyword = appliedFilters.keyword;
    const type = appliedFilters.type;
    const status = appliedFilters.status;
    const fromDate = parseIsoDate(appliedFilters.fromDate);
    const toDate = parseIsoDate(appliedFilters.toDate);

    return walletTransactions.filter(transaction => {
        const text = `${transaction.code || ''} ${transaction.description || ''}`.toLowerCase();
        const transactionDate = parseTransactionDate(transaction.createdAt);

        if (keyword && !text.includes(keyword)) return false;
        if (type && transaction.type !== type) return false;
        if (status && transaction.status !== status) return false;
        if (fromDate && transactionDate && transactionDate < fromDate) return false;
        if (toDate && transactionDate && transactionDate > endOfDay(toDate)) return false;
        return true;
    });
}

function resetFilters() {
    document.getElementById('transactionsSearchInput').value = '';
    document.getElementById('transactionsTypeFilter').value = '';
    document.getElementById('transactionsStatusFilter').value = '';
    clearDatePicker('transactionsFromDate', 'transactionsFromDateDisplay');
    clearDatePicker('transactionsToDate', 'transactionsToDateDisplay');
    appliedFilters = createEmptyFilters();
    currentPage = 1;
    renderTransactions();
}

function handleFilterSubmit(event) {
    event.preventDefault();
    appliedFilters = readCurrentFilters();
    currentPage = 1;
    renderTransactions();
}

function readCurrentFilters() {
    return {
        keyword: document.getElementById('transactionsSearchInput').value.trim().toLowerCase(),
        type: document.getElementById('transactionsTypeFilter').value,
        status: document.getElementById('transactionsStatusFilter').value,
        fromDate: document.getElementById('transactionsFromDate').value,
        toDate: document.getElementById('transactionsToDate').value
    };
}

function createEmptyFilters() {
    return {
        keyword: '',
        type: '',
        status: '',
        fromDate: '',
        toDate: ''
    };
}

function handlePageSizeChange(event) {
    pageSize = Number(event.target.value) || 5;
    currentPage = 1;
    renderTransactions();
}

function renderPagination(totalItems, totalPages) {
    const pages = document.getElementById('transactionsPaginationPages');
    const info = document.getElementById('transactionsPaginationInfo');

    info.textContent = `Tổng số: ${totalItems} giao dịch`;
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
            renderTransactions();
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

function parseTransactionDate(value) {
    if (!value) return null;

    const iso = new Date(value);
    if (!Number.isNaN(iso.getTime())) return iso;

    const match = String(value).match(/^(\d{1,2}):(\d{2}):(\d{2})\s+(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
    if (match) {
        return new Date(Number(match[6]), Number(match[5]) - 1, Number(match[4]), Number(match[1]), Number(match[2]), Number(match[3]));
    }

    return null;
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

function formatMoney(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Math.abs(Number(amount) || 0));
}

function formatSignedMoney(transaction) {
    const amount = Number(transaction.amount) || 0;
    const sign = getAmountClass(transaction) === 'transactions-amount--positive' ? '+' : '-';
    return `${sign}${formatMoney(amount)}`;
}

function getAmountClass(transaction) {
    if (['TOPUP', 'REFUND'].includes(transaction.type)) {
        return 'transactions-amount--positive';
    }
    return 'transactions-amount--negative';
}

function formatTransactionType(type) {
    const map = {
        TOPUP: 'Nạp tiền',
        PAYMENT: 'Thanh toán',
        REFUND: 'Hoàn tiền',
        ESCROW: 'Escrow',
        WITHDRAWAL: 'Rút tiền'
    };
    return map[type] || type || '-';
}

function getDefaultDescription(type) {
    const map = {
        TOPUP: 'Nạp tiền vào ví',
        PAYMENT: 'Thanh toán đơn hàng',
        REFUND: 'Hoàn tiền',
        ESCROW: 'Giao dịch escrow',
        WITHDRAWAL: 'Rút tiền khỏi ví'
    };
    return map[type] || 'Giao dịch ví';
}

function formatStatus(status) {
    const map = {
        SUCCESS: 'Thành công',
        PENDING: 'Đang xử lý',
        FAILED: 'Thất bại'
    };
    return map[status] || status || '-';
}

function getStatusBadgeClass(status) {
    if (status === 'SUCCESS') return 'ds-badge-success';
    if (status === 'PENDING') return 'ds-badge-warning';
    if (status === 'FAILED') return 'ds-badge-danger';
    return 'ds-badge-muted';
}

function showTransactionsMessage(message, type) {
    const messageElement = document.getElementById('transactionsMessage');
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
