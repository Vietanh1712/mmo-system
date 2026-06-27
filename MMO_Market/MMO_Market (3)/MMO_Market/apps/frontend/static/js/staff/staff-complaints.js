let allComplaints = [];

async function loadStaffComplaintsFromApi() {
    const token = sessionStorage.getItem('accessToken');
    if (!token) {
        console.warn('No access token found, falling back to mock.');
        loadMockComplaints();
        return;
    }

    try {
        const res = await fetch('/api/complaints/all', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (res.ok) {
            const data = await res.json();
            allComplaints = data.map(item => ({
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
            sessionStorage.setItem('mmoMarketComplaintsMockGlobal', JSON.stringify(allComplaints));
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
    } catch(e) {
        return dateStr;
    }
}

function loadMockComplaints() {
    const key = 'mmoMarketComplaintsMockGlobal';
    initComplaints();
    try {
        allComplaints = JSON.parse(sessionStorage.getItem(key)) || [];
    } catch(e) {
        allComplaints = [];
    }
}

function initComplaints() {
    const key = 'mmoMarketComplaintsMockGlobal';
    if (!sessionStorage.getItem(key)) {
        const initialList = [
            {
                id: 'CMP-3310',
                senderName: 'Nguyễn An',
                senderEmail: 'buyer@mmo.com',
                target: 'Shop GameHub Pro / Sản phẩm Gmail Premium',
                category: 'Sản phẩm',
                amount: 350000,
                status: 'InProgress',
                createdAt: '05/06/2026 08:12',
                evidence: 'https://imgur.com/error_gmail.jpg',
                detail: 'Tôi mua tài khoản Gmail Premium nhưng nhận được tài khoản đã bị khóa sau 2 giờ sử dụng. Đã liên hệ shop nhưng không được phản hồi. Yêu cầu hoàn tiền hoặc cấp tài khoản thay thế.'
            },
            {
                id: 'CMP-3308',
                senderName: 'Trần Hùng',
                senderEmail: 'hung.tran@mmo.com',
                target: 'Đơn #ORD-9921',
                category: 'Giao dịch',
                amount: 240000,
                status: 'New',
                createdAt: '04/06/2026 14:30',
                evidence: 'Không có',
                detail: 'Giao dịch lỗi, tài khoản không tự động kích hoạt.'
            },
            {
                id: 'CMP-3295',
                senderName: 'Lê Mai',
                senderEmail: 'mai.le@mmo.com',
                target: 'Shop Digital Keys',
                category: 'Shop',
                amount: 150000,
                status: 'Resolved',
                createdAt: '03/06/2026 10:15',
                evidence: 'https://imgur.com/evidence_chat.jpg',
                detail: 'Người bán không phản hồi chat.'
            }
        ];
        sessionStorage.setItem(key, JSON.stringify(initialList));
    }
}

function renderStaffComplaints() {
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

    const tbody = document.getElementById('staff-complaints-body');
    if (!tbody) return;

    if (filtered.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 24px; color: var(--ds-text-subtle);">
                    Không tìm thấy khiếu nại nào phù hợp với bộ lọc.
                </td>
            </tr>
        `;
        updatePaginationMeta(0);
        return;
    }

    tbody.innerHTML = filtered.map(item => {
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
                        <button class="ds-icon-btn ds-icon-btn-view" onclick="viewComplaintDetail('${item.id}')" aria-label="Xem chi tiết" style="border:none; background:transparent; cursor:pointer;">
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

    updatePaginationMeta(filtered.length);
}

function updatePaginationMeta(count) {
    const totalMeta = document.querySelector('.ds-pagination-meta span');
    if (totalMeta) {
        totalMeta.textContent = `Tổng số: ${count} bản ghi`;
    }
}

function viewComplaintDetail(id) {
    sessionStorage.setItem('selectedComplaintId', id);
    window.location.href = '/staff/complaints/detail';
}

function applyStaffFilters() {
    renderStaffComplaints();
}

function resetStaffFilters() {
    document.getElementById('staff-search-input').value = '';
    document.getElementById('staff-status-select').value = '';
    document.getElementById('staff-category-select').value = '';
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

document.addEventListener('DOMContentLoaded', async () => {
    await loadStaffComplaintsFromApi();
    renderStaffComplaints();
});
