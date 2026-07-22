let currentTicket = null;

function escapeHtml(value) {
    return String(value || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

async function loadTicketDetails() {
    const id = sessionStorage.getItem('selectedSupportTicketId');
    if (!id) {
        showErrorToast('Không tìm thấy mã phiếu hỗ trợ!');
        window.location.href = '/staff/support-tickets';
        return;
    }

    try {
        const res = await authFetch(`/support-tickets/${id}`);
        if (!res.ok) {
            throw new Error('Lỗi lấy chi tiết phiếu hỗ trợ');
        }
        currentTicket = await res.json();
        populateFields();
    } catch (e) {
        console.error(e);
        showErrorToast('Lỗi tải dữ liệu phiếu hỗ trợ từ máy chủ.');
        window.location.href = '/staff/support-tickets';
    }
}

function populateFields() {
    if (!currentTicket) return;

    // Populate header & breadcrumbs
    document.getElementById('breadcrumb-ticket-id').textContent = `#ST-${currentTicket.id}`;
    document.getElementById('header-ticket-subtitle').textContent = `Mã Phiếu #ST-${currentTicket.id} — Danh mục: ${currentTicket.category}`;
    
    // Status badge
    const badge = document.getElementById('header-status-badge');
    let badgeClass = 'ds-badge-warning';
    let statusText = 'Đang xử lý';
    if (currentTicket.status === 'Open') {
        badgeClass = 'ds-badge-info';
        statusText = 'Mới';
    } else if (currentTicket.status === 'Resolved') {
        badgeClass = 'ds-badge-success';
        statusText = 'Đã giải quyết';
    } else if (currentTicket.status === 'Processing') {
        badgeClass = 'ds-badge-warning';
        statusText = 'Đang xử lý';
    }
    badge.className = `ds-badge ${badgeClass}`;
    badge.textContent = statusText;

    // Populate table fields
    const userName = currentTicket.user ? currentTicket.user.fullName : 'Guest';
    const userEmail = currentTicket.user ? currentTicket.user.email : 'guest@mmo.com';
    const formattedDate = currentTicket.createdAt ? currentTicket.createdAt.substring(0, 16).replace('T', ' ') : 'N/A';

    document.getElementById('detail-id').textContent = `#ST-${currentTicket.id}`;
    document.getElementById('detail-sender').textContent = `${userName} (${userEmail})`;
    document.getElementById('detail-category').textContent = currentTicket.category;
    document.getElementById('detail-title').textContent = currentTicket.title;
    document.getElementById('detail-date').textContent = formattedDate;

    // Content
    document.getElementById('detail-description').textContent = currentTicket.description;

    // Form fields
    document.getElementById('ticketStatus').value = currentTicket.status;
    document.getElementById('ticketResolution').value = currentTicket.resolution || '';

    // Timeline
    renderTimeline();
}

function renderTimeline() {
    const formattedDate = currentTicket.createdAt ? currentTicket.createdAt.substring(0, 16).replace('T', ' ') : 'N/A';
    let html = `
        <div class="staff-timeline__item">
            <span class="staff-timeline__dot" style="background-color: #0f172a;"></span>
            <div class="staff-timeline__content">
                <strong>Khởi tạo yêu cầu hỗ trợ</strong>
                <p class="ds-caption">Người dùng gửi — ${escapeHtml(formattedDate)}</p>
            </div>
        </div>
    `;

    if (currentTicket.status === 'Processing' || currentTicket.status === 'Resolved') {
        html += `
            <div class="staff-timeline__item">
                <span class="staff-timeline__dot" style="background-color: #f59e0b;"></span>
                <div class="staff-timeline__content">
                    <strong>Staff tiếp nhận &amp; Đang xử lý</strong>
                    <p class="ds-caption">Nhân viên hỗ trợ</p>
                </div>
            </div>
        `;
    }

    if (currentTicket.resolution) {
        let dotColor = '#10b981';
        let titleText = 'Đã phản hồi &amp; Giải quyết';

        html += `
            <div class="staff-timeline__item">
                <span class="staff-timeline__dot" style="background-color: ${dotColor};"></span>
                <div class="staff-timeline__content">
                    <strong>${titleText}</strong>
                    <p class="ds-caption">Staff phản hồi</p>
                    <p class="ds-body" style="font-size: 13px; font-style: italic; margin-top: 6px; padding: 8px; background: #f8fafc; border-left: 3px solid ${dotColor}; border-radius: 0 6px 6px 0;">
                        ${escapeHtml(currentTicket.resolution)}
                    </p>
                </div>
            </div>
        `;
    }

    document.getElementById('detail-timeline').innerHTML = html;
}

async function submitStaffAction() {
    const status = document.getElementById('ticketStatus').value;
    const resolution = document.getElementById('ticketResolution').value.trim();

    if (status === 'Resolved' && !resolution) {
        showWarningToast('Vui lòng nhập nội dung phản hồi trực tiếp khi giải quyết phiếu hỗ trợ!');
        return;
    }

    try {
        const res = await authFetch(`/support-tickets/${currentTicket.id}/status`, {
            method: 'PUT',
            body: JSON.stringify({ status, resolution })
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Lỗi cập nhật phiếu hỗ trợ');
        }

        showSuccessToast('Đã cập nhật trạng thái và phản hồi phiếu hỗ trợ thành công!');
        window.location.href = '/staff/support-tickets';
    } catch (e) {
        console.error(e);
        showErrorToast('Lỗi cập nhật phiếu hỗ trợ: ' + e.message);
    }
}

document.addEventListener('DOMContentLoaded', loadTicketDetails);
