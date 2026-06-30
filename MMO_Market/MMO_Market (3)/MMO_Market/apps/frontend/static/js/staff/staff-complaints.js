let allComplaints = [];
let currentPage = 0;
let pageSize = 10;
let totalElements = 0;
let totalPages = 0;

async function loadStaffComplaintsFromApi() {
    const token = sessionStorage.getItem('accessToken');
    if (!token) {
        console.warn('No access token found, falling back to mock.');
        loadMockComplaints();
        return;
    }

    const searchInput = document.getElementById('staff-search-input');
    const search = searchInput ? searchInput.value.trim() : '';
    const statusSelect = document.getElementById('staff-status-select');
    const status = statusSelect ? statusSelect.value : '';

    let url = `/api/complaints/all?page=${currentPage}&size=${pageSize}`;
    if (search) {
        url += `&keyword=${encodeURIComponent(search)}`;
    }
    if (status) {
        url += `&status=${status}`;
    }

    try {
        const res = await fetch(url, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (res.ok) {
            const data = await res.json();
            const list = data.content || [];
            allComplaints = list.map(item => ({
                id: item.id ? `CMP-${item.id}` : 'CMP-UNKNOWN',
                senderName: item.customer ? item.customer.fullName : 'N/A',
                senderEmail: item.customer ? item.customer.email : 'N/A',
                target: item.transaction && item.transaction.productName ? item.transaction.productName : 'Sản phẩm',
                category: 'Sản phẩm',
                amount: item.transaction ? item.transaction.amountVnd : 0,
                status: item.status,
                createdAt: item.createdAt ? formatDateString(item.createdAt) : 'N/A',
                evidence: item.evidence || 'Không có',
                detail: item.description || ''
            }));
            totalElements = data.totalElements || 0;
            totalPages = data.totalPages || 0;
        } else {
            loadMockComplaints();
        }
    } catch (err) {
        console.error(err);
        loadMockComplaints();
    }
}

function formatDateString(dateStr) {
    try {
        const d = new Date(dateStr);
        const pad = (n) => String(n).padStart(2, '0');
        return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    } catch (e) {
        return dateStr;
    }
}

function loadMockComplaints() {
    const key = 'mmoMarketComplaintsMockGlobal';
    initComplaints();
    try {
        allComplaints = JSON.parse(sessionStorage.getItem(key)) || [];
    } catch (e) {
        allComplaints = [];
    }
}

function initComplaints() {
    const key = 'mmoMarketComplaintsMockGlobal';
    if (!sessionStorage.getItem(key)) {
        const initialList = [];
        sessionStorage.setItem(key, JSON.stringify(initialList));
    }
}

function renderStaffComplaintsTable(list, isBackendDriven = true) {
    const tbody = document.getElementById('staff-complaints-body');
    if (!tbody) return;

    if (list.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 24px; color: var(--ds-text-subtle);">
                    Không tìm thấy khiếu nại nào phù hợp với bộ lọc.
                </td>
            </tr>
        `;
        renderPagination(0, 0);
        return;
    }

    tbody.innerHTML = list.map(item => {
        let badgeClass = 'ds-badge-warning';
        let statusText = 'Đang xử lý';
        if (item.status === 'New') {
            badgeClass = 'ds-badge-info';
            statusText = 'Mới';
        } else if (item.status === 'Resolved' || item.status === 'Completed') {
            badgeClass = 'ds-badge-success';
            statusText = 'Đã giải quyết';
        } else if (item.status === 'Rejected') {
            badgeClass = 'ds-badge-danger';
            statusText = 'Từ chối';
        } else if (item.status === 'InProgress') {
            badgeClass = 'ds-badge-warning';
            statusText = 'Đang xử lý';
        }

        const nameInitials = item.senderName ? item.senderName.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() : 'NA';

        return `
            <tr>
                <td>#${escapeHtml(item.id)}</td>
                <td>
                    <div class="ds-entity">
                        <span class="ds-avatar ds-avatar-sm ds-avatar-primary">${escapeHtml(nameInitials)}</span>
                        <div>
                            <div class="ds-entity-title">${escapeHtml(item.senderName)}</div>
                            <div class="ds-entity-subtitle">${escapeHtml(item.senderEmail)}</div>
                        </div>
                    </div>
                </td>
                <td>${escapeHtml(item.target)}</td>
                <td>${escapeHtml(item.category)}</td>
                <td class="ds-table-center"><span class="ds-badge ${badgeClass}">${statusText}</span></td>
                <td>${escapeHtml(item.createdAt.split(' ')[0])}</td>
                <td class="ds-table-center">
                    <div class="ds-table-actions">
                        <button class="ds-icon-btn ds-icon-btn-view" onclick="viewComplaintDetail('${item.id}')" aria-label="Xem chi tiết">
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

    if (isBackendDriven) {
        renderPagination(totalElements, totalPages);
    } else {
        const localTotalPages = Math.ceil(list.length / pageSize);
        renderPagination(list.length, localTotalPages);
    }
}

async function renderStaffComplaints() {
    const token = sessionStorage.getItem('accessToken');
    if (!token) {
        loadMockComplaints();
        let list = allComplaints;
        const search = document.getElementById('staff-search-input').value.trim().toLowerCase();
        const status = document.getElementById('staff-status-select').value;
        const category = document.getElementById('staff-category-select').value;

        const filtered = list.filter(item => {
            const matchesSearch = !search ||
                item.id.toLowerCase().includes(search) ||
                item.senderName.toLowerCase().includes(search) ||
                item.senderEmail.toLowerCase().includes(search) ||
                item.target.toLowerCase().includes(search);
            const matchesStatus = !status || item.status === status;
            const matchesCategory = !category || item.category === category;
            return matchesSearch && matchesStatus && matchesCategory;
        });

        filtered.sort((a, b) => {
            const idA = parseInt(a.id.replace('CMP-', '')) || 0;
            const idB = parseInt(b.id.replace('CMP-', '')) || 0;
            return idA - idB;
        });

        const localTotalPages = Math.ceil(filtered.length / pageSize);
        if (currentPage >= localTotalPages) currentPage = 0;
        const pageItems = filtered.slice(currentPage * pageSize, (currentPage + 1) * pageSize);
        renderStaffComplaintsTable(pageItems, false);
        return;
    }

    await loadStaffComplaintsFromApi();
    renderStaffComplaintsTable(allComplaints, true);
}

function renderPagination(totalElements, totalPages) {
    mountStaffPagination('complaintsPagination', {
        page: currentPage,
        totalPages: totalPages,
        totalElements: totalElements,
        pageSize: pageSize
    }, {
        onPage: (p) => { currentPage = p; renderStaffComplaints(); },
        onSize: (s) => { pageSize = s; currentPage = 0; renderStaffComplaints(); }
    });
}

function viewComplaintDetail(id) {
    sessionStorage.setItem('selectedComplaintId', id);
    window.location.href = '/staff/complaints/detail';
}

function applyStaffFilters() {
    currentPage = 0;
    renderStaffComplaints();
}

function resetStaffFilters() {
    document.getElementById('staff-search-input').value = '';
    document.getElementById('staff-status-select').value = '';
    document.getElementById('staff-category-select').value = '';
    currentPage = 0;
    renderStaffComplaints();
}

function escapeHtml(value) {
    return String(value || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

async function loadComplaintStats() {
    const token = sessionStorage.getItem('accessToken');
    if (!token) return;

    try {
        const res = await fetch('/api/complaints/stats', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (res.ok) {
            const stats = await res.json();
            const totalEl = document.getElementById('stat-total-complaints');
            const inprogressEl = document.getElementById('stat-inprogress-complaints');
            const resolvedEl = document.getElementById('stat-resolved-complaints');
            const rejectedEl = document.getElementById('stat-rejected-complaints');

            if (totalEl) totalEl.textContent = stats.total || 0;
            if (inprogressEl) inprogressEl.textContent = stats.inProgress || 0;
            if (resolvedEl) resolvedEl.textContent = stats.resolved || 0;
            if (rejectedEl) rejectedEl.textContent = stats.rejected || 0;
        }
    } catch (err) {
        console.error("Lỗi tải thống kê khiếu nại:", err);
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    await loadComplaintStats();
    await renderStaffComplaints();
});
