let ticketsList = [];
let currentPage = 0;
let pageSize = 10;

async function loadTickets() {
    try {
        const res = await authFetch('/support-tickets/all');
        if (!res.ok) {
            throw new Error('Không thể tải danh sách phiếu hỗ trợ');
        }
        ticketsList = await res.json();
        updateStats();
        currentPage = 0;
        renderTickets();
    } catch (e) {
        console.error(e);
        document.getElementById('staff-tickets-body').innerHTML = `
            <tr>
                <td colspan="8" style="text-align: center; padding: 24px; color: red;">
                    Lỗi khi tải danh sách phiếu hỗ trợ từ máy chủ. Vui lòng kiểm tra đăng nhập.
                </td>
            </tr>
        `;
    }
}

function updateStats() {
    document.getElementById('stat-total').textContent = ticketsList.length;
    document.getElementById('stat-new').textContent = ticketsList.filter(t => t.status === 'Open').length;
    document.getElementById('stat-processing').textContent = ticketsList.filter(t => t.status === 'Processing').length;
    document.getElementById('stat-resolved').textContent = ticketsList.filter(t => t.status === 'Resolved').length;
}

function renderTickets() {
    const search = document.getElementById('staff-search-input').value.trim().toLowerCase();
    const status = document.getElementById('staff-status-select').value;
    const category = document.getElementById('staff-category-select').value;

    const filtered = ticketsList.filter(item => {
        const matchesSearch = !search || 
            String(item.id).includes(search) || 
            item.title.toLowerCase().includes(search) || 
            (item.user && item.user.email.toLowerCase().includes(search)) ||
            (item.user && item.user.fullName.toLowerCase().includes(search));
        const matchesStatus = !status || item.status === status;
        const matchesCategory = !category || item.category === category;
        return matchesSearch && matchesStatus && matchesCategory;
    });

    filtered.sort((a, b) => a.id - b.id);

    const tbody = document.getElementById('staff-tickets-body');
    if (!tbody) return;

    if (filtered.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align: center; padding: 24px; color: var(--ds-text-subtle);">
                    Không tìm thấy phiếu hỗ trợ nào phù hợp với bộ lọc.
                </td>
            </tr>
        `;
        renderPagination(0, 0);
        return;
    }

    const totalPages = Math.ceil(filtered.length / pageSize);
    if (currentPage >= totalPages) {
        currentPage = 0;
    }

    const pageItems = filtered.slice(currentPage * pageSize, (currentPage + 1) * pageSize);

    tbody.innerHTML = pageItems.map((item, index) => {
        const stt = currentPage * pageSize + index + 1;
        let badgeClass = 'ds-badge-warning';
        let statusText = 'Đang xử lý';
        if (item.status === 'Open') {
            badgeClass = 'ds-badge-info';
            statusText = 'Mới';
        } else if (item.status === 'Resolved') {
            badgeClass = 'ds-badge-success';
            statusText = 'Đã giải quyết';
        } else if (item.status === 'Processing') {
            badgeClass = 'ds-badge-warning';
            statusText = 'Đang xử lý';
        }

        const userName = item.user ? item.user.fullName : 'Guest';
        const userEmail = item.user ? item.user.email : 'guest@mmo.com';
        const nameInitials = userName ? userName.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() : 'US';
        const formattedDate = item.createdAt ? item.createdAt.substring(0, 16).replace('T', ' ') : 'N/A';

        return `
            <tr>
                <td class="ds-table-center">${stt}</td>
                <td>#ST-${escapeHtml(item.id)}</td>
                <td>
                    <div class="ds-entity">
                        <span class="ds-avatar ds-avatar-sm ds-avatar-primary">${escapeHtml(nameInitials)}</span>
                        <div>
                            <div class="ds-entity-title">${escapeHtml(userName)}</div>
                            <div class="ds-entity-subtitle">${escapeHtml(userEmail)}</div>
                        </div>
                    </div>
                </td>
                <td>${escapeHtml(item.category)}</td>
                <td style="max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500;">
                    ${escapeHtml(item.title)}
                </td>
                <td class="ds-table-center"><span class="ds-badge ${badgeClass}">${statusText}</span></td>
                <td>${formattedDate}</td>
                <td class="ds-table-center">
                    <div class="ds-table-actions">
                        <button class="ds-icon-btn ds-icon-btn-view" onclick="viewTicketDetail('${item.id}')" aria-label="Xem chi tiết">
                            <svg class="ds-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true" style="width:16px; height:16px;">
                                <path d="M2.25 12C3.73 8.12 7.49 5.25 12 5.25C16.51 5.25 20.27 8.12 21.75 12C20.27 15.88 16.51 18.75 12 18.75C7.49 18.75 3.73 15.88 2.25 12Z" stroke="currentColor" stroke-width="2"/>
                                <path d="M12 15.25C13.79 15.25 15.25 13.79 15.25 12C15.25 10.21 13.79 8.75 12 8.75C10.21 8.75 8.75 10.21 8.75 12C8.75 13.79 10.21 15.25 12 15.25Z" stroke="currentColor" stroke-width="2"/>
                            </svg>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');

    renderPagination(filtered.length, totalPages);
}

function renderPagination(totalElements, totalPages) {
    mountStaffPagination('ticketPagination', {
        page: currentPage,
        totalPages: totalPages,
        totalElements: totalElements,
        pageSize: pageSize
    }, {
        onPage: (p) => { currentPage = p; renderTickets(); },
        onSize: (s) => { pageSize = s; currentPage = 0; renderTickets(); }
    });
}

function viewTicketDetail(id) {
    sessionStorage.setItem('selectedSupportTicketId', id);
    window.location.href = '/staff/support-tickets/detail';
}

function applyStaffFilters() {
    currentPage = 0;
    renderTickets();
}

function resetStaffFilters() {
    document.getElementById('staff-search-input').value = '';
    document.getElementById('staff-status-select').value = '';
    document.getElementById('staff-category-select').value = '';
    currentPage = 0;
    renderTickets();
}

function escapeHtml(value) {
    return String(value || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

document.addEventListener('DOMContentLoaded', () => {
    loadTickets();
});
