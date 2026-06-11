// ==============================================================================
// SELLER CONSOLE JS
// File: seller-console.js
// Description: Controls all dynamic operations and REST API data binding
//              for the Seller dashboard pages.
// ==============================================================================

const SELLER_API_BASE = 'http://localhost:8080/api/seller';

document.addEventListener('DOMContentLoaded', () => {
    // 1. Guard check for authentication
    const token = sessionStorage.getItem('accessToken');
    if (!token || token === 'null' || token === 'undefined') {
        sessionStorage.setItem('redirectPath', window.location.pathname + window.location.search);
        window.location.href = '/login';
        return;
    }

    // 2. Initialize layout / sidebar stats
    initSellerLayout();

    // 3. Page-specific routing
    const path = window.location.pathname;
    if (path.endsWith('/seller') || path.endsWith('/seller/') || path.endsWith('/dashboard')) {
        initDashboard();
    } else if (path.endsWith('/shop-info')) {
        initShopInfo();
    } else if (path.endsWith('/inventory')) {
        initInventory();
    } else if (path.endsWith('/products/new')) {
        initProductAdd();
    } else if (path.endsWith('/products/edit')) {
        initProductEdit();
    } else if (path.endsWith('/variants/new') || path.endsWith('/variants/edit')) {
        initVariantForm();
    } else if (path.endsWith('/transactions')) {
        initTransactions();
    } else if (path.endsWith('/withdrawals')) {
        initWithdrawals();
    } else if (path.endsWith('/withdrawal-detail') || path.endsWith('/withdrawals/detail')) {
        initWithdrawalDetail();
    } else if (path.endsWith('/statistics')) {
        initStatistics();
    } else if (path.endsWith('/shop-flags')) {
        initShopFlags();
    } else if (path.endsWith('/reviews')) {
        initReviews();
    } else if (path.endsWith('/complaints')) {
        initComplaints();
    } else if (path.endsWith('/complaint-detail') || path.endsWith('/complaints/detail')) {
        initComplaintDetail();
    }
});

// ==============================================================================
// HELPERS
// ==============================================================================
function formatVND(value) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value) || 0);
}

async function sellerFetch(url, options = {}) {
    const token = sessionStorage.getItem('accessToken');
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...options.headers
    };

    const response = await fetch(`${SELLER_API_BASE}${url}`, { ...options, headers });
    if (response.status === 401 || response.status === 403) {
        sessionStorage.clear();
        window.location.href = '/login';
        throw new Error('Phiên làm việc hết hạn. Vui lòng đăng nhập lại.');
    }
    return response;
}

// Show feedback message
function showToast(message, type = 'success') {
    // Create element if not exists
    let toast = document.getElementById('seller-toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'seller-toast';
        toast.style.cssText = `
            position: fixed;
            bottom: 24px;
            right: 24px;
            padding: 14px 24px;
            border-radius: 8px;
            color: #fff;
            font-weight: 600;
            z-index: 9999;
            box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1);
            transition: opacity 0.3s;
        `;
        document.body.appendChild(toast);
    }
    toast.style.backgroundColor = type === 'success' ? '#22c55e' : '#ef4444';
    toast.textContent = message;
    toast.style.opacity = '1';
    setTimeout(() => {
        toast.style.opacity = '0';
    }, 3000);
}

// ==============================================================================
// 1. GENERAL LAYOUT & SIDEBAR
// ==============================================================================
async function initSellerLayout() {
    try {
        const res = await sellerFetch('/shop-info');
        if (!res.ok) return;
        const data = await res.json();

        // Update sidebar
        const nameEl = document.querySelector('.seller-sidebar__name');
        const statusEl = document.querySelector('.seller-sidebar__status');
        const avatarEl = document.querySelector('.seller-sidebar__avatar');

        if (nameEl) nameEl.textContent = data.shopName || 'Cửa hàng của tôi';
        if (statusEl) statusEl.textContent = `Trạng thái: ${data.shopStatus || 'Active'}`;
        if (avatarEl && data.shopName) {
            avatarEl.textContent = data.shopName.charAt(0).toUpperCase();
        }

        // Fetch dashboard to update balance
        const dashRes = await sellerFetch('/dashboard');
        if (dashRes.ok) {
            const dashData = await dashRes.json();
            const balanceEl = document.querySelector('.seller-sidebar__balance');
            if (balanceEl) balanceEl.textContent = formatVND(dashData.balanceVnd);
        }
    } catch (err) {
        console.error('Lỗi khởi tạo layout người bán:', err);
    }
}

// ==============================================================================
// 2. DASHBOARD VIEW
// ==============================================================================
async function initDashboard() {
    const statsGrid = document.querySelector('.stat-grid');
    if (!statsGrid) return;

    try {
        const res = await sellerFetch('/dashboard');
        if (!res.ok) throw new Error('Không thể tải số liệu thống kê.');
        const data = await res.json();

        // Bind stats card values
        const cards = statsGrid.querySelectorAll('.stat-card__value');
        if (cards.length >= 4) {
            cards[0].textContent = formatVND(data.totalRevenue);
            cards[1].textContent = data.completedSales;
            cards[2].textContent = data.activeProductsCount;
            cards[3].textContent = data.openComplaintsCount;
        }

        // Bind recent orders/transactions table
        const tbody = document.querySelector('.seller-table tbody');
        if (tbody) {
            if (!data.recentTransactions || data.recentTransactions.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;">Chưa có đơn hàng nào gần đây.</td></tr>';
                return;
            }

            tbody.innerHTML = data.recentTransactions.map(t => {
                let badgeClass = 'pending';
                if (t.status === 'Completed') badgeClass = 'ok';
                else if (t.status === 'Held') badgeClass = 'held';

                return `
                    <tr>
                        <td>#TX-${t.id}</td>
                        <td>${t.productName}</td>
                        <td>${t.customerEmail}</td>
                        <td class="text-right">${formatVND(t.amountVnd)}</td>
                        <td><span class="badge ${badgeClass}">${t.status}</span></td>
                    </tr>
                `;
            }).join('');
        }
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==============================================================================
// 3. SHOP INFO
// ==============================================================================
async function initShopInfo() {
    const form = document.querySelector('.seller-form-panel');
    if (!form) return;

    try {
        const res = await sellerFetch('/shop-info');
        if (!res.ok) throw new Error('Không thể tải thông tin cửa hàng.');
        const data = await res.json();

        // Populate fields
        document.getElementById('shopName').value = data.shopName || '';
        document.getElementById('shopDesc').value = data.description || '';
        document.getElementById('bankName').value = data.bankName || '';
        document.getElementById('accountNumber').value = data.accountNumber || '';
        document.getElementById('accountHolder').value = data.accountHolder || '';
        document.getElementById('branch').value = data.branch || '';

        // Enable button
        const saveBtn = form.querySelector('.profile-button--primary');
        if (saveBtn) {
            saveBtn.removeAttribute('disabled');
            saveBtn.addEventListener('click', async () => {
                const payload = {
                    shopName: document.getElementById('shopName').value.trim(),
                    description: document.getElementById('shopDesc').value.trim(),
                    bankName: document.getElementById('bankName').value.trim(),
                    accountNumber: document.getElementById('accountNumber').value.trim(),
                    branch: document.getElementById('branch').value.trim()
                };

                try {
                    const putRes = await sellerFetch('/shop-info', {
                        method: 'PUT',
                        body: JSON.stringify(payload)
                    });
                    const putData = await putRes.json();
                    if (!putRes.ok) throw new Error(putData.message || 'Lưu thất bại.');
                    showToast(putData.message || 'Lưu thành công!');
                    initSellerLayout(); // refresh avatar/name
                } catch (err) {
                    showToast(err.message, 'error');
                }
            });
        }
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==============================================================================
// 4. INVENTORY & PRODUCT MANAGEMENT
// ==============================================================================
async function initInventory() {
    const tbody = document.querySelector('.seller-table tbody');
    if (!tbody) return;

    try {
        const res = await sellerFetch('/products');
        if (!res.ok) throw new Error('Không thể tải danh sách kho hàng.');
        const products = await res.json();

        if (products.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">Bạn chưa đăng sản phẩm nào.</td></tr>';
            return;
        }

        tbody.innerHTML = products.map(p => {
            const statusClass = p.status === 'Active' ? 'ok' : 'locked';
            return `
                <tr>
                    <td>
                        <div class="product-cell">
                            <div class="product-thumb"><i class="fa fa-shopping-bag"></i></div>
                            <div>
                                <strong>${p.name}</strong>
                                <div class="muted">ID #${p.id}</div>
                            </div>
                        </div>
                    </td>
                    <td>${p.categoryName}</td>
                    <td>${p.variantCount}</td>
                    <td class="text-right">${p.totalStock}</td>
                    <td><span class="badge ${statusClass}">${p.status}</span></td>
                    <td class="text-right">
                        <div class="row-actions">
                            <a class="icon-button" href="/seller/products/edit?id=${p.id}" title="Sửa sản phẩm"><i class="fa fa-pencil"></i></a>
                            <button class="icon-button danger btn-delete-product" data-id="${p.id}" title="Xóa sản phẩm"><i class="fa fa-trash"></i></button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        // Attach delete handlers
        tbody.querySelectorAll('.btn-delete-product').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.dataset.id;
                if (!confirm('Bạn có chắc chắn muốn xóa sản phẩm này cùng toàn bộ biến thể của nó không?')) return;
                try {
                    const delRes = await sellerFetch(`/products/${id}`, { method: 'DELETE' });
                    if (!delRes.ok) throw new Error('Không thể xóa sản phẩm.');
                    showToast('Đã xóa sản phẩm thành công!');
                    initInventory(); // Reload
                } catch (err) {
                    showToast(err.message, 'error');
                }
            });
        });

    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==============================================================================
// 5. PRODUCT ADD
// ==============================================================================
async function initProductAdd() {
    const select = document.getElementById('category');
    if (!select) return;

    try {
        // Load categories
        const res = await sellerFetch('/categories');
        if (res.ok) {
            const categories = await res.json();
            select.innerHTML = categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        }

        const form = document.querySelector('.profile-edit-form');
        const nextBtn = form.querySelector('.profile-button--primary');
        if (nextBtn) {
            // Replace link with post submit action
            nextBtn.outerHTML = `<button type="submit" class="profile-button profile-button--primary">Đăng sản phẩm &amp; Tiếp tục</button>`;
            
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                const name = document.getElementById('productName').value.trim();
                const description = document.getElementById('description').value.trim();
                const categoryId = select.value;

                if (!name) {
                    showToast('Vui lòng nhập tên sản phẩm.', 'error');
                    return;
                }

                try {
                    const postRes = await sellerFetch('/products', {
                        method: 'POST',
                        body: JSON.stringify({ name, description, categoryId })
                    });
                    const postData = await postRes.json();
                    if (!postRes.ok) throw new Error(postData.message || 'Đăng sản phẩm thất bại.');
                    showToast('Tạo sản phẩm thành công. Đang chuyển đến thêm biến thể...');
                    setTimeout(() => {
                        window.location.href = `/seller/variants/new?productId=${postData.id}`;
                    }, 1500);
                } catch (err) {
                    showToast(err.message, 'error');
                }
            });
        }
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==============================================================================
// 6. PRODUCT EDIT
// ==============================================================================
async function initProductEdit() {
    const urlParams = new URLSearchParams(window.location.search);
    const productId = urlParams.get('id');
    if (!productId) {
        window.location.href = '/seller/inventory';
        return;
    }

    const select = document.getElementById('category');
    const form = document.querySelector('.profile-edit-form');
    const tbody = document.querySelector('.seller-table tbody');
    if (!form || !tbody) return;

    try {
        // Load categories
        const catRes = await sellerFetch('/categories');
        if (catRes.ok) {
            const categories = await catRes.json();
            select.innerHTML = categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        }

        // Load Product Detail
        const pRes = await sellerFetch(`/products/${productId}`);
        if (!pRes.ok) throw new Error('Không thể tải thông tin sản phẩm.');
        const p = await pRes.json();

        // Populate fields
        document.querySelector('.seller-card__subtitle').textContent = `Sản phẩm #${p.id} — ${p.name}`;
        document.getElementById('productName').value = p.name || '';
        document.getElementById('description').value = p.description || '';
        select.value = p.categoryId;

        // Activate variants new link
        const addVarBtn = document.querySelector('a[href*="/seller/variants/new"]');
        if (addVarBtn) {
            addVarBtn.href = `/seller/variants/new?productId=${p.id}`;
        }

        // Render variants table
        if (!p.variants || p.variants.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;">Sản phẩm chưa có biến thể nào.</td></tr>';
        } else {
            tbody.innerHTML = p.variants.map(v => {
                const statusClass = v.status === 'Active' ? 'ok' : 'locked';
                return `
                    <tr>
                        <td>${v.variantName}</td>
                        <td class="text-right">${formatVND(v.priceVnd)}</td>
                        <td class="text-right">${v.stock}</td>
                        <td><span class="badge ${statusClass}">${v.status}</span></td>
                        <td class="text-right">
                            <div class="row-actions">
                                <a class="icon-button" href="/seller/variants/edit?id=${v.id}" title="Sửa biến thể"><i class="fa fa-pencil"></i></a>
                                <button class="icon-button danger btn-delete-variant" data-id="${v.id}" title="Xóa biến thể"><i class="fa fa-trash"></i></button>
                            </div>
                        </td>
                    </tr>
                `;
            }).join('');

            // Attach delete variant actions
            tbody.querySelectorAll('.btn-delete-variant').forEach(btn => {
                btn.addEventListener('click', async () => {
                    const varId = btn.dataset.id;
                    if (!confirm('Bạn có muốn xóa biến thể này không?')) return;
                    try {
                        const delRes = await sellerFetch(`/variants/${varId}`, { method: 'DELETE' });
                        if (!delRes.ok) throw new Error('Không thể xóa biến thể.');
                        showToast('Đã xóa biến thể thành công!');
                        initProductEdit(); // Reload page content
                    } catch (err) {
                        showToast(err.message, 'error');
                    }
                });
            });
        }

        // Handle Form Submit
        const saveBtn = form.querySelector('.profile-button--primary');
        if (saveBtn) {
            saveBtn.removeAttribute('disabled');
            saveBtn.addEventListener('click', async () => {
                const payload = {
                    name: document.getElementById('productName').value.trim(),
                    description: document.getElementById('description').value.trim(),
                    categoryId: select.value
                };

                try {
                    const putRes = await sellerFetch(`/products/${productId}`, {
                        method: 'PUT',
                        body: JSON.stringify(payload)
                    });
                    if (!putRes.ok) throw new Error('Cập nhật sản phẩm thất bại.');
                    showToast('Cập nhật sản phẩm thành công!');
                } catch (err) {
                    showToast(err.message, 'error');
                }
            });
        }

    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==============================================================
// 7. VARIANT FORM (ADD & EDIT)
// ==============================================================
async function initVariantForm() {
    const urlParams = new URLSearchParams(window.location.search);
    const form = document.querySelector('.profile-edit-form');
    if (!form) return;

    // Check Mode: Edit or Create
    const isEdit = urlParams.has('id');
    const variantId = urlParams.get('id');
    const productId = urlParams.get('productId');

    if (!isEdit && !productId) {
        window.location.href = '/seller/inventory';
        return;
    }

    try {
        let currentProdId = productId;

        if (isEdit) {
            // Load Variant Details
            const res = await sellerFetch(`/variants/${variantId}`);
            if (!res.ok) throw new Error('Không thể tải thông tin biến thể.');
            const v = await res.json();

            currentProdId = v.productId;
            document.querySelector('.seller-card__subtitle').textContent = `Sản phẩm: ${v.productName}`;
            document.getElementById('variantName').value = v.variantName;
            document.getElementById('priceVnd').value = v.priceVnd;
            document.getElementById('stock').value = v.stock;
            document.getElementById('status').value = v.status;
        } else {
            // Load Product Info to display
            const prodRes = await sellerFetch(`/products/${productId}`);
            if (prodRes.ok) {
                const p = await prodRes.json();
                document.querySelector('.seller-card__subtitle').textContent = `Sản phẩm: ${p.name}`;
            }
        }

        // Back link update
        const backBtn = document.querySelector('.seller-card__header .profile-button--secondary');
        const cancelBtn = form.querySelector('.profile-actions .profile-button--secondary');
        const backUrl = `/seller/products/edit?id=${currentProdId}`;
        if (backBtn) backBtn.href = backUrl;
        if (cancelBtn) cancelBtn.href = backUrl;

        // Submitting variant form
        const saveBtn = form.querySelector('.profile-button--primary');
        if (saveBtn) {
            saveBtn.removeAttribute('disabled');
            saveBtn.addEventListener('click', async () => {
                const payload = {
                    variantName: document.getElementById('variantName').value.trim(),
                    priceVnd: document.getElementById('priceVnd').value,
                    stock: document.getElementById('stock').value,
                    status: document.getElementById('status').value,
                    productId: currentProdId
                };

                if (!payload.variantName || !payload.priceVnd) {
                    showToast('Vui lòng nhập tên biến thể và giá bán.', 'error');
                    return;
                }

                try {
                    const method = isEdit ? 'PUT' : 'POST';
                    const endpoint = isEdit ? `/variants/${variantId}` : '/variants';

                    const actionRes = await sellerFetch(endpoint, {
                        method: method,
                        body: JSON.stringify(payload)
                    });
                    const actionData = await actionRes.json();
                    if (!actionRes.ok) throw new Error(actionData.message || 'Thao tác thất bại.');

                    showToast(isEdit ? 'Đã cập nhật biến thể!' : 'Đã tạo biến thể thành công!');
                    setTimeout(() => {
                        window.location.href = backUrl;
                    }, 1500);
                } catch (err) {
                    showToast(err.message, 'error');
                }
            });
        }

    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==============================================================
// 8. TRANSACTIONS VIEW (SALES HISTORY)
// ==============================================================
async function initTransactions() {
    const tbody = document.querySelector('.seller-table tbody');
    if (!tbody) return;

    try {
        const res = await sellerFetch('/transactions');
        if (!res.ok) throw new Error('Không thể tải lịch sử bán hàng.');
        const transactions = await res.json();

        if (transactions.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;">Chưa có lịch sử bán hàng.</td></tr>';
            return;
        }

        tbody.innerHTML = transactions.map(t => {
            let badgeClass = 'pending';
            if (t.status === 'Completed') badgeClass = 'ok';
            else if (t.status === 'Held') badgeClass = 'held';
            else if (t.status === 'Refunded' || t.status === 'Cancelled') badgeClass = 'locked';

            return `
                <tr>
                    <td>#TX-${t.id}</td>
                    <td>
                        <strong>${t.productName}</strong>
                        <div class="muted">${t.variantName}</div>
                    </td>
                    <td>${t.customerEmail}</td>
                    <td class="text-right">${formatVND(t.amountVnd)}</td>
                    <td class="text-right text-success">+${formatVND(t.netEarningVnd)}</td>
                    <td><span class="badge ${badgeClass}">${t.status}</span></td>
                    <td>${t.createdAt.replace('T', ' ').substring(0, 16)}</td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==============================================================
// 9. WITHDRAWALS VIEW
// ==============================================================
async function initWithdrawals() {
    const tbody = document.querySelector('.seller-table tbody');
    if (!tbody) return;

    try {
        // Load bank info check
        const infoRes = await sellerFetch('/shop-info');
        const infoData = await infoRes.json();

        // Check if bank info is set, if not warn
        if (!infoData.bankName || !infoData.accountNumber) {
            const warningAlert = document.createElement('div');
            warningAlert.className = 'badge critical';
            warningAlert.style.width = '100%';
            warningAlert.style.marginBottom = '20px';
            warningAlert.style.borderRadius = '8px';
            warningAlert.style.padding = '14px';
            warningAlert.innerHTML = '<i class="fa fa-warning"></i> Bạn chưa thiết lập thông tin Ngân hàng! Vui lòng cấu hình tại <a href="/seller/shop-info" style="color: inherit; text-decoration: underline;">Thông tin cửa hàng</a> trước khi làm lệnh rút.';
            tbody.closest('.seller-card').insertBefore(warningAlert, tbody.closest('.seller-table-wrap'));
        }

        // Load dashboard stats for balances
        const dashRes = await sellerFetch('/dashboard');
        const dashData = await dashRes.json();

        const balanceEl = document.querySelector('.balance-highlight__value');
        if (balanceEl) balanceEl.textContent = formatVND(dashData.balanceVnd);

        // Load withdrawals history
        const wRes = await sellerFetch('/withdrawals');
        if (!wRes.ok) throw new Error('Không thể tải lịch sử rút tiền.');
        const withdrawals = await wRes.json();

        if (withdrawals.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">Chưa có yêu cầu rút tiền nào.</td></tr>';
        } else {
            tbody.innerHTML = withdrawals.map(w => {
                const badgeClass = w.status === 'Completed' ? 'ok' : 'pending';
                return `
                    <tr>
                        <td>#WD-${w.id}</td>
                        <td class="text-right">${formatVND(w.amountVnd)}</td>
                        <td>${w.bankName} (${w.accountNumber})</td>
                        <td>${w.createdAt.replace('T', ' ').substring(0, 10)}</td>
                        <td><span class="badge ${badgeClass}">${w.status}</span></td>
                        <td class="text-right">
                            <div class="row-actions">
                                <a class="icon-button" href="/seller/withdrawals/detail?id=${w.id}" title="Xem chi tiết"><i class="fa fa-eye"></i></a>
                            </div>
                        </td>
                    </tr>
                `;
            }).join('');
        }

        // Handle Request Withdrawal Button
        const withdrawBtn = document.querySelector('.profile-button--primary');
        if (withdrawBtn) {
            withdrawBtn.addEventListener('click', async () => {
                const amountText = prompt('Nhập số tiền muốn rút (Tối thiểu 50,000 VNĐ):');
                if (amountText === null) return;

                const amount = parseInt(amountText.replace(/,/g, ''));
                if (isNaN(amount) || amount < 50000) {
                    alert('Số tiền rút không hợp lệ hoặc nhỏ hơn 50,000 VNĐ.');
                    return;
                }

                if (dashData.balanceVnd < amount) {
                    alert('Số dư ví khả dụng của bạn không đủ.');
                    return;
                }

                try {
                    const postRes = await sellerFetch('/withdrawals', {
                        method: 'POST',
                        body: JSON.stringify({ amountVnd: amount })
                    });
                    const postData = await postRes.json();
                    if (!postRes.ok) throw new Error(postData.message || 'Rút tiền thất bại.');

                    showToast(postData.message || 'Đã tạo yêu cầu rút tiền!');
                    window.location.reload(); // Refresh screen
                } catch (err) {
                    showToast(err.message, 'error');
                }
            });
        }

    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==============================================================
// 10. STATISTICS VIEW (BAR CHART)
// ==============================================================
async function initStatistics() {
    const barsContainer = document.querySelector('.chart-bars');
    if (!barsContainer) return;

    try {
        const res = await sellerFetch('/statistics');
        if (!res.ok) throw new Error('Không thể tải số liệu thống kê.');
        const stats = await res.json();

        // Escrow balance display
        const escrowEl = document.querySelector('.balance-highlight__value');
        if (escrowEl) escrowEl.textContent = formatVND(stats.escrowBalance);

        // Stats card display
        const statCards = document.querySelectorAll('.stat-card__value');
        if (statCards.length >= 2) {
            statCards[1].textContent = stats.totalSalesCount + ' đơn';
        }

        // Draw weekly sales HTML bar chart
        if (!stats.chartData || stats.chartData.length === 0) {
            barsContainer.innerHTML = '<div style="margin: auto; color: var(--seller-muted);">Chưa có dữ liệu biểu đồ.</div>';
        } else {
            const maxValue = Math.max(...stats.chartData.map(d => d.value), 100000);
            const vietnameseDayMap = {
                'MONDAY': 'T2', 'TUESDAY': 'T3', 'WEDNESDAY': 'T4',
                'THURSDAY': 'T5', 'FRIDAY': 'T6', 'SATURDAY': 'T7', 'SUNDAY': 'CN'
            };
            barsContainer.innerHTML = stats.chartData.map(d => {
                const percentHeight = Math.max((d.value / maxValue) * 100, 2);
                const label = vietnameseDayMap[d.label] || d.label.substring(0, 3);
                return `
                    <div class="chart-bar" title="${formatVND(d.value)}">
                        <div class="chart-bar__fill" style="height: ${percentHeight}%;"></div>
                        <span class="chart-bar__label">${label}</span>
                    </div>
                `;
            }).join('');
        }

        // Top products table
        const topTbody = document.getElementById('top-products-tbody');
        if (topTbody) {
            if (!stats.topProducts || stats.topProducts.length === 0) {
                topTbody.innerHTML = '<tr><td colspan="4" style="text-align:center; padding: 20px;">Chưa có đơn hàng hoàn thành.</td></tr>';
            } else {
                topTbody.innerHTML = stats.topProducts.map((p, idx) => `
                    <tr>
                        <td>${idx + 1}</td>
                        <td>${p.productName}</td>
                        <td class="text-right">${p.soldCount}</td>
                        <td class="text-right">${formatVND(p.revenue)}</td>
                    </tr>
                `).join('');
            }
        }

    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==============================================================
// 11. SHOP FLAGS VIEW
// ==============================================================
async function initShopFlags() {
    const container = document.getElementById('flagListContainer') || document.querySelector('.flag-list');
    if (!container) return;

    try {
        const res = await sellerFetch('/shop-flags');
        if (!res.ok) throw new Error('Không thể tải cờ cảnh báo.');
        const flags = await res.json();

        // Update stat cards
        const totalEl = document.getElementById('totalFlags');
        const warnEl = document.getElementById('warningCount');
        const critEl = document.getElementById('criticalCount');
        const compEl = document.getElementById('flagWithComplaint');
        if (totalEl) totalEl.textContent = flags.length;
        if (warnEl) warnEl.textContent = flags.filter(f => f.flagLevel === 'Warning').length;
        if (critEl) critEl.textContent = flags.filter(f => f.flagLevel === 'Critical' || f.flagLevel === 'Suspension').length;
        if (compEl) compEl.textContent = flags.filter(f => f.complaintId).length;

        if (flags.length === 0) {
            container.innerHTML = '<div class="badge ok" style="padding:16px; width:100%; border-radius:8px; display:block;"><i class="fa fa-check"></i> Cửa hàng của bạn hiện tại hoạt động rất tốt, không có cờ cảnh báo nào!</div>';
            return;
        }

        container.innerHTML = flags.map(f => {
            const isCritical = f.flagLevel === 'Suspension' || f.flagLevel === 'Critical';
            const cardClass = isCritical ? 'flag-card--critical' : 'flag-card--warning';
            const badgeClass = isCritical ? 'critical' : 'warning';
            const compLink = f.complaintId 
                ? `<div style="margin-top:10px;"><a href="/seller/complaints/detail?id=${f.complaintId}" class="badge held"><i class="fa fa-eye"></i> Xem khiếu nại liên quan</a></div>` 
                : '';

            return `
                <div class="flag-card ${cardClass}">
                    <div class="flag-card__header">
                        <h3 class="flag-card__title">Vi phạm: Cấp độ ${f.flagLevel}</h3>
                        <span class="badge ${badgeClass}">${f.flagLevel}</span>
                    </div>
                    <div class="flag-card__meta">Nhân viên kiểm duyệt: ${f.staffName} · ${f.createdAt.replace('T', ' ').substring(0, 16)}</div>
                    <p class="flag-card__reason">${f.reason}</p>
                    ${compLink}
                </div>
            `;
        }).join('');

    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==============================================================
// 12. REVIEWS VIEW
// ==============================================================
async function initReviews() {
    const tbody = document.getElementById('reviews-tbody');
    if (!tbody) return;

    try {
        const res = await sellerFetch('/reviews');
        if (!res.ok) throw new Error('Không thể tải đánh giá sản phẩm.');
        const reviews = await res.json();

        // Update stat cards
        const avgEl = document.getElementById('avgRating');
        const totalEl = document.getElementById('totalReviews');
        const fiveEl = document.getElementById('fiveStarCount');
        const lowEl = document.getElementById('lowStarCount');

        if (reviews.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding: 30px;">Sản phẩm của shop chưa có lượt đánh giá nào.</td></tr>';
            if (avgEl) avgEl.textContent = '—';
            if (totalEl) totalEl.textContent = '0';
            if (fiveEl) fiveEl.textContent = '0';
            if (lowEl) lowEl.textContent = '0';
            return;
        }

        // Compute stats
        const avgRating = (reviews.reduce((s, r) => s + r.rating, 0) / reviews.length).toFixed(1);
        const fiveStars = reviews.filter(r => r.rating === 5).length;
        const lowStars = reviews.filter(r => r.rating <= 2).length;

        if (avgEl) avgEl.innerHTML = `${avgRating} <span class="review-stars"><i class="fa fa-star"></i></span>`;
        if (totalEl) totalEl.textContent = reviews.length;
        if (fiveEl) fiveEl.textContent = fiveStars;
        if (lowEl) lowEl.textContent = lowStars;

        tbody.innerHTML = reviews.map(r => {
            let stars = '';
            for (let i = 1; i <= 5; i++) {
                stars += i <= r.rating ? '<i class="fa fa-star"></i>' : '<i class="fa fa-star-o"></i>';
            }
            return `
                <tr>
                    <td>#RV-${r.id}</td>
                    <td>${r.productName}</td>
                    <td>${r.customerName}</td>
                    <td><span class="review-stars">${stars}</span></td>
                    <td>${r.comment ? (r.comment.length > 50 ? r.comment.substring(0, 50) + '...' : r.comment) : '<span class="muted">Không có bình luận</span>'}</td>
                    <td>${r.createdAt.substring(0, 10)}</td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==============================================================
// 13. COMPLAINTS LIST VIEW
// ==============================================================
async function initComplaints() {
    const tbody = document.querySelector('.seller-table tbody');
    if (!tbody) return;

    try {
        const res = await sellerFetch('/complaints');
        if (!res.ok) throw new Error('Không thể tải danh sách khiếu nại.');
        const complaints = await res.json();

        if (complaints.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;">Không có khiếu nại nào chống lại shop của bạn.</td></tr>';
            return;
        }

        tbody.innerHTML = complaints.map(c => {
            let badgeClass = 'open';
            if (c.status === 'Resolved' || c.status === 'Closed') badgeClass = 'resolved';
            else if (c.status === 'In_Progress') badgeClass = 'pending';

            return `
                <tr>
                    <td>#CP-${c.id}</td>
                    <td>#TX-${c.transactionId}</td>
                    <td>
                        <strong>${c.productName}</strong><br>
                        <span class="muted" style="font-size:12px;">${c.variantName}</span>
                    </td>
                    <td>${c.customerEmail}</td>
                    <td>${c.description.length > 40 ? c.description.substring(0, 40) + '...' : c.description}</td>
                    <td class="text-right">${formatVND(c.amountVnd)}</td>
                    <td><span class="badge ${badgeClass}">${c.status}</span></td>
                    <td>${c.createdAt.replace('T', ' ').substring(0, 10)}</td>
                    <td class="text-right">
                        <div class="row-actions">
                            <a class="icon-button" href="/seller/complaints/detail?id=${c.id}" title="Xem chi tiết khiếu nại"><i class="fa fa-eye"></i></a>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==============================================================
// 14. COMPLAINT DETAILS & CHAT
// ==============================================================
async function initComplaintDetail() {
    const urlParams = new URLSearchParams(window.location.search);
    const complaintId = urlParams.get('id');
    if (!complaintId) {
        window.location.href = '/seller/complaints';
        return;
    }

    const card = document.querySelector('.seller-card');
    if (!card) return;

    try {
        const res = await sellerFetch(`/complaints/${complaintId}`);
        if (!res.ok) throw new Error('Không thể tải chi tiết khiếu nại.');
        const c = await res.json();

        // 1. Populate details info grid
        document.querySelector('.seller-card__title').textContent = `Chi tiết khiếu nại #CP-${c.id}`;
        
        let badgeClass = 'open';
        if (c.status === 'Resolved' || c.status === 'Closed') badgeClass = 'resolved';
        else if (c.status === 'In_Progress') badgeClass = 'pending';

        const headerRight = card.querySelector('.seller-card__header').lastElementChild;
        if (headerRight && headerRight.classList.contains('badge')) {
            headerRight.className = `badge ${badgeClass}`;
            headerRight.textContent = c.status;
        }

        // Map data to DL info list
        const dl = card.querySelector('.seller-info-grid');
        if (dl) {
            dl.innerHTML = `
                <dt>Khách hàng</dt>
                <dd>${c.customerName} (${c.customerEmail})</dd>
                <dt>Mã đơn hàng</dt>
                <dd>#TX-${c.transactionId}</dd>
                <dt>Sản phẩm</dt>
                <dd>${c.productName} (${c.variantName})</dd>
                <dt>Đơn giá</dt>
                <dd>${formatVND(c.amountVnd)}</dd>
                <dt>Nội dung khiếu nại</dt>
                <dd>${c.description}</dd>
                <dt>Ảnh minh chứng</dt>
                <dd>${c.evidence ? `<a href="${c.evidence}" target="_blank" style="color:var(--seller-danger); text-decoration: underline;">Xem ảnh chứng cứ</a>` : 'Không có'}</dd>
                <dt>Hướng giải quyết</dt>
                <dd>${c.resolution || 'Chưa có phương án xử lý cuối cùng'}</dd>
            `;
        }

        // 2. Load Chat History
        const messagesContainer = card.querySelector('.chat-messages');
        if (messagesContainer) {
            if (!c.chats || c.chats.length === 0) {
                messagesContainer.innerHTML = '<div style="text-align:center; padding: 20px; color: var(--seller-muted);">Chưa có tin nhắn hội thoại.</div>';
            } else {
                messagesContainer.innerHTML = c.chats.map(msg => {
                    let bubbleClass = 'chat-bubble--customer';
                    if (msg.senderRole === 'Seller') bubbleClass = 'chat-bubble--seller';
                    else if (msg.senderRole === 'Staff') bubbleClass = 'chat-bubble--staff';

                    return `
                        <div class="chat-bubble ${bubbleClass}">
                            <span class="chat-bubble__meta">${msg.senderName} (${msg.senderRole}) · ${msg.createdAt.substring(11, 16)}</span>
                            <div>${msg.message}</div>
                        </div>
                    `;
                }).join('');

                // Scroll to bottom
                messagesContainer.scrollTop = messagesContainer.scrollHeight;
            }
        }

        // 3. Bind Chat Compose send event
        const textInput = card.querySelector('.chat-compose textarea');
        const sendBtn = card.querySelector('.chat-compose .profile-button--primary');
        if (sendBtn && textInput) {
            sendBtn.addEventListener('click', async () => {
                const text = textInput.value.trim();
                if (!text) return;

                try {
                    const postRes = await sellerFetch(`/complaints/${c.id}/chat`, {
                        method: 'POST',
                        body: JSON.stringify({ message: text })
                    });
                    if (!postRes.ok) throw new Error('Không thể gửi tin nhắn.');
                    textInput.value = '';
                    initComplaintDetail(); // Reload chat history
                } catch (err) {
                    showToast(err.message, 'error');
                }
            });
        }

    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==============================================================
// 15. WITHDRAWAL DETAIL VIEW
// ==============================================================
async function initWithdrawalDetail() {
    const urlParams = new URLSearchParams(window.location.search);
    const withdrawalId = urlParams.get('id');
    if (!withdrawalId) {
        window.location.href = '/seller/withdrawals';
        return;
    }

    const card = document.querySelector('.seller-card');
    if (!card) return;

    try {
        const res = await sellerFetch(`/withdrawals/${withdrawalId}`);
        if (!res.ok) throw new Error('Không thể tải chi tiết yêu cầu rút tiền.');
        const w = await res.json();

        // Populate header details
        document.querySelector('.seller-card__subtitle').textContent = `Lệnh #WD-${w.id}`;

        const badgeClass = w.status === 'Completed' ? 'ok' : 'pending';

        // Map data to DL info list
        const dl = card.querySelector('.seller-info-grid');
        if (dl) {
            dl.innerHTML = `
                <dt>Mã lệnh</dt>
                <dd>#WD-${w.id}</dd>
                <dt>Số tiền</dt>
                <dd>${formatVND(w.amountVnd)}</dd>
                <dt>Trạng thái</dt>
                <dd><span class="badge ${badgeClass}">${w.status}</span></dd>
                <dt>Ngân hàng</dt>
                <dd>${w.bankName}</dd>
                <dt>Số tài khoản</dt>
                <dd>${w.accountNumber}</dd>
                <dt>Chủ tài khoản</dt>
                <dd>${w.accountHolder}</dd>
                <dt>Chi nhánh</dt>
                <dd>${w.branch || 'Chưa thiết lập'}</dd>
                <dt>Ngày tạo</dt>
                <dd>${w.createdAt.replace('T', ' ').substring(0, 16)}</dd>
            `;
        }

        // Render proof receipt section
        const proofSection = card.querySelector('.proof-placeholder');
        if (proofSection) {
            if (w.proofFile) {
                proofSection.innerHTML = `
                    <a href="/images/${w.proofFile}" target="_blank" style="display:block; text-align:center;">
                        <img src="/images/${w.proofFile}" alt="Biên lai rút tiền" style="max-width:100%; max-height:300px; border-radius:8px; border:1px solid var(--seller-border);"/>
                    </a>
                `;
            } else {
                proofSection.innerHTML = `
                    <div style="text-align:center; padding: 20px; background:#f8fafc; border:1px dashed var(--seller-border); border-radius:8px; color:var(--seller-muted);">
                        <i class="fa fa-picture-o" style="font-size:24px; display:block; margin-bottom:8px;"></i>
                        Chưa có ảnh biên lai (Đang chờ xử lý)
                    </div>
                `;
            }
        }

    } catch (err) {
        showToast(err.message, 'error');
    }
}
