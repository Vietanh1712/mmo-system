const ACCOUNT_ORDERS_MOCK_KEY = 'mmoMarketMyOrdersMock';

let accountSidebar = null;
let currentOrder = null;

document.addEventListener('DOMContentLoaded', initializeOrderDetailPage);

function initializeOrderDetailPage() {
    accountSidebar = new AccountSidebar();
    bindOrderDetailEvents();
    loadOrderDetailPage();
}

function bindOrderDetailEvents() {
    document.getElementById('orderViewProductButton').addEventListener('click', () => {
        if (currentOrder && currentOrder.productId) {
            window.location.href = `/products/${currentOrder.productId}`;
        } else {
            window.location.href = '/products/1';
        }
    });

    document.getElementById('orderComplaintButton').addEventListener('click', () => {
        openComplaintModal();
    });

    const complaintForm = document.getElementById('complaintForm');
    if (complaintForm) {
        complaintForm.addEventListener('submit', handleComplaintSubmit);
    }
}

function openComplaintModal() {
    if (!currentOrder) return;

    // Reset form fields
    document.getElementById('complaintDescription').value = '';
    document.getElementById('complaintEvidence').value = '';
    const fileInput = document.getElementById('complaintEvidenceFile');
    if (fileInput) fileInput.value = '';
    const statusText = document.getElementById('uploadStatusText');
    if (statusText) statusText.textContent = '';

    document.getElementById('complaintModal').classList.add('active');
}

window.closeComplaintModal = function () {
    document.getElementById('complaintModal').classList.remove('active');
}

async function handleComplaintSubmit(e) {
    e.preventDefault();
    if (!currentOrder) return;

    const token = sessionStorage.getItem('accessToken');
    if (!token) {
        showOrderDetailMessage('Vui lòng đăng nhập trước khi gửi khiếu nại.', 'danger');
        closeComplaintModal();
        return;
    }

    const description = document.getElementById('complaintDescription').value.trim();
    let evidence = '';

    if (!description) {
        showWarningToast('Vui lòng nhập chi tiết lý do khiếu nại.');
        return;
    }

    const fileInput = document.getElementById('complaintEvidenceFile');
    const statusText = document.getElementById('uploadStatusText');

    if (!fileInput || fileInput.files.length === 0) {
        showWarningToast('Vui lòng cung cấp ảnh hoặc video bằng chứng.');
        return;
    }

    if (fileInput && fileInput.files.length > 0) {
        if (statusText) statusText.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang tải lên ảnh/video bằng chứng...';
        const file = fileInput.files[0];
        const formData = new FormData();
        formData.append('file', file);

        try {
            const uploadRes = await fetch('/api/upload', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`
                },
                body: formData
            });
            const uploadData = await uploadRes.json();
            if (!uploadRes.ok) {
                throw new Error(uploadData.message || 'Lỗi tải lên ảnh bằng chứng.');
            }
            evidence = uploadData.url;
            if (statusText) statusText.innerHTML = '<span style="color: #16a34a;"><i class="fa fa-check-circle"></i> Đã tải lên bằng chứng thành công.</span>';
        } catch (uploadErr) {
            showErrorToast(uploadErr.message || 'Không thể tải lên file bằng chứng.');
            if (statusText) statusText.innerHTML = '<span style="color: #ef4444;"><i class="fa fa-times-circle"></i> Lỗi tải lên.</span>';
            return;
        }
    }

    const transactionId = currentOrder.transactionId;

    const btnSubmit = e.target.querySelector('button[type="submit"]');
    const oldBtnHtml = btnSubmit.innerHTML;
    btnSubmit.setAttribute('disabled', 'true');
    btnSubmit.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang gửi...';

    try {
        const response = await fetch('/api/complaints', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                transactionId: Number(transactionId),
                description: description,
                evidence: evidence || null,
                preferredSolution: document.getElementById('complaintPreferredSolution').value
            })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Lỗi gửi khiếu nại từ hệ thống.');
        }

        // Cập nhật trạng thái
        currentOrder.status = 'DISPUTED';

        // 3. Close modal and re-render UI
        closeComplaintModal();
        renderOrderDetail(currentOrder);
        showOrderDetailMessage('Đã gửi khiếu nại đơn hàng thành công. Tiền thanh toán đã được đóng băng.', 'success');
        showSuccessToast('Đã gửi khiếu nại đơn hàng thành công. Tiền thanh toán đã được đóng băng.');

    } catch (err) {
        showErrorToast(err.message || 'Lỗi khi gửi khiếu nại.');
    } finally {
        btnSubmit.removeAttribute('disabled');
        btnSubmit.innerHTML = oldBtnHtml;
    }
}



async function loadOrderDetailPage() {
    const token = sessionStorage.getItem('accessToken');
    if (!token || token === 'null' || token === 'undefined') {
        window.location.href = '/login';
        return;
    }

    try {
        const response = await authFetch('/v1/profile');
        if (!response.ok) {
            throw new Error('Không thể tải thông tin tài khoản.');
        }

        const profile = await response.json();
        accountSidebar.render(profile);

        const orderCode = getOrderCodeFromPath();
        const transactionIdMatch = orderCode.match(/MMO-ORD-(\d+)/);
        if (!transactionIdMatch) {
            showNotFound(orderCode);
            return;
        }

        const transactionId = transactionIdMatch[1];

        const txResponse = await authFetch(`/transactions/${transactionId}`);
        if (!txResponse.ok) {
            if (txResponse.status === 404 || txResponse.status === 403 || txResponse.status === 400) {
                showNotFound(orderCode);
                return;
            }
            throw new Error('Không thể lấy chi tiết đơn hàng.');
        }

        currentOrder = await txResponse.json();

        if (!currentOrder) {
            showNotFound(orderCode);
            return;
        }

        renderOrderDetail(currentOrder);
    } catch (error) {
        showOrderDetailMessage(error.message || 'Không thể tải chi tiết đơn hàng.', 'danger');
    }
}

function getOrderCodeFromPath() {
    const parts = window.location.pathname.split('/').filter(Boolean);
    return decodeURIComponent(parts[parts.length - 1] || '');
}



function renderOrderDetail(order) {
    document.getElementById('orderDetailOverview').hidden = false;
    document.getElementById('orderDetailContent').hidden = false;
    document.getElementById('orderTimeline').hidden = false;
    document.getElementById('orderDetailEmpty').hidden = true;

    const msgEl = document.getElementById('orderDetailMessage');
    if (msgEl) {
        msgEl.hidden = true;
        msgEl.classList.add('ds-hidden');
    }

    const variantText = order.variantLabel ? ` (${order.variantLabel})` : '';
    document.getElementById('orderDetailCode').textContent = order.orderCode;
    document.getElementById('orderProductName').textContent = `${order.productName}${variantText}`;

    const sellerNameEl = document.getElementById('orderSellerName');
    if (sellerNameEl) {
        if (order.sellerId) {
            sellerNameEl.innerHTML = `Người bán: <a href="/shop/${order.sellerId}" style="text-decoration: underline; color: var(--ds-primary, #2563eb); font-weight: 500;">${order.sellerName}</a>`;
        } else {
            sellerNameEl.textContent = `Người bán: ${order.sellerName}`;
        }
    }

    setBadge('orderStatusBadge', formatOrderStatus(order.status), getOrderStatusBadgeClass(order.status));
    setBadge('orderPaymentBadge', formatPaymentStatus(order.paymentStatus), getPaymentBadgeClass(order.paymentStatus));

    document.getElementById('orderCodeValue').textContent = order.orderCode;
    document.getElementById('orderCreatedAt').textContent = order.createdAt;
    document.getElementById('orderAmount').textContent = formatMoney(order.amount);
    document.getElementById('orderProductTitle').textContent = `${order.productName}${variantText}`;
    document.getElementById('orderAccessInfo').innerHTML = createAccessInfo(order);
    document.getElementById('orderTransactionCode').textContent = `TX-${order.orderCode.replace('MMO-ORD-', '')}`;
    document.getElementById('orderPaymentText').textContent = formatPaymentStatus(order.paymentStatus);
    document.getElementById('orderPaymentAmount').textContent = formatMoney(order.amount);
    document.getElementById('orderActionHint').textContent = getActionHint(order);

    const viewProductBtn = document.getElementById('orderViewProductButton');
    if (viewProductBtn) {
        viewProductBtn.innerHTML = '<i class="fa fa-eye" aria-hidden="true"></i> Xem sản phẩm';
    }

    const feedbackBtn = document.getElementById('orderFeedbackButton');
    if (feedbackBtn) {
        if (order.isReviewed) {
            feedbackBtn.style.display = 'inline-flex';
            feedbackBtn.removeAttribute('href');
            feedbackBtn.style.pointerEvents = 'none';
            feedbackBtn.style.opacity = '0.7';
            feedbackBtn.className = 'ds-btn ds-btn-outline';
            feedbackBtn.innerHTML = '<i class="fa fa-check-circle" style="color: #16a34a;"></i> Đã đánh giá';
        } else if (['COMPLETED', 'PAID', 'DELIVERED', 'HELD'].includes(order.status)) {
            feedbackBtn.style.display = 'inline-flex';
            feedbackBtn.style.pointerEvents = 'auto';
            feedbackBtn.style.opacity = '1';
            feedbackBtn.className = 'ds-btn ds-btn-primary';
            feedbackBtn.href = `/account/orders/${order.orderCode}/feedback`;
            feedbackBtn.innerHTML = '<i class="fa fa-star" aria-hidden="true"></i> Đánh giá sản phẩm';
        } else {
            feedbackBtn.style.display = 'none';
        }
    }

    const complaintBtn = document.getElementById('orderComplaintButton');
    if (complaintBtn) {
        if (order.status === 'DISPUTED') {
            complaintBtn.setAttribute('disabled', 'true');
            complaintBtn.style.opacity = '0.6';
            complaintBtn.style.pointerEvents = 'none';
            complaintBtn.className = 'ds-btn ds-btn-danger';
            complaintBtn.innerHTML = '<i class="fa fa-shield"></i> Đang tranh chấp';
        } else if (['CANCELLED', 'REFUNDED'].includes(order.status)) {
            complaintBtn.style.display = 'none';
        } else {
            complaintBtn.removeAttribute('disabled');
            complaintBtn.style.opacity = '1';
            complaintBtn.style.pointerEvents = 'auto';
            complaintBtn.style.display = 'inline-flex';
            complaintBtn.className = 'ds-btn ds-btn-danger';
            complaintBtn.innerHTML = '<i class="fa fa-gavel"></i> Khiếu nại';
        }
    }

    renderTimeline(order);
}

function showNotFound(orderCode) {
    document.getElementById('orderDetailCode').textContent = orderCode || 'Không xác định';
    document.getElementById('orderDetailOverview').hidden = true;
    document.getElementById('orderDetailContent').hidden = true;
    document.getElementById('orderTimeline').hidden = true;
    document.getElementById('orderDetailEmpty').hidden = false;
    showOrderDetailMessage('Không tìm thấy đơn hàng này.', 'warning');
}

function renderTimeline(order) {
    const activeSteps = getActiveSteps(order);

    document.querySelectorAll('[data-order-step]').forEach(step => {
        const stepName = step.dataset.orderStep;

        // Reset classes
        step.classList.remove('order-timeline-item--active', 'order-timeline-item--disputed', 'order-timeline-item--failed');

        if (stepName === 'completed') {
            const titleEl = step.querySelector('strong');
            const descEl = step.querySelector('p');
            const status = (order.status || '').toUpperCase();

            if (status === 'DISPUTED') {
                step.classList.add('order-timeline-item--disputed');
                if (titleEl) titleEl.textContent = 'Tranh chấp / Khiếu nại';
                if (descEl) descEl.textContent = 'Đang trong quá trình xử lý khiếu nại.';
            } else if (status === 'REFUNDED') {
                step.classList.add('order-timeline-item--failed');
                if (titleEl) titleEl.textContent = 'Đã hoàn tiền';
                if (descEl) descEl.textContent = 'Đơn hàng đã được hoàn tiền cho người mua.';
            } else if (status === 'CANCELLED') {
                step.classList.add('order-timeline-item--failed');
                if (titleEl) titleEl.textContent = 'Đã hủy đơn';
                if (descEl) descEl.textContent = 'Đơn hàng đã bị hủy bỏ.';
            } else if (status === 'COMPLETED') {
                step.classList.add('order-timeline-item--active');
                if (titleEl) titleEl.textContent = 'Hoàn tất';
                if (descEl) descEl.textContent = 'Đơn hàng đã hoàn tất thành công.';
            } else {
                // Các trạng thái khác (PENDING, PAID, DELIVERED, HELD)
                if (titleEl) titleEl.textContent = 'Hoàn tất / tranh chấp';
                if (descEl) descEl.textContent = 'Kết thúc đơn hoặc mở xử lý khiếu nại.';
            }
        } else {
            // Các bước 1, 2, 3
            step.classList.toggle('order-timeline-item--active', activeSteps.includes(stepName));
        }
    });
}

function getActiveSteps(order) {
    const steps = ['created'];
    const status = (order.status || '').toUpperCase();
    const payStatus = (order.paymentStatus || '').toUpperCase();

    // Bước 2 (Thanh toán): Đã thanh toán hoặc đang giữ tiền
    if (['HELD', 'COMPLETED', 'DISPUTED', 'REFUNDED', 'PAID'].includes(status) || payStatus === 'PAID') {
        steps.push('paid');
    }

    // Bước 3 (Seller giao hàng): Khi đã ở trạng thái tạm giữ bảo lãnh (HELD) hoặc các trạng thái sau đó
    if (['HELD', 'COMPLETED', 'DISPUTED', 'REFUNDED'].includes(status)) {
        steps.push('delivered');
    }

    // Bước 4 (Hoàn tất): Đơn hàng hoàn tất giải ngân thành công
    if (status === 'COMPLETED') {
        steps.push('completed');
    }
    return steps;
}

function createAccessInfo(order) {
    if (order.status === 'PENDING') return '<div class="cred-status-msg cred-status-pending"><i class="fa fa-clock-o"></i> Đơn hàng đang chờ xử lý, thông tin nhận hàng chưa sẵn sàng.</div>';
    if (order.status === 'CANCELLED') return '<div class="cred-status-msg cred-status-danger"><i class="fa fa-times-circle"></i> Đơn hàng đã hủy, không có thông tin nhận hàng.</div>';
    if (order.status === 'DISPUTED') return '<div class="cred-status-msg cred-status-warning"><i class="fa fa-shield"></i> Thông tin nhận hàng đang được giữ để xử lý tranh chấp.</div>';

    let credsList = order.credentialsList;
    let creds = order.credentials;

    if (credsList && credsList.length > 0) {
        if (credsList.length === 1) {
            const singleCred = credsList[0];
            const isKeyOnly = singleCred.password === '(Product Key)';
            return renderSingleCredentialCard(singleCred, isKeyOnly);
        }
        return renderMultipleCredentialsTable(credsList);
    }

    if (creds) {
        const isKeyOnly = creds.password === '(Product Key)';
        return renderSingleCredentialCard(creds, isKeyOnly);
    }

    return '<div class="cred-status-msg"><i class="fa fa-info-circle"></i> Thông tin nhận hàng sẽ được hiển thị tại đây khi sản phẩm được giao thành công.</div>';
}

/**
 * Tạo giao diện hiển thị cho một tài khoản (Credential) duy nhất dưới dạng Card.
 * @param {Object} creds - Đối tượng chứa thông tin tài khoản (username, password, note)
 * @param {boolean} isKeyOnly - Biểu thị đây là dạng Product Key hay dạng Tài khoản/Mật khẩu
 * @returns {string} - Mã HTML của Card hiển thị tài khoản
 */
function renderSingleCredentialCard(creds, isKeyOnly) {
    return `
        <div class="cred-card">
            <div class="cred-card__header">
                <span class="cred-card__icon"><i class="fa fa-key"></i></span>
                <span class="cred-card__title">Thông tin đăng nhập</span>
                <span class="cred-card__badge">Bảo mật</span>
            </div>

            <div class="cred-field">
                <span class="cred-field__label">${isKeyOnly ? 'Mã kích hoạt (Key):' : 'Tài khoản (Email/Username):'}</span>
                <div class="cred-field__row">
                    <code class="cred-field__value" id="credUsername">${escapeHtml(creds.username)}</code>
                    <button class="cred-copy-btn" onclick="copyToClipboard('${escapeHtml(creds.username).replace(/'/g, '&#039;')}', '${isKeyOnly ? 'M\u00e3 k\u00edch ho\u1ea1t' : 'T\u00e0i kho\u1ea3n'}', this)" title="Sao chép">
                        <i class="fa fa-copy"></i><span>Copy</span>
                    </button>
                </div>
            </div>

            ${isKeyOnly ? '' : `
            <div class="cred-field">
                <span class="cred-field__label">Mật khẩu:</span>
                <div class="cred-field__row">
                    <code class="cred-field__value" id="credPassword">${escapeHtml(creds.password)}</code>
                    <button class="cred-copy-btn" onclick="copyToClipboard('${escapeHtml(creds.password).replace(/'/g, '&#039;')}', 'M\u1eadt kh\u1ea9u', this)" title="Sao chép">
                        <i class="fa fa-copy"></i><span>Copy</span>
                    </button>
                </div>
            </div>
            `}

            ${creds.note ? `
            <div class="cred-field">
                <span class="cred-field__label">Ghi chú:</span>
                <div class="cred-field__row">
                    <code class="cred-field__value" id="credNote">${escapeHtml(creds.note)}</code>
                    <button class="cred-copy-btn" onclick="copyToClipboard('${escapeHtml(creds.note).replace(/'/g, '&#039;')}', 'Ghi ch\u00fa', this)" title="Sao chép">
                        <i class="fa fa-copy"></i><span>Copy</span>
                    </button>
                </div>
            </div>
            ` : ''}

            <div class="cred-card__warning">
                <i class="fa fa-exclamation-triangle"></i>
                Vui lòng không thay đổi mật khẩu hoặc thông tin bảo mật để tránh ảnh hưởng đến thời gian bảo hành.
            </div>
        </div>
    `;
}

let activeCredsList = [];
let renderedCredsCount = 0;
const CREDS_CHUNK_SIZE = 10;

/**
 * Chức năng xuất toàn bộ danh sách tài khoản (Credentials) ra file Excel (.xls).
 * Hữu ích khi người dùng mua số lượng lớn tài khoản.
 */
window.exportCredentialsToExcel = function () {
    if (!activeCredsList || activeCredsList.length === 0) return;

    const isKeyOnly = activeCredsList[0].password === '(Product Key)';
    let html = `
        <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
        <head>
            <!--[if gte mso 9]>
            <xml>
                <x:ExcelWorkbook>
                    <x:ExcelWorksheets>
                        <x:ExcelWorksheet>
                            <x:Name>MMO_Market_Credentials</x:Name>
                            <x:WorksheetOptions>
                                <x:DisplayGridlines/>
                            </x:WorksheetOptions>
                        </x:ExcelWorksheet>
                    </x:ExcelWorksheets>
                </x:ExcelWorkbook>
            </xml>
            <![endif]-->
            <meta http-equiv="content-type" content="text/plain; charset=UTF-8"/>
        </head>
        <body>
            <table border="1">
                <thead>
                    <tr style="background-color: #f2f2f2; font-weight: bold;">
                        <th>STT</th>
                        <th>${isKeyOnly ? 'Mã kích hoạt (Key)' : 'Tài khoản (Username)'}</th>
                        ${isKeyOnly ? '' : '<th>Mật khẩu</th>'}
                        <th>Ghi chú</th>
                    </tr>
                </thead>
                <tbody>
    `;

    activeCredsList.forEach((c, idx) => {
        html += `
            <tr>
                <td>${idx + 1}</td>
                <td>${escapeHtml(c.username)}</td>
                ${isKeyOnly ? '' : `<td>${escapeHtml(c.password)}</td>`}
                <td>${escapeHtml(c.note || '')}</td>
            </tr>
        `;
    });

    html += `
                </tbody>
            </table>
        </body>
        </html>
    `;

    const blob = new Blob([html], { type: 'application/vnd.ms-excel;charset=utf-8;' });
    const link = document.createElement('a');
    const orderCode = currentOrder ? currentOrder.orderCode : 'MMO-ORDER';
    link.href = URL.createObjectURL(blob);
    link.setAttribute('download', `${orderCode}_tai_khoan.xls`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    if (typeof showSuccessToast === 'function') {
        showSuccessToast('Đã xuất file excel thành công.');
    } else {
        alert('Đã xuất file excel thành công.');
    }
};

/**
 * Tải thêm (Lazy load) một số lượng tài khoản nhất định vào bảng HTML (Table).
 * Chống đơ trình duyệt khi hiển thị hàng ngàn tài khoản cùng lúc.
 */
window.loadMoreCredentials = function () {
    const container = document.getElementById('credTableContainer');
    if (!container) return;
    const tbody = container.querySelector('tbody');
    if (!tbody) return;

    const total = activeCredsList.length;
    if (renderedCredsCount >= total) return;

    const isKeyOnly = activeCredsList[0].password === '(Product Key)';
    const nextBatch = activeCredsList.slice(renderedCredsCount, renderedCredsCount + CREDS_CHUNK_SIZE);

    let newRowsHtml = nextBatch.map((c, index) => {
        const idx = renderedCredsCount + index;
        let usernameCopyVal = escapeHtml(c.username).replace(/'/g, '&#039;');
        let passwordCopyVal = escapeHtml(c.password).replace(/'/g, '&#039;');

        let actionsHtml = '';
        if (isKeyOnly) {
            actionsHtml = `
                <button class="ds-btn ds-btn-text" style="padding: 4px 8px; font-size: 13px;" onclick="copyToClipboard('${usernameCopyVal}', 'M\u00e3 k\u00edch ho\u1ea1t', this)">
                    <i class="fa fa-copy"></i> Copy Key
                </button>
            `;
        } else {
            actionsHtml = `
                <button class="ds-btn ds-btn-text" style="padding: 4px 8px; font-size: 13px; margin-right: 6px;" onclick="copyToClipboard('${usernameCopyVal}', 'T\u00e0i kho\u1ea1t', this)" title="Copy tài khoản">
                    <i class="fa fa-user"></i> Copy User
                </button>
                <button class="ds-btn ds-btn-text" style="padding: 4px 8px; font-size: 13px;" onclick="copyToClipboard('${passwordCopyVal}', 'M\u1eadt kh\u1ea9u', this)" title="Copy mật khẩu">
                    <i class="fa fa-lock"></i> Copy Pass
                </button>
            `;
        }

        return `
            <tr style="border-bottom: 1px solid #f1f5f9;">
                <td style="text-align: center; font-weight: 600; color: #64748b; padding: 12px 10px;">${idx + 1}</td>
                <td style="font-family: monospace; font-weight: 600; color: #0f172a; word-break: break-all; padding: 12px 10px;">${escapeHtml(c.username)}</td>
                ${isKeyOnly ? '' : `<td style="font-family: monospace; font-weight: 600; color: #0f172a; word-break: break-all; padding: 12px 10px;">${escapeHtml(c.password)}</td>`}
                <td style="font-size: 13px; color: #475569; padding: 12px 10px; max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${escapeHtml(c.note || '')}">${escapeHtml(c.note || '-')}</td>
                <td style="text-align: center; padding: 12px 10px; white-space: nowrap;">
                    ${actionsHtml}
                </td>
            </tr>
        `;
    }).join('');

    tbody.insertAdjacentHTML('beforeend', newRowsHtml);
    renderedCredsCount += nextBatch.length;
};

/**
 * Tạo giao diện hiển thị cho nhiều tài khoản dưới dạng Bảng (Table).
 * Kèm theo các chức năng tiện ích như: Copy tất cả, Xuất file Excel, Cuộn để tải thêm (Infinite Scroll).
 * @param {Array} credsList - Danh sách các đối tượng tài khoản
 * @returns {string} - Mã HTML của Bảng hiển thị danh sách tài khoản
 */
function renderMultipleCredentialsTable(credsList) {
    activeCredsList = credsList;
    renderedCredsCount = 0;
    const isKeyOnly = credsList[0].password === '(Product Key)';

    let copyAllText = '';
    credsList.forEach((c) => {
        if (isKeyOnly) {
            copyAllText += `${c.username}\n`;
        } else {
            copyAllText += `${c.username} | ${c.password}${c.note ? ' | ' + c.note : ''}\n`;
        }
    });
    copyAllText = copyAllText.trim();

    setTimeout(() => {
        window.loadMoreCredentials();

        const container = document.getElementById('credTableContainer');
        if (container) {
            container.addEventListener('scroll', () => {
                if (container.scrollHeight - container.scrollTop - container.clientHeight < 50) {
                    window.loadMoreCredentials();
                }
            });
        }
    }, 50);

    return `
        <div class="cred-card" style="border-color: #2563eb; box-shadow: 0 4px 20px rgba(37,99,235,0.12);">
            <div class="cred-card__header" style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px;">
                <div style="display: flex; align-items: center; gap: 10px;">
                    <span class="cred-card__icon"><i class="fa fa-list"></i></span>
                    <div>
                        <span class="cred-card__title" style="display: block;">Danh sách tài khoản đã mua (${credsList.length})</span>
                    </div>
                </div>
                <div style="display: flex; gap: 8px;">
                    <button class="ds-btn ds-btn-secondary" style="padding: 6px 12px; font-size: 12.5px; border-color: rgba(255,255,255,0.4); background: rgba(255,255,255,0.15); color: #fff; border-radius: 6px;" onclick="copyToClipboard('${copyAllText.replace(/'/g, '&#039;').replace(/\n/g, '\\n')}', 'T\u1ea5t c\u1ea3 tài khoản', this)">
                        <i class="fa fa-clone"></i> Sao chép tất cả
                    </button>
                    <button class="ds-btn ds-btn-secondary" style="padding: 6px 12px; font-size: 12.5px; border-color: rgba(255,255,255,0.4); background: rgba(255,255,255,0.15); color: #fff; border-radius: 6px;" onclick="window.exportCredentialsToExcel()">
                        <i class="fa fa-file-excel-o"></i> Xuất Excel
                    </button>
                </div>
            </div>
            
            <div id="credTableContainer" style="overflow-x: auto; max-height: 220px; overflow-y: auto; width: 100%; padding: 0; border-bottom: 1px solid #e2e8f0;">
                <table style="width: 100%; border-collapse: collapse; margin: 0; font-size: 13.5px; border-spacing: 0;">
                    <thead style="position: sticky; top: 0; z-index: 10; background: #f8fafc; box-shadow: 0 1px 0 #e2e8f0;">
                        <tr style="text-align: left;">
                            <th style="width: 50px; text-align: center; padding: 10px 10px; font-weight: 700; color: #475569; font-size: 11px; text-transform: uppercase; border-bottom: 1px solid #e2e8f0;">STT</th>
                            <th style="padding: 10px 10px; font-weight: 700; color: #475569; font-size: 11px; text-transform: uppercase; border-bottom: 1px solid #e2e8f0;">${isKeyOnly ? 'Mã kích hoạt (Key)' : 'Tài khoản (Username)'}</th>
                            ${isKeyOnly ? '' : `<th style="padding: 10px 10px; font-weight: 700; color: #475569; font-size: 11px; text-transform: uppercase; border-bottom: 1px solid #e2e8f0;">Mật khẩu</th>`}
                            <th style="padding: 10px 10px; font-weight: 700; color: #475569; font-size: 11px; text-transform: uppercase; border-bottom: 1px solid #e2e8f0;">Ghi chú</th>
                            <th style="width: 150px; text-align: center; padding: 10px 10px; font-weight: 700; color: #475569; font-size: 11px; text-transform: uppercase; border-bottom: 1px solid #e2e8f0;">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody style="background: #fff;">
                        <!-- Rows rendered dynamically by infinite scroll -->
                    </tbody>
                </table>
            </div>

            <div class="cred-card__warning" style="border-top: 1px solid #e2e8f0;">
                <i class="fa fa-exclamation-triangle"></i>
                Vui lòng không thay đổi thông tin bảo mật để tránh ảnh hưởng đến thời gian bảo hành sản phẩm.
            </div>
        </div>
    `;
}

function getActionHint(order) {
    if (order.status === 'COMPLETED') return 'Đơn hàng đã hoàn tất. Bạn có thể xem lại thông tin mua hàng.';
    if (order.status === 'DISPUTED') return 'Đơn hàng đang trong trạng thái tranh chấp.';
    if (order.status === 'CANCELLED') return 'Đơn hàng đã hủy, không còn thao tác xử lý.';
    return 'Bạn có thể theo dõi đơn hoặc gửi khiếu nại khi cần.';
}

function setBadge(elementId, text, badgeClass) {
    const element = document.getElementById(elementId);
    element.textContent = text;
    element.className = `ds-badge ${badgeClass}`;
}

function formatOrderStatus(status) {
    if (!status) return '-';
    const upperStatus = status.toUpperCase().trim();
    const map = {
        PENDING: 'Chờ xử lý',
        HELD: 'Tạm giữ',
        PAID: 'Đã thanh toán',
        DELIVERED: 'Đã giao',
        COMPLETED: 'Hoàn tất',
        CANCELLED: 'Đã hủy',
        DISPUTED: 'Tranh chấp',
        REFUNDED: 'Đã hoàn tiền'
    };
    return map[upperStatus] || status || '-';
}

function getOrderStatusBadgeClass(status) {
    if (!status) return 'ds-badge-muted';
    const upperStatus = status.toUpperCase().trim();
    if (upperStatus === 'COMPLETED' || upperStatus === 'DELIVERED') return 'ds-badge-success';
    if (upperStatus === 'DISPUTED' || upperStatus === 'CANCELLED') return 'ds-badge-danger';
    if (upperStatus === 'PENDING' || upperStatus === 'PAID') return 'ds-badge-warning';
    if (upperStatus === 'HELD' || upperStatus === 'REFUNDED') return 'ds-badge-info';
    return 'ds-badge-muted';
}

function formatPaymentStatus(status) {
    if (!status) return '-';
    const upperStatus = status.toUpperCase().trim();
    const map = {
        PAID: 'Đã thanh toán',
        FAILED: 'Thất bại',
        REFUNDED: 'Đã hoàn tiền'
    };
    return map[upperStatus] || status || '-';
}

function getPaymentBadgeClass(status) {
    if (!status) return 'ds-badge-muted';
    const upperStatus = status.toUpperCase().trim();
    if (upperStatus === 'PAID') return 'ds-badge-success';
    if (upperStatus === 'PENDING') return 'ds-badge-warning';
    if (upperStatus === 'FAILED') return 'ds-badge-danger';
    if (upperStatus === 'REFUNDED') return 'ds-badge-info';
    return 'ds-badge-muted';
}

function addDays(date, days) {
    const next = new Date(date);
    next.setDate(next.getDate() + days);
    return next;
}

function formatDateTime(date) {
    return date.toLocaleString('vi-VN');
}

function formatDate(date) {
    return date.toLocaleDateString('vi-VN');
}

function formatMoney(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(amount) || 0);
}

function showOrderDetailMessage(message, type) {
    const messageElement = document.getElementById('orderDetailMessage');
    messageElement.textContent = message;
    messageElement.hidden = false;
    messageElement.classList.remove('ds-alert-info', 'ds-alert-warning', 'ds-alert-danger', 'ds-alert-success');
    messageElement.classList.add(`ds-alert-${type}`);
}

window.copyToClipboard = async function (text, label, btn) {
    try {
        await navigator.clipboard.writeText(text);
        // Animate button to show success
        if (btn) {
            const icon = btn.querySelector('i');
            const span = btn.querySelector('span');
            btn.classList.add('cred-copy-btn--success');
            if (icon) icon.className = 'fa fa-check';
            if (span) span.textContent = 'Đã copy!';
            setTimeout(() => {
                btn.classList.remove('cred-copy-btn--success');
                if (icon) icon.className = 'fa fa-copy';
                if (span) span.textContent = 'Copy';
            }, 2000);
        }
        showSuccessToast(`Đã sao chép ${label} vào bộ nhớ tạm.`);
    } catch {
        showWarningToast('Không thể copy tự động. Vui lòng chọn và sao chép thủ công.');
    }
};

window.toggleCredVisibility = function (elementId, actualValue, btn) {
    const element = document.getElementById(elementId);
    if (!element) return;
    const icon = btn.querySelector('i');
    const isHidden = element.textContent === '••••••••••••';
    
    if (isHidden) {
        element.textContent = actualValue;
        if (icon) {
            icon.className = 'fa fa-eye-slash';
        }
    } else {
        element.textContent = '••••••••••••';
        if (icon) {
            icon.className = 'fa fa-eye';
        }
    }
};

function showSuccessToast(message) {
    let container = document.querySelector('.ds-toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'ds-toast-container';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = 'ds-toast ds-toast-success';
    toast.innerHTML = `
        <div class="ds-toast-icon"><i class="fa fa-check-circle"></i></div>
        <div class="ds-toast-content">
            <h4 class="ds-toast-title">Thành công</h4>
            <p class="ds-toast-message">${message}</p>
        </div>
        <button class="ds-toast-close" onclick="this.parentElement.remove()">&times;</button>
    `;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity 0.5s ease';
        setTimeout(() => {
            toast.remove();
        }, 500);
    }, 3200);
}

function escapeHtml(value) {
    return String(value || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

let chatIntervalId = null;

async function checkAndLoadDisputeChat(transactionId) {
    const token = sessionStorage.getItem('accessToken');
    if (!token) return;
    try {
        const res = await fetch('/api/complaints', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (!res.ok) return;
        const list = await res.json();
        const complaint = list.find(c => c.transaction && Number(c.transaction.id) === Number(transactionId));
        if (complaint) {
            const statusVal = (complaint.status || '').toLowerCase();
            if (statusVal === 'in_progress' || statusVal === 'inprogress' || statusVal === 'resolved' || statusVal === 'rejected' || statusVal === 'completed') {
                const chatCard = document.getElementById('customer-dispute-chat-card');
                if (chatCard) chatCard.style.display = 'block';

                loadCustomerDisputeChats(complaint.id);

                if (chatIntervalId) clearInterval(chatIntervalId);
                if (statusVal === 'in_progress' || statusVal === 'inprogress') {
                    chatIntervalId = setInterval(() => loadCustomerDisputeChats(complaint.id), 5000);
                }

                const chatForm = document.getElementById('customer-dispute-chat-form');
                if (chatForm) {
                    const newForm = chatForm.cloneNode(true);
                    chatForm.parentNode.replaceChild(newForm, chatForm);

                    newForm.addEventListener('submit', async (e) => {
                        e.preventDefault();
                        const inputEl = document.getElementById('customer-dispute-chat-input');
                        const text = inputEl.value.trim();
                        if (!text) return;

                        try {
                            const sendRes = await fetch(`/api/complaints/${complaint.id}/chats`, {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json',
                                    'Authorization': `Bearer ${token}`
                                },
                                body: JSON.stringify({ message: text })
                            });
                            if (!sendRes.ok) {
                                const errData = await sendRes.json();
                                throw new Error(errData.message || 'Lỗi gửi tin nhắn.');
                            }
                            inputEl.value = '';
                            loadCustomerDisputeChats(complaint.id);
                        } catch (err) {
                            showErrorToast(err.message);
                        }
                    });
                }
            }
        }
    } catch (err) {
        console.error('Error checking dispute chat:', err);
    }
}

async function loadCustomerDisputeChats(complaintId) {
    const token = sessionStorage.getItem('accessToken');
    if (!token) return;
    try {
        const res = await fetch(`/api/complaints/${complaintId}/chats`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (!res.ok) return;
        const chats = await res.json();
        const container = document.getElementById('customer-dispute-chat-messages');
        if (!container) return;

        if (chats.length === 0) {
            container.innerHTML = `<div style="text-align: center; color: #94a3b8; font-size: 13px; font-style: italic;">Chưa có tin nhắn đối chất nào.</div>`;
            return;
        }

        container.innerHTML = chats.map(msg => {
            if (msg.message && msg.message.startsWith('Hệ thống:')) {
                return `
                    <div style="text-align: center; margin: 12px 0; width: 100%; box-sizing: border-box; display: flex; align-items: center; justify-content: center; gap: 8px;">
                        <div style="width: 24px; height: 24px; border-radius: 50%; background: #64748b; color: #fff; display: inline-flex; align-items: center; justify-content: center; font-size: 11px;"><i class="fa fa-cog"></i></div>
                        <span style="font-size: 12px; background: #f1f5f9; color: #334155; padding: 6px 14px; border-radius: 6px; border: 1px solid #cbd5e1; display: inline-block; font-weight: 500; text-align: left;">
                            ${escapeHtml(msg.message)}
                        </span>
                    </div>
                `;
            }

            let roleLabel = 'Khách hàng';
            let bg = 'rgba(37, 99, 235, 0.08)';
            let border = '1px solid rgba(37, 99, 235, 0.15)';
            let titleColor = '#2563eb';
            let avatarBg = '#2563eb';
            let initials = msg.senderName ? msg.senderName.trim().split(/\s+/).map(w => w[0]).join('').substring(0, 2).toUpperCase() : 'KH';
            let avatarContent = escapeHtml(initials);

            if (msg.senderRole === 'Seller') {
                roleLabel = 'Người bán';
                bg = 'rgba(217, 119, 6, 0.08)';
                border = '1px solid rgba(217, 119, 6, 0.15)';
                titleColor = '#d97706';
                avatarBg = '#d97706';
                initials = msg.senderName ? msg.senderName.trim().split(/\s+/).map(w => w[0]).join('').substring(0, 2).toUpperCase() : 'NB';
                avatarContent = escapeHtml(initials);
            } else if (msg.senderRole === 'Staff' || msg.senderRole === 'Admin') {
                roleLabel = 'Nhân viên hỗ trợ';
                bg = 'rgba(124, 58, 237, 0.08)';
                border = '1px solid rgba(124, 58, 237, 0.15)';
                titleColor = '#7c3aed';
                avatarBg = '#7c3aed';
                avatarContent = '<i class="fa fa-user-shield"></i>';
            }

            return `
                <div style="display: flex; gap: 10px; align-items: flex-start; background: ${bg}; border: ${border}; border-radius: 8px; padding: 12px; font-size: 13px; line-height: 1.5; text-align: left;">
                    <div style="width: 32px; height: 32px; border-radius: 50%; background: ${avatarBg}; color: #ffffff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 11px; flex-shrink: 0; margin-top: 2px;" title="${escapeHtml(msg.senderName)} (${roleLabel})">
                        ${avatarContent}
                    </div>
                    <div style="flex: 1; min-width: 0;">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                            <span style="font-weight: 700; color: ${titleColor};">${escapeHtml(msg.senderName)} <small style="font-weight: 500; opacity: 0.85;">(${roleLabel})</small></span>
                            <small style="color: #64748b; font-size: 11px;">${msg.createdAt ? msg.createdAt.replace('T', ' ').substring(0, 16) : ''}</small>
                        </div>
                        <div style="color: #1e293b; white-space: pre-wrap;">${escapeHtml(msg.message)}</div>
                    </div>
                </div>
            `;
        }).join('');

        container.scrollTop = container.scrollHeight;
    } catch (err) {
        console.error('Error rendering dispute chats for customer:', err);
    }
}
