(function () {

let accountSidebar = null;
let walletProfile = null;

registerAccountPage('/js/customer/account-wallet.js', initializeWalletPage);

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

        // Fetch Stats
        const statsRes = await authFetch('/v1/wallet/stats');
        if (!statsRes.ok) throw new Error('Không thể tải thống kê ví.');
        const stats = await statsRes.json();

        // Fetch Recent Transactions
        const txnsRes = await authFetch('/v1/wallet/transactions?page=0&size=5');
        if (!txnsRes.ok) throw new Error('Không thể tải lịch sử giao dịch.');
        const txnsPage = await txnsRes.json();

        renderWalletDashboard(walletProfile, stats, txnsPage.content || []);
    } catch (error) {
        showWalletMessage(error.message || 'Không thể tải màn ví.', 'danger');
    }
}

function renderWalletDashboard(profile, stats, recentTransactions) {
    const balance = profile.balanceVnd || 0;

    document.getElementById('walletBalance').textContent = formatMoney(balance);
    document.getElementById('walletTotalTopup').textContent = formatMoney(stats.totalTopup || 0);
    document.getElementById('walletTotalSpent').textContent = formatMoney(stats.totalSpent || 0);
    document.getElementById('walletPendingCount').textContent = `${stats.pendingCount || 0} giao dịch`;
    const escrowEl = document.getElementById('walletEscrowAmount');
    if (escrowEl) {
        escrowEl.textContent = formatMoney(stats.escrowAmount || 0);
    }

    renderKycNotice();
    renderRecentTransactions(recentTransactions);
    document.getElementById('walletMessage').hidden = true;
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
    return (walletProfile && walletProfile.kycStatus) ? walletProfile.kycStatus : 'NOT_SUBMITTED';
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
    switch (status) {
        case 'SUCCESS': return 'ds-badge-success';
        case 'PENDING': return 'ds-badge-warning';
        case 'FAILED': return 'ds-badge-danger';
        default: return 'ds-badge-neutral';
    }
}

function registerAccountPage(scriptPath, initializer) {
    window.AccountPageInitializers = window.AccountPageInitializers || {};
    window.AccountPageInitializers[scriptPath] = initializer;
    if (document.currentScript?.dataset.accountPartial !== 'true') {
        document.addEventListener('DOMContentLoaded', initializer);
    }
}
})();
