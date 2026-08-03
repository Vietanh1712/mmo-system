// ==============================================================================
// SELLER CONSOLE JS
// File: seller-console.js
// Description: Controls all dynamic operations and REST API data binding
//              for the Seller dashboard pages.
// ==============================================================================

const SELLER_API_BASE = '/api/seller';

function getCurrentSellerStorageUser() {
    try {
        const raw = sessionStorage.getItem('userInfo') || sessionStorage.getItem('user');
        return raw ? JSON.parse(raw) : null;
    } catch (error) {
        return null;
    }
}

function getSellerSidebarCacheKey() {
    const user = getCurrentSellerStorageUser();
    return user && user.id != null ? `sellerSidebarCache:${user.id}` : null;
}

function cacheSellerSidebar(data) {
    const key = getSellerSidebarCacheKey();
    if (!key) return;
    const value = JSON.stringify(data);
    sessionStorage.setItem(key, value);
    localStorage.setItem(key, value);
}

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
    } else if (path.endsWith('/preorders')) {
        initPreOrders();
        
        const preSearch = document.getElementById('preorderSearchInput');
        const preStatus = document.getElementById('preorderStatusFilter');
        const preSort = document.getElementById('preorderSortSelect');
        if (preSearch) preSearch.addEventListener('input', applyPreOrderFilters);
        if (preStatus) preStatus.addEventListener('change', applyPreOrderFilters);
        if (preSort) preSort.addEventListener('change', applyPreOrderFilters);
    }
});

// ==============================================================================
// HELPERS
// ==============================================================================
function formatVND(value) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value) || 0);
}

function translateStatus(status) {
    if (!status) return '-';
    const statusLower = status.toLowerCase().trim();
    const map = {
        'pending': 'Chờ xử lý',
        'held': 'Tạm giữ (Bảo lãnh)',
        'paid': 'Đã thanh toán',
        'delivered': 'Đã giao',
        'processing': 'Đang xử lý',
        'completed': 'Hoàn tất',
        'cancelled': 'Đã hủy',
        'disputed': 'Tranh chấp',
        'refunded': 'Đã hoàn tiền',
        'failed': 'Thất bại',
        'active': 'Đang bán',
        'inactive': 'Tạm ẩn',
        'rejected': 'Bị từ chối',
        'approved': 'Đã duyệt',
        'open': 'Chờ xử lý',
        'pending_review': 'Chờ duyệt',
        'in_progress': 'Đang giải quyết',
        'inprogress': 'Đang giải quyết',
        'resolved': 'Đã giải quyết',
        'closed': 'Đã đóng'
    };
    return map[statusLower] || status;
}

function getBadgeClassForStatus(status) {
    if (!status) return 'pending';
    const statusLower = status.toLowerCase().trim();
    if (statusLower === 'completed' || statusLower === 'approved') return 'ok';
    if (statusLower === 'pending') return 'pending';
    if (statusLower === 'held') return 'held';
    if (statusLower === 'rejected' || statusLower === 'failed' || statusLower === 'locked') return 'critical';
    return 'pending';
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
    // If the global design system toast is loaded, delegate to it
    if (type === 'success' && typeof window.showSuccessToast === 'function') {
        window.showSuccessToast(message);
        return;
    }
    if ((type === 'error' || type === 'danger') && typeof window.showErrorToast === 'function') {
        window.showErrorToast(message);
        return;
    }
    if (type === 'warning' && typeof window.showWarningToast === 'function') {
        window.showWarningToast(message);
        return;
    }
    if (type === 'info' && typeof window.showInfoToast === 'function') {
        window.showInfoToast(message);
        return;
    }

    // Fallback to local element if global system is not present
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
    
    let bgColor = '#10a37f'; // success (DESIGN.md)
    if (type === 'error' || type === 'danger') bgColor = '#ef4444'; // danger (DESIGN.md)
    else if (type === 'warning') bgColor = '#f59e0b'; // warning (DESIGN.md)
    else if (type === 'primary') bgColor = '#ea580c'; // primary (DESIGN.md)
    
    toast.style.backgroundColor = bgColor;
    toast.textContent = message;
    toast.style.opacity = '1';
    setTimeout(() => {
        toast.style.opacity = '0';
    }, 3000);
}

// ==============================================================================
// 1. GENERAL LAYOUT & SIDEBAR
function formatShopStatusVi(st) {
    const s = String(st || 'Active').toUpperCase();
    if (s === 'SUSPENDED' || s === 'TEMP_LOCKED' || s === 'TEMPORARILY_CLOSED') return 'Tạm ngưng';
    if (s === 'LOCKED' || s === 'INDEFINITE_LOCKED' || s === 'CLOSED') return 'Tạm khóa';
    if (s === 'BANNED' || s === 'PERMANENT_BANNED') return 'Khóa vĩnh viễn';
    if (s === 'WITHDRAWN') return 'Đã đóng Shop';
    if (s === 'PENDING') return 'Chờ duyệt';
    return 'Hoạt động';
}

let shopLockCountdownInterval = null;

function showShopLockedOverlay(lockTimeStr) {
    const existing = document.getElementById('shopStatusOverlayModal');
    if (existing) existing.remove();

    const container = document.createElement('div');
    container.id = 'shopStatusOverlayModal';
    container.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100vw;
        height: 100vh;
        background: rgba(15, 23, 42, 0.95);
        backdrop-filter: blur(8px);
        z-index: 999999;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 20px;
        box-sizing: border-box;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    `;

    // Parse target lock time
    let targetDate = null;
    if (lockTimeStr && lockTimeStr.includes(':')) {
        if (lockTimeStr.includes('/')) {
            const parts = lockTimeStr.trim().split(' ');
            if (parts.length === 2) {
                const [timePart, datePart] = parts;
                const [hh, mm] = timePart.split(':');
                const [dd, MM, yyyy] = datePart.split('/');
                targetDate = new Date(yyyy, MM - 1, dd, hh, mm);
            }
        } else {
            targetDate = new Date(lockTimeStr);
        }
    } else if (lockTimeStr) {
        targetDate = new Date(lockTimeStr);
    }

    container.innerHTML = `
        <div style="background: #ffffff; border-radius: 16px; max-width: 520px; width: 100%; padding: 36px 32px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); text-align: center; border: 1px solid #e2e8f0;">
            <div style="width: 64px; height: 64px; background: #fffbe6; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px auto; color: #d97706; font-size: 28px;">
                <i class="fa fa-clock-o"></i>
            </div>
            <h2 style="margin: 0 0 12px 0; font-size: 22px; font-weight: 700; color: #1e293b;">Shop Của Bạn Đang Bị Tạm Khóa</h2>
            <p style="margin: 0 0 20px 0; font-size: 14px; color: #64748b; line-height: 1.6;">
                Cửa hàng của bạn đang trong thời gian tạm khóa / tạm ngưng theo quyết định của Ban quản trị. Trong thời gian này, bạn không thể truy cập Seller Dashboard.
            </p>
            
            <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px; margin-bottom: 24px;">
                <div style="font-size: 13px; color: #64748b; margin-bottom: 6px;">Thời điểm mở khóa dự kiến:</div>
                <div style="font-size: 15px; font-weight: 700; color: #0f172a; margin-bottom: 12px;" id="overlayLockTargetText">
                    ${lockTimeStr || 'Đang cập nhật...'}
                </div>
                <div style="font-size: 13px; color: #64748b; margin-bottom: 4px;">Thời gian còn lại:</div>
                <div style="font-size: 20px; font-weight: 800; color: #2563eb; letter-spacing: 0.5px;" id="overlayLockCountdown">
                    Đang tính toán...
                </div>
            </div>

            <div style="display: flex; gap: 12px; justify-content: center;">
                <a href="/" style="background: #f1f5f9; color: #334155; padding: 12px 24px; border-radius: 10px; text-decoration: none; font-weight: 600; font-size: 14px; flex: 1; text-align: center; border: 1px solid #cbd5e1; transition: all 0.2s;">
                    <i class="fa fa-home"></i> Trang Chủ
                </a>
            </div>
        </div>
    `;

    document.body.appendChild(container);

    if (shopLockCountdownInterval) clearInterval(shopLockCountdownInterval);

    function updateCountdown() {
        const countdownEl = document.getElementById('overlayLockCountdown');
        if (!countdownEl) return;

        if (!targetDate || isNaN(targetDate.getTime())) {
            countdownEl.textContent = 'Vui lòng liên hệ Staff';
            return;
        }

        const now = new Date();
        const diff = targetDate.getTime() - now.getTime();

        if (diff <= 0) {
            countdownEl.innerHTML = '<span style="color: #16a34a;">Đã hết thời gian tạm khóa! Đang tải lại...</span>';
            clearInterval(shopLockCountdownInterval);
            setTimeout(() => window.location.reload(), 1500);
            return;
        }

        const days = Math.floor(diff / (1000 * 60 * 60 * 24));
        const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((diff % (1000 * 60)) / 1000);

        let parts = [];
        if (days > 0) parts.push(`${days} ngày`);
        parts.push(`${String(hours).padStart(2, '0')} giờ`);
        parts.push(`${String(minutes).padStart(2, '0')} phút`);
        parts.push(`${String(seconds).padStart(2, '0')} giây`);

        countdownEl.textContent = parts.join(' ');
    }

    updateCountdown();
    shopLockCountdownInterval = setInterval(updateCountdown, 1000);
}

function showShopBannedOverlay(reason) {
    const existing = document.getElementById('shopStatusOverlayModal');
    if (existing) existing.remove();

    const container = document.createElement('div');
    container.id = 'shopStatusOverlayModal';
    container.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100vw;
        height: 100vh;
        background: rgba(15, 23, 42, 0.95);
        backdrop-filter: blur(8px);
        z-index: 999999;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 20px;
        box-sizing: border-box;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    `;

    container.innerHTML = `
        <div style="background: #ffffff; border-radius: 16px; max-width: 520px; width: 100%; padding: 36px 32px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); text-align: center; border: 1px solid #e2e8f0;">
            <div style="width: 64px; height: 64px; background: #fef2f2; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px auto; color: #dc2626; font-size: 28px;">
                <i class="fa fa-ban"></i>
            </div>
            <h2 style="margin: 0 0 12px 0; font-size: 22px; font-weight: 700; color: #991b1b;">Shop Của Bạn Đã Bị Khóa Vĩnh Viễn</h2>
            <p style="margin: 0 0 20px 0; font-size: 14px; color: #64748b; line-height: 1.6;">
                ${reason || 'Gian hàng của bạn đã bị khóa vĩnh viễn do vi phạm nghiêm trọng chính sách của sàn giao dịch MMO Market.'}
            </p>
            
            <div style="background: #fff5f5; border: 1px solid #fecaca; border-radius: 12px; padding: 14px 16px; margin-bottom: 24px; text-align: left; font-size: 13px; color: #991b1b; line-height: 1.5;">
                <i class="fa fa-info-circle"></i> Tài khoản cá nhân của bạn vẫn có thể sử dụng để đăng nhập và mua sắm sản phẩm, nhưng không thể truy cập Seller Dashboard.
            </div>

            <div style="display: flex; gap: 12px; justify-content: center;">
                <a href="/" style="background: #dc2626; color: #ffffff; padding: 12px 24px; border-radius: 10px; text-decoration: none; font-weight: 600; font-size: 14px; flex: 1; text-align: center; border: none; transition: all 0.2s;">
                    <i class="fa fa-home"></i> Về Trang Chủ
                </a>
            </div>
        </div>
    `;

    document.body.appendChild(container);
}

function showShopWithdrawnOverlay(msg) {
    const existing = document.getElementById('shopStatusOverlayModal');
    if (existing) existing.remove();

    const container = document.createElement('div');
    container.id = 'shopStatusOverlayModal';
    container.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100vw;
        height: 100vh;
        background: rgba(15, 23, 42, 0.95);
        backdrop-filter: blur(8px);
        z-index: 999999;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 20px;
        box-sizing: border-box;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    `;

    container.innerHTML = `
        <div style="background: #ffffff; border-radius: 16px; max-width: 520px; width: 100%; padding: 36px 32px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); text-align: center; border: 1px solid #e2e8f0;">
            <div style="width: 64px; height: 64px; background: #eff6ff; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px auto; color: #2563eb; font-size: 28px;">
                <i class="fa fa-info-circle"></i>
            </div>
            <h2 style="margin: 0 0 12px 0; font-size: 22px; font-weight: 700; color: #1e293b;">Shop Của Bạn Đã Đóng Cửa (Hoàn Phí)</h2>
            <p style="margin: 0 0 20px 0; font-size: 14px; color: #64748b; line-height: 1.6;">
                Cửa hàng cũ của bạn đã được đóng và hoàn trả 100% tiền cọc về ví tài khoản. Bạn có thể đăng ký mở Shop mới bất kỳ lúc nào!
            </p>
            
            <div style="background: #f0f9ff; border: 1px solid #bae6fd; border-radius: 12px; padding: 14px 16px; margin-bottom: 24px; font-size: 13px; color: #0369a1; font-weight: 600;">
                ✔ Trạng thái: Đã đóng Shop & Hoàn cọc
            </div>

            <div style="display: flex; gap: 12px; justify-content: center;">
                <a href="/account/register-shop" style="background: #2563eb; color: #ffffff; padding: 12px 24px; border-radius: 10px; text-decoration: none; font-weight: 600; font-size: 14px; flex: 1; text-align: center; border: none; transition: all 0.2s;">
                    <i class="fa fa-shopping-bag"></i> Đăng ký Mở Shop Mới
                </a>
                <a href="/" style="background: #f1f5f9; color: #334155; padding: 12px 24px; border-radius: 10px; text-decoration: none; font-weight: 600; font-size: 14px; flex: 1; text-align: center; border: 1px solid #cbd5e1; transition: all 0.2s;">
                    <i class="fa fa-home"></i> Trang chủ
                </a>
            </div>
        </div>
    `;

    document.body.appendChild(container);
}

async function initSellerLayout() {
    try {
        const [res, dashRes] = await Promise.all([
            sellerFetch('/shop-info').catch(e => null),
            sellerFetch('/dashboard').catch(e => null)
        ]);

        let errMsg = '';
        if (res && !res.ok) {
            const errObj = await res.json().catch(() => ({}));
            errMsg = errObj.message || errObj.description || '';
        } else if (dashRes && !dashRes.ok) {
            const errObj = await dashRes.json().catch(() => ({}));
            errMsg = errObj.message || errObj.description || '';
        }

        if (errMsg.includes('SHOP_TEMPORARILY_LOCKED:')) {
            showShopLockedOverlay(errMsg.split('SHOP_TEMPORARILY_LOCKED:')[1]);
            return;
        }
        if (errMsg.includes('SHOP_WITHDRAWN:')) {
            showShopWithdrawnOverlay(errMsg.split('SHOP_WITHDRAWN:')[1]);
            return;
        }
        if (errMsg.includes('SHOP_BANNED:')) {
            showShopBannedOverlay(errMsg.split('SHOP_BANNED:')[1]);
            return;
        }

        if (!res || !res.ok) return;
        const data = await res.json();

        const stLower = String(data.shopStatus || '').toLowerCase();
        if (stLower === 'locked' || stLower === 'temp_locked' || stLower === 'indefinite_locked') {
            if (data.suspendedUntil) {
                showShopLockedOverlay(data.suspendedUntil);
                return;
            }
        }
        if (stLower === 'banned' || stLower === 'permanent_banned') {
            showShopBannedOverlay('Shop của bạn đã bị khóa vĩnh viễn.');
            return;
        }
        if (stLower === 'withdrawn' || stLower === 'closed' || stLower === 'deleted') {
            showShopWithdrawnOverlay('Shop đã được đóng và hoàn phí.');
            return;
        }

        // Update sidebar
        const nameEl = document.querySelector('.seller-sidebar__name');
        const statusEl = document.querySelector('.seller-sidebar__status');
        const avatarEl = document.querySelector('.seller-sidebar__avatar');

        if (nameEl) nameEl.textContent = data.shopName || 'Cửa hàng của tôi';
        if (statusEl) statusEl.textContent = `Trạng thái: ${formatShopStatusVi(data.shopStatus)}`;
        if (avatarEl && data.shopName) {
            avatarEl.textContent = data.shopName.charAt(0).toUpperCase();
        } else if (avatarEl) {
            avatarEl.textContent = 'S';
        }

        const shopContainer = document.querySelector('.seller-sidebar__shop');
        if (shopContainer) shopContainer.setAttribute('aria-busy', 'false');

        const levelBadgeEl = document.querySelector('.seller-sidebar__level-badge');
        if (levelBadgeEl) {
            const lvl = data.shopLevel !== undefined ? data.shopLevel : 1;
            if (lvl === 0) {
                levelBadgeEl.innerHTML = `<span style="color: #dc2626; background: #fee2e2; padding: 2px 6px; border-radius: 4px; display: inline-block; font-weight: 700; font-size: 11px;"><i class="fa fa-exclamation-triangle"></i> Cảnh cáo (Lvl 0)</span>`;
            } else if (lvl === 2) {
                levelBadgeEl.innerHTML = `<span style="color: #16a34a; background: #dcfce7; padding: 2px 6px; border-radius: 4px; display: inline-block; font-weight: 700; font-size: 11px;"><i class="fa fa-check-circle"></i> Uy tín (Lvl 2)</span>`;
            } else {
                levelBadgeEl.innerHTML = `<span style="color: #0284c7; background: #e0f2fe; padding: 2px 6px; border-radius: 4px; display: inline-block; font-weight: 700; font-size: 11px;"><i class="fa fa-star-o"></i> Shop mới (Lvl 1)</span>`;
            }
        }

        const storedUser = getCurrentSellerStorageUser();
        const sidebarCache = {
            shopName: data.shopName || 'Cửa hàng của tôi',
            shopStatus: data.shopStatus || 'Active',
            shopLevel: data.shopLevel !== undefined ? data.shopLevel : 1,
            balanceVnd: storedUser ? storedUser.balanceVnd : 0
        };

        if (dashRes.ok) {
            const dashData = await dashRes.json();
            const balanceEl = document.querySelector('.seller-sidebar__balance');
            if (balanceEl) balanceEl.textContent = formatVND(dashData.balanceVnd);
            sidebarCache.balanceVnd = dashData.balanceVnd;
        }
        cacheSellerSidebar(sidebarCache);
    } catch (err) {
        console.error('Lỗi khởi tạo layout người bán:', err);
    }
}

// ==============================================================================
// 2. DASHBOARD VIEW
// ==============================================================================
async function initDashboard() {
    const statsGrid = document.querySelector('.stats-grid-4');
    if (!statsGrid) return;

    try {
        const res = await sellerFetch('/dashboard');
        if (!res.ok) throw new Error('Không thể tải số liệu thống kê.');
        const data = await res.json();

        // Bind stats card values
        const cards = statsGrid.querySelectorAll('.stat-card-value');
        if (cards.length >= 4) {
            cards[0].textContent = formatVND(data.totalRevenue);
            cards[1].textContent = data.completedSales;
            cards[2].textContent = data.activeProductsCount;
            cards[3].textContent = data.openComplaintsCount;
        }

        const lvlBadge = document.getElementById('dashboard-shop-level-badge');
        if (lvlBadge) {
            const lvl = data.shopLevel !== undefined ? data.shopLevel : 1;
            if (lvl === 0) {
                lvlBadge.innerHTML = `<span style="color: #dc2626; background: #fee2e2; padding: 4px 10px; border-radius: 6px; display: inline-block; font-weight: 700; font-size: 13px;"><i class="fa fa-exclamation-triangle"></i> Shop Cảnh Cáo (Level 0)</span>`;
            } else if (lvl === 2) {
                lvlBadge.innerHTML = `<span style="color: #16a34a; background: #dcfce7; padding: 4px 10px; border-radius: 6px; display: inline-block; font-weight: 700; font-size: 13px;"><i class="fa fa-check-circle"></i> Shop Uy Tín (Level 2)</span>`;
            } else {
                lvlBadge.innerHTML = `<span style="color: #0284c7; background: #e0f2fe; padding: 4px 10px; border-radius: 6px; display: inline-block; font-weight: 700; font-size: 13px;"><i class="fa fa-star-o"></i> Shop Mới (Level 1)</span>`;
            }
        }

        const alertContainer = document.getElementById('shop-warning-alert-container');
        if (alertContainer) {
            let htmlAlerts = '';
            const stLower = String(data.shopStatus || '').toLowerCase();
            if (stLower === 'suspended') {
                htmlAlerts += `
                    <div style="background-color: #fffbe6; border-left: 4px solid #d97706; padding: 16px; border-radius: 6px; margin-bottom: 20px; color: #92400e;">
                        <h4 style="margin: 0 0 6px 0; font-weight: 700; display: flex; align-items: center; gap: 8px; color: #b45309;">
                            <i class="fa fa-pause-circle" style="font-size: 18px;"></i> THÔNG BÁO: CỬA HÀNG ĐANG TRONG TRẠNG THÁI TẠM NGƯNG
                        </h4>
                        <p style="margin: 0; font-size: 13.5px;">
                            Cửa hàng của bạn đang tạm ngưng hoạt động theo quyết định của Ban quản trị. Bạn vẫn có thể xem số liệu báo cáo và lịch sử đơn hàng nhưng <strong>không thể đăng bán sản phẩm mới</strong>.
                        </p>
                    </div>
                `;
            }
            const lvl = data.shopLevel !== undefined ? data.shopLevel : 1;
            if (lvl === 0) {
                const disputePercent = (data.disputeRate * 100).toFixed(2);
                htmlAlerts += `
                    <div style="background-color: #fee2e2; border-left: 4px solid #ef4444; padding: 16px; border-radius: 6px; margin-bottom: 24px; color: #7f1d1d;">
                        <h4 style="margin: 0 0 6px 0; font-weight: 700; display: flex; align-items: center; gap: 8px; color: #dc2626;">
                            <i class="fa fa-exclamation-circle" style="font-size: 18px;"></i> CẢNH BÁO: CỬA HÀNG ĐANG Ở CHẾ ĐỘ THẮT CHẶT (LEVEL 0)
                        </h4>
                        <p style="margin: 0; font-size: 13.5px;">
                            Tỷ lệ khiếu nại lỗi tổng thể của Shop đã đạt từ 2% trở lên (hiện tại: <strong>${disputePercent}%</strong>).
                        </p>
                        <ul style="margin: 6px 0 0 0; padding-left: 20px; font-size: 13px; line-height: 1.5;">
                            <li>Thời gian giam tiền (Escrow) tăng lên <strong>7 ngày</strong> đối với mọi đơn hàng mới phát sinh.</li>
                            <li>Giới hạn hiển thị công khai tối đa <strong>5 sản phẩm</strong> cùng lúc trên sàn.</li>
                            <li><strong>Cách khắc phục:</strong> Xử lý các khiếu nại tồn đọng và tiếp tục giao dịch an toàn để giảm tỷ lệ lỗi xuống dưới 2%, hệ thống sẽ tự động khôi phục level của Shop.</li>
                        </ul>
                    </div>
                `;
            }
            alertContainer.innerHTML = htmlAlerts;
        }

        // Bind recent orders/transactions table
        const tbody = document.querySelector('.seller-table tbody') || document.querySelector('.admin-table tbody');
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
                        <td><span class="badge ${badgeClass}">${translateStatus(t.status)}</span></td>
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
    const tbody = document.querySelector('.seller-table tbody') || document.querySelector('.admin-table tbody');
    if (!tbody) return;

    try {
        const res = await sellerFetch('/products');
        if (!res.ok) throw new Error('Không thể tải danh sách kho hàng.');
        const products = await res.json();

        let activeTab = 'all'; // Biến lưu trạng thái tab hiện tại (tất cả, sắp hết, tạm khóa)
        let searchQuery = '';  // Biến lưu từ khóa tìm kiếm
        let mainCategoryFilter = ''; // Biến lưu danh mục chính cần lọc
        let subCategoryFilter = ''; // Biến lưu danh mục phụ cần lọc
        let sortMode = 'newest'; // Biến lưu tiêu chí sắp xếp

        const mainSelect = document.getElementById('mainCategoryFilter');
        const subSelect = document.getElementById('subCategoryFilter');
        if (mainSelect && subSelect) {
            setupCategorySelectors(mainSelect, subSelect, null, true);
        }

        // Lấy nút Lọc và gán sự kiện click
        const btnFilter = document.getElementById('btnFilterProduct');
        
        const searchInput = document.getElementById('productSearch');
        if (searchInput) {
            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    if (btnFilter) btnFilter.click();
                }
            });
        }

        if (btnFilter) {
            btnFilter.addEventListener('click', () => {
                const sortSelect = document.getElementById('productSort');
                
                // Cập nhật giá trị lọc từ các ô input/select
                searchQuery = searchInput ? searchInput.value.toLowerCase().trim() : '';
                mainCategoryFilter = mainSelect ? mainSelect.value : '';
                subCategoryFilter = subSelect ? subSelect.value : '';
                sortMode = sortSelect ? sortSelect.value : 'newest';
                
                // Gọi hàm render lại danh sách sản phẩm
                renderProducts();
            });
        }

        // Hàm xử lý lọc, sắp xếp và hiển thị sản phẩm
        function renderProducts() {
            // Bước 1: Lọc sản phẩm theo các tiêu chí
            let filteredProducts = products.filter(p => {
                // Lọc theo tab
                if (activeTab === 'low-stock' && p.totalStock > 5) return false;
                if (activeTab === 'inactive' && p.status === 'Active') return false;
                
                // Lọc theo từ khóa tìm kiếm (tên hoặc ID sản phẩm)
                if (searchQuery && !p.name.toLowerCase().includes(searchQuery) && !(p.id + '').includes(searchQuery)) return false;
                
                // Lọc theo danh mục
                if (mainCategoryFilter && p.mainCategoryId != mainCategoryFilter) return false;
                if (subCategoryFilter && p.categoryId != subCategoryFilter) return false;
                
                return true;
            });

            // Bước 2: Sắp xếp sản phẩm
            if (sortMode === 'newest') {
                filteredProducts.sort((a, b) => b.id - a.id); // Mới nhất (ID lớn nhất)
            } else if (sortMode === 'oldest') {
                filteredProducts.sort((a, b) => a.id - b.id); // Cũ nhất (ID nhỏ nhất)
            } else if (sortMode === 'stock_desc') {
                filteredProducts.sort((a, b) => b.totalStock - a.totalStock); // Tồn kho giảm dần
            } else if (sortMode === 'stock_asc') {
                filteredProducts.sort((a, b) => a.totalStock - b.totalStock); // Tồn kho tăng dần
            }

            const emptyState = document.getElementById('emptyState');

            if (filteredProducts.length === 0) {
                tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding: 40px; color: var(--seller-muted);"><i class="fa fa-inbox" style="font-size: 24px; display: block; margin-bottom: 8px;"></i> Không tìm thấy sản phẩm nào.</td></tr>`;
                if (emptyState) emptyState.style.display = 'none';
            } else {
                if (emptyState) emptyState.style.display = 'none';
                
                tbody.innerHTML = filteredProducts.map(p => {
                    const statusClass = p.status === 'Active' ? 'ok' : 'locked';
                    
                    // Map product type to icon and Vietnamese label
                    let typeLabel = 'Tài khoản';
                    let iconClass = 'fa-user-circle';
                    let typeBg = '#f0f7ff';
                    let typeColor = '#1e40af';
                    
                    if (p.productType === 'KEY') {
                        typeLabel = 'Mã Key';
                        iconClass = 'fa-key';
                        typeBg = '#fef3c7';
                        typeColor = '#92400e';
                    } else if (p.productType === 'GAME_CARD') {
                        typeLabel = 'Thẻ Game';
                        iconClass = 'fa-credit-card';
                        typeBg = '#fee2e2';
                        typeColor = '#991b1b';
                    }

                    // Low stock highlight (stock <= 5)
                    const stockDisplay = p.totalStock <= 5 
                        ? `<span style="color: #f59e0b; font-weight: 600;">${p.totalStock}</span>`
                        : p.totalStock;

                    let statusBadgeHtml = `<span class="badge ${statusClass}">${translateStatus(p.status)}</span>`;
                    if (p.totalStock === 0 && p.status === 'Active') {
                        statusBadgeHtml = `<span class="badge locked"><i class="fa fa-lock"></i> Hết hàng</span>`;
                    } else if (p.status !== 'Active') {
                        statusBadgeHtml = `<span class="badge locked"><i class="fa fa-pause"></i> Tạm ẩn</span>`;
                    }

                    // Fallback product image
                    const imgUrl = p.image || 'https://via.placeholder.com/50x50/f1f5f9/94a3b8?text=MMO';

                    return `
                        <tr>
                            <td>
                                <img src="${imgUrl}" class="product-image" alt="${p.name}">
                            </td>
                            <td>
                                <div class="product-cell">
                                    <div>
                                        <strong>${p.name}</strong>
                                        <div class="muted">ID #${p.id}</div>
                                        <span class="asset-badge"><i class="fa fa-database"></i> ${p.unusedAssetsCount || 0} tài sản</span>
                                    </div>
                                </div>
                            </td>
                            <td>${p.categoryName}</td>
                            <td>
                                <span style="background: ${typeBg}; color: ${typeColor}; padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: 500;">
                                    <i class="fa ${iconClass}"></i> ${typeLabel}
                                </span>
                            </td>
                            <td class="text-right">${p.variantCount}</td>
                            <td class="text-right">${stockDisplay}</td>
                            <td>${statusBadgeHtml}</td>
                            <td class="text-right">
                                <div class="row-actions" style="justify-content: flex-end; gap: 8px;">
                                    <button class="view-assets-btn" onclick="viewAssets(${p.id})"><i class="fa fa-eye"></i> Xem</button>
                                    <a class="icon-button" href="/seller/products/edit?id=${p.id}" title="Sửa sản phẩm"><i class="fa fa-pencil"></i></a>
                                    <button class="icon-button danger btn-delete-product" data-id="${p.id}" title="Xóa sản phẩm"><i class="fa fa-trash"></i></button>
                                </div>
                            </td>
                        </tr>
                    `;
                }).join('');

                // Re-attach delete handlers
                tbody.querySelectorAll('.btn-delete-product').forEach(btn => {
                    btn.addEventListener('click', async () => {
                        const id = btn.dataset.id;
                        if (!confirm('Bạn có chắc chắn muốn xóa sản phẩm này cùng toàn bộ biến thể của nó không?')) return;
                        try {
                            const delRes = await sellerFetch(`/products/${id}`, { method: 'DELETE' });
                            if (!delRes.ok) throw new Error('Không thể xóa sản phẩm.');
                            showToast('Đã xóa sản phẩm thành công!');
                            initInventory(); // Reload from server
                        } catch (err) {
                            showToast(err.message, 'error');
                        }
                    });
                });
            }
        }

        // Initialize display
        renderProducts();

        // Bind filter tabs click handlers
        document.querySelectorAll('.inventory-tab').forEach(tab => {
            const newTab = tab.cloneNode(true);
            tab.parentNode.replaceChild(newTab, tab);

            newTab.addEventListener('click', function() {
                document.querySelectorAll('.inventory-tab').forEach(t => t.classList.remove('active'));
                this.classList.add('active');
                activeTab = this.dataset.tab;
                renderProducts();
            });
        });

    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function viewAssets(productId) {
    const modal = document.getElementById('assetModal');
    const body = document.getElementById('assetModalBody');
    if (!modal || !body) return;

    body.innerHTML = '<div style="text-align:center; padding: 20px;"><i class="fa fa-spinner fa-spin" style="font-size: 24px; color: var(--seller-primary-strong);"></i> Đang tải...</div>';
    modal.classList.add('active');

    try {
        // Fetch product info to get its variants
        const prodRes = await sellerFetch(`/products/${productId}`);
        if (!prodRes.ok) throw new Error('Không thể tải thông tin sản phẩm.');
        const product = await prodRes.json();

        const variants = product.variants || [];
        if (variants.length === 0) {
            body.innerHTML = '<div class="empty-state"><i class="fa fa-inbox"></i><p>Sản phẩm này chưa có biến thể nào.</p></div>';
            return;
        }

        // Fetch assets for all variants in parallel
        const assetsPromises = variants.map(async (v) => {
            try {
                const assetsRes = await sellerFetch(`/variants/${v.id}/assets`);
                if (assetsRes.ok) {
                    const assets = await assetsRes.json();
                    return { variantName: v.variantName, assets };
                }
            } catch (err) {
                console.error(err);
            }
            return { variantName: v.variantName, assets: [] };
        });

        const variantsWithAssets = await Promise.all(assetsPromises);
        const totalAssetsCount = variantsWithAssets.reduce((sum, item) => sum + item.assets.length, 0);

        if (totalAssetsCount === 0) {
            body.innerHTML = '<div class="empty-state"><i class="fa fa-inbox"></i><p>Không có tài sản nào trong kho cho sản phẩm này.</p></div>';
            return;
        }

        const pType = product.productType || 'ACCOUNT';
        let html = '';

        for (const item of variantsWithAssets) {
            if (item.assets.length === 0) continue;

            html += `<h4 style="margin: 16px 0 8px 0; font-size: 14px; font-weight: 600; color: var(--seller-ink);">Biến thể: ${item.variantName}</h4>`;
            html += '<table class="asset-list-table"><thead><tr>';

            if (pType === 'ACCOUNT') {
                html += '<th style="width: 40px;">STT</th><th>Tài khoản</th><th>Mật khẩu</th><th>Ghi chú</th><th>Trạng thái</th></tr></thead><tbody>';
                item.assets.forEach((asset, index) => {
                    html += `
                        <tr>
                            <td>${index + 1}</td>
                            <td><code>${asset.accountUsername || ''}</code></td>
                            <td><code>••••••</code> <small>(ẩn)</small></td>
                            <td>${asset.notes || ''}</td>
                            <td><span class="asset-status ${asset.isUsed ? 'sold' : ''}">${asset.isUsed ? 'Đã bán' : 'Còn hàng'}</span></td>
                        </tr>
                    `;
                });
            } else if (pType === 'KEY') {
                html += '<th style="width: 40px;">STT</th><th>Mã Key</th><th>Ghi chú</th><th>Trạng thái</th></tr></thead><tbody>';
                item.assets.forEach((asset, index) => {
                    html += `
                        <tr>
                            <td>${index + 1}</td>
                            <td><code>${asset.keyCode || ''}</code></td>
                            <td>${asset.notes || ''}</td>
                            <td><span class="asset-status ${asset.isUsed ? 'sold' : ''}">${asset.isUsed ? 'Đã bán' : 'Còn hàng'}</span></td>
                        </tr>
                    `;
                });
            } else if (pType === 'GAME_CARD') {
                html += '<th style="width: 40px;">STT</th><th>Mã Thẻ</th><th>PIN</th><th>Ghi chú</th><th>Trạng thái</th></tr></thead><tbody>';
                item.assets.forEach((asset, index) => {
                    html += `
                        <tr>
                            <td>${index + 1}</td>
                            <td><code>${asset.cardCode || 'N/A'}</code></td>
                            <td><code>••••</code></td>
                            <td>${asset.notes || ''}</td>
                            <td><span class="asset-status ${asset.isUsed ? 'sold' : ''}">${asset.isUsed ? 'Đã bán' : 'Còn hàng'}</span></td>
                        </tr>
                    `;
                });
            }

            html += '</tbody></table>';
        }

        body.innerHTML = html;

    } catch (err) {
        body.innerHTML = `<div class="empty-state" style="color:var(--seller-danger);"><i class="fa fa-exclamation-triangle"></i><p>Lỗi: ${err.message}</p></div>`;
    }
}

function closeAssetModal() {
    const modal = document.getElementById('assetModal');
    if (modal) modal.classList.remove('active');
}

// Expose functions globally for onclick attributes in dynamically rendered elements
window.viewAssets = viewAssets;
window.closeAssetModal = closeAssetModal;


// Helper to setup category selectors
async function setupCategorySelectors(mainSelect, subSelect, currentCategoryId = null, isFilter = false) {
    try {
        const res = await sellerFetch('/categories');
        if (!res.ok) return;
        
        const categories = await res.json();
        
        // Sort main categories: push "Khác" and "Dịch vụ khác" to the end
        categories.sort((a, b) => {
            const nameA = (a.name || '').toLowerCase().trim();
            const nameB = (b.name || '').toLowerCase().trim();
            const isKhacA = nameA === 'khác' || nameA === 'dịch vụ khác';
            const isKhacB = nameB === 'khác' || nameB === 'dịch vụ khác';
            if (isKhacA && !isKhacB) return 1;
            if (!isKhacA && isKhacB) return -1;
            return 0;
        });

        // Sort subCategories for each category: by ID asc (oldest first), then push "Khác" to end
        categories.forEach(parent => {
            if (parent.subCategories && parent.subCategories.length > 0) {
                parent.subCategories.sort((a, b) => {
                    const nameA = (a.name || '').toLowerCase().trim();
                    const nameB = (b.name || '').toLowerCase().trim();
                    const isKhacA = nameA.includes('khác');
                    const isKhacB = nameB.includes('khác');
                    if (isKhacA && !isKhacB) return 1;
                    if (!isKhacA && isKhacB) return -1;
                    // Sort by ID ascending so earlier-created (lower ID) appears first
                    return (a.id || 0) - (b.id || 0);
                });
            }
        });
        
        // Populate mainCategory
        mainSelect.innerHTML = `<option value="">${isFilter ? 'Tất cả danh mục chính' : '-- Chọn danh mục chính --'}</option>` + 
                              categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        
        // Change handler
        const updateSubCategories = (selectedParentId, selectValue = null) => {
            const parentCat = categories.find(c => c.id == selectedParentId);
            if (parentCat && parentCat.subCategories && parentCat.subCategories.length > 0) {
                subSelect.innerHTML = `<option value="">${isFilter ? 'Tất cả danh mục phụ' : '-- Chọn danh mục phụ --'}</option>` +
                                     parentCat.subCategories.map(sub => `<option value="${sub.id}">${sub.name}</option>`).join('');
                subSelect.disabled = false;
                if (!isFilter) subSelect.setAttribute('required', 'required');
                if (selectValue) {
                    subSelect.value = selectValue;
                }
            } else {
                subSelect.innerHTML = `<option value="">${isFilter ? 'Tất cả danh mục phụ' : '-- Không có danh mục phụ --'}</option>`;
                subSelect.disabled = true;
                subSelect.removeAttribute('required');
                subSelect.value = '';
            }
        };
        
        mainSelect.addEventListener('change', () => {
            updateSubCategories(mainSelect.value);
        });
        
        // If initializing with an existing category ID (edit mode)
        if (currentCategoryId) {
            let foundParent = null;
            let foundSub = null;
            
            for (const cat of categories) {
                if (cat.id == currentCategoryId) {
                    foundParent = cat;
                    break;
                }
                if (cat.subCategories) {
                    const sub = cat.subCategories.find(s => s.id == currentCategoryId);
                    if (sub) {
                        foundParent = cat;
                        foundSub = sub;
                        break;
                    }
                }
            }
            
            if (foundParent) {
                mainSelect.value = foundParent.id;
                if (foundSub) {
                    updateSubCategories(foundParent.id, foundSub.id);
                } else {
                    updateSubCategories(foundParent.id);
                }
            }
        }
    } catch (err) {
        console.error('Error setupCategorySelectors:', err);
    }
}

// ==============================================================================
// 5. PRODUCT ADD
// ==============================================================================
async function initProductAdd() {
    const mainSelect = document.getElementById('mainCategory');
    const subSelect = document.getElementById('subCategory');
    if (!mainSelect || !subSelect) return;

    try {
        await setupCategorySelectors(mainSelect, subSelect);


        // Setup Variants UI
        let variantCount = 0;
        const variantsContainer = document.getElementById('variantsContainer');
        const btnAddVariant = document.getElementById('btnAddVariant');

        const addVariantCard = () => {
            variantCount++;
            const currentIdx = variantCount;
            const div = document.createElement('div');
            div.className = 'variant-card';
            div.dataset.index = currentIdx;
            div.innerHTML = `
                <div class="variant-card__header">
                    <span>Biến thể #${currentIdx}</span>
                    <button type="button" class="variant-card__delete"><i class="fa fa-trash"></i> Xóa</button>
                </div>
                <div class="variant-card__grid">
                    <div class="profile-edit-form__group" style="margin-bottom: 0;">
                        <label>Tên biến thể <span style="color: #ef4444;">*</span></label>
                        <input type="text" class="variant-name-input" placeholder="VD: Gói 1 tháng" required autocomplete="off">
                    </div>
                    <div class="profile-edit-form__group" style="margin-bottom: 0;">
                        <label>Giá bán (VND) <span style="color: #ef4444;">*</span></label>
                        <input type="number" class="variant-price-input" placeholder="VD: 50000" min="0" required autocomplete="off">
                    </div>
                </div>
            `;

            // Delete action
            div.querySelector('.variant-card__delete').addEventListener('click', () => {
                div.remove();
            });

            variantsContainer.appendChild(div);
        };

        if (btnAddVariant) {
            btnAddVariant.addEventListener('click', addVariantCard);
        }

        // Add 1 default variant card
        if (variantsContainer) {
            addVariantCard();
        }

        const form = document.querySelector('.profile-edit-form');
        if (form) {
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                const name = document.getElementById('productName').value.trim();
                const description = document.getElementById('description').value.trim();
                const userGuide = document.getElementById('userGuide') ? document.getElementById('userGuide').value.trim() : '';
                const categoryId = subSelect.value || mainSelect.value;
                const typeEl = document.querySelector('input[name="productType"]:checked');
                const productType = typeEl ? typeEl.value : 'ACCOUNT';

                if (!name) return showToast('Vui lòng nhập tên sản phẩm.', 'error');
                if (!categoryId) return showToast('Vui lòng chọn danh mục.', 'error');

                const variantCards = variantsContainer.querySelectorAll('.variant-card');
                if (variantCards.length === 0) {
                    return showToast('Sản phẩm phải có ít nhất 1 biến thể.', 'error');
                }

                const variants = [];
                for (let card of variantCards) {
                    const varName = card.querySelector('.variant-name-input').value.trim();
                    const varPrice = card.querySelector('.variant-price-input').value.trim();

                    if (!varName) return showToast('Có biến thể chưa nhập tên.', 'error');
                    if (!varPrice) return showToast('Có biến thể chưa nhập giá bán.', 'error');

                    variants.push({
                        variantName: varName,
                        priceVnd: varPrice
                    });
                }

                let mainProductImageUrl = '';
                
                const productImageInput = document.getElementById('productImage');
                if (productImageInput && productImageInput.files.length > 0) {
                    try {
                        const file = productImageInput.files[0];
                        const base64Data = await new Promise((resolve) => {
                            const reader = new FileReader();
                            reader.onload = (ev) => resolve(ev.target.result);
                            reader.readAsDataURL(file);
                        });
                        const uploadRes = await sellerFetch('/upload-image', {
                            method: 'POST',
                            body: JSON.stringify({ image: base64Data })
                        });
                        if (uploadRes.ok) {
                            const uploadData = await uploadRes.json();
                            mainProductImageUrl = uploadData.url;
                        } else {
                            showToast('Không thể tải lên ảnh sản phẩm.', 'error');
                            return;
                        }
                    } catch (e) {
                        console.error(e);
                        return;
                    }
                }

                try {
                    const postRes = await sellerFetch('/products', {
                        method: 'POST',
                        body: JSON.stringify({ 
                            name, 
                            description, 
                            userGuide,
                            categoryId, 
                            productType, 
                            image: mainProductImageUrl,
                            variants 
                        })
                    });
                    const postData = await postRes.json();
                    if (!postRes.ok) throw new Error(postData.message || 'Đăng sản phẩm thất bại.');
                    showToast('Tạo sản phẩm và biến thể thành công!');
                    setTimeout(() => {
                        window.location.href = `/seller/products/edit?id=${postData.id}`;
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

    const mainSelect = document.getElementById('mainCategory');
    const subSelect = document.getElementById('subCategory');
    const form = document.querySelector('.profile-edit-form');
    const tbody = document.querySelector('.seller-table tbody') || document.querySelector('.admin-table tbody');
    if (!form || !tbody || !mainSelect || !subSelect) return;

    let categoryData = [];
    let currentProductImageUrl = '';
    let isImageUpdated = false;
    let base64ImageData = '';

    // Lắng nghe sự kiện chọn file ảnh sản phẩm
    const imageInput = document.getElementById('productImageInput');
    const imagePreview = document.getElementById('productImagePreview');
    if (imageInput && imagePreview) {
        imageInput.addEventListener('change', async (e) => {
            if (imageInput.files && imageInput.files.length > 0) {
                const file = imageInput.files[0];
                try {
                    base64ImageData = await new Promise((resolve) => {
                        const reader = new FileReader();
                        reader.onload = (ev) => resolve(ev.target.result);
                        reader.readAsDataURL(file);
                    });
                    imagePreview.src = base64ImageData;
                    isImageUpdated = true;
                } catch (err) {
                    console.error('Lỗi đọc file ảnh sản phẩm:', err);
                }
            }
        });
    }

    try {
        // Load Product Detail
        const pRes = await sellerFetch(`/products/${productId}`);
        if (!pRes.ok) throw new Error('Không thể tải thông tin sản phẩm.');
        const p = await pRes.json();

        // Setup categories
        await setupCategorySelectors(mainSelect, subSelect, p.categoryId);

        // Populate fields
        const subtitleEl = document.querySelector('.seller-card__subtitle') || document.querySelector('.view-header p');
        if (subtitleEl) subtitleEl.textContent = `Sản phẩm #${p.id} — ${p.name}`;
        document.getElementById('productName').value = p.name || '';
        document.getElementById('productName').placeholder = 'Nhập tên sản phẩm...';
        document.getElementById('description').value = p.description || '';
        document.getElementById('description').placeholder = 'Nhập mô tả sản phẩm...';
        if (document.getElementById('userGuide')) {
            document.getElementById('userGuide').value = p.userGuide || '';
            document.getElementById('userGuide').placeholder = 'Nhập hướng dẫn sử dụng sản phẩm...';
        }

        // Set Product Image Preview
        if (p.image) {
            currentProductImageUrl = p.image;
            if (imagePreview) {
                imagePreview.src = p.image;
            }
        }

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
                        <td><span class="badge ${statusClass}">${translateStatus(v.status)}</span></td>
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
                let finalImageUrl = currentProductImageUrl;

                // Nếu hình ảnh bị thay đổi, thực hiện upload trước
                if (isImageUpdated && base64ImageData) {
                    try {
                        const uploadRes = await sellerFetch('/upload-image', {
                            method: 'POST',
                            body: JSON.stringify({ image: base64ImageData })
                        });
                        if (uploadRes.ok) {
                            const uploadData = await uploadRes.json();
                            finalImageUrl = uploadData.url;
                        } else {
                            showToast('Không thể tải lên ảnh sản phẩm mới.', 'error');
                            return;
                        }
                    } catch (err) {
                        showToast('Lỗi tải lên hình ảnh sản phẩm.', 'error');
                        return;
                    }
                }

                const payload = {
                    name: document.getElementById('productName').value.trim(),
                    description: document.getElementById('description').value.trim(),
                    userGuide: document.getElementById('userGuide') ? document.getElementById('userGuide').value.trim() : '',
                    categoryId: subSelect.value || mainSelect.value,
                    image: finalImageUrl
                };

                try {
                    const putRes = await sellerFetch(`/products/${productId}`, {
                        method: 'PUT',
                        body: JSON.stringify(payload)
                    });
                    if (!putRes.ok) throw new Error('Cập nhật sản phẩm thất bại.');
                    showToast('Cập nhật sản phẩm thành công!');
                    currentProductImageUrl = finalImageUrl;
                    isImageUpdated = false;
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
        let productType = 'ACCOUNT';

        if (isEdit) {
            // Load Variant Details
            const res = await sellerFetch(`/variants/${variantId}`);
            if (!res.ok) throw new Error('Không thể tải thông tin biến thể.');
            const v = await res.json();

            currentProdId = v.productId;
            const subtitleEl1 = document.querySelector('.seller-card__subtitle') || document.querySelector('.view-header p');
            if (subtitleEl1) subtitleEl1.textContent = `Sản phẩm: ${v.productName}`;
            document.getElementById('variantName').value = v.variantName;
            document.getElementById('priceVnd').value = v.priceVnd;
            document.getElementById('status').value = v.status;

            // Fetch Product type
            const prodRes = await sellerFetch(`/products/${currentProdId}`);
            if (prodRes.ok) {
                const p = await prodRes.json();
                productType = p.productType || 'ACCOUNT';
            }

            // Set global productType and refresh layout
            window.productType = productType;
            if (typeof updateProductDisplay === 'function') updateProductDisplay(productType, v.productName);
            if (typeof window.switchAssetInputType === 'function') {
                window.switchAssetInputType(productType);
            } else if (typeof renderAssetFields === 'function') {
                renderAssetFields(productType);
            }

            // Update title to Edit variant mode
            const titleEl = document.querySelector('.seller-card__title');
            if (titleEl) {
                titleEl.textContent = 'Cập nhật biến thể & nhập kho tài sản số';
            }

            // Load existing assets
            const assetsRes = await sellerFetch(`/variants/${variantId}/assets`);
            if (assetsRes.ok) {
                const existingAssets = await assetsRes.json();
                assets = existingAssets.map(ea => {
                    const type = ea.assetType || productType;
                    if (type === 'ACCOUNT') {
                        return {
                            id: ea.id,
                            type: 'ACCOUNT',
                            username: ea.accountUsername,
                            password: ea.accountPassword || '',
                            notes: ea.notes || '',
                            isUsed: ea.isUsed === true
                        };
                    } else if (type === 'KEY') {
                        return {
                            id: ea.id,
                            type: 'KEY',
                            keyCode: ea.keyCode,
                            notes: ea.notes || '',
                            isUsed: ea.isUsed === true
                        };
                    } else if (type === 'GAME_CARD') {
                        return {
                            id: ea.id,
                            type: 'GAME_CARD',
                            cardCode: ea.cardCode,
                            notes: ea.notes || '',
                            isUsed: ea.isUsed === true
                        };
                    }
                }).filter(Boolean);

                // Switch the active tab to match the first asset type if exists
                if (assets.length > 0) {
                    const firstType = assets[0].type;
                    if (firstType && firstType !== productType) {
                        if (typeof window.switchAssetInputType === 'function') {
                            window.switchAssetInputType(firstType);
                        }
                    }
                }

                if (typeof updateAssetList === 'function') updateAssetList();
            }
        } else {
            // Load Product Info to display
            const prodRes = await sellerFetch(`/products/${productId}`);
            if (prodRes.ok) {
                const p = await prodRes.json();
                const subtitleEl2 = document.querySelector('.seller-card__subtitle') || document.querySelector('.view-header p');
                if (subtitleEl2) subtitleEl2.textContent = `Sản phẩm: ${p.name}`;
                productType = p.productType || 'ACCOUNT';
            }

            // Set global productType and refresh layout
            window.productType = productType;
            if (typeof updateProductDisplay === 'function') updateProductDisplay(productType);
            if (typeof window.switchAssetInputType === 'function') {
                window.switchAssetInputType(productType);
            } else if (typeof renderAssetFields === 'function') {
                renderAssetFields(productType);
            }
        }

        // Back link update
        const backBtn = document.querySelector('.seller-card__header .profile-button--secondary');
        const cancelBtn = form.querySelector('.profile-actions .profile-button--secondary');
        const backUrl = `/seller/products/edit?id=${currentProdId}`;
        if (backBtn) backBtn.href = backUrl;
        if (cancelBtn) cancelBtn.href = backUrl;

        // Submitting variant form
        const saveBtn = form.querySelector('.profile-actions .profile-button--primary') || form.querySelector('button[type="submit"]') || form.querySelector('.profile-button--primary');
        if (saveBtn) {
            saveBtn.removeAttribute('disabled');
            saveBtn.addEventListener('click', async (e) => {
                e.preventDefault();
                
                const variantName = document.getElementById('variantName').value.trim();
                const priceVnd = document.getElementById('priceVnd').value;
                const status = document.getElementById('status').value;

                if (!variantName || !priceVnd) {
                    showToast('Vui lòng nhập tên biến thể và giá bán.', 'error');
                    return;
                }

                // Check assets: must have at least 1 asset if creating new variant
                const newAssets = assets.filter(a => !a.id);
                if (productType !== 'SERVICE') {
                    if (!isEdit && assets.length === 0) {
                        showToast('Bạn phải nhập ít nhất 1 tài sản trước khi lưu.', 'error');
                        return;
                    }
                }

                const payload = {
                    variantName: variantName,
                    priceVnd: priceVnd,
                    stock: productType === 'SERVICE' ? 99999 : assets.length,
                    status: status,
                    productId: currentProdId
                };

                try {
                    const method = isEdit ? 'PUT' : 'POST';
                    const endpoint = isEdit ? `/variants/${variantId}` : '/variants';

                    const actionRes = await sellerFetch(endpoint, {
                        method: method,
                        body: JSON.stringify(payload)
                    });
                    const actionData = await actionRes.json();
                    if (!actionRes.ok) throw new Error(actionData.message || 'Thao tác thất bại.');

                    const savedVariantId = isEdit ? variantId : actionData.id;

                    // Batch save any new assets
                    if (newAssets.length > 0) {
                        const groups = {};
                        newAssets.forEach(a => {
                            const type = a.type || 'ACCOUNT';
                            if (!groups[type]) groups[type] = [];
                            groups[type].push(a);
                        });

                        for (const type of Object.keys(groups)) {
                            const typeAssets = groups[type];
                            const mappedAssets = typeAssets.map(a => {
                                if (type === 'ACCOUNT') {
                                    return {
                                        accountUsername: a.username,
                                        accountPassword: a.password,
                                        notes: a.notes
                                    };
                                } else if (type === 'KEY') {
                                    return {
                                        keyCode: a.keyCode,
                                        notes: a.notes
                                    };
                                } else if (type === 'GAME_CARD') {
                                    return {
                                        cardCode: a.cardCode,
                                        cardPin: "",
                                        notes: a.notes
                                    };
                                }
                            });

                            const assetRes = await sellerFetch('/digital-assets', {
                                method: 'POST',
                                body: JSON.stringify({
                                    variantId: savedVariantId,
                                    assetType: type,
                                    assets: mappedAssets
                                })
                            });
                            const assetData = await assetRes.json();
                            if (!assetRes.ok) throw new Error(assetData.message || 'LÆ°u tÃ i sáº£n tháº¥t báº¡i.');
                        }
                    }

                    showToast(isEdit ? 'Đã cập nhật biến thể & tài sản!' : 'Đã tạo biến thể & tài sản thành công!');
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
    const tbody = document.querySelector('.seller-table tbody') || document.querySelector('.admin-table tbody');
    if (!tbody) return;

    try {
        const res = await sellerFetch('/transactions');
        if (!res.ok) throw new Error('Không thể tải lịch sử bán hàng.');
        const transactions = await res.json();
        window.allTransactions = transactions;

        let searchQuery = ''; // Từ khóa tìm kiếm giao dịch
        let statusFilter = ''; // Trạng thái giao dịch cần lọc
        let sortMode = 'newest'; // Tiêu chí sắp xếp mặc định

        const btnFilter = document.getElementById('btnFilterTransaction');
        const searchInput = document.getElementById('transactionSearch');
        if (searchInput) {
            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    if (btnFilter) btnFilter.click();
                }
            });
        }

        // Lấy nút Lọc giao dịch và gán sự kiện
        if (btnFilter) {
            btnFilter.addEventListener('click', () => {
                const statusSelect = document.getElementById('transactionStatus');
                const sortSelect = document.getElementById('transactionSort');
                
                // Cập nhật giá trị lọc
                searchQuery = searchInput ? searchInput.value.toLowerCase().trim() : '';
                statusFilter = statusSelect ? statusSelect.value : '';
                sortMode = sortSelect ? sortSelect.value : 'newest';
                
                // Gọi hàm hiển thị lại danh sách
                renderTransactions();
            });
        }

        // Hàm xử lý lọc, sắp xếp và render danh sách giao dịch
        function renderTransactions() {
            // Bước 1: Lọc giao dịch
            let filteredTx = transactions.filter(t => {
                // Lọc theo từ khóa (ID, Tên sản phẩm, Tên biến thể, Email KH)
                if (searchQuery) {
                    const matchId = (t.id + '').includes(searchQuery);
                    const matchProduct = t.productName && t.productName.toLowerCase().includes(searchQuery);
                    const matchVariant = t.variantName && t.variantName.toLowerCase().includes(searchQuery);
                    const matchEmail = t.customerEmail && t.customerEmail.toLowerCase().includes(searchQuery);
                    if (!matchId && !matchProduct && !matchVariant && !matchEmail) return false;
                }
                
                // Lọc theo trạng thái đơn hàng
                if (statusFilter) {
                    const st = t.status.toLowerCase();
                    if (statusFilter === 'pending' && !st.includes('pending')) return false;
                    if (statusFilter === 'held' && !st.includes('held')) return false;
                    if (statusFilter === 'completed' && !st.includes('completed')) return false;
                    if (statusFilter === 'complaint' && !st.includes('disputed') && !st.includes('khiếu nại')) return false;
                }
                return true;
            });

            // Bước 2: Sắp xếp giao dịch
            if (sortMode === 'newest') {
                filteredTx.sort((a, b) => b.id - a.id); // Mới nhất
            } else if (sortMode === 'oldest') {
                filteredTx.sort((a, b) => a.id - b.id); // Cũ nhất
            } else if (sortMode === 'highest_amount') {
                filteredTx.sort((a, b) => b.amountVnd - a.amountVnd); // Số tiền cao đến thấp
            } else if (sortMode === 'lowest_amount') {
                filteredTx.sort((a, b) => a.amountVnd - b.amountVnd); // Số tiền thấp đến cao
            }

            if (filteredTx.length === 0) {
                tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; padding: 40px; color: var(--seller-muted);"><i class="fa fa-inbox" style="font-size: 24px; display: block; margin-bottom: 8px;"></i> Không tìm thấy giao dịch nào.</td></tr>';
                return;
            }

            tbody.innerHTML = filteredTx.map(t => {
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
                        <td><a href="/messages?sellerId=${t.customerId}" style="text-decoration: underline; color: inherit; cursor: pointer;" title="Nhắn tin với khách hàng">${t.customerEmail}</a></td>
                        <td class="text-right">${formatVND(t.amountVnd)}</td>
                        <td class="text-right text-success">+${formatVND(t.netEarningVnd)}</td>
                        <td><span class="badge ${badgeClass}">${translateStatus(t.status)}</span></td>
                        <td>${t.createdAt.replace('T', ' ').substring(0, 16)}</td>
                        <td class="text-right">
                            <a class="icon-button" href="#" title="Chi tiết" onclick="showTxDetail('${t.id}'); return false;"><i class="fa fa-eye"></i></a>
                        </td>
                    </tr>
                `;
            }).join('');
        }

        renderTransactions();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==============================================================
// 9. WITHDRAWALS VIEW
// ==============================================================
async function initWithdrawals() {
    const tbody = document.querySelector('.seller-table tbody') || document.querySelector('.admin-table tbody');
    if (!tbody) return;

    try {
        // Load bank info check
        const infoRes = await sellerFetch('/shop-info');
        const infoData = await infoRes.json();

        // Populate bank select with actual seller bank info
        const bankAccountSelect = document.getElementById('bankAccount');
        if (bankAccountSelect) {
            if (infoData.bankName && infoData.accountNumber) {
                bankAccountSelect.innerHTML = `<option value="">${infoData.bankName} — ${infoData.accountNumber} — ${infoData.accountHolder ? infoData.accountHolder.toUpperCase() : ''}</option>`;
            } else {
                bankAccountSelect.innerHTML = `<option value="">(Chưa cấu hình thông tin ngân hàng nhận tiền)</option>`;
            }
        }

        // Check if bank info is set, if not warn
        if (!infoData.bankName || !infoData.accountNumber) {
            const warningAlert = document.createElement('div');
            warningAlert.className = 'badge critical';
            warningAlert.style.width = '100%';
            warningAlert.style.marginBottom = '20px';
            warningAlert.style.borderRadius = '8px';
            warningAlert.style.padding = '14px';
            warningAlert.innerHTML = '<i class="fa fa-warning"></i> Bạn chưa thiết lập thông tin Ngân hàng! Vui lòng cấu hình tại <a href="/seller/shop-info" style="color: inherit; text-decoration: underline;">Thông tin cửa hàng</a> trước khi làm lệnh rút.';
            
            const card = tbody.closest('.seller-card') || tbody.closest('.admin-card');
            const wrap = tbody.closest('.seller-table-wrap') || tbody.closest('.table-wrap');
            if (card && wrap) {
                card.insertBefore(warningAlert, wrap);
            }
        }

        // Load dashboard stats for balances
        const dashRes = await sellerFetch('/dashboard');
        const dashData = await dashRes.json();

        // Load withdrawals history
        const wRes = await sellerFetch('/withdrawals');
        if (!wRes.ok) throw new Error('Không thể tải lịch sử rút tiền.');
        const withdrawals = await wRes.json();

        const pendingAmount = withdrawals.filter(w => w.status === 'Pending').reduce((sum, w) => sum + w.amountVnd, 0);
        const pendingCount = withdrawals.filter(w => w.status === 'Pending').length;

        // Support old layout balance and pending elements
        const balanceEl = document.querySelector('.balance-highlight__value');
        if (balanceEl) balanceEl.textContent = formatVND(dashData.balanceVnd);

        const pendingValueEl = document.querySelector('.stat-card__value');
        if (pendingValueEl) pendingValueEl.textContent = formatVND(pendingAmount);

        const pendingHintEl = document.querySelector('.stat-card__hint');
        if (pendingHintEl) pendingHintEl.textContent = `${pendingCount} lệnh Pending`;

        // Support new layout balance and pending elements (if any)
        const statCards = document.querySelectorAll('.stats-grid-4 .stat-card-value');
        if (statCards.length >= 2) {
            statCards[0].textContent = formatVND(dashData.balanceVnd);
            statCards[1].textContent = formatVND(pendingAmount);
        }
        const trends = document.querySelectorAll('.stats-grid-4 .stat-card-trend');
        if (trends.length >= 2) {
            trends[1].textContent = `${pendingCount} lệnh đang xử lý`;
        }

        if (withdrawals.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">Chưa có yêu cầu rút tiền nào.</td></tr>';
        } else {
            tbody.innerHTML = withdrawals.map(w => {
                const badgeClass = getBadgeClassForStatus(w.status);
                return `
                    <tr>
                        <td>#WD-${w.id}</td>
                        <td class="text-right">${formatVND(w.amountVnd)}</td>
                        <td>${w.bankName} (${w.accountNumber})</td>
                        <td>${w.createdAt.replace('T', ' ').substring(0, 10)}</td>
                        <td><span class="badge ${badgeClass}">${translateStatus(w.status)}</span></td>
                        <td class="text-right">
                            <div class="row-actions">
                                <a class="icon-button" href="/seller/withdrawals/detail?id=${w.id}" title="Xem chi tiết"><i class="fa fa-eye"></i></a>
                            </div>
                        </td>
                    </tr>
                `;
            }).join('');
        }

        // Load withdrawal configuration dynamically
        let minLimit = 50000;
        let maxLimit = 50000000;
        let feePercent = 1.5;
        let minFee = 5000; // Thêm minFee mặc định
        let require2FA = true;

        try {
            const configRes = await sellerFetch('/withdrawals/config');
            if (configRes.ok) {
                const configData = await configRes.json();
                minLimit = configData.minWithdrawalLimit || minLimit;
                maxLimit = configData.maxWithdrawalLimit || maxLimit;
                feePercent = configData.withdrawalFeePercent !== undefined ? configData.withdrawalFeePercent : feePercent;
                require2FA = configData.requireWithdraw2FA !== undefined ? configData.requireWithdraw2FA : require2FA;
            }
        } catch (e) {
            console.error("Failed to load withdrawal config", e);
        }

        // Update subtitle and input hint dynamically
        const subtitleEl = document.querySelector('.view-header p') || document.querySelector('.seller-card__subtitle');
        if (subtitleEl) {
            subtitleEl.textContent = `Rút tiền về tài khoản ngân hàng — tối thiểu ${formatVND(minLimit)}`;
        }
        
        const hintEl = document.querySelector('.profile-edit-form__hint');
        if (hintEl) {
            hintEl.textContent = `Hạn mức: ${formatVND(minLimit)} - ${formatVND(maxLimit)} · Phí rút tiền: ${feePercent}%`;
        }

        const inputEl = document.getElementById('withdrawAmount');
        if (inputEl) {
            inputEl.min = minLimit;
            inputEl.value = minLimit;

            // Live fee feedback
            let feeInfoEl = document.getElementById('withdrawalFeeInfo');
            if (!feeInfoEl) {
                feeInfoEl = document.createElement('div');
                feeInfoEl.id = 'withdrawalFeeInfo';
                feeInfoEl.style.marginTop = '12px';
                feeInfoEl.style.fontSize = '14px';
                feeInfoEl.style.color = '#475569';
                feeInfoEl.style.background = '#f8fafc';
                feeInfoEl.style.padding = '12px';
                feeInfoEl.style.borderRadius = '8px';
                feeInfoEl.style.border = '1px solid #e2e8f0';
                inputEl.parentNode.appendChild(feeInfoEl);
            }

            const updateFeeDisplay = () => {
                const val = parseInt(inputEl.value) || 0;
                if (val <= 0) {
                    feeInfoEl.innerHTML = '';
                    return;
                }
                const fee = Math.floor(val * (feePercent / 100));
                const total = val + fee;
                feeInfoEl.innerHTML = `
                    <div style="display:flex; justify-content:space-between; margin-bottom:6px;">
                        <span>Số tiền yêu cầu rút:</span>
                        <strong>${formatVND(val)}</strong>
                    </div>
                    <div style="display:flex; justify-content:space-between; margin-bottom:6px;">
                        <span>Phí dịch vụ rút tiền:</span>
                        <span style="color:#ef4444; font-weight:500;">${formatVND(fee)}</span>
                    </div>
                    <div style="display:flex; justify-content:space-between; border-top:1px dashed #cbd5e1; padding-top:6px; font-weight:600; color: #1e293b;">
                        <span>Tổng số tiền trừ vào ví:</span>
                        <span style="color:#2563eb;">${formatVND(total)}</span>
                    </div>
                `;
            };

            inputEl.addEventListener('input', updateFeeDisplay);
            updateFeeDisplay(); // Initial trigger
        }

        // Handle Request Withdrawal Button
        const withdrawBtn = document.querySelector('.profile-button--primary');
        if (withdrawBtn) {
            withdrawBtn.addEventListener('click', async () => {
                if (!infoData || !infoData.bankName || !infoData.accountNumber) {
                    showToast('Vui lòng thiết lập tài khoản ngân hàng nhận tiền tại mục "Thông tin cửa hàng" trước khi rút tiền.', 'error');
                    return;
                }

                let amount = minLimit;
                if (inputEl) {
                    amount = parseInt(inputEl.value) || 0;
                } else {
                    const amountText = prompt(`Nhập số tiền muốn rút (Hạn mức: ${formatVND(minLimit)} - ${formatVND(maxLimit)}):`);
                    if (amountText === null) return;
                    amount = parseInt(amountText.replace(/,/g, '')) || 0;
                }

                if (isNaN(amount) || amount < minLimit) {
                    showToast(`Số tiền rút tối thiểu là ${formatVND(minLimit)}.`, 'error');
                    return;
                }
                if (amount > maxLimit) {
                    showToast(`Số tiền rút tối đa là ${formatVND(maxLimit)}.`, 'error');
                    return;
                }

                let fee = Math.floor(amount * (feePercent / 100));
                const totalDeducted = amount + fee;

                if (dashData.balanceVnd < totalDeducted) {
                    showToast(`Số dư ví khả dụng không đủ (cần ${formatVND(totalDeducted)} bao gồm cả phí dịch vụ).`, 'error');
                    return;
                }

                const otpModalEl = document.getElementById('otpWithdrawModal');
                const otpInputEl = document.getElementById('withdrawOtpInput');
                const otpErrorEl = document.getElementById('withdrawOtpError');
                const btnOtpConfirm = document.getElementById('btnSubmitWithdrawalWithOtp');

                if (otpModalEl && otpInputEl && btnOtpConfirm) {
                    try {
                        showToast('Đang gửi mã OTP đến email của bạn...', 'info');

                        // Reset modal inputs and display modal immediately
                        otpInputEl.value = '';
                        btnOtpConfirm.disabled = false;
                        btnOtpConfirm.innerHTML = 'Xác nhận';
                        otpModalEl.style.display = 'flex';

                        // Request OTP
                        const otpRes = await sellerFetch('/withdrawals/send-otp', { method: 'POST' });
                        const otpData = await otpRes.json();
                        if (!otpRes.ok) {
                            const errText = otpData.message || 'Không thể gửi mã OTP.';
                            if (otpErrorEl) {
                                otpErrorEl.textContent = errText;
                                otpErrorEl.style.color = '#ef4444';
                            }
                            showToast(errText, 'error');
                        } else {
                            if (otpErrorEl) otpErrorEl.textContent = '';
                            showToast(otpData.message || 'Mã OTP đã được gửi về email của bạn.', 'success');
                        }

                             // Setup Resend Button logic with 60s cooldown
                             const btnResend = document.getElementById('btnResendOtp');
                             if (btnResend) {
                                 let countdown = 60;
                                 let timer = null;
                                 const startCountdown = () => {
                                     const activeBtn = document.getElementById('btnResendOtp');
                                     if (!activeBtn) return;
                                     activeBtn.disabled = true;
                                     activeBtn.style.color = '#94a3b8';
                                     activeBtn.style.cursor = 'not-allowed';
                                     activeBtn.style.textDecoration = 'none';
                                     activeBtn.textContent = `Gửi lại mã (${countdown}s)`;
                                     if (timer) clearInterval(timer);
                                     timer = setInterval(() => {
                                         countdown--;
                                         const currentBtn = document.getElementById('btnResendOtp');
                                         if (!currentBtn) {
                                             clearInterval(timer);
                                             return;
                                         }
                                         if (countdown <= 0) {
                                             clearInterval(timer);
                                             currentBtn.disabled = false;
                                             currentBtn.style.color = '#2563eb';
                                             currentBtn.style.cursor = 'pointer';
                                             currentBtn.style.textDecoration = 'underline';
                                             currentBtn.textContent = 'Gửi lại mã';
                                         } else {
                                             currentBtn.textContent = `Gửi lại mã (${countdown}s)`;
                                         }
                                     }, 1000);
                                 };
 
                                 // Clean old listeners
                                 const newBtnResend = btnResend.cloneNode(true);
                                 btnResend.parentNode.replaceChild(newBtnResend, btnResend);
 
                                 newBtnResend.addEventListener('click', async () => {
                                     try {
                                         showToast('Đang gửi lại mã OTP đến email của bạn...', 'info');
                                         const resendRes = await sellerFetch('/withdrawals/send-otp', { method: 'POST' });
                                         const resendData = await resendRes.json();
                                         if (!resendRes.ok) throw new Error(resendData.message || 'Không thể gửi lại mã OTP.');
 
                                         showToast(resendData.message || 'Mã OTP đã được gửi lại về email của bạn.', 'success');
                                         countdown = 60;
                                         startCountdown();
                                     } catch (err) {
                                         showToast(err.message, 'error');
                                     }
                                 });
 
                                 countdown = 60;
                                 startCountdown();
                             }

                            // Clean old event listeners
                            const newBtnOtpConfirm = btnOtpConfirm.cloneNode(true);
                            btnOtpConfirm.parentNode.replaceChild(newBtnOtpConfirm, btnOtpConfirm);

                            newBtnOtpConfirm.addEventListener('click', async () => {
                                const otp = otpInputEl.value.trim();
                                if (!otp) {
                                    if (otpErrorEl) otpErrorEl.textContent = 'Vui lòng nhập mã OTP.';
                                    return;
                                }
                                if (otp.length < 6) {
                                    if (otpErrorEl) otpErrorEl.textContent = 'Mã OTP phải có 6 chữ số.';
                                    return;
                                }

                                newBtnOtpConfirm.disabled = true;
                                newBtnOtpConfirm.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang xác thực...';

                                try {
                                    const postRes = await sellerFetch('/withdrawals', {
                                        method: 'POST',
                                        body: JSON.stringify({ amountVnd: amount, otp: otp })
                                    });
                                    const postData = await postRes.json();
                                    if (!postRes.ok) throw new Error(postData.message || 'Rút tiền thất bại.');

                                    showToast(postData.message || 'Đã tạo yêu cầu rút tiền thành công!', 'success');
                                    otpModalEl.style.display = 'none';
                                    setTimeout(() => window.location.reload(), 1500);
                                } catch (err) {
                                    if (otpErrorEl) otpErrorEl.textContent = err.message;
                                    newBtnOtpConfirm.disabled = false;
                                    newBtnOtpConfirm.innerHTML = 'Xác nhận';
                                }
                            });

                        } catch (err) {
                            showToast(err.message, 'error');
                        }
                    } else {
                        // Fallback to native confirm/prompt if modal HTML is missing
                        try {
                            const otpRes = await sellerFetch('/withdrawals/send-otp', { method: 'POST' });
                            const otpData = await otpRes.json();
                            if (!otpRes.ok) throw new Error(otpData.message || 'Không thể gửi mã OTP.');

                            showToast(otpData.message || 'Mã OTP đã được gửi về email của bạn.', 'success');

                            const otpText = prompt('Vui lòng nhập mã OTP 6 chữ số được gửi tới email của bạn để hoàn tất:');
                            if (otpText === null) return;
                            const otp = otpText.trim();
                            if (!otp) {
                                showToast('Bạn chưa nhập mã OTP.', 'error');
                                return;
                            }

                            const postRes = await sellerFetch('/withdrawals', {
                                method: 'POST',
                                body: JSON.stringify({ amountVnd: amount, otp: otp })
                            });
                            const postData = await postRes.json();
                            if (!postRes.ok) throw new Error(postData.message || 'Rút tiền thất bại.');

                            showToast(postData.message || 'Đã tạo yêu cầu rút tiền thành công!', 'success');
                            setTimeout(() => window.location.reload(), 1500);
                        } catch (err) {
                            showToast(err.message, 'error');
                        }
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

        // Stats card display
        const statCards = document.querySelectorAll('.stats-grid-4 .stat-card-value');
        if (statCards.length >= 2) {
            statCards[0].textContent = formatVND(stats.escrowBalance);
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

        const ratingFilter = document.getElementById('ratingFilter');
        const cardTotal = document.getElementById('cardTotalReviews');
        const cardFive = document.getElementById('cardFiveStars');
        const cardLow = document.getElementById('cardLowStars');

        function updateActiveStatCard(activeId) {
            [cardTotal, cardFive, cardLow].forEach(card => {
                if (card) card.classList.remove('active');
            });
            const activeCard = document.getElementById(activeId);
            if (activeCard) activeCard.classList.add('active');
        }

        function renderReviewsList(filteredList) {
            if (filteredList.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding: 30px; color: var(--seller-muted);">Không có đánh giá nào phù hợp bộ lọc.</td></tr>';
                return;
            }

            tbody.innerHTML = filteredList.map(r => {
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
        }

        function applyFilter(filterVal) {
            let filtered = reviews;
            if (filterVal === '5') {
                filtered = reviews.filter(r => r.rating === 5);
            } else if (filterVal === '4') {
                filtered = reviews.filter(r => r.rating === 4);
            } else if (filterVal === '3') {
                filtered = reviews.filter(r => r.rating === 3);
            } else if (filterVal === '2') {
                filtered = reviews.filter(r => r.rating === 2);
            } else if (filterVal === '1') {
                filtered = reviews.filter(r => r.rating === 1);
            } else if (filterVal === 'low') {
                filtered = reviews.filter(r => r.rating <= 2);
            }
            renderReviewsList(filtered);
        }

        // Initialize display
        renderReviewsList(reviews);

        // Bind filter select change listener
        if (ratingFilter) {
            ratingFilter.addEventListener('change', function() {
                const val = this.value;
                applyFilter(val);
                if (val === 'all') {
                    updateActiveStatCard('cardTotalReviews');
                } else if (val === '5') {
                    updateActiveStatCard('cardFiveStars');
                } else if (val === 'low') {
                    updateActiveStatCard('cardLowStars');
                } else {
                    updateActiveStatCard('');
                }
            });
        }

        // Bind stat cards click events to filter
        if (cardTotal) {
            cardTotal.addEventListener('click', () => {
                if (ratingFilter) ratingFilter.value = 'all';
                updateActiveStatCard('cardTotalReviews');
                applyFilter('all');
            });
        }

        if (cardFive) {
            cardFive.addEventListener('click', () => {
                if (ratingFilter) ratingFilter.value = '5';
                updateActiveStatCard('cardFiveStars');
                applyFilter('5');
            });
        }

        if (cardLow) {
            cardLow.addEventListener('click', () => {
                if (ratingFilter) ratingFilter.value = 'low';
                updateActiveStatCard('cardLowStars');
                applyFilter('low');
            });
        }

    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==============================================================
// 13. COMPLAINTS LIST VIEW
// ==============================================================
async function initComplaints() {
    const tbody = document.querySelector('.seller-table tbody') || document.querySelector('.admin-table tbody');
    if (!tbody) return;

    try {
        const res = await sellerFetch('/complaints');
        if (!res.ok) throw new Error('Không thể tải danh sách khiếu nại.');
        const complaints = await res.json();

        // Bind stats card values
        const statCards = document.querySelectorAll('.stats-grid-4 .stat-card-value');
        if (statCards.length >= 4) {
            const openCount = complaints.filter(c => {
                const s = (c.status || '').toLowerCase();
                return s === 'open' || s === 'pending_review' || s === 'pending';
            }).length;
            const inProgressCount = complaints.filter(c => {
                const s = (c.status || '').toLowerCase();
                return s === 'in_progress' || s === 'inprogress';
            }).length;
            const resolvedCount = complaints.filter(c => {
                const s = (c.status || '').toLowerCase();
                return s === 'resolved' || s === 'closed';
            }).length;
            
            const heldCount = complaints.filter(c => {
                const s = (c.status || '').toLowerCase();
                return s !== 'resolved' && s !== 'closed';
            }).length;
            const heldAmount = complaints.filter(c => {
                const s = (c.status || '').toLowerCase();
                return s !== 'resolved' && s !== 'closed';
            }).reduce((sum, c) => sum + c.amountVnd, 0);

            statCards[0].textContent = openCount;
            statCards[1].textContent = inProgressCount;
            statCards[2].textContent = resolvedCount;
            statCards[3].textContent = formatVND(heldAmount);
            
            const trends = document.querySelectorAll('.stats-grid-4 .stat-card-trend');
            if (trends.length >= 4) {
                trends[3].textContent = `${heldCount} giao dịch tranh chấp`;
            }
        }

        const searchInput = document.querySelector('input[placeholder="Tìm kiếm khiếu nại, mã GD..."]');
        const statusSelect = document.querySelector('select[aria-label="Lọc trạng thái"]');
        
        const mainSelect = document.getElementById('mainCategoryFilter');
        const subSelect = document.getElementById('subCategoryFilter');

        // Filter states
        let searchQuery = searchInput ? searchInput.value.toLowerCase().trim() : '';
        let statusFilter = statusSelect ? statusSelect.value : '';
        let mainCategoryFilter = mainSelect ? mainSelect.value : '';
        let subCategoryFilter = subSelect ? subSelect.value : '';

        if (mainSelect && subSelect) {
            setupCategorySelectors(mainSelect, subSelect, null, true);
        }

        const btnFilter = document.getElementById('btnFilterComplaint');

        if (searchInput) {
            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    if (btnFilter) btnFilter.click();
                }
            });
        }

        if (btnFilter) {
            btnFilter.addEventListener('click', () => {
                searchQuery = searchInput ? searchInput.value.toLowerCase().trim() : '';
                statusFilter = statusSelect ? statusSelect.value : '';
                mainCategoryFilter = mainSelect ? mainSelect.value : '';
                subCategoryFilter = subSelect ? subSelect.value : '';
                renderComplaints();
            });
        }

        function renderComplaints() {
            let filteredComplaints = complaints.filter(c => {
                // Lọc theo từ khóa
                if (searchQuery) {
                    const matchId = ('#CP-' + c.id).toLowerCase().includes(searchQuery);
                    const matchTx = ('#TX-' + c.transactionId).toLowerCase().includes(searchQuery);
                    const matchEmail = c.customerEmail && c.customerEmail.toLowerCase().includes(searchQuery);
                    if (!matchId && !matchTx && !matchEmail) return false;
                }
                
                // Lọc theo trạng thái
                if (statusFilter) {
                    const st = c.status.toLowerCase();
                    if (statusFilter === 'open') {
                        if (st !== 'open' && st !== 'pending_review' && st !== 'pending') return false;
                    } else if (statusFilter === 'in_progress') {
                        if (st !== 'in_progress' && st !== 'inprogress') return false;
                    } else if (statusFilter === 'resolved') {
                        if (st !== 'resolved') return false;
                    } else if (statusFilter === 'closed') {
                        if (st !== 'closed') return false;
                    }
                }

                // Lọc theo danh mục
                if (mainCategoryFilter && c.mainCategoryId != mainCategoryFilter) return false;
                if (subCategoryFilter && c.categoryId != subCategoryFilter) return false;
                
                return true;
            });

            if (filteredComplaints.length === 0) {
                tbody.innerHTML = '<tr><td colspan="9" style="text-align:center; padding: 40px; color: var(--seller-muted);"><i class="fa fa-inbox" style="font-size: 24px; display: block; margin-bottom: 8px;"></i> Không tìm thấy khiếu nại nào phù hợp.</td></tr>';
                return;
            }

            tbody.innerHTML = filteredComplaints.map(c => {
                let badgeClass = 'open';
                if (c.status === 'Resolved' || c.status === 'Closed') badgeClass = 'resolved';
                else if (c.status === 'In_Progress' || c.status === 'In_progress' || c.status === 'IN_PROGRESS') badgeClass = 'pending';

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
                    <td><span class="badge ${badgeClass}">${translateStatus(c.status)}</span></td>
                    <td>${c.createdAt.replace('T', ' ').substring(0, 10)}</td>
                    <td class="text-right">
                        <div class="row-actions">
                            <a class="icon-button" href="/seller/complaints/detail?id=${c.id}" title="Xem chi tiết khiếu nại"><i class="fa fa-eye"></i></a>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    renderComplaints();
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

    try {
        const res = await sellerFetch(`/complaints/${complaintId}`);
        if (!res.ok) throw new Error('Không thể tải chi tiết khiếu nại.');
        const c = await res.json();

        const titleEl = document.getElementById('complaintTitle');
        if (titleEl) titleEl.textContent = `Chi tiết khiếu nại #CP-${c.id}`;
        
        const subtitleEl = document.getElementById('complaintSubtitle');
        if (subtitleEl) subtitleEl.textContent = `Mã giao dịch: #TX-${c.transactionId}`;
        
        let badgeClass = 'ds-badge-neutral';
        if (c.status === 'Resolved' || c.status === 'Closed') badgeClass = 'ds-badge-success';
        else if (c.status === 'In_Progress') badgeClass = 'ds-badge-warning';

        const badge = document.getElementById('complaintStatusBadge');
        if (badge) {
            badge.className = `ds-badge ${badgeClass}`;
            badge.textContent = translateStatus(c.status);
        }

        const dl = document.getElementById('complaint-details-dl');
        if (dl) {
            dl.innerHTML = `
                <dt>Khách hàng</dt>
                <dd>${c.customerName || 'N/A'} (${c.customerEmail || 'N/A'})</dd>
                <dt>Sản phẩm</dt>
                <dd>${c.productName || 'N/A'} (${c.variantName || 'N/A'})</dd>
                <dt>Đơn giá</dt>
                <dd>${formatVND(c.amountVnd)}</dd>
                <dt>Thời gian</dt>
                <dd>${new Date(c.createdAt).toLocaleString('vi-VN')}</dd>
            `;
        }

        const descEl = document.getElementById('c-description');
        if (descEl) descEl.textContent = c.description || '-';

        const solutionEl = document.getElementById('c-preferred-solution');
        if (solutionEl) {
            let solText = c.preferredSolution || 'Không có';
            if (solText === 'REFUND') solText = 'Yêu cầu hoàn tiền';
            else if (solText === 'REPLACEMENT') solText = 'Yêu cầu đổi sản phẩm khác';
            solutionEl.textContent = solText;
        }
        
        if (c.evidence) {
            const evSec = document.getElementById('c-evidence-section');
            if (evSec) {
                evSec.hidden = false;
                const evImg = document.getElementById('c-evidence-img');
                if (evImg) evImg.src = c.evidence;
            }
        }

        if (c.resolution) {
            const resCard = document.getElementById('staffResolutionCard');
            if (resCard) {
                resCard.hidden = false;
                const resEl = document.getElementById('c-resolution');
                if (resEl) resEl.textContent = c.resolution;
            }
        }

        const chatBtn = document.getElementById('chatDisputeBtn');
        if (chatBtn) {
            chatBtn.style.display = 'inline-flex';
            chatBtn.href = `/messages?sellerId=${c.customerId}`;
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

        const badgeClass = getBadgeClassForStatus(w.status);
        let extraStatusInfo = '';
        if (w.status === 'Failed' || w.status === 'Rejected') {
            extraStatusInfo = `
                <dt>Lý do từ chối</dt>
                <dd style="color:var(--seller-danger);">${w.rejectionReason || 'Không có lý do'}</dd>
            `;
        }
        if (w.reviewedAt) {
            extraStatusInfo += `
                <dt>Ngày xử lý</dt>
                <dd>${w.reviewedAt.replace('T', ' ').substring(0, 16)}</dd>
            `;
        }

        // Map data to DL info list
        const dl = card.querySelector('.seller-info-grid');
        if (dl) {
            dl.innerHTML = `
                <dt>Mã lệnh</dt>
                <dd>#WD-${w.id}</dd>
                <dt>Số tiền nhận</dt>
                <dd style="color:var(--seller-success); font-weight:600;">${formatVND(w.amountVnd)}</dd>
                <dt>Phí dịch vụ</dt>
                <dd style="color:var(--seller-danger);">${formatVND(w.feeVnd || 0)}</dd>
                <dt>Tổng trừ ví</dt>
                <dd style="font-weight:600;">${formatVND(w.amountVnd + (w.feeVnd || 0))}</dd>
                <dt>Trạng thái</dt>
                <dd><span class="badge ${badgeClass}">${translateStatus(w.status)}</span></dd>
                ${extraStatusInfo}
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
        const proofSection = document.querySelector('.proof-placeholder');
        if (proofSection) {
            if (w.proofFile) {
                // Determine URL for proofFile (it might already start with '/' e.g., '/uploads/...')
                let proofUrl = w.proofFile;
                if (!proofUrl.startsWith('http') && !proofUrl.startsWith('/')) {
                    proofUrl = '/uploads/' + proofUrl;
                }
                console.log("Seller proofUrl parsed:", proofUrl);
                
                let isImage = proofUrl.toLowerCase().endsWith('.jpg') || proofUrl.toLowerCase().endsWith('.jpeg') || proofUrl.toLowerCase().endsWith('.png');
                
                if (isImage) {
                    proofSection.innerHTML = `
                        <a href="${proofUrl}" target="_blank" style="display:block; text-align:center;">
                            <img src="${proofUrl}" alt="Biên lai rút tiền" style="max-width:100%; max-height:300px; border-radius:8px; border:1px solid var(--seller-border);"/>
                        </a>
                    `;
                } else {
                    proofSection.innerHTML = `
                        <div style="padding: 16px; text-align: center; background: #f8fafc; border:1px solid var(--seller-border); border-radius:8px;">
                            <a href="${proofUrl}" target="_blank" class="ds-btn" style="display: inline-block; padding: 8px 16px; background-color: #3b82f6; color: white; text-decoration: none; border-radius: 4px;">
                                <i class="fa fa-file"></i> Tải xuống hóa đơn/chứng từ
                            </a>
                        </div>
                    `;
                }
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

// ==============================================================================
// PREORDERS MANAGEMENT
// ==============================================================================
const preOrderDetailsById = new Map();
let currentPreOrders = [];

async function initPreOrders() {
    const tbody = document.querySelector('#preOrdersTable tbody');
    if (!tbody) return;

    try {
        const token = sessionStorage.getItem('accessToken');
        const res = await fetch('/api/v1/pre-orders/seller', {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!res.ok) throw new Error('Không thể tải danh sách đơn đặt trước.');
        const orders = await res.json();

        currentPreOrders = orders || [];
        preOrderDetailsById.clear();
        currentPreOrders.forEach(order => preOrderDetailsById.set(Number(order.id), order));

        applyPreOrderFilters();
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center" style="color: var(--seller-danger);"><i class="fa fa-exclamation-triangle"></i> ${err.message}</td></tr>`;
        showToast(err.message, 'error');
    }
}

function applyPreOrderFilters() {
    const tbody = document.querySelector('#preOrdersTable tbody');
    if (!tbody) return;

    try {
        const searchInput = document.getElementById('preorderSearchInput');
    const statusSelect = document.getElementById('preorderStatusFilter');
    const sortSelect = document.getElementById('preorderSortSelect');

    let searchQuery = searchInput ? searchInput.value.toLowerCase().trim() : '';
    let statusFilter = statusSelect ? statusSelect.value : '';
    let sortMode = sortSelect ? sortSelect.value : 'newest';

    if (currentPreOrders.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding: 40px; color: var(--seller-muted);"><i class="fa fa-inbox" style="font-size: 24px; display: block; margin-bottom: 8px;"></i> Không có đơn đặt trước nào.</td></tr>`;
        return;
    }

    let filteredOrders = currentPreOrders.filter(o => {
        if (searchQuery) {
            const matchId = ('#PO-' + o.id).toLowerCase().includes(searchQuery);
            const matchCustomer = o.customerEmail && o.customerEmail.toLowerCase().includes(searchQuery);
            const matchProduct = o.productName && o.productName.toLowerCase().includes(searchQuery);
            if (!matchId && !matchCustomer && !matchProduct) return false;
        }
        
        if (statusFilter) {
            const st = (o.status || '').toUpperCase();
            if (statusFilter === 'pending' && st !== 'PENDING' && o.status !== 'Chờ xử lý') return false;
            if (statusFilter === 'completed' && st !== 'COMPLETED' && o.status !== 'Hoàn tất') return false;
            if (statusFilter === 'cancelled' && st !== 'CANCELLED' && o.status !== 'Hủy đơn') return false;
        }
        
        return true;
    });

    if (sortMode === 'newest') {
        filteredOrders.sort((a, b) => b.id - a.id);
    } else if (sortMode === 'oldest') {
        filteredOrders.sort((a, b) => a.id - b.id);
    }

    if (filteredOrders.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding: 40px; color: var(--seller-muted);"><i class="fa fa-inbox" style="font-size: 24px; display: block; margin-bottom: 8px;"></i> Không tìm thấy đơn đặt trước phù hợp.</td></tr>`;
        return;
    }

    tbody.innerHTML = filteredOrders.map(o => {
            const st = (o.status || '').toUpperCase();
            const statusClass = (st === 'PENDING' || o.status === 'Chờ xử lý') ? 'pending' 
                              : (st === 'APPROVED' || st === 'ACCEPTED' || o.status === 'Đã duyệt') ? 'pending'
                              : (st === 'COMPLETED' || o.status === 'Hoàn tất') ? 'ok' 
                              : 'locked';

            return `
                <tr>
                    <td>#PO-${o.id}</td>
                    <td>${o.customerEmail}</td>
                    <td>${o.productName}</td>
                    <td class="text-center">${o.quantity}</td>
                    <td>${o.notes || ''}</td>
                    <td><span class="badge ${statusClass}">${translateStatus(o.status)}</span></td>
                    <td>${o.createdAt ? new Date(o.createdAt).toLocaleString('vi-VN') : ''}</td>
                    <td class="text-center">
                        ${(st === 'PENDING' || st === 'APPROVED' || o.status === 'Chờ xử lý' || o.status === 'Đã duyệt') ? `
                        <div style="display: flex; align-items: center; justify-content: center; width: 100%;">
                            <select class="ds-select" style="padding: 6px 12px; font-size: 13px; border-radius: 6px; border: 1px solid var(--seller-border); height: auto; width: 110px; font-weight: 500; background-color: #fff;" onchange="onPreOrderStatusSelect(${o.id}, this, '${o.status}')">
                                <option value="PENDING" ${(st === 'PENDING' || o.status === 'Chờ xử lý') ? 'selected' : ''}>Chờ xử lý</option>
                                <option value="APPROVED" ${(st === 'APPROVED' || o.status === 'Đã duyệt') ? 'selected' : ''}>Đã duyệt</option>
                                <option value="CANCELLED">Hủy đơn</option>
                            </select>
                        </div>
                        ` : `
                        <button class="ds-btn ds-btn-outline" style="padding: 4px 8px; font-size: 12px;" onclick="openPreOrderDetailModal(${o.id})">
                            <i class="fa fa-eye"></i> Chi tiết
                        </button>
                        `}
                    </td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        showToast(err.message, 'error');
    }
}

function setPreOrderDetailText(elementId, value, fallback = '-') {
    const element = document.getElementById(elementId);
    if (element) element.textContent = value || fallback;
}

function openPreOrderDetailModal(id) {
    const order = preOrderDetailsById.get(Number(id));
    const modal = document.getElementById('preOrderDetailModal');
    if (!order || !modal) {
        showToast('Không tìm thấy thông tin đơn đặt trước.', 'error');
        return;
    }

    const deliveryData = (order.deliveryData || '').trim();
    setPreOrderDetailText('preOrderDetailCode', `#PO-${order.id}`);
    setPreOrderDetailText('preOrderDetailStatus', translateStatus(order.status));
    setPreOrderDetailText('preOrderDetailCustomer', order.customerEmail);
    setPreOrderDetailText('preOrderDetailProduct', order.productName);
    setPreOrderDetailText('preOrderDetailQuantity', String(order.quantity || 0));
    setPreOrderDetailText('preOrderDetailNotes', order.notes, 'Không có ghi chú.');
    setPreOrderDetailText(
        'preOrderDetailDate',
        order.createdAt ? new Date(order.createdAt).toLocaleString('vi-VN') : ''
    );
    setPreOrderDetailText(
        'preOrderDetailDelivery',
        deliveryData,
        'Chưa có thông tin giao hàng.'
    );

    const copyButton = document.getElementById('copyPreOrderDetailButton');
    if (copyButton) copyButton.hidden = !deliveryData;
    modal.hidden = false;
    document.body.style.overflow = 'hidden';
    modal.querySelector('button[aria-label="Đóng cửa sổ chi tiết"]')?.focus();
}

function closePreOrderDetailModal() {
    const modal = document.getElementById('preOrderDetailModal');
    if (modal) modal.hidden = true;
    document.body.style.overflow = '';
}

function handlePreOrderDetailBackdrop(event) {
    if (event.target === event.currentTarget) closePreOrderDetailModal();
}

async function copyPreOrderDetailDelivery() {
    const deliveryData = document.getElementById('preOrderDetailDelivery')?.textContent?.trim();
    if (!deliveryData || deliveryData === 'Chưa có thông tin giao hàng.') return;

    try {
        await navigator.clipboard.writeText(deliveryData);
        showToast('Đã sao chép thông tin giao hàng.');
    } catch (error) {
        showToast('Không thể sao chép thông tin giao hàng.', 'error');
    }
}

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

async function updatePreOrderStatus(id, status, skipConfirm = false) {
    if (!skipConfirm) {
        const ok = await showCustomConfirm('Xác nhận thao tác', 'Bạn có chắc chắn muốn cập nhật trạng thái đơn này?');
        if (!ok) return;
    }
    
    try {
        const token = sessionStorage.getItem('accessToken');
        const res = await fetch(`/api/v1/pre-orders/seller/${id}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ status: status })
        });
        
        if (!res.ok) {
            const errData = await res.json().catch(() => ({}));
            throw new Error(errData.message || 'Cập nhật thất bại.');
        }
        
        showToast('Cập nhật trạng thái thành công!');
        initPreOrders();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function onPreOrderStatusSelect(id, selectElement, currentStatus) {
    const newStatus = selectElement.value;
    if (newStatus.toUpperCase() === currentStatus.toUpperCase()) return;

    let confirmTitle = "Xác nhận thao tác";
    let confirmMsg = "";
    if (newStatus === "APPROVED") {
        confirmTitle = "Phê duyệt đơn hàng";
        confirmMsg = `Xác nhận phê duyệt đơn đặt trước #PO-${id}?\nTrạng thái của đơn hàng phía người mua sẽ đổi sang "Đã duyệt - Chuẩn bị hàng".`;
    } else if (newStatus === "CANCELLED") {
        confirmTitle = "Cảnh báo hủy đơn";
        confirmMsg = `CẢNH BÁO: Xác nhận HỦY đơn đặt trước #PO-${id}?\nHệ thống sẽ tự động hoàn trả 100% tiền đặt trước về ví của khách hàng ngay lập tức! Thao tác này không thể hoàn tác.`;
    } else if (newStatus === "PENDING") {
        confirmTitle = "Khôi phục trạng thái";
        confirmMsg = `Xác nhận chuyển đơn đặt trước #PO-${id} trở lại trạng thái "Chờ xử lý"?`;
    }

    const ok = await showCustomConfirm(confirmTitle, confirmMsg);
    if (!ok) {
        selectElement.value = currentStatus.toUpperCase();
        return;
    }

    try {
        await updatePreOrderStatus(id, newStatus, true);
    } catch (err) {
        selectElement.value = currentStatus.toUpperCase();
    }
}

document.addEventListener('keydown', event => {
    if (event.key === 'Escape') {
        closePreOrderDetailModal();
        closeTxDetailModal();
    }
});

function showTxDetail(txId) {
    const t = (window.allTransactions || []).find(x => String(x.id) === String(txId));
    if (!t) return;

    document.getElementById('modalTxId').textContent = `#TX-${t.id}`;
    document.getElementById('modalTxDate').textContent = t.createdAt.replace('T', ' ').substring(0, 16);
    document.getElementById('modalTxProduct').textContent = t.productName || 'N/A';
    document.getElementById('modalTxVariant').textContent = t.variantName || 'N/A';
    const customerEl = document.getElementById('modalTxCustomer');
    if (t.customerEmail) {
        customerEl.innerHTML = `<a href="/messages?sellerId=${t.customerId}" style="text-decoration: underline; color: #2563eb; cursor: pointer;" title="Nhắn tin với khách hàng">${t.customerEmail}</a>`;
    } else {
        customerEl.textContent = 'N/A';
    }
    
    // Status badge
    let badgeClass = 'ds-badge-neutral';
    let statusText = t.status || 'N/A';
    if (t.status === 'Completed') {
        badgeClass = 'ds-badge-success';
        statusText = 'Hoàn tất';
    } else if (t.status === 'Held') {
        badgeClass = 'ds-badge-warning';
        statusText = 'Tạm giữ (Escrow)';
    } else if (t.status === 'Disputed') {
        badgeClass = 'ds-badge-danger';
        statusText = 'Tranh chấp (Khiếu nại)';
    } else if (t.status === 'Cancelled' || t.status === 'Refunded') {
        badgeClass = 'ds-badge-danger';
        statusText = 'Đã hủy/Hoàn tiền';
    }
    
    const statusEl = document.getElementById('modalTxStatus');
    statusEl.innerHTML = `<span class="ds-badge ${badgeClass}">${statusText}</span>`;
    
    document.getElementById('modalTxAmount').textContent = formatVND(t.amountVnd);
    document.getElementById('modalTxCommission').textContent = `-${formatVND(t.commissionVnd || 0)}`;
    document.getElementById('modalTxNet').textContent = formatVND(t.amountVnd - (t.commissionVnd || 0));
    
    const escrowEl = document.getElementById('modalTxEscrowRelease');
    if (t.escrowReleaseDate) {
        escrowEl.textContent = t.escrowReleaseDate.replace('T', ' ').substring(0, 16);
    } else {
        escrowEl.textContent = 'N/A';
    }

    const modal = document.getElementById('txDetailModal');
    if (modal) {
        modal.style.display = 'flex';
    }
}

function closeTxDetailModal() {
    const modal = document.getElementById('txDetailModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

window.initPreOrders = initPreOrders;
window.showCustomConfirm = showCustomConfirm;
window.onPreOrderStatusSelect = onPreOrderStatusSelect;
window.updatePreOrderStatus = updatePreOrderStatus;
window.openPreOrderDetailModal = openPreOrderDetailModal;
window.closePreOrderDetailModal = closePreOrderDetailModal;
window.handlePreOrderDetailBackdrop = handlePreOrderDetailBackdrop;
window.copyPreOrderDetailDelivery = copyPreOrderDetailDelivery;
window.showTxDetail = showTxDetail;
window.closeTxDetailModal = closeTxDetailModal;
