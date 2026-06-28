document.addEventListener('DOMContentLoaded', () => {
    loadKycList();
    const searchBtn = document.getElementById('kycSearchBtn');
    if (searchBtn) searchBtn.addEventListener('click', () => loadKycList());
});

async function loadKycList(page = 0) {
    const statusSelect = document.getElementById('kycStatusFilter');
    const status = statusSelect ? statusSelect.value : '';

    let url = `/v1/staff/kyc?page=${page}&size=10`;
    if (status) {
        url += `&status=${status}`;
    }

    try {
        const response = await authFetch(url);
        if (!response.ok) throw new Error('Failed to load');
        const data = await response.json();
        
        renderTable(data.content);
        renderPagination(data);
    } catch (e) {
        console.error("Lỗi tải dữ liệu KYC", e);
    }
}

function renderTable(content) {
    const tbody = document.getElementById('kycTableBody');
    if (!tbody) return;
    tbody.innerHTML = '';
    
    if (content.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="ds-table-center">Không có dữ liệu</td></tr>';
        return;
    }

    content.forEach(kyc => {
        const statusBadge = getStatusBadge(kyc.status);
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>#${kyc.requestCode}</td>
            <td>${kyc.idNumber}</td>
            <td>${kyc.idType}</td>
            <td class="ds-table-center">${statusBadge}</td>
            <td>${new Date(kyc.createdAt).toLocaleDateString('vi-VN')}</td>
            <td class="ds-table-center">
                <a class="ds-btn ds-btn-outline" href="/staff/kyc/detail?id=${kyc.id}">Xem hồ sơ</a>
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
    const paginationPages = document.getElementById('kycPaginationPages');
    const paginationMeta = document.getElementById('kycPaginationMeta');
    
    if (paginationMeta) {
        paginationMeta.innerHTML = `<span>Tổng số: ${data.totalElements} bản ghi</span>`;
    }
    
    if (paginationPages) {
        paginationPages.innerHTML = '';
        for (let i = 0; i < data.totalPages; i++) {
            const span = document.createElement('span');
            span.className = `ds-page-link ${i === data.number ? 'ds-page-link-active' : ''}`;
            span.textContent = i + 1;
            span.onclick = () => loadKycList(i);
            paginationPages.appendChild(span);
        }
    }
}
