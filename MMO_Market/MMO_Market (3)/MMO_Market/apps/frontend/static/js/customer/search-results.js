// =======================================================
// LOGIC TRANG KẾT QUẢ TÌM KIẾM SẢN PHẨM
// =======================================================

// Dữ liệu Mock: Danh sách sản phẩm mẫu dùng làm fallback khi API không khả dụng
const MOCK_PRODUCTS = [
    {
        productId: 1,
        productName: "Tài khoản Netflix Premium 4K UHD 1 Tháng (Xem riêng 1 thiết bị, bảo hành 1 đổi 1)",
        imageUrl: "https://images.unsplash.com/photo-1574375927938-d5a98e8edd85?w=500&auto=format&fit=crop&q=60",
        categoryName: "Tài khoản",
        categoryId: 2,
        parentCategoryId: 2,
        sellerName: "StoreMaster",
        sellerIsVerified: true,
        price: 65000,
        stock: 156,
        averageRating: 5.0,
        isInstant: true,
        isBestseller: false
    },
    {
        productId: 2,
        productName: "Tài khoản Netflix Premium 4K UHD Gói 1 Năm (Chính chủ gia hạn ổn định)",
        imageUrl: "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?w=500&auto=format&fit=crop&q=60",
        categoryName: "Tài khoản",
        categoryId: 2,
        parentCategoryId: 2,
        sellerName: "Netflix_Vip_Store",
        sellerIsVerified: true,
        price: 650000,
        stock: 42,
        averageRating: 4.9,
        isInstant: false,
        isBestseller: true
    },
    {
        productId: 3,
        productName: "Tài khoản ChatGPT Plus (OpenAI GPT-4o) Chính Chủ Sẵn 20$ Hạn 1 Tháng",
        imageUrl: "https://images.unsplash.com/photo-1677442136019-21780efad99a?w=500&auto=format&fit=crop&q=60",
        categoryName: "Tài khoản",
        categoryId: 2,
        parentCategoryId: 2,
        sellerName: "AI_Helper",
        sellerIsVerified: true,
        price: 150000,
        stock: 89,
        averageRating: 4.8,
        isInstant: true,
        isBestseller: false
    },
    {
        productId: 4,
        productName: "Spotify Premium 1 Năm Giá Siêu Rẻ (Nâng cấp Family email của bạn)",
        imageUrl: "https://images.unsplash.com/photo-1614680376593-902f74fa0d41?w=500&auto=format&fit=crop&q=60",
        categoryName: "Tài khoản",
        categoryId: 2,
        parentCategoryId: 2,
        sellerName: "MusicLovers",
        sellerIsVerified: false,
        price: 250000,
        stock: 45,
        averageRating: 4.7,
        isInstant: false,
        isBestseller: true
    },
    {
        productId: 5,
        productName: "Key Windows 11 Pro Bản Quyền Vĩnh Viễn (Kèm hướng dẫn active chi tiết)",
        imageUrl: "https://images.unsplash.com/photo-1624561172888-ac93c696e10c?w=500&auto=format&fit=crop&q=60",
        categoryName: "Phần mềm",
        categoryId: 3,
        parentCategoryId: 3,
        sellerName: "Microsoft_Reseller",
        sellerIsVerified: true,
        price: 99000,
        stock: 999,
        averageRating: 5.0,
        isInstant: true,
        isBestseller: false
    },
    {
        productId: 6,
        productName: "Youtube Premium Không Quảng Cáo 6 Tháng (Add Family bao chạy mượt)",
        imageUrl: "https://images.unsplash.com/photo-1611162617213-7d7a39e9b1d7?w=500&auto=format&fit=crop&q=60",
        categoryName: "Tài khoản",
        categoryId: 2,
        parentCategoryId: 2,
        sellerName: "RedPremium",
        sellerIsVerified: true,
        price: 120000,
        stock: 230,
        averageRating: 4.9,
        isInstant: false,
        isBestseller: true
    },
    {
        productId: 7,
        productName: "Gói Tài Khoản Canva Pro Thiết Kế 1 Năm Trọn Gói",
        imageUrl: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=60",
        categoryName: "Dịch vụ phần mềm",
        categoryId: 5,
        parentCategoryId: 5,
        sellerName: "DesignHub",
        sellerIsVerified: true,
        price: 180000,
        stock: 120,
        averageRating: 4.8,
        isInstant: true,
        isBestseller: false
    },
    {
        productId: 8,
        productName: "Combo 10 Gmail Việt Cổ 2018 - 2020 Cực Sạch Có Sẵn Kênh Youtube",
        imageUrl: "https://images.unsplash.com/photo-1596526139099-b3de59b72093?w=500&auto=format&fit=crop&q=60",
        categoryName: "Email",
        categoryId: 1,
        parentCategoryId: 1,
        sellerName: "MailMaster",
        sellerIsVerified: false,
        price: 35000,
        stock: 500,
        averageRating: 4.6,
        isInstant: true,
        isBestseller: false
    },
    {
        productId: 9,
        productName: "Tool Nuôi Nick Facebook Auto Like Post Share Độc Quyền",
        imageUrl: "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=500&auto=format&fit=crop&q=60",
        categoryName: "Dịch vụ phần mềm",
        categoryId: 5,
        parentCategoryId: 5,
        sellerName: "MMO_Coder",
        sellerIsVerified: true,
        price: 850000,
        stock: 75,
        averageRating: 4.7,
        isInstant: true,
        isBestseller: false
    },
    {
        productId: 10,
        productName: "Tài khoản NordVPN Premium 1 Năm Bảo Mật Mã Hóa Cao",
        imageUrl: "https://images.unsplash.com/photo-1563986768494-0de2c4917a22?w=500&auto=format&fit=crop&q=60",
        categoryName: "Tài khoản",
        categoryId: 2,
        parentCategoryId: 2,
        sellerName: "SecureNet",
        sellerIsVerified: true,
        price: 350000,
        stock: 15,
        averageRating: 4.8,
        isInstant: true,
        isBestseller: false
    },
    {
        productId: 11,
        productName: "Tăng 1000 Follower Thật TikTok Việt Tốc Độ Nhanh Tự Nhiên",
        imageUrl: "https://images.unsplash.com/photo-1598257006458-087169a1f08d?w=500&auto=format&fit=crop&q=60",
        categoryName: "Tăng tương tác",
        categoryId: 4,
        parentCategoryId: 4,
        sellerName: "SocialMediaUp",
        sellerIsVerified: true,
        price: 95000,
        stock: 9999,
        averageRating: 4.9,
        isInstant: false,
        isBestseller: true
    },
    {
        productId: 12,
        productName: "Ví Điện Tử Trust Wallet Hạn Cổ Có Sẵn Cụm Từ Bảo Mật",
        imageUrl: "https://images.unsplash.com/photo-1621761191319-c6fb62004040?w=500&auto=format&fit=crop&q=60",
        categoryName: "Blockchain",
        categoryId: 6,
        parentCategoryId: 6,
        sellerName: "CryptoSafe",
        sellerIsVerified: false,
        price: 150000,
        stock: 0,
        averageRating: 4.5,
        isInstant: true,
        isBestseller: false
    },
    {
        productId: 13,
        productName: "Gói Dịch Vụ Thiết Kế Logo & Banner Chuyên Nghiệp (Không thuộc danh mục chính)",
        imageUrl: "https://images.unsplash.com/photo-1626785774573-4b799315345d?w=500&auto=format&fit=crop&q=60",
        categoryName: "Dịch vụ phần mềm",
        categoryId: 31,
        parentCategoryId: 7,
        sellerName: "CreativeHub",
        sellerIsVerified: true,
        price: 299000,
        stock: 50,
        averageRating: 4.6,
        isInstant: false,
        isBestseller: false
    },
    {
        productId: 14,
        productName: "Gmail Việt Cổ 2019 Siêu Sạch - Bảo hành đổi 1-1 trong 7 ngày",
        imageUrl: "https://images.unsplash.com/photo-1596526139099-b3de59b72093?w=500&auto=format&fit=crop&q=60",
        categoryName: "Gmail",
        categoryId: 9,
        parentCategoryId: 1,
        sellerName: "GmailPro_VN",
        sellerIsVerified: true,
        price: 28000,
        stock: 320,
        averageRating: 4.8,
        isInstant: true,
        isBestseller: false
    }
];

// Biến trạng thái toàn cục của trang tìm kiếm
let currentKeyword = '';
let currentSubCategory = '';
let currentCategory = '';
let currentMaxPrice = 5000000;
let currentStockStatus = '';
let currentSort = 'createdAt,desc';
let currentPage = 0;
let isUsingMock = false;

// Đồng bộ bộ lọc giao diện theo các tham số URL hiện tại
function syncUIFromURL() {
    const urlParams = new URLSearchParams(window.location.search);
    currentKeyword = urlParams.get('q') || urlParams.get('keyword') || '';
    currentSubCategory = urlParams.get('subCategory') || '';
    currentCategory = urlParams.get('categoryId') || '';
    currentMaxPrice = parseInt(urlParams.get('maxPrice')) || 5000000;
    currentStockStatus = urlParams.get('stockStatus') || '';
    currentSort = urlParams.get('sort') || 'createdAt,desc';
    currentPage = parseInt(urlParams.get('page')) || 0;

    // Hiển thị từ khóa hiện tại trong ô tìm kiếm của header
    const headerSearchInput = document.getElementById('header-search-input');
    if (headerSearchInput) {
        headerSearchInput.value = currentKeyword || currentSubCategory;
    }

    const titleSpan = document.querySelector('.title-section h1 span');
    const displayLabel = currentKeyword || currentSubCategory || 'tất cả sản phẩm';
    if (titleSpan) titleSpan.innerText = displayLabel;

    document.getElementById('category-filter').value = currentCategory;
    document.getElementById('price-slider').value = currentMaxPrice;
    updatePriceLabel(currentMaxPrice);
    document.getElementById('sort-select').value = currentSort;

    // Đặt lại trạng thái các checkbox tồn kho (In Stock / Out of Stock)
    if (currentStockStatus === 'In Stock') {
        document.getElementById('stock-instock').checked = true;
        document.getElementById('stock-outofstock').checked = false;
    } else if (currentStockStatus === 'Out of Stock') {
        document.getElementById('stock-instock').checked = false;
        document.getElementById('stock-outofstock').checked = true;
    } else {
        document.getElementById('stock-instock').checked = true;
        document.getElementById('stock-outofstock').checked = true;
    }

    // Đặt lại và tái áp dụng bộ lọc đánh giá sao (Rating filter)
    document.querySelectorAll('input[name="rating-filter"]').forEach(cb => cb.checked = false);
    const ratings = urlParams.getAll('rating');
    ratings.forEach(val => {
        const cb = document.querySelector(`input[name="rating-filter"][value="${val}"]`);
        if (cb) cb.checked = true;
    });
}

// Tải dropdown danh mục cha từ API
async function initCategoryFilterDropdown() {
    const catSelect = document.getElementById('category-filter');
    if (!catSelect) return;
    try {
        const res = await fetch('/api/search/categories?parentOnly=true');
        if (!res.ok) return;
        const categories = await res.json();
        
        // Đẩy danh mục "Khác" và "Dịch vụ khác" xuống cuối danh sách
        categories.sort((a, b) => {
            const nameA = (a.name || '').toLowerCase().trim();
            const nameB = (b.name || '').toLowerCase().trim();
            const isKhacA = nameA.includes('khác');
            const isKhacB = nameB.includes('khác');
            if (isKhacA && !isKhacB) return 1;
            if (!isKhacA && isKhacB) return -1;
            return (a.id || 0) - (b.id || 0);
        });

        catSelect.innerHTML = '<option value="">Tất cả danh mục</option>' +
            categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');

        if (currentCategory) {
            catSelect.value = currentCategory;
        }
    } catch (e) {
        console.error('Error loading category filter:', e);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    syncUIFromURL();
    initCategoryFilterDropdown();
    fetchProducts();

    // Lắng nghe thay đổi trên các bộ lọc để tự động tìm kiếm lại
    const filterForm = document.getElementById('filter-form');
    if (filterForm) {
        filterForm.querySelectorAll('select, input[type="checkbox"]').forEach(el => {
            el.addEventListener('change', () => applyFilters());
        });
        // Lắng nghe sự kiện `change` trên thanh trượt giá khi người dùng thả tay
        const slider = document.getElementById('price-slider');
        if (slider) {
            slider.addEventListener('change', () => applyFilters());
        }
    }
});

// Lắng nghe sự kiện trình duyệt khi người dùng nhấn nút Back/Forward
window.addEventListener('popstate', () => {
    syncUIFromURL();
    fetchProducts();
});

// Xác định danh mục cha của sản phẩm qua tên (cho dữ liệu mock không có parentCategoryId)
function getParentCategoryName(categoryName, productName = '') {
    const text = ((categoryName || '') + ' ' + (productName || '')).toLowerCase();
    
    if (text.includes('email') || text.includes('gmail') || text.includes('hotmail') || text.includes('outlook') || text.includes('mail')) {
        return 'Email';
    }
    if (text.includes('dịch vụ tiktok') || text.includes('dịch vụ facebook') || text.includes('dịch vụ google') || 
        text.includes('dịch vụ youtube') || text.includes('tăng tương tác') || text.includes('follower') || text.includes('tương tác')) {
        return 'Tăng tương tác';
    }
    if (text.includes('đồ họa') || text.includes('design') || text.includes('tool') || text.includes('video editor') || 
        text.includes('plugin') || text.includes('script') || text.includes('dịch vụ phần mềm')) {
        return 'Dịch vụ phần mềm';
    }
    if (text.includes('phần mềm') || text.includes('key window') || text.includes('key win') || text.includes('diệt virus')) {
        return 'Phần mềm';
    }
    if (text.includes('blockchain') || text.includes('crypto') || text.includes('nft') || text.includes('ví điện tử') || text.includes('coinlist')) {
        return 'Blockchain';
    }
    if (text.includes('tài khoản') || text.includes('account') || text.includes('netflix') || text.includes('chatgpt') || 
        text.includes('spotify') || text.includes('canva') || text.includes('nordvpn') || text.includes('tiktok') || text.includes('bm')) {
        return 'Tài khoản';
    }
    
    return 'Khác';
}

// Kiểm tra sản phẩm có khớp với bộ lọc danh mục cha không
function matchesCategoryFilter(product, categoryFilter) {
    if (!categoryFilter) return true;
    const catId = parseInt(categoryFilter, 10);
    if (product.parentCategoryId != null) {
        return product.parentCategoryId === catId;
    }
    return product.categoryId === catId;
}

// Kiểm tra sản phẩm có khớp với bộ lọc danh mục con không
function matchesSubCategoryFilter(product, subCategory) {
    if (!subCategory) return true;
    const term = subCategory.toLowerCase().trim();
    const name = (product.productName || '').toLowerCase();
    const cat = (product.categoryName || '').toLowerCase();
    const subCatName = (product.subCategoryName || '').toLowerCase();
    return name.includes(term) || cat.includes(term) || subCatName.includes(term) || (product.categoryId && String(product.categoryId) === term);
}

// Lấy nhãn hiển thị kết quả tìm kiếm
function getSearchDisplayLabel() {
    return currentKeyword || currentSubCategory || 'tất cả sản phẩm';
}

// Format tiền VNĐ
function formatVND(value) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })
        .format(value)
        .replace('₫', 'VNĐ');
}

// Cập nhật nhãn hiển thị giá trên thanh trượt
function updatePriceLabel(value) {
    document.getElementById('price-max-label').innerText = formatVND(value);
}

// Xử lý sự kiện tìm kiếm từ thanh tìm kiếm trong header
function triggerHeaderSearch(e) {
    e.preventDefault();
    const keyword = document.getElementById('header-search-input').value.trim();
    if (keyword) {
        window.location.href = `/search?q=${encodeURIComponent(keyword)}`;
    } else {
        window.location.href = `/search`;
    }
}

// Cập nhật URL trình duyệt theo bộ lọc hiện tại mà không reload trang
function updateBrowserURL() {
    const params = new URLSearchParams();
    if (currentKeyword) params.append('q', currentKeyword);
    if (currentSubCategory) params.append('subCategory', currentSubCategory);
    if (currentCategory) params.append('categoryId', currentCategory);
    if (currentMaxPrice) params.append('maxPrice', currentMaxPrice.toString());
    if (currentStockStatus) params.append('stockStatus', currentStockStatus);
    if (currentSort) params.append('sort', currentSort);
    params.append('page', currentPage.toString());

    const ratingChecks = document.querySelectorAll('input[name="rating-filter"]:checked');
    ratingChecks.forEach(cb => {
        params.append('rating', cb.value);
    });

    window.history.pushState(null, '', '?' + params.toString());
}

// Áp dụng bộ lọc khi người dùng submit form hoặc thay đổi bộ lọc
function applyFilters(e) {
    if (e) e.preventDefault();
    currentPage = 0;
    currentCategory = document.getElementById('category-filter').value;
    currentMaxPrice = parseInt(document.getElementById('price-slider').value);

    const instock = document.getElementById('stock-instock').checked;
    const outofstock = document.getElementById('stock-outofstock').checked;
    if (instock && !outofstock) {
        currentStockStatus = 'In Stock';
    } else if (!instock && outofstock) {
        currentStockStatus = 'Out of Stock';
    } else {
        currentStockStatus = '';
    }

    updateBrowserURL();
    fetchProducts();
}

// Xử lý khi người dùng thay đổi tiêu chí sắp xếp
function handleSortChange(sortVal) {
    currentSort = sortVal;
    currentPage = 0;
    updateBrowserURL();
    fetchProducts();
}

// Gọi API lấy sản phẩm từ backend, fallback sang mock nếu lỗi
async function fetchProducts() {
    const params = new URLSearchParams();
    if (currentKeyword) params.append('keyword', currentKeyword);
    if (currentSubCategory) params.append('subCategory', currentSubCategory);
    if (currentCategory) params.append('categoryId', currentCategory);
    if (currentMaxPrice) {
        params.append('minPrice', '0');
        params.append('maxPrice', currentMaxPrice.toString());
    }
    if (currentStockStatus) params.append('stockStatus', currentStockStatus);
    params.append('page', currentPage.toString());
    params.append('size', '12');

    // Định dạng tham số sort
    const sortParts = currentSort.split(',');
    params.append('sort', sortParts[0] + ',' + (sortParts[1] || 'desc'));

    // Thêm bộ lọc đánh giá sao
    const ratingChecks = document.querySelectorAll('input[name="rating-filter"]:checked');
    ratingChecks.forEach(cb => {
        params.append('rating', cb.value);
    });

    try {
        // Gọi API tìm kiếm sản phẩm thực
        const response = await fetch(`/api/search/products?${params.toString()}`);
        if (response.ok) {
            const data = await response.json();
            isUsingMock = false;
            const content = (data && data.content) ? data.content : [];
            renderProducts(content);
            const totalPages = data ? data.totalPages : 0;
            const pageNumber = data ? data.number : 0;
            const totalElements = data ? data.totalElements : 0;
            renderPagination(totalPages, pageNumber);
            updateResultSummary(totalElements, totalElements > 0 ? pageNumber * 12 + 1 : 0, Math.min((pageNumber + 1) * 12, totalElements));
        } else {
            loadInteractiveMock();
        }
    } catch (err) {
        console.error("API error, fallback to mock data", err);
        loadInteractiveMock();
    }
}

// Tải và lọc dữ liệu Mock cục bộ khi API không khả dụng
function loadInteractiveMock() {
    isUsingMock = true;

    let filtered = [...MOCK_PRODUCTS];

    // 1. Lọc theo từ khóa
    if (currentKeyword) {
        const kw = currentKeyword.toLowerCase();
        filtered = filtered.filter(p => p.productName.toLowerCase().includes(kw));
    }

    // 2. Lọc theo danh mục cha (bao gồm nhóm "Khác" = 7)
    if (currentCategory) {
        filtered = filtered.filter(p => matchesCategoryFilter(p, currentCategory));
    }

    // 2b. Lọc theo danh mục con
    if (currentSubCategory) {
        filtered = filtered.filter(p => matchesSubCategoryFilter(p, currentSubCategory));
    }

    // 3. Lọc theo giá tối đa
    filtered = filtered.filter(p => p.price <= currentMaxPrice);

    // 4. Lọc theo trạng thái tồn kho
    if (currentStockStatus === 'In Stock') {
        filtered = filtered.filter(p => p.stock > 0);
    } else if (currentStockStatus === 'Out of Stock') {
        filtered = filtered.filter(p => p.stock === 0);
    }

    // 5. Lọc theo đánh giá sao
    const ratingChecks = document.querySelectorAll('input[name="rating-filter"]:checked');
    if (ratingChecks.length > 0) {
        const selectedRatings = Array.from(ratingChecks).map(c => parseInt(c.value));
        filtered = filtered.filter(p => {
            return selectedRatings.some(rate => {
                if (rate === 5) {
                    return p.averageRating >= 5.0;
                } else if (rate === 4) {
                    return p.averageRating >= 4.0 && p.averageRating < 5.0;
                } else if (rate === 3) {
                    return p.averageRating >= 3.0 && p.averageRating < 4.0;
                }
                return false;
            });
        });
    }

    // 6. Sắp xếp theo tiêu chí được chọn
    if (currentSort === 'price,asc') {
        filtered.sort((a, b) => a.price - b.price);
    } else if (currentSort === 'price,desc') {
        filtered.sort((a, b) => b.price - a.price);
    } else if (currentSort === 'averageRating,desc') {
        filtered.sort((a, b) => b.averageRating - a.averageRating);
    } else if (currentSort === 'productId,desc') {
        filtered.sort((a, b) => b.productId - a.productId);
    } else {
        filtered.sort((a, b) => b.productId - a.productId);
    }

    const totalElements = filtered.length;
    const itemsPerPage = 12;
    const totalPages = Math.max(1, Math.ceil(totalElements / itemsPerPage));

    if (currentPage >= totalPages) currentPage = 0;

    const startIndex = currentPage * itemsPerPage;
    const paginatedItems = filtered.slice(startIndex, startIndex + itemsPerPage);

    renderProducts(paginatedItems);
    renderPagination(totalPages, currentPage);
    updateResultSummary(totalElements, totalElements > 0 ? startIndex + 1 : 0, Math.min(startIndex + itemsPerPage, totalElements));
}

// Map tên sản phẩm sang icon và màu sắc tương ứng
function getProductIconAndColor(productName) {
    const nameLower = productName.toLowerCase();
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

// Render danh sách thẻ sản phẩm vào container
function renderProducts(items) {
    const container = document.getElementById('products-container');
    container.innerHTML = '';
    window.currentRenderedProducts = items;

    if (items.length === 0) {
        container.innerHTML = `
            <div class="no-results-container">
                <div class="no-results-icon"><i class="fa fa-search"></i></div>
                <div class="no-results-title">Không tìm thấy sản phẩm phù hợp</div>
                <div class="no-results-desc">Vui lòng thử lại với từ khóa khác hoặc điều chỉnh các bộ lọc bên trái.</div>
            </div>
        `;
        return;
    }

    items.forEach(product => {
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
        const buyButtonHtml = isOutOfStock
            ? `<button class="btn-buy btn-buy--preorder" onclick="event.stopPropagation(); preOrderNow(${product.productId})"><i class="fa fa-clock-o"></i> Đặt trước</button>`
            : `<button class="btn-buy" onclick="event.stopPropagation(); buyNow(${product.productId})">Mua ngay</button>`;
        const cartButtonHtml = isOutOfStock
            ? `<button class="btn-cart btn-cart--disabled" title="Sản phẩm hết hàng, vui lòng đặt trước" onclick="event.stopPropagation(); preOrderNow(${product.productId})"><i class="fa fa-shopping-cart"></i></button>`
            : `<button class="btn-cart" title="Thêm vào giỏ hàng" onclick="event.stopPropagation(); addToCartFromCard(${product.productId})"><i class="fa fa-shopping-cart"></i></button>`;

        let mediaHtml = `<i class="fa ${iconInfo.icon} product-card__img-icon" style="color: ${iconInfo.color};"></i>`;
        if (product.imageUrl && !product.imageUrl.includes('placeholder.com')) {
            mediaHtml = `<img src="${product.imageUrl}" class="product-card__img-real" alt="${product.productName}" style="width:100%; height:100%; object-fit:cover;">`;
        }

        const cardHtml = `
            <div class="product-card" onclick="window.location.href='/products/${product.productId}' + window.location.search">
                <div class="product-card__image-container">
                    ${mediaHtml}
                </div>
                <div class="product-info">
                    <div class="category-rating">
                        <span class="category-badge">${product.categoryName || 'Khác'}</span>
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
        container.innerHTML += cardHtml;
    });
}

// Render bộ nút phân trang (Pagination) động dựa trên tổng số trang và trang hiện tại
function renderPagination(totalPages, activePage) {
    const container = document.getElementById('pagination-container');
    container.innerHTML = '';

    if (totalPages <= 1) return;

    // Nút "Trang trước" (Previous Page)
    const prevButton = document.createElement('button');
    prevButton.className = 'page-btn';
    prevButton.innerHTML = '<i class="fa fa-angle-left"></i>';
    prevButton.disabled = activePage === 0;
    prevButton.onclick = () => changePage(activePage - 1);
    container.appendChild(prevButton);

    // Các nút số trang cụ thể
    for (let i = 0; i < totalPages; i++) {
        const pageButton = document.createElement('button');
        pageButton.className = `page-btn ${i === activePage ? 'active' : ''}`;
        pageButton.innerText = (i + 1).toString();
        pageButton.onclick = () => changePage(i);
        container.appendChild(pageButton);
    }

    // Nút "Trang sau" (Next Page)
    const nextButton = document.createElement('button');
    nextButton.className = 'page-btn';
    nextButton.innerHTML = '<i class="fa fa-angle-right"></i>';
    nextButton.disabled = activePage === totalPages - 1;
    nextButton.onclick = () => changePage(activePage + 1);
    container.appendChild(nextButton);
}

// Xử lý khi người dùng đổi trang
function changePage(pageNum) {
    currentPage = pageNum;
    updateBrowserURL();
    if (isUsingMock) {
        loadInteractiveMock();
    } else {
        fetchProducts();
    }
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// Cập nhật lại số liệu thống kê kết quả tìm kiếm trên tiêu đề trang
function updateResultSummary(total, start, end) {
    const summaryTotal = document.getElementById('summary-total');
    if (summaryTotal) summaryTotal.innerText = total;

    const summaryRange = document.getElementById('summary-range');
    if (summaryRange) summaryRange.innerText = total > 0 ? `${start} - ${end}` : '0';

    const queryLabel = getSearchDisplayLabel();
    const titleSpan = document.querySelector('.title-section h1 span');
    if (titleSpan) titleSpan.innerText = queryLabel;

    const descText = document.querySelector('.title-section p');
    if (descText) descText.innerHTML = `Chúng tôi tìm thấy <strong>${total}</strong> sản phẩm số chất lượng phù hợp với tìm kiếm của bạn.`;
}

// Xử lý nút "Mua ngay" trên card sản phẩm, chuyển hướng sang trang chi tiết (PDP)
function buyNow(productId) {
    if (typeof window.isEmployeeUser === 'function' && window.isEmployeeUser()) {
        return; // Tài khoản nhân viên không được phép mua hàng
    }
    window.location.href = `/products/${productId}` + window.location.search;
}

// Xử lý nút "Đặt trước" trên card sản phẩm hết hàng
function preOrderNow(productId) {
    if (typeof window.isEmployeeUser === 'function' && window.isEmployeeUser()) {
        return; // Tài khoản nhân viên không được phép đặt trước
    }
    window.location.href = `/products/${productId}` + window.location.search;
}

// Xử lý nút "Thêm vào giỏ hàng" từ card sản phẩm trong danh sách kết quả
function addToCartFromCard(productId) {
    if (typeof window.isEmployeeUser === 'function' && window.isEmployeeUser()) {
        return; // Tài khoản nhân viên không được phép thêm vào giỏ
    }
    const product = (window.currentRenderedProducts || []).find(item => item.productId === productId);
    if (!product) {
        showWarningToast('Không tìm thấy thông tin sản phẩm để thêm vào giỏ hàng.');
        return;
    }
    if (Number(product.stock || 0) <= 0) {
        showWarningToast('Sản phẩm đã hết hàng, vui lòng đặt trước.');
        preOrderNow(productId);
        return;
    }
    const cartItem = {
        productId: product.productId,
        duration: 1,
        variantLabel: '',
        productName: product.productName,
        sellerName: product.sellerName || 'VipStore',
        price: product.price,
        icon: getProductIconAndColor(product.productName).icon,
        iconColor: getProductIconAndColor(product.productName).color,
        quantity: 1
    };
    const cartKey = typeof window.getCartStorageKey === 'function' ? window.getCartStorageKey() : 'mmoCart';
    const cart = JSON.parse(localStorage.getItem(cartKey) || '[]');
    const existing = cart.find(item => item.productId === cartItem.productId && item.duration === cartItem.duration);
    if (existing) {
        existing.quantity += 1;
    } else {
        cart.push(cartItem);
    }
    localStorage.setItem(cartKey, JSON.stringify(cart));
    showSuccessToast('Đã thêm sản phẩm vào giỏ hàng thành công!');
    if (typeof window.refreshHeaderCartBadge === 'function') window.refreshHeaderCartBadge();
}
