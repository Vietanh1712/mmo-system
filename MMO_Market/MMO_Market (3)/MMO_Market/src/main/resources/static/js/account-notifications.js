const ACCOUNT_NOTIFICATIONS_MOCK_KEY = 'mmoMarketNotificationsMock';

let accountSidebar = null;
let notifications = [];
let currentPage = 1;
let pageSize = 5;
let appliedFilters = createEmptyFilters();

document.addEventListener('DOMContentLoaded', initializeNotificationsPage);

function initializeNotificationsPage() {
    accountSidebar = new AccountSidebar();
    bindNotificationEvents();
    loadNotificationsPage();
}

function bindNotificationEvents() {
    document.getElementById('notificationsFilterForm').addEventListener('submit', handleFilterSubmit);
    document.getElementById('notificationsResetButton').addEventListener('click', resetFilters);
    document.getElementById('notificationsPageSize').addEventListener('change', handlePageSizeChange);
    document.getElementById('markAllReadButton').addEventListener('click', markAllAsRead);
}

async function loadNotificationsPage() {
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
        notifications = readNotifications();
        renderSummary();
        renderNotifications();
        showNotificationsMessage('Thông báo hiện dùng dữ liệu mock frontend.', 'info');
    } catch (error) {
        showNotificationsMessage(error.message || 'Không thể tải thông báo.', 'danger');
    }
}

function readNotifications() {
    try {
        const saved = JSON.parse(sessionStorage.getItem(ACCOUNT_NOTIFICATIONS_MOCK_KEY));
        if (Array.isArray(saved) && saved.length) {
            return saved;
        }
    } catch {
        // fallback to seeded data below
    }

    const seeded = createSeedNotifications();
    saveNotifications(seeded);
    return seeded;
}

function createSeedNotifications() {
    const now = new Date();
    return [
        createNotification('NTF-001', 'ORDER', 'Đơn hàng đã hoàn tất', 'Đơn MMO-ORD-1001 đã hoàn tất. Bạn có thể xem lại chi tiết đơn hàng.', 'UNREAD', 'SUCCESS', addDays(now, -1), '/account/orders/MMO-ORD-1001'),
        createNotification('NTF-002', 'WALLET', 'Nạp tiền đang xử lý', 'Yêu cầu nạp tiền của bạn đang chờ xác nhận thanh toán.', 'UNREAD', 'WARNING', addDays(now, -1), '/wallet/transactions'),
        createNotification('NTF-003', 'KYC', 'Hồ sơ KYC cần bổ sung', 'Vui lòng kiểm tra lại thông tin giấy tờ định danh trước khi gửi lại.', 'READ', 'WARNING', addDays(now, -2), '/account/kyc'),
        createNotification('NTF-004', 'SECURITY', 'Đổi mật khẩu thành công', 'Tài khoản của bạn vừa cập nhật mật khẩu đăng nhập.', 'READ', 'SUCCESS', addDays(now, -3), '/account/security'),
        createNotification('NTF-005', 'ORDER', 'Đơn hàng đang tranh chấp', 'Đơn MMO-ORD-1004 đang ở trạng thái tranh chấp, vui lòng theo dõi phản hồi.', 'UNREAD', 'DANGER', addDays(now, -4), '/account/orders/MMO-ORD-1004'),
        createNotification('NTF-006', 'SYSTEM', 'Chào mừng đến MMO Market', 'Hãy hoàn tất hồ sơ cá nhân để sử dụng tài khoản hiệu quả hơn.', 'READ', 'INFO', addDays(now, -5), '/profile'),
        createNotification('NTF-007', 'COMPLAINT', 'Khiếu nại có phản hồi mới', 'Staff đã cập nhật phản hồi cho yêu cầu hỗ trợ của bạn.', 'READ', 'INFO', addDays(now, -6), '#'),
        createNotification('NTF-008', 'WALLET', 'Hoàn tiền thành công', 'Một giao dịch hoàn tiền đã được ghi nhận vào ví của bạn.', 'READ', 'SUCCESS', addDays(now, -7), '/wallet/transactions')
    ];
}

function createNotification(id, type, title, message, status, severity, createdDate, targetUrl) {
    return {
        id,
        type,
        title,
        message,
        status,
        severity,
        createdAt: formatDateTime(createdDate),
        targetUrl
    };
}

function saveNotifications(nextNotifications) {
    sessionStorage.setItem(ACCOUNT_NOTIFICATIONS_MOCK_KEY, JSON.stringify(nextNotifications));
}

function renderSummary() {
    const summary = notifications.reduce((result, item) => {
        result.total += 1;
        if (item.status === 'UNREAD') result.unread += 1;
        if (item.type === 'WALLET') result.wallet += 1;
        if (item.type === 'ORDER') result.order += 1;
        return result;
    }, { total: 0, unread: 0, wallet: 0, order: 0 });

    document.getElementById('notificationsTotalCount').textContent = summary.total;
    document.getElementById('notificationsUnreadCount').textContent = summary.unread;
    document.getElementById('notificationsWalletCount').textContent = summary.wallet;
    document.getElementById('notificationsOrderCount').textContent = summary.order;
}

function renderNotifications() {
    const filtered = getFilteredNotifications();
    const list = document.getElementById('notificationsList');
    const emptyState = document.getElementById('notificationsEmptyState');
    const pagination = document.getElementById('notificationsPagination');
    const summary = document.getElementById('notificationsResultSummary');
    const totalPages = Math.max(Math.ceil(filtered.length / pageSize), 1);

    currentPage = Math.min(currentPage, totalPages);
    const startIndex = (currentPage - 1) * pageSize;
    const pagedNotifications = filtered.slice(startIndex, startIndex + pageSize);

    if (!filtered.length) {
        list.innerHTML = '';
        emptyState.hidden = false;
        pagination.hidden = true;
        summary.textContent = `Hiển thị 0/${notifications.length} thông báo.`;
        return;
    }

    emptyState.hidden = true;
    pagination.hidden = false;
    summary.textContent = `Hiển thị ${startIndex + 1}-${startIndex + pagedNotifications.length}/${filtered.length} thông báo.`;
    list.innerHTML = pagedNotifications.map(notification => `
        <article class="notification-item notification-item--${notification.severity.toLowerCase()} ${notification.status === 'UNREAD' ? 'notification-item--unread' : ''}">
            <span class="notification-item__icon"><i class="${getNotificationIcon(notification.type)}" aria-hidden="true"></i></span>
            <div class="notification-item__content">
                <div class="notification-item__header">
                    <strong class="notification-item__title">${escapeHtml(notification.title)}</strong>
                    <span class="ds-badge ${getTypeBadgeClass(notification.type)}">${formatNotificationType(notification.type)}</span>
                    <span class="ds-badge ${notification.status === 'UNREAD' ? 'ds-badge-info' : 'ds-badge-muted'}">${formatReadStatus(notification.status)}</span>
                </div>
                <p class="notification-item__message">${escapeHtml(notification.message)}</p>
                <span class="notification-item__time">${escapeHtml(notification.createdAt)}</span>
            </div>
            <div class="notification-item__actions">
                <button class="ds-btn ds-btn-outline" type="button" data-notification-id="${escapeHtml(notification.id)}">Xem chi tiết</button>
            </div>
        </article>
    `).join('');
    bindNotificationItemActions();
    renderPagination(filtered.length, totalPages);
}

function bindNotificationItemActions() {
    document.querySelectorAll('[data-notification-id]').forEach(button => {
        button.addEventListener('click', () => openNotification(button.dataset.notificationId));
    });
}

function openNotification(notificationId) {
    const notification = notifications.find(item => item.id === notificationId);
    if (!notification) return;

    notification.status = 'READ';
    saveNotifications(notifications);
    renderSummary();

    if (notification.targetUrl && notification.targetUrl !== '#') {
        window.location.href = notification.targetUrl;
        return;
    }

    renderNotifications();
    showNotificationsMessage('Thông báo này chưa có màn chi tiết riêng.', 'warning');
}

function markAllAsRead() {
    notifications = notifications.map(item => ({ ...item, status: 'READ' }));
    saveNotifications(notifications);
    renderSummary();
    renderNotifications();
    showNotificationsMessage('Đã đánh dấu tất cả thông báo là đã đọc.', 'success');
}

function getFilteredNotifications() {
    const keyword = appliedFilters.keyword;
    const type = appliedFilters.type;
    const status = appliedFilters.status;
    const fromDate = parseIsoDate(appliedFilters.fromDate);
    const toDate = parseIsoDate(appliedFilters.toDate);

    return notifications.filter(notification => {
        const text = `${notification.title || ''} ${notification.message || ''}`.toLowerCase();
        const notificationDate = parseVietnameseDateTime(notification.createdAt);

        if (keyword && !text.includes(keyword)) return false;
        if (type && notification.type !== type) return false;
        if (status && notification.status !== status) return false;
        if (fromDate && notificationDate && notificationDate < fromDate) return false;
        if (toDate && notificationDate && notificationDate > endOfDay(toDate)) return false;
        return true;
    });
}

function handleFilterSubmit(event) {
    event.preventDefault();
    appliedFilters = readCurrentFilters();
    currentPage = 1;
    renderNotifications();
}

function readCurrentFilters() {
    return {
        keyword: document.getElementById('notificationsSearchInput').value.trim().toLowerCase(),
        type: document.getElementById('notificationsTypeFilter').value,
        status: document.getElementById('notificationsStatusFilter').value,
        fromDate: document.getElementById('notificationsFromDate').value,
        toDate: document.getElementById('notificationsToDate').value
    };
}

function resetFilters() {
    document.getElementById('notificationsSearchInput').value = '';
    document.getElementById('notificationsTypeFilter').value = '';
    document.getElementById('notificationsStatusFilter').value = '';
    clearDatePicker('notificationsFromDate', 'notificationsFromDateDisplay');
    clearDatePicker('notificationsToDate', 'notificationsToDateDisplay');
    appliedFilters = createEmptyFilters();
    currentPage = 1;
    renderNotifications();
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
    renderNotifications();
}

function renderPagination(totalItems, totalPages) {
    const pages = document.getElementById('notificationsPaginationPages');
    const info = document.getElementById('notificationsPaginationInfo');

    info.textContent = `Tổng số: ${totalItems} thông báo`;
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
            renderNotifications();
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

function getNotificationIcon(type) {
    const map = {
        SYSTEM: 'fa fa-info-circle',
        ORDER: 'fa fa-shopping-bag',
        WALLET: 'fa fa-credit-card',
        KYC: 'fa fa-id-card-o',
        SECURITY: 'fa fa-shield',
        COMPLAINT: 'fa fa-flag'
    };
    return map[type] || 'fa fa-bell-o';
}

function formatNotificationType(type) {
    const map = {
        SYSTEM: 'Hệ thống',
        ORDER: 'Đơn hàng',
        WALLET: 'Ví',
        KYC: 'KYC',
        SECURITY: 'Bảo mật',
        COMPLAINT: 'Khiếu nại'
    };
    return map[type] || type || '-';
}

function getTypeBadgeClass(type) {
    if (type === 'ORDER' || type === 'WALLET') return 'ds-badge-info';
    if (type === 'KYC' || type === 'SECURITY') return 'ds-badge-warning';
    if (type === 'COMPLAINT') return 'ds-badge-danger';
    return 'ds-badge-muted';
}

function formatReadStatus(status) {
    return status === 'UNREAD' ? 'Chưa đọc' : 'Đã đọc';
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

function showNotificationsMessage(message, type) {
    const messageElement = document.getElementById('notificationsMessage');
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
