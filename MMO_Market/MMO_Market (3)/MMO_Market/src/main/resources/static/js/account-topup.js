const TOPUP_MIN_AMOUNT = 10000;
const TOPUP_MAX_AMOUNT = 50000000;
const WALLET_MOCK_TRANSACTIONS_KEY = 'mmoMarketWalletTransactionsMock';

let topupProfile = null;
let accountSidebar = null;
let currentTransferContent = '';

document.addEventListener('DOMContentLoaded', initializeTopupPage);

function initializeTopupPage() {
    accountSidebar = new AccountSidebar();
    bindTopupEvents();
    loadTopupPage();
}

function bindTopupEvents() {
    document.getElementById('topupForm').addEventListener('submit', handleTopupSubmit);
    document.getElementById('topupAmount').addEventListener('input', handleAmountInput);
    document.getElementById('copyTransferContentButton').addEventListener('click', copyTransferContent);
    document.getElementById('topupHistoryButton').addEventListener('click', showHistoryComingSoon);

    document.querySelectorAll('.topup-quick-btn').forEach(button => {
        button.addEventListener('click', () => selectQuickAmount(button));
    });
}

async function loadTopupPage() {
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

        topupProfile = await response.json();
        accountSidebar.render(topupProfile);
        document.getElementById('topupBalance').textContent = formatMoney(topupProfile.balanceVnd || 0);
        showTopupMessage('Nhập số tiền để tạo hướng dẫn chuyển khoản.', 'info');
    } catch (error) {
        showTopupMessage(error.message || 'Không thể tải màn nạp tiền.', 'danger');
    }
}

function handleAmountInput(event) {
    const normalized = normalizeAmount(event.target.value);
    event.target.value = normalized ? formatPlainNumber(normalized) : '';
    clearAmountError();
    setActiveQuickAmount(normalized);
}

function selectQuickAmount(button) {
    const amount = Number(button.dataset.amount);
    document.getElementById('topupAmount').value = formatPlainNumber(amount);
    setActiveQuickAmount(amount);
    clearAmountError();
}

function handleTopupSubmit(event) {
    event.preventDefault();

    const amount = normalizeAmount(document.getElementById('topupAmount').value);
    if (!validateAmount(amount)) {
        return;
    }

    renderTransferInstruction(amount);
    appendMockTopupTransaction(amount);
    showTopupMessage('Đã tạo hướng dẫn chuyển khoản. Vui lòng chuyển đúng nội dung bên dưới.', 'success');
}

function validateAmount(amount) {
    if (!amount) {
        showAmountError('Vui lòng nhập số tiền cần nạp.');
        return false;
    }

    if (amount < TOPUP_MIN_AMOUNT) {
        showAmountError(`Số tiền nạp tối thiểu là ${formatMoney(TOPUP_MIN_AMOUNT)}.`);
        return false;
    }

    if (amount > TOPUP_MAX_AMOUNT) {
        showAmountError(`Số tiền nạp tối đa là ${formatMoney(TOPUP_MAX_AMOUNT)}.`);
        return false;
    }

    clearAmountError();
    return true;
}

function renderTransferInstruction(amount) {
    currentTransferContent = createTransferContent();
    document.getElementById('topupTransferAmount').textContent = formatMoney(amount);
    document.getElementById('topupTransferContent').textContent = currentTransferContent;
    document.getElementById('topupInstruction').hidden = false;
    document.getElementById('topupInstruction').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function createTransferContent() {
    const userPart = topupProfile?.id || topupProfile?.userId || 'USER';
    return `MMO TOPUP ${userPart} ${Date.now().toString().slice(-6)}`;
}

function appendMockTopupTransaction(amount) {
    const transactions = readMockTransactions();
    transactions.unshift({
        code: currentTransferContent.replaceAll(' ', '-'),
        type: 'TOPUP',
        amount,
        status: 'PENDING',
        createdAt: new Date().toLocaleString('vi-VN')
    });
    sessionStorage.setItem(WALLET_MOCK_TRANSACTIONS_KEY, JSON.stringify(transactions.slice(0, 20)));
}

function readMockTransactions() {
    try {
        return JSON.parse(sessionStorage.getItem(WALLET_MOCK_TRANSACTIONS_KEY)) || [];
    } catch {
        return [];
    }
}

async function copyTransferContent() {
    if (!currentTransferContent) {
        showTopupMessage('Chưa có nội dung chuyển khoản để copy.', 'warning');
        return;
    }

    try {
        await navigator.clipboard.writeText(currentTransferContent);
        showTopupMessage('Đã copy nội dung chuyển khoản.', 'success');
    } catch {
        showTopupMessage('Không thể copy tự động. Vui lòng copy thủ công nội dung chuyển khoản.', 'warning');
    }
}

function showHistoryComingSoon() {
    window.location.href = '/wallet/transactions';
}

function normalizeAmount(value) {
    return Number(String(value || '').replace(/\D/g, ''));
}

function formatPlainNumber(amount) {
    return new Intl.NumberFormat('vi-VN').format(amount);
}

function formatMoney(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
}

function setActiveQuickAmount(amount) {
    document.querySelectorAll('.topup-quick-btn').forEach(button => {
        button.classList.toggle('topup-quick-btn--active', Number(button.dataset.amount) === amount);
    });
}

function showAmountError(message) {
    document.getElementById('topupAmountError').textContent = message;
    document.getElementById('topupAmount').classList.add('ds-input-error');
}

function clearAmountError() {
    document.getElementById('topupAmountError').textContent = '';
    document.getElementById('topupAmount').classList.remove('ds-input-error');
}

function showTopupMessage(message, type) {
    const messageElement = document.getElementById('topupMessage');
    messageElement.textContent = message;
    messageElement.hidden = false;
    messageElement.classList.remove('ds-alert-info', 'ds-alert-warning', 'ds-alert-danger', 'ds-alert-success');
    messageElement.classList.add(`ds-alert-${type}`);
}
