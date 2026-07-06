let currentComplaint = null;
let disputeChatPollInterval = null;

function formatMoney(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(amount) || 0);
}

function escapeHtml(value) {
    return String(value || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
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

async function loadComplaintDetails() {
    const urlParams = new URLSearchParams(window.location.search);
    let id = urlParams.get('id');
    if (id) {
        if (!id.startsWith('CMP-')) {
            id = 'CMP-' + id;
        }
        sessionStorage.setItem('selectedComplaintId', id);
    } else {
        id = sessionStorage.getItem('selectedComplaintId') || 'CMP-3310';
    }
    const numericId = id.replace('CMP-', '');
    const token = sessionStorage.getItem('accessToken');
    
    if (token) {
        try {
            const res = await fetch(`/api/complaints/${numericId}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            if (res.ok) {
                const item = await res.json();
                currentComplaint = {
                    id: item.id ? `CMP-${item.id}` : 'CMP-UNKNOWN',
                    buyerId: item.customer ? item.customer.id : null,
                    senderName: item.customer ? item.customer.fullName : 'N/A',
                    senderEmail: item.customer ? item.customer.email : 'N/A',
                    target: item.transaction && item.transaction.productName ? item.transaction.productName : 'Sản phẩm',
                    transactionId: item.transaction ? item.transaction.id : null,
                    category: 'Sản phẩm số',
                    amount: item.transaction ? item.transaction.amountVnd : 0,
                    status: item.status,
                    createdAt: item.createdAt ? formatDateString(item.createdAt) : 'N/A',
                    evidence: item.evidence || 'Không có',
                    detail: item.description || '',
                    resolution: item.resolution || '',
                    preferredSolution: item.preferredSolution === 'REPLACEMENT' ? 'Đổi tài khoản mới' : (item.preferredSolution === 'REFUND' ? 'Hoàn tiền' : 'N/A')
                };
            }
        } catch (err) {
            console.error('Error loading complaint detail from API:', err);
        }
    }

    if (!currentComplaint) {
        const key = 'mmoMarketComplaintsMockGlobal';
        let list = [];
        try {
            list = JSON.parse(sessionStorage.getItem(key)) || [];
        } catch(e) {}
        currentComplaint = list.find(c => c.id === id);
    }

    if (!currentComplaint) {
        showErrorToast('Không tìm thấy thông tin khiếu nại!');
        window.location.href = '/staff/complaints';
        return;
    }

    // Populate header & breadcrumbs
    document.getElementById('breadcrumb-complaint-id').textContent = `#${currentComplaint.id}`;
    document.getElementById('header-complaint-subtitle').textContent = `Mã khiếu nại #${currentComplaint.id} — ${currentComplaint.target}`;
    
    // Status badge & Sections
    const badge = document.getElementById('header-status-badge');
    let badgeClass = 'ds-badge-warning';
    let statusText = 'Đang xử lý';
    const statusVal = (currentComplaint.status || '').toLowerCase();

    const startSection = document.getElementById('dispute-start-section');
    const decisionSection = document.getElementById('dispute-decision-section');
    const chatCard = document.getElementById('dispute-chat-card');

    if (statusVal === 'pending_review' || statusVal === 'pending_status') {
        badgeClass = 'ds-badge-info';
        statusText = 'Chờ duyệt';
        if (startSection) startSection.style.display = 'flex';
        if (decisionSection) decisionSection.style.display = 'none';
        if (chatCard) chatCard.style.display = 'none';
        if (disputeChatPollInterval) {
            clearInterval(disputeChatPollInterval);
            disputeChatPollInterval = null;
        }
    } else {
        if (startSection) startSection.style.display = 'none';
        if (decisionSection) decisionSection.style.display = 'flex';
        if (chatCard) {
            chatCard.style.display = 'block';
            loadDisputeChats(numericId);
            // Poll dispute chat messages
            if (statusVal === 'inprogress' || statusVal === 'in_progress') {
                if (!disputeChatPollInterval) {
                    disputeChatPollInterval = setInterval(() => {
                        loadDisputeChats(numericId);
                    }, 4000);
                }
            } else {
                if (disputeChatPollInterval) {
                    clearInterval(disputeChatPollInterval);
                    disputeChatPollInterval = null;
                }
            }
        }

        if (statusVal === 'resolved' || statusVal === 'completed' || statusVal === 'success') {
            badgeClass = 'ds-badge-success';
            statusText = 'Đã giải quyết';
        } else if (statusVal === 'rejected' || statusVal === 'refused' || statusVal === 'fail' || statusVal === 'failed') {
            badgeClass = 'ds-badge-danger';
            statusText = 'Từ chối';
        } else {
            badgeClass = 'ds-badge-warning';
            statusText = 'Đang xử lý';
        }
    }
    badge.className = `ds-badge ${badgeClass}`;
    badge.textContent = statusText;


    // Populate table fields (Overwrite the list to add preferred solution row)
    const dlList = document.querySelector('.staff-info-list');
    if (dlList) {
        dlList.innerHTML = `
            <div class="staff-info-row">
                <dt>Mã khiếu nại</dt>
                <dd id="detail-id">#${currentComplaint.id}</dd>
            </div>
            <div class="staff-info-row">
                <dt>Người gửi</dt>
                <dd id="detail-sender">${escapeHtml(currentComplaint.senderName)} — ${escapeHtml(currentComplaint.senderEmail)}</dd>
            </div>
            <div class="staff-info-row">
                <dt>Đối tượng</dt>
                <dd id="detail-target">${escapeHtml(currentComplaint.target)}</dd>
            </div>
            <div class="staff-info-row">
                <dt>Loại</dt>
                <dd id="detail-category">${escapeHtml(currentComplaint.category)}</dd>
            </div>
            <div class="staff-info-row">
                <dt>Số tiền liên quan</dt>
                <dd id="detail-amount" class="ds-money" style="font-weight: 700;">${formatMoney(currentComplaint.amount)}</dd>
            </div>
            <div class="staff-info-row">
                <dt>Giải pháp mong muốn</dt>
                <dd style="font-weight: 700; color: #b45309;">${escapeHtml(currentComplaint.preferredSolution || 'N/A')}</dd>
            </div>
            <div class="staff-info-row">
                <dt>Ngày tạo</dt>
                <dd id="detail-date">${escapeHtml(currentComplaint.createdAt)}</dd>
            </div>
            <div class="staff-info-row">
                <dt>Lý do / Kết quả</dt>
                <dd id="detail-resolution" style="font-style: italic; color: #475569;">${escapeHtml(currentComplaint.resolution || 'Chưa xử lý')}</dd>
            </div>
        `;
    }

    // Content
    document.getElementById('detail-description').textContent = currentComplaint.detail;

    // Evidence
    const evContainer = document.getElementById('detail-evidence-container');
    if (currentComplaint.evidence && currentComplaint.evidence !== 'Không có' && currentComplaint.evidence !== '') {
        evContainer.innerHTML = `
            <a href="${escapeHtml(currentComplaint.evidence)}" target="_blank" style="display: inline-flex; align-items: center; gap: 6px; background: #e0f2fe; color: #0369a1; padding: 6px 12px; border-radius: 6px; font-weight: 600; text-decoration: none; font-size: 13px;">
                <i class="fa fa-external-link"></i> Xem ảnh bằng chứng / Đính kèm
            </a>
        `;
    } else {
        evContainer.innerHTML = `<span class="ds-badge ds-badge-muted">Không có ảnh đính kèm</span>`;
    }

    // Form fields
    let selectStatus = currentComplaint.status;
    if (statusVal === 'open' || statusVal === 'new' || statusVal === 'inprogress' || statusVal === 'in_progress' || statusVal === 'processing') {
        selectStatus = 'InProgress';
    } else if (statusVal === 'resolved' || statusVal === 'completed' || statusVal === 'success') {
        selectStatus = 'Resolved';
    } else if (statusVal === 'rejected' || statusVal === 'refused' || statusVal === 'fail' || statusVal === 'failed') {
        selectStatus = 'Rejected';
    }
    const cmpStatusSelect = document.getElementById('complaintStatus');
    if (cmpStatusSelect) cmpStatusSelect.value = selectStatus;
    const cmpResText = document.getElementById('complaintResolution');
    if (cmpResText) cmpResText.value = currentComplaint.resolution || '';

    // Timeline
    renderTimeline();


}

window.toggleFlaggingFields = function() {
    const enableFlagging = document.getElementById('enableFlagging').checked;
    const fields = document.getElementById('flaggingFields');
    if (fields) {
        fields.style.display = enableFlagging ? 'flex' : 'none';
    }
};



function renderTimeline() {
    const timeline = document.getElementById('detail-timeline');
    let html = `
        <div class="staff-timeline__item">
            <span class="staff-timeline__dot" style="background-color: #0f172a;"></span>
            <div class="staff-timeline__content">
                <strong>Tiếp nhận khiếu nại</strong>
                <p class="ds-caption">Hệ thống — ${escapeHtml(currentComplaint.createdAt)}</p>
            </div>
        </div>
    `;

    if (currentComplaint.status === 'InProgress' || currentComplaint.status === 'Resolved' || currentComplaint.status === 'Rejected') {
        html += `
            <div class="staff-timeline__item">
                <span class="staff-timeline__dot" style="background-color: #f59e0b;"></span>
                <div class="staff-timeline__content">
                    <strong>Nhân viên tiếp nhận &amp; Đang xử lý</strong>
                    <p class="ds-caption">Nguyễn Văn Staff — ${escapeHtml(currentComplaint.createdAt)}</p>
                </div>
            </div>
        `;
    }

    if (currentComplaint.status === 'Resolved') {
        html += `
            <div class="staff-timeline__item">
                <span class="staff-timeline__dot" style="background-color: #10b981;"></span>
                <div class="staff-timeline__content">
                    <strong>Đã giải quyết khiếu nại</strong>
                    <p class="ds-caption">Nguyễn Văn Staff — Vừa xong</p>
                    <p class="ds-body" style="font-size: 13px; font-style: italic; margin-top: 4px; padding-left: 8px; border-left: 2px solid #10b981;">
                        Ghi chú: ${escapeHtml(currentComplaint.resolution || 'Đã hoàn tất hỗ trợ.')}
                    </p>
                </div>
            </div>
        `;
    } else if (currentComplaint.status === 'Rejected') {
        html += `
            <div class="staff-timeline__item">
                <span class="staff-timeline__dot" style="background-color: #ef4444;"></span>
                <div class="staff-timeline__content">
                    <strong>Từ chối hỗ trợ / khiếu nại</strong>
                    <p class="ds-caption">Nguyễn Văn Staff — Vừa xong</p>
                    <p class="ds-body" style="font-size: 13px; font-style: italic; margin-top: 4px; padding-left: 8px; border-left: 2px solid #ef4444;">
                        Lý do: ${escapeHtml(currentComplaint.resolution || 'Khiếu nại không hợp lệ.')}
                    </p>
                </div>
            </div>
        `;
    }

    timeline.innerHTML = html;
}

async function handleStaffAction(status) {
    const resolution = document.getElementById('complaintResolution').value.trim();
    if (!resolution && status === 'Rejected') {
        showWarningToast('Vui lòng nhập lý do từ chối vào mục “Kết quả / ghi chú”!');
        return;
    }

    let flagLevel = null;
    let flagReason = null;
    const enableFlagging = document.getElementById('enableFlagging').checked;
    if (enableFlagging) {
        flagLevel = document.getElementById('flagLevel').value;
        flagReason = document.getElementById('flagReason').value.trim();
        if (!flagReason) {
            showWarningToast('Vui lòng nhập lý do gắn cờ phạt shop!');
            return;
        }
    }

    const id = currentComplaint.id;
    const numericId = id.replace('CMP-', '');
    const token = sessionStorage.getItem('accessToken');
    
    if (token) {
        try {
            const res = await fetch(`/api/complaints/${numericId}/status`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    status: status,
                    resolution: resolution || (status === 'Resolved' ? 'Đã xử lý & hoàn tất hỗ trợ.' : 'Khiếu nại không hợp lệ.'),
                    flagLevel: flagLevel,
                    flagReason: flagReason
                })
            });
            
            if (!res.ok) {
                const errData = await res.json();
                throw new Error(errData.message || 'Lỗi từ hệ thống khi cập nhật khiếu nại.');
            }
        } catch (err) {
            console.error(err);
            showWarningToast('Lỗi cập nhật backend: ' + err.message + '. Hệ thống sẽ thực hiện cập nhật mock tạm thời.');
        }
    }

    const key = 'mmoMarketComplaintsMockGlobal';
    let list = [];
    try {
        list = JSON.parse(sessionStorage.getItem(key)) || [];
    } catch(e) {}

    const index = list.findIndex(c => c.id === currentComplaint.id);
    if (index !== -1) {
        list[index].status = status;
        list[index].resolution = resolution || (status === 'Resolved' ? 'Đã xử lý & hoàn tất hỗ trợ.' : 'Khiếu nại không hợp lệ.');
        sessionStorage.setItem(key, JSON.stringify(list));
    }

    showSuccessToast(`Đã cập nhật trạng thái khiếu nại sang: ${status === 'Resolved' ? 'Đã giải quyết' : 'Từ chối'}`);
    window.location.href = '/staff/complaints';
}

async function loadDisputeChats(numericId) {
    const token = sessionStorage.getItem('accessToken');
    if (!token) return;
    try {
        const res = await fetch(`/api/complaints/${numericId}/chats`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (!res.ok) throw new Error('Không thể tải tin nhắn đối chất.');
        const chats = await res.json();
        
        const container = document.getElementById('dispute-chat-messages');
        if (!container) return;
        
        if (chats.length === 0) {
            container.innerHTML = `<div style="text-align: center; color: #94a3b8; font-size: 13px; font-style: italic;">Chưa có tin nhắn đối chất nào.</div>`;
            return;
        }
        
        container.innerHTML = chats.map(msg => {
            let roleLabel = 'Khách hàng';
            let bg = 'rgba(37, 99, 235, 0.08)';
            let border = '1px solid rgba(37, 99, 235, 0.15)';
            let titleColor = '#2563eb';
            
            if (msg.senderRole === 'Seller') {
                roleLabel = 'Người bán';
                bg = 'rgba(217, 119, 6, 0.08)';
                border = '1px solid rgba(217, 119, 6, 0.15)';
                titleColor = '#d97706';
            } else if (msg.senderRole === 'Staff') {
                roleLabel = 'Hệ thống / Staff';
                bg = 'rgba(71, 85, 105, 0.08)';
                border = '1px solid rgba(71, 85, 105, 0.15)';
                titleColor = '#475569';
            }
            
            return `
                <div style="background: ${bg}; border: ${border}; border-radius: 8px; padding: 12px; font-size: 13.5px; line-height: 1.5;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                        <span style="font-weight: 700; color: ${titleColor};">${escapeHtml(msg.senderName)} <small style="font-weight: 500; opacity: 0.85;">(${roleLabel})</small></span>
                        <small style="color: #64748b; font-size: 11px;">${formatDateString(msg.createdAt)}</small>
                    </div>
                    <div style="color: #1e293b; white-space: pre-wrap;">${escapeHtml(msg.message)}</div>
                </div>
            `;
        }).join('');
        
        container.scrollTop = container.scrollHeight;
    } catch (err) {
        console.error('Error loading dispute chats:', err);
    }
}

window.startDisputeAction = async function() {
    if (!currentComplaint) return;
    const numericId = currentComplaint.id.replace('CMP-', '');
    const token = sessionStorage.getItem('accessToken');
    if (!token) {
        showWarningToast('Vui lòng đăng nhập trước.');
        return;
    }
    
    const btn = document.getElementById('btnStartDispute');
    const oldText = btn.innerHTML;
    btn.setAttribute('disabled', 'true');
    btn.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang kích hoạt...';
    
    try {
        const res = await fetch(`/api/complaints/${numericId}/start-dispute`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!res.ok) {
            const errData = await res.json();
            throw new Error(errData.message || 'Lỗi mở đối chất.');
        }
        
        showSuccessToast('Đã kích hoạt phòng chat đối chất 3 bên thành công!');
        window.location.reload();
    } catch(err) {
        showWarningToast(err.message);
    } finally {
        btn.removeAttribute('disabled');
        btn.innerHTML = oldText;
    }
};

document.addEventListener('DOMContentLoaded', () => {
    loadComplaintDetails();
});
