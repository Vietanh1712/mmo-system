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
        alert('Không tìm thấy mã ticket!');
        window.location.href = '/staff/support-tickets';
        return;
    }

    try {
        const res = await authFetch(`/support-tickets/${id}`);
        if (!res.ok) {
            throw new Error('Lỗi lấy chi tiết ticket');
        }
        currentTicket = await res.json();
        populateFields();
    } catch (e) {
        console.error(e);
        alert('Lỗi tải dữ liệu ticket từ máy chủ.');
        window.location.href = '/staff/support-tickets';
    }
}

function populateFields() {
    if (!currentTicket) return;

    // Populate header & breadcrumbs
    document.getElementById('breadcrumb-ticket-id').textContent = `#ST-${currentTicket.id}`;
    document.getElementById('header-ticket-subtitle').textContent = `Mã Ticket #ST-${currentTicket.id} — Danh mục: ${currentTicket.category}`;
    
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
    } else if (currentTicket.status === 'Closed') {
        badgeClass = 'ds-badge-muted';
        statusText = 'Đã đóng';
    } else if (currentTicket.status === 'Processing') {
        badgeClass = 'ds-badge-warning';
        statusText = 'Đang xử lý';
    } else if (currentTicket.status === 'Replied') {
        badgeClass = 'ds-badge-success';
        statusText = 'Đã phản hồi';
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
            <span class="staff-timeline__dot" style="background-color: #0058be;"></span>
            <div class="staff-timeline__content">
                <strong>Khởi tạo yêu cầu hỗ trợ</strong>
                <p class="ds-caption">Người dùng gửi — ${escapeHtml(formattedDate)}</p>
            </div>
        </div>
    `;

    if (currentTicket.status === 'Processing' || currentTicket.status === 'Resolved' || currentTicket.status === 'Closed' || currentTicket.status === 'Replied') {
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
        if (currentTicket.status === 'Closed') {
            dotColor = '#64748b';
            titleText = 'Đã đóng ticket';
        }

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

    if ((status === 'Resolved' || status === 'Closed') && !resolution) {
        alert('Vui lòng nhập nội dung phản hồi trực tiếp khi giải quyết hoặc đóng ticket!');
        return;
    }

    try {
        const res = await authFetch(`/support-tickets/${currentTicket.id}/status`, {
            method: 'PUT',
            body: JSON.stringify({ status, resolution })
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Lỗi cập nhật ticket');
        }

        alert('Đã cập nhật trạng thái và phản hồi ticket thành công!');
        window.location.href = '/staff/support-tickets';
    } catch (e) {
        console.error(e);
        alert('Lỗi cập nhật ticket: ' + e.message);
    }
}

document.addEventListener('DOMContentLoaded', loadTicketDetails);
