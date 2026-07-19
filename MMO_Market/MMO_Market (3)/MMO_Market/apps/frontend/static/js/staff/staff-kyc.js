let kycPageSize = 10;

document.addEventListener('DOMContentLoaded', () => {
    loadKycList();
    loadKycStats();
    const searchBtn = document.getElementById('kycSearchBtn');
    if (searchBtn) searchBtn.addEventListener('click', () => {
        loadKycList();
        loadKycStats();
    });
    const codeInput = document.getElementById('kycCodeFilter');
    if (codeInput) {
        codeInput.addEventListener('keyup', (e) => {
            if (e.key === 'Enter') {
                loadKycList();
                loadKycStats();
            }
        });
    }
    const typeSelect = document.getElementById('kycTypeFilter');
    if (typeSelect) {
        typeSelect.addEventListener('change', () => {
            loadKycList();
            loadKycStats();
        });
    }
    const statusSelect = document.getElementById('kycStatusFilter');
    if (statusSelect) {
        statusSelect.addEventListener('change', () => {
            loadKycList();
            loadKycStats();
        });
    }
    const resetBtn = document.getElementById('kycResetBtn');
    if (resetBtn) resetBtn.addEventListener('click', () => {
        const codeInput = document.getElementById('kycCodeFilter');
        if (codeInput) codeInput.value = '';
        const typeSelect = document.getElementById('kycTypeFilter');
        if (typeSelect) typeSelect.value = '';
        const statusSelect = document.getElementById('kycStatusFilter');
        if (statusSelect) statusSelect.value = '';
        loadKycList();
        loadKycStats();
    });
});

async function loadKycStats() {
    try {
        const response = await authFetch('/v1/staff/kyc/stats');
        if (response.ok) {
            const stats = await response.json();
            const totalEl = document.getElementById('stat-total-kyc');
            const pendingEl = document.getElementById('stat-pending-kyc');
            const approvedEl = document.getElementById('stat-approved-kyc');
            const rejectedEl = document.getElementById('stat-rejected-kyc');
            
            if (totalEl) totalEl.textContent = stats.total || 0;
            if (pendingEl) pendingEl.textContent = stats.pending || 0;
            if (approvedEl) approvedEl.textContent = stats.approved || 0;
            if (rejectedEl) rejectedEl.textContent = stats.rejected || 0;
        }
    } catch (e) {
        console.error("Lỗi tải thống kê KYC", e);
    }
}

async function loadKycList(page = 0) {
    const statusSelect = document.getElementById('kycStatusFilter');
    const status = statusSelect ? statusSelect.value : '';
    const codeInput = document.getElementById('kycCodeFilter');
    const requestCode = codeInput ? codeInput.value.trim() : '';
    const typeSelect = document.getElementById('kycTypeFilter');
    const idType = typeSelect ? typeSelect.value : '';

    let url = `/v1/staff/kyc?page=${page}&size=${kycPageSize}`;
    if (status) {
        url += `&status=${status}`;
    }
    if (requestCode) {
        url += `&requestCode=${encodeURIComponent(requestCode)}`;
    }
    if (idType) {
        url += `&idType=${idType}`;
    }

    try {
        const response = await authFetch(url);
        if (!response.ok) throw new Error('Failed to load');
        const data = await response.json();
        
        renderTable(data.content, data.number, data.size);
        renderPagination(data);
    } catch (e) {
        console.error("Lỗi tải dữ liệu KYC", e);
    }
}

function renderTable(content, page, size) {
    const tbody = document.getElementById('kycTableBody');
    if (!tbody) return;
    tbody.innerHTML = '';
    
    if (content.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="ds-table-center">Không có dữ liệu</td></tr>';
        return;
    }

    content.forEach((kyc, index) => {
        const stt = page * size + index + 1;
        const statusBadge = getStatusBadge(kyc.status);
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td class="ds-table-center">${stt}</td>
            <td>#${kyc.requestCode}</td>
            <td>${kyc.idNumber}</td>
            <td>${kyc.idType}</td>
            <td class="ds-table-center">${statusBadge}</td>
            <td>${new Date(kyc.createdAt).toLocaleDateString('vi-VN')}</td>
            <td class="ds-table-center">
                <div class="ds-table-actions">
                    <a class="ds-icon-btn ds-icon-btn-view" href="/staff/kyc/detail?id=${kyc.id}" title="Xem chi tiết" aria-label="Xem chi tiết">
                        <svg class="ds-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true" style="width: 16px; height: 16px;">
                            <path d="M2.25 12C3.73 8.12 7.49 5.25 12 5.25C16.51 5.25 20.27 8.12 21.75 12C20.27 15.88 16.51 18.75 12 18.75C7.49 18.75 3.73 15.88 2.25 12Z" stroke="currentColor" stroke-width="2"/>
                            <path d="M12 15.25C13.79 15.25 15.25 13.79 15.25 12C15.25 10.21 13.79 8.75 12 8.75C10.21 8.75 8.75 10.21 8.75 12C8.75 13.79 10.21 15.25 12 15.25Z" stroke="currentColor" stroke-width="2"/>
                        </svg>
                    </a>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function getStatusBadge(status) {
    if (status === 'PENDING') return '<span class="ds-badge ds-badge-warning">Chờ duyệt</span>';
    if (status === 'APPROVED') return '<span class="ds-badge ds-badge-success">Đã duyệt</span>';
    return '<span class="ds-badge ds-badge-danger">Từ chối</span>';
}

function renderPagination(data) {
    mountStaffPagination('kycPagination', {
        page: data.number,
        totalPages: data.totalPages,
        totalElements: data.totalElements,
        pageSize: data.size
    }, {
        onPage: (p) => { loadKycList(p); },
        onSize: (s) => { kycPageSize = s; loadKycList(0); }
    });
}
