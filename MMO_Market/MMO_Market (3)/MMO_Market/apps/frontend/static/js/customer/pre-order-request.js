// =======================================================
// LOGIC TRANG GỬI YÊU CẦU ĐẶT TRƯỚC SẢN PHẨM
// (Yêu cầu biến `fallbackProduct` được khai báo trước từ HTML template Thymeleaf)
// =======================================================

// Đọc tham số sản phẩm từ URL hoặc dữ liệu Thymeleaf truyền xuống
const productParams = new URLSearchParams(window.location.search);

// Hàm đọc tham số URL, trả về giá trị fallback nếu tham số rỗng hoặc không tồn tại
function readParam(name, fallback) {
    const value = productParams.get(name);
    return value === null || value === '' ? fallback : value;
}

// Hàm chuyển đổi chuỗi sang số, trả về fallback nếu không phải số hợp lệ
function parseNumber(value, fallback = 0) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
}

// Format số tiền sang định dạng VNĐ
function formatVND(value) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })
        .format(Math.max(0, Number(value) || 0))
        .replace('₫', 'VNĐ');
}

// Gộp dữ liệu sản phẩm từ URL params (ưu tiên) và Thymeleaf fallback (dự phòng)
const preorderProduct = {
    productId: parseNumber(readParam('productId', fallbackProduct.productId)),
    variantId: parseNumber(readParam('variantId', null)) || null,
    variantName: readParam('variantName', ''),
    productName: readParam('productName', fallbackProduct.productName || 'Sản phẩm'),
    sellerName: readParam('sellerName', fallbackProduct.sellerName || 'Seller'),
    price: parseNumber(readParam('price', fallbackProduct.price), fallbackProduct.price || 0),
    icon: readParam('icon', fallbackProduct.icon || 'fa-shopping-basket'),
    iconColor: readParam('iconColor', fallbackProduct.iconColor || '#ea580c'),
    imageUrl: readParam('imageUrl', fallbackProduct.imageUrl || ''),
    stock: parseNumber(readParam('stock', fallbackProduct.stock), fallbackProduct.stock || 0)
};

// Giá đơn vị tối thiểu là 1 VNĐ
const unitPrice = preorderProduct.price > 0 ? preorderProduct.price : 1;

// Lấy số lượng người dùng nhập từ ô input
function getQuantity() {
    return Math.max(1, parseInt(document.getElementById('quantityInput')?.value || '1', 10) || 1);
}

// Kiểm tra xem sản phẩm này có phải loại dịch vụ không (isService=true từ URL)
const isService = readParam('isService', 'false') === 'true';

// Tính giá kỳ vọng = đơn giá x số lượng
function getExpectedPriceVnd() {
    return unitPrice * getQuantity();
}

// Cập nhật bảng tóm tắt khi người dùng thay đổi số lượng
function updatePreOrderSummary() {
    const quantity = getQuantity();
    document.getElementById('summaryQuantityText').innerText = String(quantity);
    document.getElementById('summaryExpectedPriceText').innerText = formatVND(getExpectedPriceVnd());
}

// Lấy access token từ session hoặc local storage
function getAccessToken() {
    const token = sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken');
    if (!token || token === 'null' || token === 'undefined') return null;
    return token;
}

document.addEventListener('DOMContentLoaded', () => {
    // Nếu sản phẩm còn hàng và không phải dịch vụ, chuyển hướng về trang mua trực tiếp
    if (!isService && preorderProduct.stock > 0 && preorderProduct.productId) {
        showWarningToast('Sản phẩm này còn hàng, vui lòng mua trực tiếp thay vì đặt trước.');
        window.location.href = `/products/${preorderProduct.productId}`;
        return;
    }

    // Hiển thị ảnh hoặc icon cho sản phẩm
    const thumb = document.getElementById('productThumb');
    if (preorderProduct.imageUrl && !preorderProduct.imageUrl.includes('placeholder.com')) {
        thumb.innerHTML = `<img src="${preorderProduct.imageUrl}" alt="${preorderProduct.productName}" style="width: 100%; height: 100%; object-fit: cover; border-radius: 6px;">`;
        thumb.style.color = '';
    } else {
        thumb.innerHTML = `<i class="fa ${preorderProduct.icon}"></i>`;
        thumb.style.color = preorderProduct.iconColor;
    }

    // Điền thông tin sản phẩm vào UI
    document.getElementById('productNameText').innerText = preorderProduct.productName || 'Sản phẩm';
    document.getElementById('sellerNameText').innerText = preorderProduct.sellerName || 'Seller';
    document.getElementById('stockText').innerText = String(Math.max(0, preorderProduct.stock));
    document.getElementById('referencePriceText').innerText = formatVND(preorderProduct.price);
    document.getElementById('summaryProductText').innerText = preorderProduct.productName || '-';
    document.getElementById('summarySellerText').innerText = preorderProduct.sellerName || '-';
    document.getElementById('summaryCurrentPriceText').innerText = formatVND(preorderProduct.price);

    // Nếu là dịch vụ, điều chỉnh giao diện phù hợp (thay tiêu đề, ẩn tồn kho, bắt buộc nhập thông tin)
    if (isService) {
        document.querySelector('h1.preorder-title').innerText = 'Đặt trước dịch vụ';
        document.querySelector('.preorder-subtitle').innerText = 'Nhập thông tin (như tên acc TikTok, IG, FB hoặc Link bài viết) để Seller thực hiện dịch vụ.';
        
        const notesLabel = document.querySelector('label[for="notesInput"]');
        if (notesLabel) {
            notesLabel.innerHTML = 'Thông tin cung cấp cho Seller (Tên Acc, Link, Ghi chú) <span style="color: #ef4444;">*</span>';
        }
        
        const notesInput = document.getElementById('notesInput');
        if (notesInput) {
            notesInput.placeholder = 'Ví dụ: Link bài viết TikTok: https://... hoặc User IG: @abc...';
            notesInput.required = true;
        }
        
        const stockContainer = document.getElementById('stockText').parentElement;
        if (stockContainer) stockContainer.style.display = 'none';
        const statusPill = document.querySelector('.status-pill');
        if (statusPill) {
            statusPill.innerHTML = '<i class="fa fa-bolt"></i> Dịch vụ';
            statusPill.style.backgroundColor = 'var(--surface)';
            statusPill.style.color = 'var(--brand)';
        }
    }

    // Lắng nghe thay đổi số lượng để cập nhật bảng tóm tắt
    const quantityInput = document.getElementById('quantityInput');
    quantityInput.addEventListener('input', updatePreOrderSummary);
    updatePreOrderSummary();

    document.getElementById('preOrderForm').addEventListener('submit', submitPreOrder);
});

// Gửi yêu cầu đặt trước sản phẩm lên Server
async function submitPreOrder(event) {
    event.preventDefault();

    // Kiểm tra Token đăng nhập
    const token = getAccessToken();
    if (!token) {
        showWarningToast('Vui lòng đăng nhập để gửi yêu cầu đặt trước.');
        window.location.href = '/login';
        return;
    }

    const quantity = getQuantity();
    const expectedPriceVnd = getExpectedPriceVnd();
    const notes = document.getElementById('notesInput').value.trim();

    if (!preorderProduct.productId) {
        showWarningToast('Thiếu thông tin sản phẩm để gửi yêu cầu đặt trước.');
        return;
    }

    if (quantity < 1 || expectedPriceVnd < 1) {
        showWarningToast('Vui lòng nhập số lượng hợp lệ.');
        return;
    }
    
    // Dịch vụ bắt buộc phải nhập thông tin cho Seller
    if (isService && !notes) {
        showWarningToast('Vui lòng nhập thông tin cho Seller (link, tên tài khoản...).');
        return;
    }

    // Vô hiệu hóa nút submit, hiển thị trạng thái đang gửi
    const submitBtn = document.querySelector('.submit-btn');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang gửi...';

    // Delay 900ms để hiệu ứng spinner mượt mà hơn (UX)
    await new Promise(resolve => setTimeout(resolve, 900));

    try {
        // Gọi API POST tạo đơn đặt trước mới
        const response = await fetch('/api/v1/pre-orders', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                productId: preorderProduct.productId,
                variantId: preorderProduct.variantId,
                quantity,
                expectedPriceVnd,
                notes
            })
        });

        if (response.status === 401) {
            sessionStorage.clear();
            localStorage.removeItem('accessToken');
            window.location.href = '/login';
            return;
        }

        if (!response.ok) {
            throw new Error('Gửi yêu cầu đặt trước thất bại.');
        }

        // Ẩn form và hiển thị hộp thành công
        document.getElementById('preOrderForm').style.display = 'none';

        const successMsgEl = document.getElementById('successMessage');
        if (successMsgEl) {
            successMsgEl.innerText = `Yêu cầu đặt trước sản phẩm "${preorderProduct.productName}" với số lượng ${quantity} đã được ghi nhận thành công.`;
        }
        const successBox = document.getElementById('successBox');
        if (successBox) {
            successBox.classList.add('active');
        }

        showSuccessToast(`Đặt trước thành công sản phẩm "${preorderProduct.productName}"!`);
    } catch (err) {
        showErrorToast(err.message || 'Có lỗi xảy ra, vui lòng thử lại.');
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<i class="fa fa-paper-plane"></i> Gửi yêu cầu đặt trước';
    }
}

// Quay lại trang chi tiết sản phẩm khi người dùng nhấn nút Hủy
function backToProduct() {
    if (preorderProduct.productId) {
        window.location.href = `/products/${preorderProduct.productId}`;
        return;
    }
    window.location.href = '/products';
}
