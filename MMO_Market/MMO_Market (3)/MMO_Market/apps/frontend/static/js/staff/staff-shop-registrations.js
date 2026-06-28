(function() {
    let currentRegistrationId = null;

    document.addEventListener('DOMContentLoaded', () => {
        const token = sessionStorage.getItem('accessToken');
        if (!token) {
            window.location.href = '/login';
            return;
        }

        loadRegistrations();

        const modal = document.getElementById('reviewModal');
        document.getElementById('closeReviewModal').addEventListener('click', () => modal.close());
        document.getElementById('approveBtn').addEventListener('click', () => submitReview(true));
        document.getElementById('rejectBtn').addEventListener('click', () => submitReview(false));
    });

    async function loadRegistrations() {
        const tbody = document.getElementById('shopRegistrationsTableBody');
        tbody.innerHTML = '<tr><td colspan="5" class="ds-table-center">Đang tải...</td></tr>';
        
        try {
            const response = await authFetch('/v1/shop-registrations');
            if (response.ok) {
                const data = await response.json();
                renderTable(data);
            } else {
                tbody.innerHTML = '<tr><td colspan="5" class="ds-table-center">Lỗi khi tải dữ liệu</td></tr>';
            }
        } catch (error) {
            tbody.innerHTML = '<tr><td colspan="5" class="ds-table-center">Lỗi kết nối</td></tr>';
        }
    }

    function renderTable(data) {
        const tbody = document.getElementById('shopRegistrationsTableBody');
        tbody.innerHTML = '';
        if (!data || data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="ds-table-center">Không có yêu cầu nào đang chờ.</td></tr>';
            return;
        }

        data.forEach(item => {
            let formattedDate = '-';
            try {
                formattedDate = new Date(item.submittedAt).toLocaleDateString('vi-VN');
            } catch(e) {}

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${item.code}</td>
                <td>
                    <div class="ds-entity-title">${item.shopName}</div>
                    <div class="ds-entity-subtitle">${item.supportEmail} - ${item.supportPhone}</div>
                </td>
                <td>${item.category}</td>
                <td>${formattedDate}</td>
                <td class="ds-table-center">
                    <button class="ds-btn ds-btn-sm ds-btn-outline review-btn" data-id="${item.id}">Duyệt</button>
                </td>
            `;
            tbody.appendChild(tr);
        });

        document.querySelectorAll('.review-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                currentRegistrationId = e.target.getAttribute('data-id');
                document.getElementById('reviewReason').value = '';
                document.getElementById('reviewModal').showModal();
            });
        });
    }

    async function submitReview(isApproved) {
        if (!currentRegistrationId) return;

        const reason = document.getElementById('reviewReason').value.trim();
        if (!isApproved && !reason) {
            alert('Vui lòng nhập lý do từ chối.');
            return;
        }

        try {
            const response = await authFetch(`/v1/shop-registrations/${currentRegistrationId}/review`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ approved: isApproved, reason: reason })
            });

            if (response.ok) {
                document.getElementById('reviewModal').close();
                loadRegistrations();
            } else {
                const res = await response.json();
                alert(res.description || 'Lỗi xử lý yêu cầu.');
            }
        } catch (error) {
            alert('Lỗi kết nối máy chủ.');
        }
    }
})();
