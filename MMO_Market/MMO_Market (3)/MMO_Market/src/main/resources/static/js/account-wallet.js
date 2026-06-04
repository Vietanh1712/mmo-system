const WALLET_MOCK_TRANSACTIONS_KEY = 'mmoMarketWalletTransactionsMock';

let accountSidebar = null;
let walletProfile = null;

document.addEventListener('DOMContentLoaded', initializeWalletPage);

function initializeWalletPage() {
    accountSidebar = new AccountSidebar();
    bindWalletEvents();
    loadWalletPage();
}

function bindWalletEvents() {
    document.getElementById('walletTopupButton').addEventListener('click', openTopupPage);
    document.getElementById('walletHistoryButton').addEventListener('click', openTransactionsPage);
    document.getElementById('walletViewAllButton').addEventListener('click', openTransactionsPage);
}

async function loadWalletPage() {
    const token = sessionStorage.getItem('accessToken');
    if (!token || token === 'null' || token === 'undefined') {
        window.location.href = '/login';
        return;
    }

    try {
        const response = await authFetch('/v1/profile');
        if (!response.ok) {
            throw new Error('Không thể tải thông tin ví.');
        }

        walletProfile = await response.json();
        accountSidebar.render(walletProfile);
        renderWalletDashboard(walletProfile);
    } catch (error) {
        showWalletMessage(error.message || 'Không thể tải màn ví.', 'danger');
    }
}

function renderWalletDashboard(profile) {
    const balance = profile.balanceVnd || 0;
    const transactions = readWalletTransactions();
    const stats = calculateWalletStats(transactions);

    document.getElementById('walletBalance').textContent = formatMoney(balance);
    document.getElementById('walletTotalTopup').textContent = formatMoney(stats.totalTopup);
    document.getElementById('walletTotalSpent').textContent = formatMoney(stats.totalSpent);
    document.getElementById('walletPendingCount').textContent = `${stats.pendingCount} giao dịch`;
    document.getElementById('walletEscrowAmount').textContent = formatMoney(stats.escrowAmount);

    renderKycNotice();
    renderRecentTransactions(transactions);
    showWalletMessage('Ví đã sẵn sàng. Dữ liệu thống kê hiện là mock frontend.', 'info');
}

function readWalletTransactions() {
    try {
        return JSON.parse(sessionStorage.getItem(WALLET_MOCK_TRANSACTIONS_KEY)) || [];
    } catch {
        return [];
    }
}

function calculateWalletStats(transactions) {
    return transactions.reduce((stats, transaction) => {
        if (transaction.type === 'TOPUP' && transaction.status === 'SUCCESS') {
            stats.totalTopup += transaction.amount;
        }

        if (transaction.type === 'PAYMENT' && transaction.status === 'SUCCESS') {
            stats.totalSpent += Math.abs(transaction.amount);
        }

        if (transaction.status === 'PENDING') {
            stats.pendingCount += 1;
        }

        if (transaction.type === 'ESCROW') {
            stats.escrowAmount += Math.abs(transaction.amount);
        }

        return stats;
    }, {
        totalTopup: 0,
        totalSpent: 0,
        pendingCount: 0,
        escrowAmount: 0
    });
}

function renderKycNotice() {
    const kycStatus = getKycStatus();
    const title = document.getElementById('walletKycTitle');
    const description = document.getElementById('walletKycDescription');
    const notice = document.getElementById('walletKycNotice');

    notice.classList.remove('wallet-kyc-notice--success');

    if (kycStatus === 'APPROVED') {
        title.textContent = 'Tài khoản đã định danh';
        description.textContent = 'Ví đã sẵn sàng cho các tính năng nâng cao khi backend được triển khai.';
        notice.classList.add('wallet-kyc-notice--success');
        return;
    }

    if (kycStatus === 'PENDING') {
        title.textContent = 'Hồ sơ KYC đang chờ duyệt';
        description.textContent = 'Bạn vẫn có thể theo dõi ví trong lúc staff kiểm tra hồ sơ.';
        return;
    }

    if (kycStatus === 'REJECTED') {
        title.textContent = 'KYC bị từ chối';
        description.textContent = 'Vui lòng gửi lại hồ sơ để chuẩn bị cho các tính năng ví nâng cao.';
        return;
    }

    title.textContent = 'Xác minh tài khoản để dùng ví an toàn hơn';
    description.textContent = 'Hoàn tất KYC để chuẩn bị cho các tính năng ví nâng cao.';
}

function renderRecentTransactions(transactions) {
    const tableWrap = document.getElementById('walletRecentTableWrap');
    const emptyState = document.getElementById('walletEmptyState');
    const body = document.getElementById('walletRecentTableBody');
    const recent = transactions.slice(0, 5);

    if (!recent.length) {
        tableWrap.hidden = true;
        emptyState.hidden = false;
        return;
    }

    emptyState.hidden = true;
    tableWrap.hidden = false;
    body.innerHTML = recent.map(transaction => `
        <tr>
            <td>${transaction.code}</td>
            <td>${formatTransactionType(transaction.type)}</td>
            <td><strong>${formatMoney(transaction.amount)}</strong></td>
            <td class="ds-table-center"><span class="ds-badge ${getStatusBadgeClass(transaction.status)}">${formatStatus(transaction.status)}</span></td>
            <td>${transaction.createdAt}</td>
        </tr>
    `).join('');
}

function openTopupPage() {
    window.location.href = '/wallet/topup';
}

function openTransactionsPage() {
    window.location.href = '/wallet/transactions';
}

function showWalletMessage(message, type) {
    const messageElement = document.getElementById('walletMessage');
    messageElement.textContent = message;
    messageElement.hidden = false;
    messageElement.classList.remove('ds-alert-info', 'ds-alert-warning', 'ds-alert-danger', 'ds-alert-success');
    messageElement.classList.add(`ds-alert-${type}`);
}

function getKycStatus() {
    try {
        return JSON.parse(sessionStorage.getItem('mmoMarketKycMock'))?.status || 'NOT_SUBMITTED';
    } catch {
        return 'NOT_SUBMITTED';
    }
}

function formatMoney(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })
        .format(amount || 0);
}

function formatTransactionType(type) {
    const map = {
        TOPUP: 'Nạp tiền',
        PAYMENT: 'Thanh toán',
        REFUND: 'Hoàn tiền',
        ESCROW: 'Escrow'
    };
    return map[type] || type;
}

function formatStatus(status) {
    const map = {
        SUCCESS: 'Thành công',
        PENDING: 'Đang xử lý',
        FAILED: 'Thất bại'
    };
    return map[status] || status;
}

function getStatusBadgeClass(status) {
    if (status === 'SUCCESS') return 'ds-badge-success';
    if (status === 'PENDING') return 'ds-badge-warning';
    if (status === 'FAILED') return 'ds-badge-danger';
    return 'ds-badge-muted';
}
