<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MMO Market System - Đăng Nhập</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/authen.css">
</head>
<body class="center-layout">
<div class="auth-container">
    <!-- Logo -->
    <div class="auth-header">
        <div class="logo">💎 MMO Market</div>
        <div class="tagline">Sàn giao dịch tài sản số hàng đầu</div>
    </div>

    <!-- Message Container -->
    <div id="messageContainer"></div>

    <!-- Login Form -->
    <div id="loginForm">
        <h2 style="text-align: center; margin-bottom: 30px; color: #333;">Đăng Nhập</h2>
        <form id="loginFormElement" onsubmit="handleLogin(event)">
            <div class="form-group">
                <label for="loginEmail">Email</label>
                <input
                        type="email"
                        id="loginEmail"
                        placeholder="email@example.com"
                        required
                >
            </div>

            <div class="form-group">
                <label for="loginPassword">Mật khẩu</label>
                <input
                        type="password"
                        id="loginPassword"
                        placeholder="Nhập mật khẩu"
                        required
                >
            </div>

            <button type="submit" class="btn btn-primary" id="loginBtn">
                Đăng Nhập
            </button>
        </form>

        <div class="toggle-link">
            Chưa có tài khoản? <a onclick="toggleForms()">Đăng ký ngay</a>
        </div>
    </div>

    <!-- Register Form (Hidden) -->
    <div id="registerForm" style="display: none;">
        <h2 style="text-align: center; margin-bottom: 30px; color: #333;">Đăng Ký</h2>
        <form id="registerFormElement" onsubmit="handleRegister(event)">
            <div class="form-group">
                <label for="registerEmail">Email</label>
                <input
                        type="email"
                        id="registerEmail"
                        placeholder="email@example.com"
                        required
                >
            </div>

            <div class="form-group">
                <label for="registerFullName">Họ tên</label>
                <input
                        type="text"
                        id="registerFullName"
                        placeholder="Nhập họ tên"
                        required
                >
            </div>

            <div class="form-group">
                <label for="registerPhone">Số điện thoại</label>
                <input
                        type="tel"
                        id="registerPhone"
                        placeholder="0912345678"
                >
            </div>

            <div class="form-group">
                <label for="registerPassword">Mật khẩu</label>
                <input
                        type="password"
                        id="registerPassword"
                        placeholder="Tối thiểu 6 ký tự"
                        minlength="6"
                        required
                >
            </div>

            <div class="form-group">
                <label for="registerPasswordConfirm">Xác nhận mật khẩu</label>
                <input
                        type="password"
                        id="registerPasswordConfirm"
                        placeholder="Nhập lại mật khẩu"
                        required
                >
            </div>

            <button type="submit" class="btn btn-primary" id="registerBtn">
                Đăng Ký
            </button>
        </form>

        <div class="toggle-link">
            Đã có tài khoản? <a onclick="toggleForms()">Đăng nhập</a>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/auth.js"></script>
<script>
    // Toggle between login and register forms
    function toggleForms() {
        const loginForm = document.getElementById('loginForm');
        const registerForm = document.getElementById('registerForm');
        const messageContainer = document.getElementById('messageContainer');

        loginForm.style.display = loginForm.style.display === 'none' ? 'block' : 'none';
        registerForm.style.display = registerForm.style.display === 'none' ? 'block' : 'none';

        messageContainer.innerHTML = '';
    }

    // Handle login
    async function handleLogin(event) {
        event.preventDefault();

        const email = document.getElementById('loginEmail').value;
        const password = document.getElementById('loginPassword').value;
        const loginBtn = document.getElementById('loginBtn');
        const messageContainer = document.getElementById('messageContainer');

        if (!email || !password) {
            UIHelper.showMessage('messageContainer', 'Vui lòng nhập đầy đủ thông tin', 'error');
            return;
        }

        UIHelper.showLoading(loginBtn);

        try {
            const response = await AuthAPI.login(email, password);
            UIHelper.showMessage('messageContainer', '✓ Đăng nhập thành công!', 'success');

            setTimeout(() => {
                window.location.href = '${pageContext.request.contextPath}/dashboard';
            }, 1500);
        } catch (error) {
            UIHelper.showMessage('messageContainer', error.message, 'error');
        } finally {
            UIHelper.hideLoading(loginBtn);
        }
    }

    // Handle register
    async function handleRegister(event) {
        event.preventDefault();

        const email = document.getElementById('registerEmail').value;
        const fullName = document.getElementById('registerFullName').value;
        const phone = document.getElementById('registerPhone').value;
        const password = document.getElementById('registerPassword').value;
        const confirmPassword = document.getElementById('registerPasswordConfirm').value;
        const registerBtn = document.getElementById('registerBtn');
        const messageContainer = document.getElementById('messageContainer');

        // Validation
        if (!email || !fullName || !password || !confirmPassword) {
            UIHelper.showMessage('messageContainer', 'Vui lòng nhập đầy đủ thông tin', 'error');
            return;
        }

        if (password.length < 6) {
            UIHelper.showMessage('messageContainer', 'Mật khẩu phải tối thiểu 6 ký tự', 'error');
            return;
        }

        if (password !== confirmPassword) {
            UIHelper.showMessage('messageContainer', 'Mật khẩu không trùng khớp', 'error');
            return;
        }

        UIHelper.showLoading(registerBtn);

        try {
            const response = await AuthAPI.register(email, password, fullName, phone);
            UIHelper.showMessage('messageContainer', '✓ Đăng ký thành công! Vui lòng đăng nhập.', 'success');

            setTimeout(() => {
                UIHelper.clearForm('registerFormElement');
                toggleForms();
            }, 1500);
        } catch (error) {
            UIHelper.showMessage('messageContainer', error.message, 'error');
        } finally {
            UIHelper.hideLoading(registerBtn);
        }
    }

    // Check if already logged in
    window.addEventListener('DOMContentLoaded', function() {
        if (TokenManager.isLoggedIn()) {
            window.location.href = '${pageContext.request.contextPath}/dashboard';
        }
    });
</script>
</body>
</html>

