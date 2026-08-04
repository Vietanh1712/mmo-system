let cart = [];

let allCatalogProducts = [];
let renderedRecommendations = [];

document.addEventListener('DOMContentLoaded', () => {
    const token = sessionStorage.getItem('accessToken');
    if (!token || token === 'null' || token === 'undefined') {
        window.location.href = '/login';
        return;
    }

    const cartKey = typeof window.getCartStorageKey === 'function' ? window.getCartStorageKey() : 'mmoCart';
    cart = normalizeCart(JSON.parse(localStorage.getItem(cartKey) || '[]'));
    saveCart();
    renderCart();
    loadRecommendations();
});

async function loadRecommendations() {
    try {
        const res = await fetch('/api/search/products?size=100');
        if (res.ok) {
            const data = await res.json();
            allCatalogProducts = data.content || [];
        }
    } catch (err) {
        console.warn("Failed to fetch catalog for recommendations", err);
    }
    renderRecommendations();
}

function getProductIconAndColor(productName) {
    const nameLower = (productName || '').toLowerCase();
    if (nameLower.includes('netflix')) {
        return { icon: 'fa-play-circle', color: '#E50914' };
    } else if (nameLower.includes('chatgpt') || nameLower.includes('gpt')) {
        return { icon: 'fa-comments', color: '#10a37f' };
    } else if (nameLower.includes('spotify')) {
        return { icon: 'fa-music', color: '#1DB954' };
    } else if (nameLower.includes('windows') || nameLower.includes('win')) {
        return { icon: 'fa-windows', color: '#0078d4' };
    } else if (nameLower.includes('youtube')) {
        return { icon: 'fa-youtube-play', color: '#FF0000' };
    } else if (nameLower.includes('canva')) {
        return { icon: 'fa-paint-brush', color: '#00c4cc' };
    } else if (nameLower.includes('gmail')) {
        return { icon: 'fa-envelope', color: '#ea4335' };
    } else if (nameLower.includes('facebook') || nameLower.includes('fb')) {
        return { icon: 'fa-facebook-square', color: '#1877F2' };
    } else if (nameLower.includes('vpn') || nameLower.includes('nordvpn')) {
        return { icon: 'fa-shield', color: '#4682b4' };
    } else if (nameLower.includes('tiktok')) {
        return { icon: 'fa-video-camera', color: '#010101' };
    } else if (nameLower.includes('wallet') || nameLower.includes('crypto') || nameLower.includes('trust')) {
        return { icon: 'fa-btc', color: '#f7931a' };
    } else if (nameLower.includes('logo') || nameLower.includes('thiết kế') || nameLower.includes('design') || nameLower.includes('banner')) {
        return { icon: 'fa-paint-brush', color: '#7c3aed' };
    } else {
        return { icon: 'fa-cube', color: '#94a3b8' };
    }
}

function normalizeCart(items) {
    return items.map(item => ({
        productId: Number(item.productId),
        duration: Number(item.duration || 1),
        productName: item.productName,
        sellerName: item.sellerName || 'VipStore',
        price: Number(item.price || 0),
        icon: item.icon || 'fa-cube',
        iconColor: item.iconColor || '#ea580c',
        imageUrl: item.imageUrl || '',
        quantity: Math.max(1, Number(item.quantity || 1)),
        selected: item.selected !== false,
        categoryName: item.categoryName || '',
        variantLabel: item.variantLabel || ''
    }));
}

function saveCart() {
    const cartKey = typeof window.getCartStorageKey === 'function' ? window.getCartStorageKey() : 'mmoCart';
    localStorage.setItem(cartKey, JSON.stringify(cart));
    if (typeof window.refreshHeaderCartBadge === 'function') window.refreshHeaderCartBadge();
}

function formatVND(value) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })
        .format(Math.max(0, value))
        .replace('₫', 'VNĐ');
}

function getProductTag(item) {
    const name = (item.productName || '').toLowerCase();
    if (name.includes('premium') || name.includes('netflix') || name.includes('youtube') || name.includes('spotify')) {
        return 'Premium Subscription';
    }
    if (name.includes('facebook') || name.includes('gmail')) {
        return 'Digital Account';
    }
    if (name.includes('tool') || name.includes('canva')) {
        return 'Software Tool';
    }
    return 'Digital Product';
}

function renderCart() {
    const list = document.getElementById('cartList');
    const totalCount = cart.reduce((sum, item) => sum + item.quantity, 0);
    const selectedCount = getSelectedItems().reduce((sum, item) => sum + item.quantity, 0);
    document.getElementById('cartCountLabel').innerText = `${selectedCount}/${totalCount} sản phẩm được chọn`;

    if (cart.length === 0) {
        list.innerHTML = `
            <div class="cart-empty">
                <i class="fa fa-shopping-cart"></i>
                <h2>Giỏ hàng đang trống</h2>
                <p>Hãy chọn sản phẩm bạn muốn mua rồi thêm vào giỏ hàng.</p>
            </div>
        `;
        updateSummary();
        return;
    }

    list.innerHTML = cart.map((item, index) => `
        <article class="cart-item">
            <div class="cart-item__check">
                <input class="cart-item__checkbox" type="checkbox" ${item.selected !== false ? 'checked' : ''} onchange="toggleSelected(${index}, this.checked)">
            </div>
            <div class="cart-item__thumb" onclick="window.location.href='/products/${item.productId}'" style="cursor:pointer;">
                ${item.imageUrl && !item.imageUrl.includes('placeholder.com')
                    ? `<img src="${item.imageUrl}" alt="${item.productName}" style="width: 100%; height: 100%; object-fit: cover; border-radius: 6px;">`
                    : `<i class="fa ${item.icon}" style="color: ${item.iconColor}"></i>`}
            </div>
            <div class="cart-item__info">
                <h2 class="cart-item__name" onclick="window.location.href='/products/${item.productId}'" style="cursor:pointer;">${item.productName}</h2>
                <div class="cart-item__seller">Bán bởi: <strong>${item.sellerName}</strong></div>
                ${(item.categoryName) ? `<div class="cart-item__variant" style="font-size: 12px; margin-top: 4px; color: var(--text-muted);">Danh mục: <strong style="color: var(--brand-accent);">${item.categoryName}</strong></div>` : ''}
            </div>
            <div class="cart-item__side">
                <div class="qty-control" aria-label="Điều chỉnh số lượng">
                    <button type="button" onclick="changeQuantity(${index}, -1)">-</button>
                    <input type="number" min="1" value="${item.quantity}" style="width: 42px; height: 28px; text-align: center; border: 1px solid var(--border-color); border-radius: 4px; font-weight: 600; color: var(--text-main); background: transparent; outline: none; margin: 0 4px;" onchange="setQuantity(${index}, this.value)">
                    <button type="button" onclick="changeQuantity(${index}, 1)">+</button>
                </div>
                <div class="cart-item__price">${formatVND(item.price * item.quantity)}</div>
                <button class="cart-item__remove" type="button" onclick="removeCartItem(${index})">Xóa khỏi giỏ</button>
            </div>
        </article>
    `).join('');

    updateSummary();
    enrichCartWithImages();
}

// Bổ sung hình ảnh sản phẩm thực tế từ API nếu trong giỏ hàng chưa lưu trữ link ảnh
function enrichCartWithImages() {
    const itemsWithoutImage = cart.filter(item => !item.imageUrl);
    if (itemsWithoutImage.length === 0) return;

    const uniqueIds = [...new Set(itemsWithoutImage.map(item => item.productId))];
    uniqueIds.forEach(async (productId) => {
        try {
            // Gọi API lấy thông tin chi tiết của sản phẩm để lấy ảnh thực tế
            const res = await fetch(`/api/search/products/${productId}`);
            if (!res.ok) return;
            const data = await res.json();
            const imageUrl = data.imageUrl || '';
            if (!imageUrl) return;

            // Cập nhật đường dẫn ảnh và tên danh mục vào biến cart đang lưu trong bộ nhớ
            cart.forEach(item => {
                if (item.productId === productId) {
                    item.imageUrl = imageUrl;
                    item.categoryName = item.categoryName || data.categoryName || '';
                }
            });

            // Lưu lại giỏ hàng mới cập nhật vào LocalStorage
            saveCart();

            // Cập nhật lại giao diện hiển thị ảnh (DOM) mà không cần render lại toàn bộ giỏ hàng
            cart.forEach((item, idx) => {
                if (item.productId === productId) {
                    const thumbs = document.querySelectorAll('.cart-item__thumb');
                    if (thumbs[idx]) {
                        thumbs[idx].innerHTML = `<img src="${imageUrl}" alt="${item.productName}" style="width: 100%; height: 100%; object-fit: cover; border-radius: 6px;">`;
                    }
                }
            });
        } catch (e) {
            // Thất bại trong âm thầm - giữ nguyên biểu tượng icon mặc định
        }
    });
}

// Cập nhật tổng số tiền và số lượng sản phẩm được chọn thanh toán trên màn hình
function updateSummary() {
    const selectedItems = getSelectedItems();
    const subtotal = selectedItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
    const serviceFee = 0;
    const total = subtotal;

    document.getElementById('subtotalValue').innerText = formatVND(subtotal);
    document.getElementById('serviceFeeValue').innerText = 'Miễn phí';
    document.getElementById('totalValue').innerText = formatVND(total);
}

// Lấy ra danh sách các sản phẩm đang được tích chọn (checkbox = true)
function getSelectedItems() {
    return cart.filter(item => item.selected !== false);
}

// Bật/tắt trạng thái tích chọn của một sản phẩm trong giỏ hàng
function toggleSelected(index, checked) {
    cart[index].selected = checked;
    saveCart();
    renderCart();
}

// Thay đổi số lượng sản phẩm bằng nút cộng/trừ (+/-)
function changeQuantity(index, delta) {
    cart[index].quantity = Math.max(1, cart[index].quantity + delta);
    saveCart();
    renderCart();
}

// Đặt số lượng sản phẩm cụ thể khi người dùng nhập số trực tiếp vào ô input
function setQuantity(index, value) {
    let val = parseInt(value, 10);
    if (isNaN(val) || val < 1) {
        val = 1;
    }
    cart[index].quantity = val;
    saveCart();
    renderCart();
}

// Xóa sản phẩm ra khỏi giỏ hàng
function removeCartItem(index) {
    cart.splice(index, 1);
    saveCart();
    renderCart();
    renderRecommendations();
}

// Xử lý khi nhấn nút Thanh toán giỏ hàng
function checkoutCart() {
    const selectedItems = getSelectedItems();
    if (selectedItems.length === 0) {
        showWarningToast('Vui lòng chọn ít nhất một sản phẩm để thanh toán.');
        return;
    }

    // Chuyển hướng sang trang checkout với chế độ thanh toán giỏ hàng (mode=cart)
    window.location.href = `/checkout?mode=cart`;
}

// Hàm trích xuất các từ khóa từ tên sản phẩm để phân tích độ liên quan
function getKeywordsFromProductName(name) {
    if (!name) return [];
    const nameLower = name.toLowerCase();
    // Thay thế các dấu câu bằng khoảng trắng
    const cleanName = nameLower.replace(/[.,()\-+/\[\]:]/g, ' ');
    const words = cleanName.split(/\s+/);
    // Danh sách các từ dừng (stopwords) cần loại bỏ để lọc ra từ khóa chính xác
    const stopWords = new Set([
        'tài', 'khoản', 'của', 'gói', 'cho', 'giá', 'siêu', 'rẻ', 'bán', 'chính', 'chủ', 
        'bản', 'quyền', 'dịch', 'vụ', 'mua', 'tháng', 'năm', 'ngày', 'thiết', 'bị', 
        'xem', 'riêng', 'suốt', 'thời', 'gian', 'sử', 'dụng', 'bảo', 'hành', 'đổi', 
        'các', 'những', 'để', 'và', 'kèm', 'trực', 'tiếp', 'hỗ', 'trợ', 'qua', 'link', 
        'family', 'nhanh', 'chóng', 'độc', 'quyền', 'vĩnh', 'viễn', 'combo'
    ]);
    return words.filter(w => w.length >= 3 && !stopWords.has(w));
}

// Hiển thị phần "Sản phẩm có thể bạn quan tâm" dựa trên độ liên quan
function renderRecommendations() {
    const grid = document.getElementById('recommendationGrid');
    if (!grid) return;

    // 1. Lấy danh sách ID các sản phẩm hiện có trong giỏ hàng
    const cartProductIds = cart.map(item => Number(item.productId));

    // 2. Tìm kiếm từ khóa và danh mục của các sản phẩm đang có trong giỏ hàng
    const cartKeywords = [];
    const cartCategories = [];
    const cartNames = [];

    cart.forEach(item => {
        cartNames.push(item.productName.toLowerCase());
        
        // Tìm sản phẩm tương ứng trong danh mục để lấy thông tin Category
        const catalogProd = allCatalogProducts.find(p => Number(p.productId) === Number(item.productId));
        if (catalogProd && catalogProd.categoryName) {
            cartCategories.push(catalogProd.categoryName.toLowerCase());
        }

        // Trích xuất động các từ khóa dựa trên tên sản phẩm
        const kws = getKeywordsFromProductName(item.productName);
        kws.forEach(kw => {
            if (!cartKeywords.includes(kw)) {
                cartKeywords.push(kw);
            }
        });
    });

    // 3. Lọc và tính điểm gợi ý cho các sản phẩm chưa có trong giỏ hàng
    let candidates = allCatalogProducts.filter(p => !cartProductIds.includes(Number(p.productId)));

    // Ánh xạ từng ứng viên để tính điểm số liên quan
    candidates = candidates.map(p => {
        let score = 0;
        const candidateNameLower = (p.productName || '').toLowerCase();
        
        // Cộng 15 điểm nếu cùng Category (ví dụ: cùng Premium, Email,...)
        if (p.categoryName && cartCategories.includes(p.categoryName.toLowerCase())) {
            score += 15;
        }

        // Cộng 20 điểm nếu tên sản phẩm chứa cụm từ tương đồng
        if (cartNames.some(n => candidateNameLower.includes(n) || n.includes(candidateNameLower))) {
            score += 20;
        }

        // Cộng 5 điểm cho mỗi từ khóa trùng khớp
        const candKws = getKeywordsFromProductName(p.productName);
        candKws.forEach(kw => {
            if (cartKeywords.includes(kw)) {
                score += 5;
            }
        });

        // Cộng thêm 3 điểm ưu tiên nếu sản phẩm còn hàng trong kho
        if (Number(p.stock || 0) > 0) {
            score += 3;
        }

        return { product: p, score };
    });

    // Sắp xếp danh sách ứng viên theo điểm số giảm dần, sau đó theo số lượng bán được giảm dần
    candidates.sort((a, b) => {
        if (b.score !== a.score) return b.score - a.score;
        return (b.product.salesCount || 0) - (a.product.salesCount || 0);
    });

    // Lấy ra 4 sản phẩm đứng đầu
    let selectedCandidates = candidates.slice(0, 4).map(c => c.product);

    // Nếu không đủ 4 ứng viên, lấy thêm các sản phẩm bán chạy khác để lấp đầy
    if (selectedCandidates.length < 4) {
        const remaining = allCatalogProducts.filter(p => 
            !cartProductIds.includes(Number(p.productId)) && 
            !selectedCandidates.some(s => Number(s.productId) === Number(p.productId))
        );
        remaining.sort((a, b) => (b.salesCount || 0) - (a.salesCount || 0));
        selectedCandidates = selectedCandidates.concat(remaining.slice(0, 4 - selectedCandidates.length));
    }

    window.renderedRecommendations = selectedCandidates;

    if (selectedCandidates.length === 0) {
        grid.innerHTML = '<p style="color: var(--text-muted); text-align: center; grid-column: 1/-1;">Chưa có gợi ý phù hợp.</p>';
        return;
    }

    grid.innerHTML = selectedCandidates.map(product => {
        const starsCount = Math.floor(product.averageRating || 0);
        let starHtml = '';
        for (let i = 0; i < 5; i++) {
            if (i < starsCount) {
                starHtml += '<i class="fa fa-star"></i>';
            } else {
                starHtml += '<i class="fa fa-star-o"></i>';
            }
        }

        const verifiedBadge = product.sellerIsVerified ? '<i class="fa fa-check-circle tick-xanh" title="Đã xác minh"></i>' : '';
        const iconInfo = getProductIconAndColor(product.productName);
        const stock = Number(product.stock || 0);
        const isOutOfStock = stock <= 0;
        const stockClass = isOutOfStock ? 'stock stock--out' : 'stock';
        
        // "CÓ SẴN" badge if in stock
        const badgeHtml = !isOutOfStock ? '<span class="tag-badge tag-instant">CÓ SẴN</span>' : '';

        const buyButtonHtml = isOutOfStock
            ? `<button class="btn-buy btn-buy--preorder" type="button" onclick="event.stopPropagation(); preOrderRecommendation(${product.productId})"><i class="fa fa-clock-o"></i> Đặt trước</button>`
            : `<button class="btn-buy" type="button" onclick="event.stopPropagation(); buyRecommendation(${product.productId})">Mua ngay</button>`;
        const cartButtonHtml = isOutOfStock
            ? `<button class="btn-cart btn-cart--disabled" type="button" title="Sản phẩm hết hàng, vui lòng đặt trước" onclick="event.stopPropagation(); preOrderRecommendation(${product.productId})"><i class="fa fa-shopping-cart"></i></button>`
            : `<button class="btn-cart" type="button" title="Thêm vào giỏ hàng" onclick="event.stopPropagation(); addRecommendationToCart(${product.productId})"><i class="fa fa-shopping-cart"></i></button>`;

        return `
            <div class="product-card" onclick="window.location.href='/products/${product.productId}'">
                <div class="product-card__image-container">
                    <i class="fa ${iconInfo.icon} product-card__img-icon" style="color: ${iconInfo.color};"></i>
                    ${badgeHtml}
                </div>
                <div class="product-info">
                    <div class="category-rating">
                        <span class="category-badge">${product.categoryName || 'Sản phẩm số'}</span>
                        <span class="rating-stars">${starHtml}</span>
                    </div>
                    <h4 class="product-title">${product.productName}</h4>
                    <div class="seller-badge">
                        <i class="fa fa-university"></i>
                        <span>Cửa hàng: <strong>${product.sellerName || 'VipStore'}</strong></span>
                        ${verifiedBadge}
                    </div>
                    <div class="price-stock">
                        <div class="price">${formatVND(product.price)}</div>
                        <div class="${stockClass}">Kho: <strong>${stock}</strong></div>
                    </div>
                    <div class="product-actions">
                        ${buyButtonHtml}
                        ${cartButtonHtml}
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function addRecommendationToCart(productId) {
    const product = window.renderedRecommendations.find(item => item.productId === productId);
    if (!product) return;

    const iconInfo = getProductIconAndColor(product.productName);

    const existing = cart.find(item => item.productId === product.productId && item.duration === 1);
    if (existing) {
        existing.quantity += 1;
    } else {
        cart.push({
            productId: product.productId,
            duration: 1,
            productName: product.productName,
            sellerName: product.sellerName || 'VipStore',
            price: product.price,
            icon: iconInfo.icon,
            iconColor: iconInfo.color,
            quantity: 1,
            selected: true
        });
    }

    saveCart();
    renderCart();
    renderRecommendations();
    showSuccessToast('Đã thêm sản phẩm vào giỏ hàng thành công!');
}

function buyRecommendation(productId) {
    addRecommendationToCart(productId);
    window.location.href = `/checkout?mode=cart`;
}

function preOrderRecommendation(productId) {
    window.location.href = `/pre-order-request?productId=${productId}`;
}

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
        <div style="display: flex; align-items: center; gap: 8px;">
            <i class="fa fa-check-circle" style="font-size: 18px; color: #22c55e;"></i>
            <div>
                <h4 class="ds-toast-title">Thành công</h4>
                <p class="ds-toast-message">${message}</p>
            </div>
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
    }, 3000);
}
