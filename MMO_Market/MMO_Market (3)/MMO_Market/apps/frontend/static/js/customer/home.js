// =======================================================
// LOGIC ADVANCED SEARCH BAR (CASCADING DROPDOWNS & AUTOCOMPLETE)
// =======================================================

// Dữ liệu danh mục động từ CSDL, fallback sang Mock Data nếu API lỗi
let SEARCH_DATA = {
    "all": [],
    "seller": [],
    "email": [
        "Gmail", "HotMail", "OutlookMail", "RuMail", "DomainMail",
        "YahooMail", "ProtonMail", "Loại Mail Khác"
    ],
    "account": [
        "Facebook", "Tài Khoản BM", "Tài Khoản Zalo", "Tài Khoản Twitter",
        "Tài Khoản Telegram", "Tài Khoản Instagram", "Tài Khoản Shopee",
        "Tài Khoản Discord", "Tài Khoản TikTok", "Key Diệt Virus",
        "Tài Khoản Capcut", "Key Window", "Tài Khoản Khác"
    ],
    "software": [
        "Phần Mềm Facebook", "Phần Mềm Google", "Phần Mềm Youtube",
        "Phần Mềm Tiền Ảo", "Phần Mềm PTC", "Phần Mềm Captcha",
        "Phần Mềm Offer", "Phần Mềm PTU", "Phần Mềm Khác"
    ],
    "engagement": [
        "Dịch vụ Facebook", "Dịch vụ Tiktok", "Dịch vụ Google",
        "Dịch vụ Telegram", "Dịch vụ Shopee", "Dịch vụ Discord",
        "Dịch vụ Twitter", "Dịch vụ Youtube", "Dịch vụ Zalo",
        "Dịch vụ Instagram", "Tương tác khác"
    ],
    "software_service": [
        "Tool MMO", "Tool Facebook", "Tool Google", "Tool Youtube",
        "Tool TikTok", "Tool Instagram", "Đồ họa - Design",
        "Video Editor", "Plugin & Extension", "Script & Bot", "Phần mềm khác"
    ],
    "blockchain": [
        "Tiền ảo - Crypto", "NFT", "Coinlist", "Airdrop",
        "Ví điện tử", "Tài khoản sàn", "Blockchain khác"
    ],
    "other": [],
    "other_service": []
};

// Hàm tải danh mục cha và con động từ database
async function loadDynamicCategories() {
    try {
        const res = await fetch('/api/search/categories?parentOnly=true');
        if (res.ok) {
            const categories = await res.json();
            if (categories && categories.length > 0) {
                const mainCatSelect = document.getElementById('main-category');
                mainCatSelect.innerHTML = `
                    <option value="" selected disabled hidden>Chọn danh mục chính...</option>
                    <option value="all">Tất cả</option>
                    <option value="seller">Tên người bán</option>
                `;
                
                const newSearchData = {
                    "all": [],
                    "seller": []
                };
                
                categories.forEach(cat => {
                    const opt = document.createElement('option');
                    opt.value = cat.id;
                    opt.innerText = cat.name;
                    mainCatSelect.appendChild(opt);
                    
                    const activeSubs = (cat.subCategories || [])
                        .filter(sub => !sub.isDelete && !sub.is_delete)
                        .map(sub => sub.name);
                        
                    newSearchData[cat.id] = activeSubs;
                });
                
                SEARCH_DATA = newSearchData;
            }
        }
    } catch (err) {
        console.error("Lỗi khi tải danh mục động:", err);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    loadDynamicCategories();
});

// 1. Cập nhật Dropdown 2 khi Dropdown 1 thay đổi
function updateSubCategories() {
    const mainCat = document.getElementById('main-category').value;
    const subCatSelect = document.getElementById('sub-category');

    // Lấy danh sách sub-category từ Object
    const subCategories = SEARCH_DATA[mainCat] || [];

    // Xóa dữ liệu cũ
    subCatSelect.innerHTML = '<option value="" selected disabled hidden>Lọc chi tiết...</option>';

    if (subCategories.length > 0) {
        subCatSelect.disabled = false;
        subCategories.forEach(sub => {
            const opt = document.createElement('option');
            opt.value = sub;
            opt.innerText = sub;
            subCatSelect.appendChild(opt);
        });
    } else {
        subCatSelect.disabled = true;
    }
}

// 2. Autocomplete (Debounce 300ms)
let typingTimer;
function handleAutocomplete(keyword) {
    const listEl = document.getElementById('autocomplete-list');
    clearTimeout(typingTimer);

    if (keyword.length < 2) {
        listEl.style.display = 'none';
        return;
    }

    typingTimer = setTimeout(() => {
        const normalizedKeyword = normalizeSearchText(keyword);
        const baseSuggestions = [
            'Netflix', 'ChatGPT Plus', 'Facebook', 'Youtube Premium', 'Gmail',
            'Tool Facebook', 'Tài khoản Facebook', 'Spotify Premium', 'Canva Pro'
        ];
        const matchedSuggestions = baseSuggestions.filter(item =>
            normalizeSearchText(item).includes(normalizedKeyword)
        );
        const mockSuggestions = matchedSuggestions.length > 0
            ? matchedSuggestions
            : [
                `${keyword} giá rẻ nhất`,
                `Tài khoản ${keyword} uy tín`,
                `Mua ${keyword} tự động`
            ];

        listEl.innerHTML = '';
        mockSuggestions.forEach(item => {
            const li = document.createElement('li');
            li.className = 'autocomplete-item';
            li.innerHTML = `<i class="fa fa-search autocomplete-icon"></i> ${item}`;
            li.onclick = () => {
                document.getElementById('main-search').value = item;
                listEl.style.display = 'none';
                window.location.href = `/search?q=${encodeURIComponent(item)}`;
            };
            listEl.appendChild(li);
        });

        listEl.style.display = 'block';
    }, 300);
}

// Đóng autocomplete khi click ra ngoài
document.addEventListener('click', (e) => {
    const wrapper = document.querySelector('.search-input-wrapper');
    const listEl = document.getElementById('autocomplete-list');
    if (wrapper && !wrapper.contains(e.target)) {
        if(listEl) listEl.style.display = 'none';
    }
});

function normalizeSearchText(value) {
    return (value || '')
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/\bfb\b/g, 'facebook')
        .trim();
}

// Ánh xạ danh mục chính → categoryId (API & bộ lọc trang kết quả)
const MAIN_TO_CATEGORY_ID = {
    email: '1',
    account: '2',
    software: '3',
    engagement: '4',
    software_service: '5',
    blockchain: '6',
    other: '66',
    other_service: '67'
};

// 3. Xử lý nút TÌM KIẾM (cho phép chỉ chọn danh mục con, không bắt buộc từ khóa)
function executeSearch(e) {
    e.preventDefault();
    const mainCat = document.getElementById('main-category').value;
    const rawSubCat = document.getElementById('sub-category').value;
    const subCat = normalizeSuggestionKeyword(rawSubCat);
    const keyword = document.getElementById('main-search').value.trim();

    let categoryId = MAIN_TO_CATEGORY_ID[mainCat];
    if (!categoryId && mainCat && !isNaN(mainCat)) {
        categoryId = mainCat; // If it's a dynamic parent category ID
    }
    const hasFilter = keyword || subCat || categoryId;

    if (!hasFilter) {
        showWarningToast('Vui lòng chọn danh mục hoặc nhập từ khóa tìm kiếm!');
        return;
    }

    const params = new URLSearchParams();
    if (keyword) params.append('q', keyword);
    if (subCat) params.append('subCategory', subCat);
    if (categoryId) params.append('categoryId', categoryId);

    window.location.href = '/search?' + params.toString();
}

function normalizeSuggestionKeyword(value) {
    const normalized = (value || '').trim();
    if (/^(clone\s*)?fb$/i.test(normalized) || /^t\u00e0i kho\u1ea3n fb$/i.test(normalized) || /^tai khoan fb$/i.test(normalized)) {
        return 'Facebook';
    }
    return normalized.replace(/\bFB\b/g, 'Facebook');
}

// =======================================================
// FEATURED PRODUCTS - Lấy từ API và render card có link chỉ đến trang chi tiết
// =======================================================


// Ánh xạ category → icon và màu sắc (đồng bộ với trang products)
function getProductIconInfo(productName, categoryId, categoryName) {
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
    }

    const name = (categoryName || '').toLowerCase();
    const id = categoryId || 0;

    if (id === 7 || name.includes('gmail'))      return { icon: 'fa-envelope',           color: '#EA4335' };
    if (id === 8 || name.includes('hotmail'))    return { icon: 'fa-envelope-o',         color: '#0078D4' };
    if (name.includes('netflix'))                return { icon: 'fa-play-circle',        color: '#E50914' };
    if (name.includes('spotify'))                return { icon: 'fa-music',              color: '#1DB954' };
    if (name.includes('youtube'))                return { icon: 'fa-youtube-play',       color: '#FF0000' };
    if (name.includes('chatgpt') || name.includes('openai') || name.includes('ai')) return { icon: 'fa-comments', color: '#10a37f' };
    if (name.includes('canva'))                  return { icon: 'fa-paint-brush',        color: '#00C4CC' };
    if (name.includes('facebook') || name.includes('fb') || id === 15 || id === 37 || id === 49) return { icon: 'fa-facebook-square', color: '#1877F2' };
    if (name.includes('tiktok') || id === 23 || id === 38 || id === 52) return { icon: 'fa-music',           color: '#010101' };
    if (name.includes('instagram') || id === 20 || id === 46) return { icon: 'fa-instagram',         color: '#E1306C' };
    if (name.includes('telegram') || id === 19 || id === 40) return { icon: 'fa-telegram',           color: '#2CA5E0' };
    if (name.includes('zalo') || id === 17 || id === 45)     return { icon: 'fa-comment',            color: '#0068FF' };
    if (name.includes('discord') || id === 22 || id === 42)  return { icon: 'fa-gamepad',            color: '#5865F2' };
    if (name.includes('shopee') || id === 21 || id === 41)   return { icon: 'fa-shopping-bag',       color: '#EE4D2D' };
    if (name.includes('twitter') || id === 18 || id === 43)  return { icon: 'fa-twitter',            color: '#1DA1F2' };
    if (name.includes('vpn') || name.includes('secure'))     return { icon: 'fa-shield',             color: '#6366f1' };
    if (name.includes('window') || name.includes('office') || id === 26) return { icon: 'fa-windows', color: '#0078D4' };
    if (name.includes('bitcoin') || name.includes('crypto') || name.includes('blockchain') || name.includes('wallet') || id === 59 || id === 63) return { icon: 'fa-bitcoin', color: '#F7931A' };
    if (name.includes('nft') || id === 60)      return { icon: 'fa-diamond',            color: '#8B5CF6' };
    if (name.includes('design') || name.includes('logo') || id === 54) return { icon: 'fa-paint-brush', color: '#EC4899' };
    if (name.includes('tool') || name.includes('phần mềm') || name.includes('software') || id === 48 || id === 49) return { icon: 'fa-cogs', color: '#64748b' };
    if (id >= 1 && id <= 6)                      return { icon: 'fa-envelope',           color: '#6366f1' };
    if (id >= 37 && id <= 47)                    return { icon: 'fa-line-chart',         color: '#0ea5e9' };
    return { icon: 'fa-tag', color: '#fd761a' };
}

async function loadFeaturedProducts() {
    const grid = document.getElementById('featured-product-grid');
    try {
        const res = await fetch('/api/search/products/featured?limit=8');
        if (!res.ok) throw new Error('API error');
        const products = await res.json();

        grid.innerHTML = '';

        if (!products || products.length === 0) {
            grid.innerHTML = '<p style="color:#94a3b8;text-align:center;padding:20px;">Chưa có sản phẩm nổi bật nào.</p>';
            return;
        }

        products.forEach(product => {
            const minP = product.minPrice ? product.minPrice.toLocaleString('vi-VN') + 'đ' : '--';
            const maxP = product.maxPrice && product.maxPrice !== product.minPrice
                ? ' - ' + product.maxPrice.toLocaleString('vi-VN') + 'đ'
                : '';
            const priceText = minP + maxP;
            const stock = product.totalStock != null ? product.totalStock : 0;
            const stockText = stock > 999 ? '999+' : stock;
            
            const sales = product.salesCount != null ? Number(product.salesCount) : 0;
            const salesText = sales > 0 ? sales.toLocaleString('vi-VN') + ' Bán' : 'Mới lên';

            // Lấy icon theo category (giống trang sản phẩm)
            const { icon, color } = getProductIconInfo(product.name, product.categoryId, product.categoryName);

            // Card là một thẻ <a> để click vào chỉ đến trang chi tiết sản phẩm
            const card = document.createElement('a');
            card.href = `/products/${product.id}`;
            card.className = 'product-card';
            let mediaHtml = `<i class="fa ${icon} product-card__img-icon" style="color:${color};"></i>`;
            if (product.imageUrl && !product.imageUrl.includes('placeholder.com')) {
                mediaHtml = `<img src="${product.imageUrl}" class="product-card__img-real" alt="${product.name}" style="width:100%; height:100%; object-fit:cover;">`;
            }
            
            card.innerHTML = `
                <div class="product-card__image-container">
                    ${mediaHtml}
                </div>
                <div class="product-card__body">
                    <div class="product-card__stock">Tồn: ${stockText}</div>
                    <div class="product-card__price">${priceText}</div>
                    <h4 class="product-card__title">${product.name}</h4>
                    <div class="product-card__footer">
                        <span>${salesText}</span>
                        <button class="product-card__cart-btn" type="button" title="Thêm vào giỏ hàng"
                            onclick="event.preventDefault(); event.stopPropagation(); addFeaturedProductToCart(${product.id}, '${(product.name||'').replace(/'/g, "\\'")}', '${(product.sellerName||'').replace(/'/g, "\\'")}', ${product.minPrice||0});">
                            <i class="fa fa-shopping-cart"></i>
                        </button>
                    </div>
                </div>
            `;
            grid.appendChild(card);
        });

    } catch (e) {
        console.error('Không tải được sản phẩm nổi bật:', e);
        grid.innerHTML = '<p style="color:#ef4444;text-align:center;padding:20px;">Không thể tải sản phẩm. Vui lòng thử lại sau.</p>';
    }
}

function addFeaturedProductToCart(productId, productName, sellerName, price) {
    const cartItem = { productId, duration: 1, productName, sellerName, price: price || 0, quantity: 1 };
    const cartKey = typeof window.getCartStorageKey === 'function' ? window.getCartStorageKey() : 'mmoCart';
    const cart = JSON.parse(localStorage.getItem(cartKey) || '[]');
    const existing = cart.find(i => i.productId === productId && i.duration === 1);
    if (existing) { existing.quantity += 1; } else { cart.push(cartItem); }
    localStorage.setItem(cartKey, JSON.stringify(cart));
    showSuccessToast('Đã thêm sản phẩm vào giỏ hàng!');
    if (typeof window.refreshHeaderCartBadge === 'function') window.refreshHeaderCartBadge();
}

// Gọi API khi DOM sẵn sàng
document.addEventListener('DOMContentLoaded', loadFeaturedProducts);
