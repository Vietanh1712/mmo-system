// =======================================================
// LOGIC TRANG DANH SÁCH ĐƠN ĐẶT TRƯỚC CỦA KHÁCH HÀNG
// =======================================================

const preOrderDeliveryById = new Map();

document.addEventListener('DOMContentLoaded', () => {
    const token = sessionStorage.getItem("accessToken");
    if (!token || token === "null" || token === "undefined") {
        window.location.href = '/login';
        return;
    }

    fetchPreOrders(token);
});

// Escape HTML để tránh XSS khi render nội dung người dùng nhập
function escapeHtml(unsafe) {
    return (unsafe || '').toString()
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// Gọi API lấy danh sách đơn đặt trước của người dùng hiện tại
function fetchPreOrders(token) {
    const messageEl = document.getElementById('preOrdersMessage');
    const listEl = document.getElementById('preOrdersList');
    const emptyEl = document.getElementById('preOrdersEmpty');

    fetch('/api/v1/pre-orders', {
        method: 'GET',
        headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json'
        }
    })
    .then(res => {
        if (res.status === 401) {
            sessionStorage.clear();
            window.location.href = '/login';
            throw new Error('Unauthorized');
        }
        if (!res.ok) {
            throw new Error('Lấy danh sách đơn đặt trước thất bại');
        }
        return res.json();
    })
    .then(data => {
        messageEl.hidden = true;
        preOrderDeliveryById.clear();
        if (!data || data.length === 0) {
            emptyEl.hidden = false;
            listEl.hidden = true;
            return;
        }

        emptyEl.hidden = true;
        listEl.hidden = false;

        listEl.innerHTML = data.map(po => {
            preOrderDeliveryById.set(String(po.id), {
                deliveryData: po.deliveryData || '',
                productName: po.productName || 'Sản phẩm',
                orderCode: `#PO-${po.id}`,
                proofImage: po.proofImage || ''
            });
            const formattedPrice = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(po.expectedPriceVnd || 0);
            const formattedDate = po.createdAt ? po.createdAt : '-';
            const rawNotes = po.notes ? po.notes.trim() : '';
            const hasUserNotes = rawNotes !== '' && !rawNotes.startsWith('Tự động tạo từ thanh toán giỏ hàng');

            // Xác định các CSS classes cho quy trình trạng thái (workflow) dựa trên trạng thái đơn hàng (status)
            const status = (po.status || 'pending').toLowerCase();
            let step1Class = 'workflow-step--active';
            let line1Class = '';
            let step2Class = '';
            let line2Class = '';
            let step3Class = '';
            let step2Label = 'Người bán chuẩn bị';
            let step3Label = 'Hoàn tất giao dịch';

            if (status === 'pending') {
                step1Class = 'workflow-step--success';
                line1Class = 'workflow-line--active';
                step2Class = 'workflow-step--active';
                step2Label = 'Chờ duyệt yêu cầu';
            } else if (status === 'approved' || status === 'accept' || status === 'accepted') {
                step1Class = 'workflow-step--success';
                line1Class = 'workflow-line--success';
                step2Class = 'workflow-step--success';
                line2Class = 'workflow-line--active';
                step3Class = 'workflow-step--active';
                step2Label = 'Đã duyệt - Chuẩn bị hàng';
            } else if (status === 'completed') {
                step1Class = 'workflow-step--success';
                line1Class = 'workflow-line--success';
                step2Class = 'workflow-step--success';
                line2Class = 'workflow-line--success';
                step3Class = 'workflow-step--success';
                step2Label = 'Đã duyệt';
                step3Label = 'Đã giao hàng';
            } else if (status === 'rejected' || status === 'reject') {
                step1Class = 'workflow-step--success';
                line1Class = 'workflow-line--error';
                step2Class = 'workflow-step--error';
                step2Label = 'Từ chối';
                step3Label = 'Giao dịch thất bại';
            } else if (status === 'cancelled' || status === 'cancel') {
                step1Class = 'workflow-step--success';
                line1Class = 'workflow-line--error';
                step2Class = 'workflow-step--error';
                step2Label = 'Đã hủy';
                step3Label = 'Đơn hàng đã hủy';
            }

            const deliveryAction = status === 'completed'
                ? `
                    <div class="preorder-card__actions">
                        <button type="button" class="ds-btn ds-btn-primary preorder-card__delivery-btn"
                                onclick="openPreOrderDeliveryModal('${po.id}')">
                            <i class="fa fa-eye"></i>
                            Xem tài khoản
                        </button>
                    </div>
                `
                : (status === 'pending')
                ? `
                    <div class="preorder-card__actions">
                        <button type="button" class="ds-btn ds-btn-outline" style="color: #ef4444; border-color: #ef4444; background: transparent; padding: 8px 16px; border-radius: 6px; font-weight: 500;"
                                onclick="customerCancelPreOrder('${po.id}')">
                            <i class="fa fa-times"></i>
                            Hủy đơn đặt trước
                        </button>
                    </div>
                `
                : '';

            return `
                <article class="preorder-card">
                    <div class="preorder-card__header">
                        <span class="preorder-card__id"><i class="fa fa-hashtag"></i> Đơn đặt trước: <strong>#PO-${po.id}</strong></span>
                        <span class="preorder-card__date"><i class="fa fa-calendar"></i> ${formattedDate}</span>
                    </div>
                    
                    <div class="preorder-card__body">
                        <div class="preorder-card__info-grid">
                            <div class="preorder-card__info-item">
                                <span class="preorder-card__label">Sản phẩm</span>
                                <a href="/products/${po.productId}" class="preorder-card__value preorder-card__link">${po.productName}</a>
                                ${po.variantName ? `<span class="preorder-card__variant">${po.variantName}</span>` : ''}
                            </div>
                            <div class="preorder-card__info-item">
                                <span class="preorder-card__label">Số lượng</span>
                                <span class="preorder-card__value">${po.quantity}</span>
                            </div>
                            <div class="preorder-card__info-item">
                                <span class="preorder-card__label">Giá kỳ vọng</span>
                                <span class="preorder-card__value preorder-card__price">${formattedPrice}</span>
                            </div>
                        </div>
                        
                         ${hasUserNotes ? `
                         <div class="preorder-card__notes">
                             <span class="preorder-card__label">Ghi chú của bạn</span>
                             <p class="preorder-card__notes-text">${escapeHtml(po.notes)}</p>
                         </div>
                         ` : ''}
                        

                    </div>
                    
                    <div class="preorder-card__workflow" aria-label="Quy trình đơn hàng">
                        <div class="workflow-step ${step1Class}">
                            <div class="workflow-step__circle"><i class="fa fa-paper-plane"></i></div>
                            <span class="workflow-step__label">Đã gửi yêu cầu</span>
                        </div>
                        <div class="workflow-line ${line1Class}"></div>
                        <div class="workflow-step ${step2Class}">
                            <div class="workflow-step__circle"><i class="fa fa-check"></i></div>
                            <span class="workflow-step__label">${step2Label}</span>
                        </div>
                        <div class="workflow-line ${line2Class}"></div>
                        <div class="workflow-step ${step3Class}">
                            <div class="workflow-step__circle"><i class="fa fa-gift"></i></div>
                            <span class="workflow-step__label">${step3Label}</span>
                        </div>
                    </div>
                    ${deliveryAction}
                </article>
            `;
        }).join('');
    })
    .catch(err => {
        console.error(err);
        messageEl.className = 'profile-message profile-message--error';
        messageEl.innerText = err.message || 'Không thể tải danh sách đơn đặt trước. Vui lòng thử lại sau.';
    });
}

// Mở modal xem thông tin tài khoản được giao khi đơn đặt trước hoàn thành
function openPreOrderDeliveryModal(preOrderId) {
    const order = preOrderDeliveryById.get(String(preOrderId));
    if (!order) return;

    document.getElementById('preOrderDeliveryModalMeta').textContent =
        `${order.orderCode} • ${order.productName}`;
    document.getElementById('preOrderDeliveryContent').textContent =
        order.deliveryData || 'Người bán chưa cung cấp thông tin tài khoản.';

    const imgContainer = document.getElementById('preOrderDeliveryProofContainer');
    const img = document.getElementById('preOrderDeliveryProofImg');
    if (imgContainer && img) {
        if (order.proofImage) {
            img.src = order.proofImage;
            imgContainer.style.display = 'block';
        } else {
            img.src = '';
            imgContainer.style.display = 'none';
        }
    }

    const modal = document.getElementById('preOrderDeliveryModal');
    modal.hidden = false;
    document.body.classList.add('preorder-modal-open');
    modal.querySelector('.preorder-delivery-modal__close').focus();
}

// Hiện modal xác nhận tùy chỉnh, trả về Promise (resolve true nếu xác nhận, false nếu hủy)
function showCustomConfirm(title, message) {
    return new Promise((resolve) => {
        const modal = document.getElementById('customConfirmModal');
        const titleEl = document.getElementById('confirmModalTitle');
        const msgEl = document.getElementById('confirmModalMessage');
        const confirmBtn = document.getElementById('btnConfirmAction');

        if (titleEl) titleEl.textContent = title;
        if (msgEl) msgEl.textContent = message;

        const handleConfirm = () => {
            modal.hidden = true;
            cleanup();
            resolve(true);
        };

        const cleanup = () => {
            confirmBtn.removeEventListener('click', handleConfirm);
        };

        confirmBtn.addEventListener('click', handleConfirm);
        
        window.closeConfirmModal = () => {
            modal.hidden = true;
            cleanup();
            resolve(false);
        };

        modal.hidden = false;
    });
}

// Hủy đơn đặt trước và hoàn tiền 100% về ví khách hàng
async function customerCancelPreOrder(preOrderId) {
    const confirmTitle = "Hủy đơn đặt trước";
    const confirmMsg = "Bạn có chắc chắn muốn HỦY đơn đặt trước này?\nSố tiền đã thanh toán sẽ được hoàn trả 100% về ví của bạn ngay lập tức! Thao tác này không thể hoàn tác.";
    const ok = await showCustomConfirm(confirmTitle, confirmMsg);
    if (!ok) return;

    const token = sessionStorage.getItem("accessToken");
    try {
        const res = await fetch(`/api/v1/pre-orders/${preOrderId}/cancel`, {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            }
        });
        
        if (!res.ok) {
            const errData = await res.json().catch(() => ({}));
            throw new Error(errData.message || 'Hủy đơn hàng thất bại');
        }
        
        if (typeof showSuccessToast === 'function') {
            showSuccessToast('Đã hủy đơn đặt trước và hoàn tiền thành công.');
        } else {
            alert('Đã hủy đơn đặt trước và hoàn tiền thành công.');
        }
        
        fetchPreOrders(token);
    } catch (err) {
        console.error(err);
        if (typeof showErrorToast === 'function') {
            showErrorToast(err.message);
        } else {
            alert(err.message);
        }
    }
}

// Đóng modal xem tài khoản giao hàng
function closePreOrderDeliveryModal() {
    const modal = document.getElementById('preOrderDeliveryModal');
    modal.hidden = true;
    document.body.classList.remove('preorder-modal-open');
}

// Sao chép nội dung thông tin tài khoản vào Clipboard
async function copyPreOrderDeliveryData() {
    const content = document.getElementById('preOrderDeliveryContent').textContent;
    try {
        await navigator.clipboard.writeText(content);
        if (typeof showSuccessToast === 'function') {
            showSuccessToast('Đã sao chép thông tin tài khoản.');
        }
    } catch (error) {
        if (typeof showErrorToast === 'function') {
            showErrorToast('Không thể sao chép. Vui lòng sao chép thủ công.');
        }
    }
}

// Đóng modal khi nhấp ra ngoài vùng nội dung
document.getElementById('preOrderDeliveryModal').addEventListener('click', event => {
    if (event.target.id === 'preOrderDeliveryModal') {
        closePreOrderDeliveryModal();
    }
});

// Đóng modal khi nhấn phím Escape
document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !document.getElementById('preOrderDeliveryModal').hidden) {
        closePreOrderDeliveryModal();
    }
});
