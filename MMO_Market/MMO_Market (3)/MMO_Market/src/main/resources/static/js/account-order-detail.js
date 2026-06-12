const ACCOUNT_ORDERS_MOCK_KEY = 'mmoMarketMyOrdersMock';

let accountSidebar = null;
let currentOrder = null;

document.addEventListener('DOMContentLoaded', initializeOrderDetailPage);

function initializeOrderDetailPage() {
    accountSidebar = new AccountSidebar();
    bindOrderDetailEvents();
    loadOrderDetailPage();
}

function bindOrderDetailEvents() {
    document.getElementById('orderViewProductButton').addEventListener('click', () => {
        showOrderDetailMessage('Màn chi tiết sản phẩm sẽ được triển khai ở luồng mua sắm.', 'warning');
    });

    document.getElementById('orderComplaintButton').addEventListener('click', () => {
        showOrderDetailMessage('Luồng khiếu nại sẽ được triển khai ở bước Complaint.', 'warning');
    });
}

async function loadOrderDetailPage() {
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

        const orderCode = getOrderCodeFromPath();
        currentOrder = readOrders().find(order => order.orderCode === orderCode);

        if (!currentOrder) {
            showNotFound(orderCode);
            return;
        }

        renderOrderDetail(currentOrder);
        showOrderDetailMessage('Chi tiết đơn hàng hiện dùng dữ liệu mock frontend.', 'info');
    } catch (error) {
        showOrderDetailMessage(error.message || 'Không thể tải chi tiết đơn hàng.', 'danger');
    }
}

function getOrderCodeFromPath() {
    const parts = window.location.pathname.split('/').filter(Boolean);
    return decodeURIComponent(parts[parts.length - 1] || '');
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
        const saved = sessionStorage.getItem(key);
        if (saved !== null) {
            return JSON.parse(saved);
        }
    } catch {
        // fallback below
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
    sessionStorage.setItem(key, JSON.stringify(seeded));
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

function renderOrderDetail(order) {
    document.getElementById('orderDetailOverview').hidden = false;
    document.getElementById('orderDetailContent').hidden = false;
    document.getElementById('orderTimeline').hidden = false;
    document.getElementById('orderDetailEmpty').hidden = true;

    document.getElementById('orderDetailCode').textContent = order.orderCode;
    document.getElementById('orderProductName').textContent = order.productName;
    document.getElementById('orderSellerName').textContent = `Người bán: ${order.sellerName}`;
    setBadge('orderStatusBadge', formatOrderStatus(order.status), getOrderStatusBadgeClass(order.status));
    setBadge('orderPaymentBadge', formatPaymentStatus(order.paymentStatus), getPaymentBadgeClass(order.paymentStatus));

    document.getElementById('orderCodeValue').textContent = order.orderCode;
    document.getElementById('orderCreatedAt').textContent = order.createdAt;
    document.getElementById('orderAmount').textContent = formatMoney(order.amount);
    document.getElementById('orderEscrowRelease').textContent = order.escrowReleaseDate;
    document.getElementById('orderProductTitle').textContent = order.productName;
    document.getElementById('orderAccessInfo').innerHTML = createAccessInfo(order);
    document.getElementById('orderTransactionCode').textContent = `TX-${order.orderCode.replace('MMO-ORD-', '')}`;
    document.getElementById('orderPaymentText').textContent = formatPaymentStatus(order.paymentStatus);
    document.getElementById('orderPaymentAmount').textContent = formatMoney(order.amount);
    document.getElementById('orderActionHint').textContent = getActionHint(order);

    const feedbackBtn = document.getElementById('orderFeedbackButton');
    if (feedbackBtn) {
        if (order.status === 'COMPLETED') {
            feedbackBtn.style.display = 'inline-flex';
            feedbackBtn.href = `/account/orders/${order.orderCode}/feedback`;
        } else {
            feedbackBtn.style.display = 'none';
        }
    }

    renderTimeline(order);
}

function showNotFound(orderCode) {
    document.getElementById('orderDetailCode').textContent = orderCode || 'Không xác định';
    document.getElementById('orderDetailOverview').hidden = true;
    document.getElementById('orderDetailContent').hidden = true;
    document.getElementById('orderTimeline').hidden = true;
    document.getElementById('orderDetailEmpty').hidden = false;
    showOrderDetailMessage('Không tìm thấy đơn hàng trong dữ liệu mock frontend.', 'warning');
}

function renderTimeline(order) {
    const activeSteps = getActiveSteps(order);
    document.querySelectorAll('[data-order-step]').forEach(step => {
        step.classList.toggle('order-timeline-item--active', activeSteps.includes(step.dataset.orderStep));
    });
}

function getActiveSteps(order) {
    const steps = ['created'];
    if (['PAID', 'DELIVERED', 'COMPLETED', 'DISPUTED', 'REFUNDED'].includes(order.status) || order.paymentStatus === 'PAID') {
        steps.push('paid');
    }
    if (['DELIVERED', 'COMPLETED', 'DISPUTED', 'REFUNDED'].includes(order.status)) {
        steps.push('delivered');
    }
    if (['COMPLETED', 'DISPUTED', 'REFUNDED', 'CANCELLED'].includes(order.status)) {
        steps.push('completed');
    }
    return steps;
}

function createAccessInfo(order) {
    if (order.status === 'PENDING') return 'Đơn hàng đang chờ xử lý, thông tin nhận hàng chưa sẵn sàng.';
    if (order.status === 'CANCELLED') return 'Đơn hàng đã hủy, không có thông tin nhận hàng.';
    if (order.status === 'DISPUTED') return 'Thông tin nhận hàng đang được giữ để xử lý tranh chấp.';
    
    // Check if credentials exist in the order object
    let creds = order.credentials;
    
    // If not in order object but name indicates account, auto-generate mock credentials dynamically so that all existing account orders show them!
    if (!creds) {
        const lowerName = order.productName.toLowerCase();
        const isAccount = lowerName.includes('tài khoản') || 
                          lowerName.includes('premium') || 
                          lowerName.includes('spotify') || 
                          lowerName.includes('netflix') || 
                          lowerName.includes('canva') || 
                          lowerName.includes('chatgpt') || 
                          lowerName.includes('gmail') || 
                          lowerName.includes('vpn') || 
                          lowerName.includes('key');
        if (isAccount) {
            // Generate deterministic mock credentials based on orderCode
            const hash = order.orderCode.replace(/[^0-9]/g, '') || '1234';
            const cleanName = order.productName.replace(/[^a-zA-Z0-9]/g, '').substring(0, 8).toLowerCase();
            if (lowerName.includes('key')) {
                creds = {
                    username: `KEY-${hash}-ABCD-EFGH-IJKL`,
                    password: '(Product Key)'
                };
            } else {
                creds = {
                    username: `${cleanName}_${hash}@gmail.com`,
                    password: `Pass_${hash}_Secure`
                };
            }
        }
    }
    
    if (creds) {
        const isKeyOnly = creds.password === '(Product Key)';
        return `
            <div class="credentials-card" style="margin-top: 12px; padding: 16px; background: rgba(37, 99, 235, 0.04); border: 1.5px dashed rgba(37, 99, 235, 0.2); border-radius: 8px;">
                <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px;">
                    <span style="font-size: 13.5px; color: var(--ds-text-muted, #64748b); font-weight: 500;">
                        ${isKeyOnly ? 'Mã kích hoạt (Key):' : 'Tài khoản (Email/Username):'}
                    </span>
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <strong id="credUsername" style="font-family: monospace; font-size: 14.5px; color: var(--ds-text-primary, #1e293b);">${creds.username}</strong>
                        <button class="ds-btn ds-btn-outline ds-btn-sm" style="padding: 4px 8px; font-size: 12px;" onclick="copyToClipboard('${creds.username}', '${isKeyOnly ? 'Mã kích hoạt' : 'Tài khoản'}')">
                            <i class="fa fa-copy" aria-hidden="true"></i> Copy
                        </button>
                    </div>
                </div>
                ${isKeyOnly ? '' : `
                <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px;">
                    <span style="font-size: 13.5px; color: var(--ds-text-muted, #64748b); font-weight: 500;">Mật khẩu:</span>
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <strong id="credPassword" style="font-family: monospace; font-size: 14.5px; color: var(--ds-text-primary, #1e293b);">${creds.password}</strong>
                        <button class="ds-btn ds-btn-outline ds-btn-sm" style="padding: 4px 8px; font-size: 12px;" onclick="copyToClipboard('${creds.password}', 'Mật khẩu')">
                            <i class="fa fa-copy" aria-hidden="true"></i> Copy
                        </button>
                    </div>
                </div>
                `}
                <div style="margin-top: 12px; font-size: 12px; color: #b45309; background-color: #fffbeb; padding: 8px 12px; border-radius: 6px; border: 1px solid rgba(217, 119, 6, 0.15);">
                    <i class="fa fa-exclamation-triangle" aria-hidden="true"></i> Vui lòng không thay đổi mật khẩu hoặc thông tin bảo mật để tránh ảnh hưởng đến thời gian bảo hành.
                </div>
            </div>
        `;
    }
    
    return 'Thông tin nhận hàng sẽ được hiển thị tại đây khi sản phẩm được giao thành công.';
}

function getActionHint(order) {
    if (order.status === 'COMPLETED') return 'Đơn hàng đã hoàn tất. Bạn có thể xem lại thông tin mua hàng.';
    if (order.status === 'DISPUTED') return 'Đơn hàng đang trong trạng thái tranh chấp.';
    if (order.status === 'CANCELLED') return 'Đơn hàng đã hủy, không còn thao tác xử lý.';
    return 'Bạn có thể theo dõi đơn hoặc gửi khiếu nại khi cần.';
}

function setBadge(elementId, text, badgeClass) {
    const element = document.getElementById(elementId);
    element.textContent = text;
    element.className = `ds-badge ${badgeClass}`;
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

function showOrderDetailMessage(message, type) {
    const messageElement = document.getElementById('orderDetailMessage');
    messageElement.textContent = message;
    messageElement.hidden = false;
    messageElement.classList.remove('ds-alert-info', 'ds-alert-warning', 'ds-alert-danger', 'ds-alert-success');
    messageElement.classList.add(`ds-alert-${type}`);
}

window.copyToClipboard = async function(text, label) {
    try {
        await navigator.clipboard.writeText(text);
        showOrderDetailMessage(`Đã copy ${label} vào bộ nhớ tạm thành công.`, 'success');
    } catch {
        showOrderDetailMessage('Không thể copy tự động. Vui lòng chọn và sao chép thủ công.', 'warning');
    }
};
