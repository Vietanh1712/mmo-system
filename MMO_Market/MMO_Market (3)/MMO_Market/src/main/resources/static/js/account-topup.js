const TOPUP_MIN_AMOUNT = 10000;
const TOPUP_MAX_AMOUNT = 50000000;
const WALLET_MOCK_TRANSACTIONS_KEY = 'mmoMarketWalletTransactionsMock';

let topupProfile = null;
let accountSidebar = null;
let currentTransferContent = '';
let sepayConfig = {
    bankId: 'TPB',
    accountNumber: '00000806194',
    accountName: 'NGUYEN THI NGOC LINH'
};
let pollingInterval = null;
let initialBalance = 0;

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
        initialBalance = topupProfile.balanceVnd || 0;
        accountSidebar.render(topupProfile);
        document.getElementById('topupBalance').textContent = formatMoney(initialBalance);

        try {
            const configResponse = await fetch('/api/sepay/config');
            if (configResponse.ok) {
                const configData = await configResponse.json();
                sepayConfig.bankId = configData.bankId || 'TPB';
                sepayConfig.accountNumber = configData.accountNumber || '00000806194';
                sepayConfig.accountName = configData.accountName || 'NGUYEN THI NGOC LINH';
            }
        } catch (err) {
            console.warn('Failed to load SePay config from backend, using defaults', err);
        }

        // Parse query params
        const urlParams = new URLSearchParams(window.location.search);
        const amountParam = urlParams.get('amount');
        const redirectParam = urlParams.get('redirect');

        if (amountParam) {
            const parsed = Number(amountParam);
            if (!isNaN(parsed) && parsed > 0) {
                document.getElementById('topupAmount').value = formatPlainNumber(parsed);
                setActiveQuickAmount(parsed);
            }
        }

        if (redirectParam) {
            const backLink = document.getElementById('topupBackLink');
            if (backLink) {
                backLink.href = redirectParam;
                backLink.innerHTML = `<i class="fa fa-arrow-left" aria-hidden="true"></i> Quay lại thanh toán`;
            }
            const instrBackLink = document.getElementById('topupInstructionBackLink');
            if (instrBackLink) {
                instrBackLink.href = redirectParam;
                instrBackLink.innerHTML = `<i class="fa fa-credit-card" aria-hidden="true"></i> Quay lại thanh toán`;
            }
        }

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

    initialBalance = topupProfile ? (topupProfile.balanceVnd || 0) : 0;

    renderTransferInstruction(amount);
    showTopupMessage('Hướng dẫn chuyển khoản đã được tạo. Vui lòng quét mã QR hoặc chuyển khoản để hoàn tất giao dịch.', 'warning');

    startBalancePolling(amount);
}

function startBalancePolling(topupAmount) {
    if (pollingInterval) {
        clearInterval(pollingInterval);
    }

    console.log('[Topup Polling] Bắt đầu kiểm tra số dư tự động...');

    pollingInterval = setInterval(async () => {
        try {
            const response = await authFetch('/v1/profile');
            if (response.ok) {
                const profile = await response.json();
                const currentBalance = profile.balanceVnd || 0;

                if (currentBalance > initialBalance) {
                    clearInterval(pollingInterval);
                    pollingInterval = null;

                    const addedAmount = currentBalance - initialBalance;
                    
                    topupProfile.balanceVnd = currentBalance;
                    document.getElementById('topupBalance').textContent = formatMoney(currentBalance);

                    const userStr = sessionStorage.getItem('userInfo') || sessionStorage.getItem('user');
                    if (userStr) {
                        try {
                            const user = JSON.parse(userStr);
                            user.balanceVnd = currentBalance;
                            sessionStorage.setItem('userInfo', JSON.stringify(user));
                            sessionStorage.setItem('user', JSON.stringify(user));
                            localStorage.setItem('userInfo', JSON.stringify(user));
                            localStorage.setItem('user', JSON.stringify(user));
                        } catch (e) {
                            console.error('Error saving balance:', e);
                        }
                    }

                    const badge = document.querySelector('.topup-instruction .ds-badge');
                    if (badge) {
                        badge.textContent = 'Thành công';
                        badge.className = 'ds-badge ds-badge-success';
                    }

                    appendMockTopupTransaction(addedAmount);

                    showTopupMessage(`Nạp tiền thành công! Bạn đã được cộng +${formatMoney(addedAmount)} vào tài khoản.`, 'success');
                }
            }
        } catch (err) {
            console.error('Error polling balance:', err);
        }
    }, 5000);
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
    
    document.getElementById('topupBankName').textContent = sepayConfig.bankId;
    document.getElementById('topupBankAccountName').textContent = sepayConfig.accountName;
    document.getElementById('topupBankAccount').textContent = sepayConfig.accountNumber;
    
    document.getElementById('topupTransferAmount').textContent = formatMoney(amount);
    document.getElementById('topupTransferContent').textContent = currentTransferContent;
    
    const qrUrl = `https://img.vietqr.io/image/${sepayConfig.bankId}-${sepayConfig.accountNumber}-compact2.png?amount=${amount}&addInfo=${encodeURIComponent(currentTransferContent)}&accountName=${encodeURIComponent(sepayConfig.accountName)}`;
    document.getElementById('topupQrCode').src = qrUrl;
    
    const badge = document.querySelector('.topup-instruction .ds-badge');
    if (badge) {
        badge.textContent = 'Đang chờ thanh toán';
        badge.className = 'ds-badge ds-badge-warning';
    }

    document.getElementById('topupInstruction').hidden = false;
    document.getElementById('topupInstruction').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function createTransferContent() {
    const userPart = topupProfile?.id || topupProfile?.userId || 'USER';
    return `MMO-TOPUP-${userPart}`;
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

function addDays(date, days) {
    const next = new Date(date);
    next.setDate(next.getDate() + days);
    return next;
}

function formatDateTime(date) {
    return date.toLocaleString('vi-VN');
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

function appendMockTopupTransaction(amount) {
    const key = getUserSpecificKey(WALLET_MOCK_TRANSACTIONS_KEY);
    const transactions = readMockTransactions();
    transactions.unshift({
        code: currentTransferContent.replaceAll(' ', '-'),
        type: 'TOPUP',
        amount,
        status: 'SUCCESS',
        createdAt: new Date().toLocaleString('vi-VN')
    });
    sessionStorage.setItem(key, JSON.stringify(transactions.slice(0, 20)));
}

function readMockTransactions() {
    const key = getUserSpecificKey(WALLET_MOCK_TRANSACTIONS_KEY);
    try {
        const saved = sessionStorage.getItem(key);
        if (saved !== null) {
            return JSON.parse(saved);
        }
    } catch {
        // fallback
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
