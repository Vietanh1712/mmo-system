(function () {
const ACCOUNT_NOTIFICATIONS_MOCK_KEY = 'mmoMarketNotificationsMock';

let accountSidebar = null;
let notifications = [];
let currentPage = 1;
let pageSize = 5;
let appliedFilters = createEmptyFilters();

registerAccountPage('/js/customer/account-notifications.js', initializeNotificationsPage);

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
        
        await fetchAndRenderNotifications();
    } catch (error) {
        showNotificationsMessage(error.message || 'Không thể tải thông báo.', 'danger');
    }
}

async function fetchAndRenderNotifications() {
    try {
        const response = await authFetch('/v1/notifications');
        if (!response.ok) {
            throw new Error('Không thể tải thông báo từ hệ thống.');
        }

        const data = await response.json();
        const key = getLocalNotifStorageKey();
        const readTimestamps = JSON.parse(localStorage.getItem(key) || '[]');
        
        notifications = data.map(item => {
            if (item.isBroadcast) {
                const isUnread = !readTimestamps.includes(item.originalTimestamp);
                item.status = isUnread ? 'UNREAD' : 'READ';
            }
            return item;
        });

        renderSummary();
        renderNotifications();
    } catch (error) {
        showNotificationsMessage(error.message || 'Lỗi khi đồng bộ danh sách thông báo.', 'danger');
    }
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

async function openNotification(notificationId) {
    const notification = notifications.find(item => item.id === notificationId);
    if (!notification) return;

    if (notification.isBroadcast) {
        const key = getLocalNotifStorageKey();
        const readTimestamps = JSON.parse(localStorage.getItem(key) || '[]');
        if (!readTimestamps.includes(notification.originalTimestamp)) {
            readTimestamps.push(notification.originalTimestamp);
            localStorage.setItem(key, JSON.stringify(readTimestamps));
        }
    } else {
        try {
            const response = await authFetch(`/v1/notifications/${notificationId}/read`, {
                method: 'POST'
            });
            if (!response.ok) {
                console.error('Không thể đánh dấu đã đọc thông báo này trên hệ thống.');
            }
        } catch (e) {
            console.error('Lỗi khi gọi API đánh dấu đã đọc:', e);
        }
    }

    notification.status = 'READ';
    
    if (typeof window.refreshHeaderNotifBadge === 'function') {
        window.refreshHeaderNotifBadge();
    }
    
    renderSummary();
    showNotifDetailModal(notification);
    renderNotifications();
    showNotificationsMessage('Đã đọc thông báo chi tiết.', 'success');
}

function showNotifDetailModal(notification) {
    const modal = document.getElementById('notifDetailModal');
    if (!modal) return;

    // Title & Message
    document.getElementById('modalNotifTitle').textContent = notification.title;
    document.getElementById('modalNotifContent').textContent = notification.message;
    
    // Author
    const authorName = notification.isBroadcast ? 'Ban Quản Trị' : 'Hệ thống tự động';
    document.getElementById('modalNotifAuthor').innerHTML = `<i class="fa fa-user-circle-o" aria-hidden="true" style="margin-right: 4px;"></i>${authorName}`;
    
    // Date
    document.getElementById('modalNotifDate').innerHTML = `<i class="fa fa-calendar-o" aria-hidden="true" style="margin-right: 4px;"></i>${notification.createdAt}`;

    // Type Badge
    const badge = document.getElementById('modalNotifBadge');
    if (badge) {
        badge.className = 'ds-badge ' + getTypeBadgeClass(notification.type);
        badge.textContent = formatNotificationType(notification.type);
    }

    // Status Badge
    const statusBadge = document.getElementById('modalNotifStatusBadge');
    if (statusBadge) {
        if (notification.status === 'UNREAD') {
            statusBadge.className = 'ds-badge ds-badge-info';
            statusBadge.innerHTML = `<i class="fa fa-bell-o" aria-hidden="true" style="margin-right: 4px;"></i>Chưa đọc`;
        } else {
            statusBadge.className = 'ds-badge ds-badge-success';
            statusBadge.innerHTML = `<i class="fa fa-check-circle" aria-hidden="true" style="margin-right: 4px;"></i>Đã đọc`;
        }
    }

    // Type-specific background styling
    const contentContainer = document.getElementById('modalNotifContentContainer');
    if (contentContainer) {
        const notifType = (notification.type || '').toLowerCase();
        contentContainer.className = `modal-detail-container modal-detail-container--${notifType}`;
    }

    // Target URL Link CTA Button
    const linkBtn = document.getElementById('modalNotifLinkBtn');
    if (linkBtn) {
        if (notification.targetUrl && notification.targetUrl !== '#' && notification.targetUrl !== '/account/notifications') {
            linkBtn.href = notification.targetUrl;
            linkBtn.style.display = 'inline-flex';
        } else {
            linkBtn.style.display = 'none';
        }
    }

    modal.style.display = 'grid';
}

window.closeNotifModal = function() {
    const modal = document.getElementById('notifDetailModal');
    if (modal) modal.style.display = 'none';
};

async function markAllAsRead() {
    try {
        const response = await authFetch('/v1/notifications/mark-all-read', {
            method: 'POST'
        });
        if (!response.ok) {
            throw new Error('Không thể đánh dấu tất cả đã đọc trên hệ thống.');
        }
    } catch (error) {
        showNotificationsMessage(error.message || 'Lỗi khi đánh dấu tất cả đã đọc.', 'danger');
        return;
    }

    const key = getLocalNotifStorageKey();
    const readTimestamps = JSON.parse(localStorage.getItem(key) || '[]');
    notifications = notifications.map(item => {
        if (item.isBroadcast) {
            if (!readTimestamps.includes(item.originalTimestamp)) {
                readTimestamps.push(item.originalTimestamp);
            }
        }
        return { ...item, status: 'READ' };
    });
    localStorage.setItem(key, JSON.stringify(readTimestamps));

    if (typeof window.refreshHeaderNotifBadge === 'function') {
        window.refreshHeaderNotifBadge();
    }

    renderSummary();
    renderNotifications();
    showNotificationsMessage('Đã đánh dấu tất cả thông báo là đã đọc.', 'success');
}

function getLocalNotifStorageKey() {
    if (typeof window.getNotifStorageKey === 'function') {
        return window.getNotifStorageKey();
    }
    try {
        const userString = sessionStorage.getItem("userInfo") || sessionStorage.getItem("user") || localStorage.getItem("userInfo") || localStorage.getItem("user");
        if (userString && userString !== "null" && userString !== "undefined") {
            const user = JSON.parse(userString);
            if (user && user.id) {
                return `mmoReadNotifs_${user.id}`;
            }
        }
    } catch (error) {
        console.error("Error determining notif key:", error);
    }
    return 'mmoReadNotifs_guest';
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
    return (status || '').toUpperCase().trim() === 'UNREAD' ? 'Chưa đọc' : 'Đã đọc';
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
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())} ${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
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

function registerAccountPage(scriptPath, initializer) {
    window.AccountPageInitializers = window.AccountPageInitializers || {};
    window.AccountPageInitializers[scriptPath] = initializer;
    if (document.currentScript?.dataset.accountPartial !== 'true') {
        document.addEventListener('DOMContentLoaded', initializer);
    }
}
})();
