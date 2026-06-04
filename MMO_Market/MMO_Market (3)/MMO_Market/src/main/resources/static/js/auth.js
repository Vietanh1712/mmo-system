const API_BASE = 'http://localhost:8080/api';
['accessToken', 'refreshToken', 'loginTimestamp', 'userInfo', 'user'].forEach(key => {
    if (!sessionStorage.getItem('accessToken')) {
        localStorage.removeItem(key);
    }
});

// =======================================================
// 1. UTILITIES & AUTH FETCH
// =======================================================
async function authFetch(url, options = {}) {
    const token = sessionStorage.getItem('accessToken');
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...options.headers
    };

    const response = await fetch(`${API_BASE}${url}`, { ...options, headers });

    if (response.status === 401) {
        if (typeof logout === 'function') {
            logout();
        } else {
            sessionStorage.clear();
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('loginTimestamp');
            localStorage.removeItem('userInfo');
            localStorage.removeItem('user');
            window.location.href = '/login';
        }
        throw new Error('Phiên đăng nhập hết hạn hoặc không hợp lệ.');
    }
    return response;
}

function logout() {
    const refreshToken = sessionStorage.getItem('refreshToken');
    if (refreshToken) {
        fetch('/api/auth/logout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken }),
            keepalive: true
        }).catch(() => {});
    }
    sessionStorage.clear();
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('loginTimestamp');
    localStorage.removeItem('userInfo');
    localStorage.removeItem('user');
    window.location.href = '/login';
}

// =======================================================
// 2. PRODUCT LOGIC
// =======================================================
async function loadProducts(category = '') {
    const container = document.getElementById('product-container');
    if (!container) return;

    try {
        const url = category ? `/products?category=${category}` : '/products';
        const res = await fetch(`${API_BASE}${url}`);
        const products = await res.json();

        if (!Array.isArray(products)) {
            console.error('Dữ liệu sản phẩm không hợp lệ:', products);
            container.innerHTML = '<p style="text-align:center; padding: 20px;">Không có sản phẩm nào.</p>';
            return;
        }

        if (products.length === 0) {
            container.innerHTML = '<p style="text-align:center; padding: 20px;">Danh mục này hiện chưa có sản phẩm.</p>';
            return;
        }

        container.innerHTML = products.map(p => `
            <div class="product-card">
                <div class="product-card__image-container">
                    <span class="product-card__badge-ads">ADS</span>
                    <i class="fa fa-shopping-bag product-card__img-icon"></i>
                </div>
                <div class="product-card__body">
                    <p class="product-card__stock">Tồn: ${p.stock || 0}</p>
                    <p class="product-card__price">${typeof formatCurrency === 'function' ? formatCurrency(p.price) : p.price + 'đ'}</p>
                    <h3 class="product-card__title">${p.name}</h3>
                    <div class="product-card__footer">Bán: ${p.soldCount || 0}</div>
                </div>
            </div>
        `).join('');
    } catch (err) {
        console.error('Lỗi tải sản phẩm:', err);
        container.innerHTML = '<p style="text-align:center; color: red;">Lỗi khi tải danh sách sản phẩm. Vui lòng thử lại sau.</p>';
    }
}

// =======================================================
// 3. TRANSACTION LOGIC (NẠP TIỀN / MUA HÀNG)
// =======================================================
async function topUp(amount) {
    if (!amount || isNaN(amount) || amount <= 0) {
        alert("Số tiền nạp không hợp lệ!");
        return;
    }

    try {
        const res = await authFetch('/wallet/topup', {
            method: 'POST',
            body: JSON.stringify({ amount })
        });

        const data = await res.json();

        if (res.ok) {
            alert('Yêu cầu nạp tiền thành công!');
            if (data.newBalance !== undefined) {
                updateUserBalance(data.newBalance);
            }
        } else {
            alert(data.message || 'Nạp tiền thất bại. Vui lòng thử lại.');
        }
    } catch (err) {
        console.error('Lỗi khi nạp tiền:', err);
    }
}

function updateUserBalance(newBalance) {
    const userString = sessionStorage.getItem('user');
    if (!userString) return;

    try {
        const user = JSON.parse(userString);
        user.balanceVnd = newBalance;
        sessionStorage.setItem('user', JSON.stringify(user));

        if (typeof updateHeaderForLoggedInUser === 'function') {
            updateHeaderForLoggedInUser();
        }
    } catch (e) {
        console.error('Lỗi khi cập nhật dữ liệu user cục bộ:', e);
    }
}

function normalizeRole(roleValue) {
    if (!roleValue) return 'Customer';
    try {
        const parsed = JSON.parse(roleValue);
        return parsed.role || 'Customer';
    } catch (error) {
        const role = String(roleValue).replaceAll('"', '').trim();
        if (role.toLowerCase().includes('admin')) return 'Admin';
        if (role.toLowerCase().includes('staff')) return 'Staff';
        if (role.toLowerCase().includes('seller')) return 'Seller';
        return 'Customer';
    }
}

function togglePassword(inputId, icon) {
    const input = document.getElementById(inputId);
    if (input.type === "password") {
        input.type = "text";
        icon.classList.remove("fa-eye");
        icon.classList.add("fa-eye-slash");
    } else {
        input.type = "password";
        icon.classList.remove("fa-eye-slash");
        icon.classList.add("fa-eye");
    }
}

// =======================================================
// 4. LOGIC TRANG QUÊN MẬT KHẨU (FORGOT PASSWORD)
// =======================================================
document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('forgotPasswordForm');
    if (!form) return; // Rào chắn: Không phải trang quên mật khẩu thì thoát

    const emailInput = document.getElementById('forgot-email');
    const btnForgot = document.getElementById('btnForgot');
    const alertBox = document.getElementById('forgot-msg');

    function showAlert(message, type) {
        alertBox.textContent = message;
        alertBox.className = `message ${type}`;
        alertBox.style.display = 'block';
    }

    function showFieldError(fieldId, message) {
        const inputEl = document.getElementById(fieldId);
        const errorEl = document.getElementById(fieldId + '-error');
        if (inputEl) inputEl.classList.add('input-error');
        if (errorEl) errorEl.textContent = message;
    }

    function clearFieldError(fieldId) {
        const inputEl = document.getElementById(fieldId);
        const errorEl = document.getElementById(fieldId + '-error');
        if (inputEl) inputEl.classList.remove('input-error');
        if (errorEl) errorEl.textContent = '';
    }

    // ĐÃ BỔ SUNG VALIDATE REAL-TIME CHO EMAIL
    emailInput.addEventListener('input', function() {
        alertBox.style.display = 'none';
        const val = this.value.trim();
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        if (val === '') {
            showFieldError('forgot-email', 'Vui lòng nhập địa chỉ email.');
        } else if (!emailRegex.test(val)) {
            showFieldError('forgot-email', 'Định dạng email không hợp lệ (Ví dụ: abc@gmail.com).');
        } else {
            clearFieldError('forgot-email');
        }
    });

    form.addEventListener('submit', function(e) {
        e.preventDefault();
        alertBox.style.display = 'none';
        const emailValue = emailInput.value.trim();

        if (emailValue === '') {
            showFieldError('forgot-email', 'Vui lòng nhập địa chỉ email.');
            return;
        }

        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailPattern.test(emailValue)) {
            showFieldError('forgot-email', 'Định dạng email không hợp lệ.');
            return;
        }

        btnForgot.disabled = true;
        btnForgot.innerHTML = '<i class="fas fa-spinner fa-spin"></i> <span>ĐANG GỬI...</span>';

        fetch('/api/auth/forgot-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: emailValue })
        })
            .then(response => response.json().then(data => ({ status: response.status, body: data })))
            .then(res => {
                if (res.status === 200 && res.body.success) {
                    showAlert('Mã khôi phục đã được gửi!', 'success');
                    sessionStorage.setItem('resetEmail', emailValue);
                    setTimeout(() => { window.location.href = '/reset-password'; }, 2000);
                } else {
                    showFieldError('forgot-email', res.body.message || 'Lỗi hệ thống, vui lòng thử lại.');
                    btnForgot.disabled = false;
                    btnForgot.innerHTML = '<span>GỬI MÃ KHÔI PHỤC</span>';
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showAlert('Lỗi kết nối đến máy chủ. Vui lòng kiểm tra mạng.', 'error');
                btnForgot.disabled = false;
                btnForgot.innerHTML = '<span>GỬI MÃ KHÔI PHỤC</span>';
            });
    });
});

// =======================================================
// 5. LOGIC TRANG ĐĂNG NHẬP (LOGIN)
// =======================================================
document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('loginForm');
    if (!form) return; // Rào chắn: Không phải trang đăng nhập thì thoát

    const alertBox = document.getElementById('alert-message');
    const btnLogin = document.getElementById('btnLogin');
    const googleLoginButton = document.getElementById('googleLoginButton');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const passwordToggle = document.querySelector('[data-toggle-password="password"]');

    function showLoginAlert(message, type) {
        alertBox.textContent = message;
        alertBox.className = `message ${type}`;
        alertBox.style.display = 'block';
    }

    function getSafeReturnUrl() {
        const returnUrl = new URLSearchParams(window.location.search).get('returnUrl');
        if (!returnUrl) return null;
        if (!returnUrl.startsWith('/') || returnUrl.startsWith('//')) return null;
        if (returnUrl.startsWith('/login') || returnUrl.startsWith('/register')) return null;
        return returnUrl;
    }

    function showFieldError(fieldId, message) {
        const inputEl = document.getElementById(fieldId);
        const errorEl = document.getElementById(fieldId + '-error');
        if (inputEl) inputEl.classList.add('input-error');
        if (errorEl) errorEl.textContent = message;
    }

    function clearFieldError(fieldId) {
        const inputEl = document.getElementById(fieldId);
        const errorEl = document.getElementById(fieldId + '-error');
        if (inputEl) inputEl.classList.remove('input-error');
        if (errorEl) errorEl.textContent = '';
    }

    if (passwordToggle) {
        passwordToggle.addEventListener('click', function() {
            const isHidden = passwordInput.type === 'password';
            passwordInput.type = isHidden ? 'text' : 'password';

            const icon = passwordToggle.querySelector('i');
            if (icon) {
                icon.classList.toggle('fa-eye', !isHidden);
                icon.classList.toggle('fa-eye-slash', isHidden);
            }

            passwordToggle.setAttribute('aria-label', isHidden ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu');
        });
    }

    if (googleLoginButton) {
        googleLoginButton.addEventListener('click', function() {
            showLoginAlert('Đăng nhập Google sẽ được triển khai sau khi backend OAuth2 sẵn sàng.', 'info');
        });
    }

    emailInput.addEventListener('input', function() {
        alertBox.style.display = 'none';
        const val = this.value.trim();
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (val === '') {
            showFieldError('email', 'Địa chỉ email không được để trống.');
        } else if (!emailRegex.test(val)) {
            showFieldError('email', 'Định dạng email không hợp lệ (Ví dụ: abc@gmail.com).');
        } else {
            clearFieldError('email');
        }
    });

    passwordInput.addEventListener('input', function() {
        alertBox.style.display = 'none';
        if (this.value === '') {
            showFieldError('password', 'Mật khẩu không được để trống.');
        } else {
            clearFieldError('password');
        }
    });

    form.addEventListener('submit', function(e) {
        e.preventDefault();
        alertBox.style.display = 'none';

        let isValid = true;
        const email = emailInput.value.trim();
        const password = passwordInput.value;

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (email === '') {
            showFieldError('email', 'Địa chỉ email không được để trống.');
            isValid = false;
        } else if (!emailRegex.test(email)) {
            showFieldError('email', 'Định dạng email không hợp lệ.');
            isValid = false;
        }

        if (password === '') {
            showFieldError('password', 'Mật khẩu không được để trống.');
            isValid = false;
        }

        if (!isValid) return;

        btnLogin.disabled = true;
        btnLogin.innerHTML = '<span>Đang đăng nhập...</span>';

        fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        })
            .then(response => response.json().then(data => ({ status: response.status, body: data })))
            .then(res => {
                if (res.status === 200 && res.body.accessToken) {
                    showLoginAlert('Đăng nhập thành công!', 'success');

                    sessionStorage.setItem('accessToken', res.body.accessToken);
                    if (res.body.refreshToken) {
                        sessionStorage.setItem('refreshToken', res.body.refreshToken);
                    }
                    sessionStorage.setItem('loginTimestamp', Date.now().toString());

                    const userInfo = {
                        id: res.body.userId,
                        email: res.body.email,
                        fullName: res.body.fullName,
                        role: res.body.role,
                        balanceVnd: res.body.balanceVnd || 0
                    };
                    sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
                    sessionStorage.setItem('user', JSON.stringify(userInfo));

                    const redirectUrl = getSafeReturnUrl()
                        || (normalizeRole(userInfo.role) === 'Admin' ? '/admin/users' : '/');
                    setTimeout(() => window.location.href = redirectUrl, 1000);
                } else {
                    showLoginAlert(res.body.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.', 'error');
                    btnLogin.disabled = false;
                    btnLogin.innerHTML = '<span>Đăng nhập</span>';
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showLoginAlert('Lỗi kết nối máy chủ. Vui lòng thử lại sau.', 'error');
                btnLogin.disabled = false;
                btnLogin.innerHTML = '<span>Đăng nhập</span>';
            });
    });
});

// =======================================================
// 6. LOGIC TRANG ĐĂNG KÝ (REGISTER)
// =======================================================
document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('registerForm');
    if (!form) return; // Rào chắn: Không phải trang đăng ký thì thoát

    const alertBox = document.getElementById('alert-message');
    const btnRegister = document.getElementById('btnRegister');
    const fullNameInput = document.getElementById('fullName');
    const emailInput = document.getElementById('email');
    const phoneInput = document.getElementById('phone');
    const passwordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const passwordToggles = document.querySelectorAll('[data-toggle-password]');
    const passwordRegex = /^(?=.*[A-Z])(?=.*[\W_]).{6,}$/;

    function showRegisterAlert(message, type) {
        alertBox.textContent = message;
        alertBox.className = `message ${type}`;
        alertBox.style.display = 'block';
    }

    function showFieldError(fieldId, message) {
        const inputEl = document.getElementById(fieldId);
        const errorEl = document.getElementById(fieldId + '-error');
        if (inputEl) inputEl.classList.add('input-error');
        if (errorEl) errorEl.textContent = message;
    }

    function clearFieldError(fieldId) {
        const inputEl = document.getElementById(fieldId);
        const errorEl = document.getElementById(fieldId + '-error');
        if (inputEl) inputEl.classList.remove('input-error');
        if (errorEl) errorEl.textContent = '';
    }

    passwordToggles.forEach(toggle => {
        toggle.addEventListener('click', function() {
            const inputId = this.dataset.togglePassword;
            const input = document.getElementById(inputId);
            if (!input) return;

            const isHidden = input.type === 'password';
            input.type = isHidden ? 'text' : 'password';

            const icon = this.querySelector('i');
            if (icon) {
                icon.classList.toggle('fa-eye', !isHidden);
                icon.classList.toggle('fa-eye-slash', isHidden);
            }

            this.setAttribute('aria-label', isHidden ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu');
        });
    });

    fullNameInput.addEventListener('input', function() {
        alertBox.style.display = 'none';
        const val = this.value.trim();
        if (val === '') {
            showFieldError('fullName', 'Họ và tên không được để trống.');
        } else if (val.length < 3) {
            showFieldError('fullName', 'Họ và tên phải có ít nhất 3 ký tự.');
        } else {
            clearFieldError('fullName');
        }
    });

    emailInput.addEventListener('input', function() {
        alertBox.style.display = 'none';
        const val = this.value.trim();
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (val === '') {
            showFieldError('email', 'Địa chỉ email không được để trống.');
        } else if (!emailRegex.test(val)) {
            showFieldError('email', 'Định dạng email không hợp lệ (Ví dụ: abc@gmail.com).');
        } else {
            clearFieldError('email');
        }
    });

    phoneInput.addEventListener('input', function() {
        alertBox.style.display = 'none';
        const val = this.value.trim();
        if (val !== '') {
            const phoneRegex = /^0\d{9}$/;
            if (!phoneRegex.test(val)) {
                showFieldError('phone', 'Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0.');
            } else {
                clearFieldError('phone');
            }
        } else {
            clearFieldError('phone');
        }
    });

    passwordInput.addEventListener('input', function() {
        alertBox.style.display = 'none';
        const val = this.value;
        if (val === '') {
            showFieldError('password', 'Mật khẩu không được để trống.');
        } else if (!passwordRegex.test(val)) {
            showFieldError('password', 'Mật khẩu phải có ít nhất 6 ký tự, gồm 1 chữ viết hoa và 1 ký tự đặc biệt.');
        } else {
            clearFieldError('password');
        }

        if (confirmPasswordInput.value !== '') {
            if (confirmPasswordInput.value !== val) {
                showFieldError('confirmPassword', 'Mật khẩu xác nhận không trùng khớp.');
            } else {
                clearFieldError('confirmPassword');
            }
        }
    });

    confirmPasswordInput.addEventListener('input', function() {
        alertBox.style.display = 'none';
        if (this.value === '') {
            showFieldError('confirmPassword', 'Vui lòng nhập lại mật khẩu.');
        } else if (this.value !== passwordInput.value) {
            showFieldError('confirmPassword', 'Mật khẩu xác nhận không trùng khớp.');
        } else {
            clearFieldError('confirmPassword');
        }
    });

    form.addEventListener('submit', function(e) {
        e.preventDefault();
        alertBox.style.display = 'none';

        let isValid = true;
        const email = emailInput.value.trim();
        const password = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;
        const fullName = fullNameInput.value.trim();
        const phone = phoneInput.value.trim();

        if (fullName === '') {
            showFieldError('fullName', 'Họ và tên không được để trống.');
            isValid = false;
        } else if (fullName.length < 3) {
            showFieldError('fullName', 'Họ và tên phải có ít nhất 3 ký tự.');
            isValid = false;
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (email === '') {
            showFieldError('email', 'Địa chỉ email không được để trống.');
            isValid = false;
        } else if (!emailRegex.test(email)) {
            showFieldError('email', 'Định dạng email không hợp lệ.');
            isValid = false;
        }

        if (phone !== '') {
            const phoneRegex = /^0\d{9}$/;
            if (!phoneRegex.test(phone)) {
                showFieldError('phone', 'Số điện thoại không hợp lệ. Phải đúng 10 số và bắt đầu bằng số 0.');
                isValid = false;
            }
        }

        if (password === '') {
            showFieldError('password', 'Mật khẩu không được để trống.');
            isValid = false;
        } else if (!passwordRegex.test(password)) {
            showFieldError('password', 'Mật khẩu chưa đủ độ mạnh theo yêu cầu.');
            isValid = false;
        }

        if (confirmPassword === '') {
            showFieldError('confirmPassword', 'Vui lòng nhập lại mật khẩu.');
            isValid = false;
        } else if (confirmPassword !== password) {
            showFieldError('confirmPassword', 'Mật khẩu xác nhận không trùng khớp.');
            isValid = false;
        }

        if (!isValid) return;

        btnRegister.disabled = true;
        btnRegister.innerHTML = '<span>Đang xử lý...</span>';

        const payload = {
            email: email,
            password: password,
            fullName: fullName,
            phone: phone,
            role: "CUSTOMER"
        };

        fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
            .then(response => response.json().then(data => ({ status: response.status, body: data })))
            .then(res => {
                if (res.status === 201 && res.body.success) {
                    showRegisterAlert('Tạo tài khoản thành công. Vui lòng xác thực OTP trong email.', 'success');

                    sessionStorage.setItem('registeredEmail', email);
                    setTimeout(() => window.location.href = '/verify-otp', 1500);
                } else {
                    showRegisterAlert(res.body.message || 'Đăng ký thất bại. Vui lòng thử lại.', 'error');
                    btnRegister.disabled = false;
                    btnRegister.innerHTML = '<span>Tạo tài khoản</span>';
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showRegisterAlert('Lỗi kết nối máy chủ. Vui lòng thử lại sau.', 'error');
                btnRegister.disabled = false;
                btnRegister.innerHTML = '<span>Tạo tài khoản</span>';
            });
    });
});

// =======================================================
// 7. LOGIC TRANG ĐẶT LẠI MẬT KHẨU (RESET PASSWORD)
// =======================================================
document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('resetPasswordForm');
    if (!form) return; // Rào chắn: Không phải trang đặt lại mật khẩu thì thoát

    const resetEmail = sessionStorage.getItem('resetEmail');
    const step1 = document.getElementById('step-1');
    const step2 = document.getElementById('step-2');
    const loginLink = document.getElementById('login-link-container');
    const btnNextStep = document.getElementById('btnNextStep');
    const btnBackToStep1 = document.getElementById('btnBackToStep1');
    const btnSubmitReset = document.getElementById('btnSubmitReset');
    const resendBtn = document.getElementById('resendOtp');
    const otpInput = document.getElementById('reset-otp');
    const passwordInput = document.getElementById('new-password');
    const confirmInput = document.getElementById('confirm-password');
    const alertBox = document.getElementById('reset-msg');
    const passwordPattern = /^(?=.*[A-Z])(?=.*[\W_]).{6,}$/;

    if (!resetEmail) {
        alertBox.textContent = 'Lỗi hệ thống: Không tìm thấy thông tin yêu cầu khôi phục.';
        alertBox.className = 'message error';
        alertBox.style.display = 'block';
        setTimeout(() => window.location.href = '/forgot-password', 3000);
        return;
    }

    function showAlert(message, type) {
        alertBox.textContent = message;
        alertBox.className = `message ${type}`;
        alertBox.style.display = 'block';
    }

    function showFieldError(fieldId, message) {
        const inputEl = document.getElementById(fieldId);
        const errorEl = document.getElementById(fieldId + '-error');
        if (inputEl) inputEl.classList.add('input-error');
        if (errorEl) errorEl.textContent = message;
    }

    function clearFieldError(fieldId) {
        const inputEl = document.getElementById(fieldId);
        const errorEl = document.getElementById(fieldId + '-error');
        if (inputEl) inputEl.classList.remove('input-error');
        if (errorEl) errorEl.textContent = '';
    }

    form.addEventListener('submit', function(e) { e.preventDefault(); });

    otpInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') { e.preventDefault(); btnNextStep.click(); }
    });

    otpInput.addEventListener('input', function() {
        let val = this.value.replace(/[^0-9]/g, '');
        if (val.length > 6) val = val.slice(0, 6);
        this.value = val;

        if (val === '') showFieldError('reset-otp', 'Mã OTP không được để trống.');
        else if (val.length < 6) showFieldError('reset-otp', 'Vui lòng nhập đủ 6 chữ số.');
        else clearFieldError('reset-otp');
    });

    passwordInput.addEventListener('input', function() {
        const val = this.value;
        if (val === '') showFieldError('new-password', 'Mật khẩu mới không được để trống.');
        else if (!passwordPattern.test(val)) showFieldError('new-password', 'Mật khẩu phải ít nhất 6 ký tự, gồm 1 chữ HOA và 1 ký tự đặc biệt.');
        else clearFieldError('new-password');

        if (confirmInput.value !== '') {
            if (val !== confirmInput.value) showFieldError('confirm-password', 'Mật khẩu xác nhận không trùng khớp.');
            else clearFieldError('confirm-password');
        }
    });

    confirmInput.addEventListener('input', function() {
        const val = this.value;
        if (val === '') showFieldError('confirm-password', 'Vui lòng xác nhận lại mật khẩu.');
        else if (val !== passwordInput.value) showFieldError('confirm-password', 'Mật khẩu xác nhận không trùng khớp.');
        else clearFieldError('confirm-password');
    });

    let countdownTimer = null;
    resendBtn.addEventListener('click', function(e) {
        e.preventDefault();
        if (this.classList.contains('disabled')) return;

        alertBox.style.display = 'none';
        clearFieldError('reset-otp');

        fetch('/api/auth/resend-otp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: resetEmail })
        })
            .then(response => response.json().then(data => ({ status: response.status, body: data })))
            .then(res => {
                if (res.status === 200 && res.body.success) {
                    showAlert('Một mã OTP mới đã được gửi.', 'success');
                    otpInput.value = '';
                    let timeLeft = 60;
                    resendBtn.classList.add('disabled');
                    resendBtn.textContent = `Gửi lại (${timeLeft}s)`;

                    countdownTimer = setInterval(() => {
                        timeLeft--;
                        resendBtn.textContent = `Gửi lại (${timeLeft}s)`;
                        if (timeLeft <= 0) {
                            clearInterval(countdownTimer);
                            resendBtn.classList.remove('disabled');
                            resendBtn.textContent = 'Gửi lại';
                        }
                    }, 1000);
                } else {
                    showAlert(res.body.message || 'Không thể gửi lại mã lúc này.', 'error');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showAlert('Lỗi kết nối máy chủ khi xin mã mới.', 'error');
            });
    });

    btnNextStep.addEventListener('click', function() {
        const otpValue = otpInput.value.trim();
        if (otpValue === '' || otpValue.length < 6) {
            showFieldError('reset-otp', 'Vui lòng nhập đủ 6 chữ số trước khi tiếp tục.');
            return;
        }

        btnNextStep.disabled = true;
        btnNextStep.innerHTML = '<i class="fas fa-spinner fa-spin"></i> <span>ĐANG KIỂM TRA...</span>';
        alertBox.style.display = 'none';

        fetch('/api/auth/check-reset-otp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: resetEmail, otp: otpValue })
        })
            .then(response => response.json().then(data => ({ status: response.status, body: data })))
            .then(res => {
                btnNextStep.disabled = false;
                btnNextStep.innerHTML = '<span>TIẾP TỤC</span>';

                if (res.status === 200 && res.body.success) {
                    step1.style.display = 'none';
                    loginLink.style.display = 'none';
                    step2.style.display = 'block';
                } else {
                    showFieldError('reset-otp', res.body.message || 'Mã xác thực không hợp lệ hoặc đã hết hạn.');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showFieldError('reset-otp', 'Lỗi kết nối máy chủ. Vui lòng thử lại.');
                btnNextStep.disabled = false;
                btnNextStep.innerHTML = '<span>TIẾP TỤC</span>';
            });
    });

    btnBackToStep1.addEventListener('click', function() {
        step2.style.display = 'none';
        step1.style.display = 'block';
        loginLink.style.display = 'block';
        alertBox.style.display = 'none';
    });

    btnSubmitReset.addEventListener('click', function() {
        alertBox.style.display = 'none';
        const otpValue = otpInput.value.trim();
        const passwordValue = passwordInput.value;
        const confirmValue = confirmInput.value;
        let isValid = true;

        if (passwordValue === '' || !passwordPattern.test(passwordValue)) {
            showFieldError('new-password', 'Mật khẩu phải ít nhất 6 ký tự, gồm 1 chữ HOA và 1 ký tự đặc biệt.');
            isValid = false;
        }
        if (confirmValue === '' || passwordValue !== confirmValue) {
            showFieldError('confirm-password', 'Mật khẩu xác nhận không trùng khớp.');
            isValid = false;
        }
        if (!isValid) return;

        btnSubmitReset.disabled = true;
        btnSubmitReset.innerHTML = '<i class="fas fa-spinner fa-spin"></i> <span>ĐANG LƯU...</span>';

        fetch('/api/auth/reset-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: resetEmail, otp: otpValue, newPassword: passwordValue })
        })
            .then(response => response.json().then(data => ({ status: response.status, body: data })))
            .then(res => {
                if (res.status === 200 && res.body.success) {
                    showAlert('Đặt lại mật khẩu thành công!', 'success');
                    sessionStorage.removeItem('resetEmail');
                    setTimeout(() => window.location.href = '/login', 2500);
                } else {
                    if (res.body.message && (res.body.message.toLowerCase().includes('otp') || res.body.message.toLowerCase().includes('xác thực') || res.body.message.toLowerCase().includes('sử dụng'))) {
                        step2.style.display = 'none';
                        step1.style.display = 'block';
                        loginLink.style.display = 'block';
                        showFieldError('reset-otp', res.body.message || 'Mã OTP không hợp lệ.');
                    } else {
                        showAlert(res.body.message || 'Có lỗi xảy ra, vui lòng thử lại.', 'error');
                    }
                    btnSubmitReset.disabled = false;
                    btnSubmitReset.innerHTML = '<span>LƯU MẬT KHẨU</span>';
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showAlert('Lỗi kết nối máy chủ. Vui lòng thử lại.', 'error');
                btnSubmitReset.disabled = false;
                btnSubmitReset.innerHTML = '<span>LƯU MẬT KHẨU</span>';
            });
    });
});

// =======================================================
// 8. LOGIC TRANG XÁC THỰC OTP TÀI KHOẢN MỚI (VERIFY OTP)
// =======================================================
document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('verifyOtpForm');
    if (!form) return; // Rào chắn: Không phải trang xác thực tài khoản OTP thì thoát hoàn toàn

    const registeredEmail = sessionStorage.getItem('registeredEmail');
    const alertBox = document.getElementById('alert-message');
    const displayEmailSpan = document.getElementById('displayEmail');
    const otpInput = document.getElementById('otp');
    const btnVerify = document.getElementById('btnVerify');
    const resendBtn = document.getElementById('resendOtp');

    function showFieldError(fieldId, message) {
        const inputEl = document.getElementById(fieldId);
        const errorEl = document.getElementById(fieldId + '-error');
        if (inputEl) inputEl.classList.add('input-error');
        if (errorEl) errorEl.textContent = message;
    }

    function clearFieldError(fieldId) {
        const inputEl = document.getElementById(fieldId);
        const errorEl = document.getElementById(fieldId + '-error');
        if (inputEl) inputEl.classList.remove('input-error');
        if (errorEl) errorEl.textContent = '';
    }

    function showAlert(message, type) {
        alertBox.textContent = message;
        alertBox.className = `message ${type}`;
        alertBox.style.display = 'block';
    }

    if (!registeredEmail) {
        showAlert('Lỗi hệ thống: Không tìm thấy địa chỉ email. Vui lòng đăng ký lại.', 'error');
        btnVerify.disabled = true;
        setTimeout(() => window.location.href = '/register', 3000);
        return;
    }

    displayEmailSpan.textContent = registeredEmail;

    otpInput.addEventListener('input', function() {
        alertBox.style.display = 'none';
        let val = this.value.replace(/[^0-9]/g, '');
        if (val.length > 6) val = val.slice(0, 6);
        this.value = val;

        if (val === '') {
            showFieldError('otp', 'Mã OTP không được để trống.');
        } else if (val.length < 6) {
            showFieldError('otp', 'Vui lòng nhập đủ 6 chữ số.');
        } else {
            clearFieldError('otp');
        }
    });

    form.addEventListener('submit', function(e) {
        e.preventDefault();
        alertBox.style.display = 'none';
        const otp = otpInput.value.trim();

        if (otp === '' || otp.length < 6) {
            showFieldError('otp', 'Vui lòng nhập đủ 6 chữ số.');
            return;
        }

        btnVerify.disabled = true;
        btnVerify.innerHTML = '<i class="fas fa-spinner fa-spin"></i> <span>Đang xác thực...</span>';

        fetch('/api/auth/verify-otp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: registeredEmail, otp: otp })
        })
            .then(response => response.json().then(data => ({ status: response.status, body: data })))
            .then(res => {
                if (res.status === 200 && res.body.success) {
                    showAlert('Xác thực tài khoản thành công!', 'success');
                    sessionStorage.removeItem('registeredEmail');
                    setTimeout(() => window.location.href = '/login', 2000);
                } else {
                    showFieldError('otp', res.body.message || 'Mã OTP không hợp lệ hoặc đã hết hạn.');
                    btnVerify.disabled = false;
                    btnVerify.innerHTML = '<span>Xác thực</span>';
                    otpInput.value = '';
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showFieldError('otp', 'Lỗi kết nối máy chủ. Vui lòng thử lại.');
                btnVerify.disabled = false;
                btnVerify.innerHTML = '<span>Xác thực</span>';
            });
    });

    let countdownTimer = null;
    resendBtn.addEventListener('click', function(e) {
        e.preventDefault();
        if (this.classList.contains('disabled')) return;

        alertBox.style.display = 'none';
        clearFieldError('otp');

        fetch('/api/auth/resend-otp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: registeredEmail })
        })
            .then(response => response.json().then(data => ({ status: response.status, body: data })))
            .then(res => {
                if (res.status === 200 && res.body.success) {
                    showAlert('Một mã OTP mới đã được gửi.', 'success');
                    otpInput.value = '';
                    let timeLeft = 60;
                    resendBtn.classList.add('disabled');
                    resendBtn.textContent = `Gửi lại (${timeLeft}s)`;

                    countdownTimer = setInterval(() => {
                        timeLeft--;
                        resendBtn.textContent = `Gửi lại (${timeLeft}s)`;
                        if (timeLeft <= 0) {
                            clearInterval(countdownTimer);
                            resendBtn.classList.remove('disabled');
                            resendBtn.textContent = 'Gửi lại';
                        }
                    }, 1000);
                } else {
                    showAlert(res.body.message || 'Không thể gửi lại mã lúc này.', 'error');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showAlert('Lỗi kết nối máy chủ khi xin mã mới.', 'error');
            });
    });
});
