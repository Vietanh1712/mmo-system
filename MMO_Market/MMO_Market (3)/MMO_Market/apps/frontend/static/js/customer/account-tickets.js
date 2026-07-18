const TICKET_STATUS_MAP = {
    'Open':       { label: 'Mới',            cls: 'ticket-badge--processing', icon: 'fa-plus-circle' },
    'Processing': { label: 'Đang xử lý',     cls: 'ticket-badge--processing', icon: 'fa-spinner fa-spin' },
    'Resolved':   { label: 'Đã giải quyết',  cls: 'ticket-badge--resolved',   icon: 'fa-check-circle' },
};

function formatTicketDate(dateStr) {
    if (!dateStr) return '—';
    try {
        return new Date(dateStr).toLocaleDateString('vi-VN', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    } catch {
        return dateStr;
    }
}

function renderTicketBadge(status) {
    const info = TICKET_STATUS_MAP[status] || { label: status, cls: 'ticket-badge--closed', icon: 'fa-question' };
    return `<span class="ticket-badge ${info.cls}"><i class="fa ${info.icon}"></i> ${info.label}</span>`;
}

async function loadTickets() {
    const token = sessionStorage.getItem('accessToken');
    if (!token || token === 'null' || token === 'undefined') {
        window.location.href = '/login';
        return;
    }

    try {
        // Dùng authFetch — tự động thêm Authorization header + base URL
        const res = await authFetch('/support-tickets');
        if (res.status === 403) {
            window.location.href = '/profile';
            return;
        }
        if (!res.ok) return;

        const tickets = await res.json();
        const tbody = document.getElementById('tickets-table-body');
        if (!tickets || tickets.length === 0) return;

        tbody.innerHTML = tickets.map(t => `
            <tr onclick="viewTicketDetail(${t.id})" style="cursor: pointer;">
                <td><span class="ticket-id">#${String(t.id || '').padStart(4, '0')}</span></td>
                <td>${t.category || '—'}</td>
                <td><div class="ticket-title">${t.title || '—'}</div></td>
                <td><div class="ticket-reply">${t.resolution || '<span style="color:var(--ds-text-subtle);font-style:italic;">Chưa có phản hồi</span>'}</div></td>
                <td>${renderTicketBadge(t.status || 'Pending')}</td>
                <td><span class="ticket-date">${formatTicketDate(t.createdAt)}</span></td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Lỗi tải phiếu hỗ trợ:', e);
    }
}

function openNewTicket() {
    document.getElementById('ticket-modal-overlay').classList.add('is-open');
    document.body.style.overflow = 'hidden';
}

function closeNewTicket() {
    document.getElementById('ticket-modal-overlay').classList.remove('is-open');
    document.body.style.overflow = '';
}

function handleOverlayClick(e) {
    if (e.target === document.getElementById('ticket-modal-overlay')) closeNewTicket();
}

async function submitNewTicket(e) {
    e.preventDefault();

    const token = sessionStorage.getItem('accessToken');
    if (!token || token === 'null' || token === 'undefined') {
        window.location.href = '/login';
        return;
    }

    const btn = e.target.querySelector('button[type="submit"]');
    btn.disabled = true;
    btn.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang gửi...';

    const payload = {
        category:    document.getElementById('tk-category').value,
        title:       document.getElementById('tk-subject').value,
        description: document.getElementById('tk-detail').value
    };

    try {
        // Dùng authFetch — tự động thêm Authorization header + base URL
        const res = await authFetch('/support-tickets', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            closeNewTicket();
            e.target.reset();
            loadTickets();
        } else {
            const err = await res.json().catch(() => ({}));
            showErrorToast(err.message || 'Gửi phiếu hỗ trợ thất bại, vui lòng thử lại.');
        }
    } catch (ex) {
        // authFetch tự xử lý 401 (redirect về login)
        // Chỉ catch các lỗi network khác
        if (ex.message && ex.message.includes('đăng nhập')) return;
        showErrorToast('Lỗi kết nối, vui lòng thử lại.');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa fa-paper-plane"></i> Gửi phiếu hỗ trợ';
    }
}

document.addEventListener('DOMContentLoaded', loadTickets);

async function viewTicketDetail(id) {
    try {
        const res = await authFetch('/support-tickets/' + id);
        if (!res.ok) return;
        const t = await res.json();
        
        document.getElementById('dt-id').innerText = '#' + String(t.id || '').padStart(4, '0');
        document.getElementById('dt-category').innerText = t.category || '—';
        
        const statusHtml = renderTicketBadge(t.status || 'Pending');
        document.getElementById('dt-status').innerHTML = statusHtml;
        
        document.getElementById('dt-date').innerText = formatTicketDate(t.createdAt);
        document.getElementById('dt-title').innerText = t.title || '—';
        document.getElementById('dt-description').innerText = t.description || '—';
        
        const replyContainer = document.getElementById('dt-reply-container');
        if (t.resolution && t.resolution.trim() !== '') {
            replyContainer.style.display = 'block';
            document.getElementById('dt-reply').innerText = t.resolution;
        } else {
            replyContainer.style.display = 'none';
        }
        
        document.getElementById('ticket-detail-modal').classList.add('is-open');
        document.body.style.overflow = 'hidden';
    } catch (e) {
        console.error('Lỗi lấy chi tiết phiếu hỗ trợ:', e);
    }
}

function closeTicketDetail() {
    document.getElementById('ticket-detail-modal').classList.remove('is-open');
    document.body.style.overflow = '';
}

function handleDetailOverlayClick(e) {
    if (e.target === document.getElementById('ticket-detail-modal')) closeTicketDetail();
}

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        closeNewTicket();
        closeTicketDetail();
    }
});
