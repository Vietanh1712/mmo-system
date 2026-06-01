<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MMO Market System - Bảng điều khiển</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/authen.css">
    <style>
        body {
            background: #f5f7fa;
        }

        .dashboard-content {
            margin-top: 30px;
            margin-bottom: 30px;
        }

        .welcome-section {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px;
            border-radius: 10px;
            margin-bottom: 30px;
        }

        .welcome-text h1 {
            font-size: 28px;
            margin-bottom: 10px;
        }

        .welcome-text p {
            font-size: 16px;
            opacity: 0.9;
        }

        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }

        .stat-card {
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
            text-align: center;
        }

        .stat-number {
            font-size: 32px;
            font-weight: bold;
            color: #667eea;
        }

        .stat-label {
            color: #999;
            font-size: 14px;
            margin-top: 10px;
        }

        .action-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }

        .action-card {
            background: white;
            padding: 30px 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
            text-align: center;
            transition: transform 0.3s ease;
        }

        .action-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
        }

        .action-icon {
            font-size: 40px;
            margin-bottom: 15px;
        }

        .action-title {
            font-weight: 600;
            color: #333;
            margin-bottom: 10px;
        }

        .action-description {
            color: #999;
            font-size: 14px;
            margin-bottom: 15px;
        }

        .table-container {
            background: white;
            border-radius: 10px;
            overflow: hidden;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
        }

        table {
            width: 100%;
            border-collapse: collapse;
        }

        table thead {
            background: #f0f0f0;
        }

        table th {
            padding: 15px;
            text-align: left;
            font-weight: 600;
            color: #333;
            border-bottom: 2px solid #e0e0e0;
        }

        table td {
            padding: 15px;
            border-bottom: 1px solid #f0f0f0;
        }

        table tbody tr:hover {
            background: #f9f9f9;
        }

        .badge {
            display: inline-block;
            padding: 5px 10px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
        }

        .badge-success {
            background: #d4edda;
            color: #155724;
        }

        .badge-pending {
            background: #fff3cd;
            color: #856404;
        }

        .badge-error {
            background: #f8d7da;
            color: #721c24;
        }

        .empty-state {
            padding: 40px;
            text-align: center;
            color: #999;
        }

        .empty-state-icon {
            font-size: 48px;
            margin-bottom: 15px;
        }

        @media (max-width: 768px) {
            .welcome-section {
                padding: 20px;
            }

            .welcome-text h1 {
                font-size: 22px;
            }

            .stats-grid {
                grid-template-columns: repeat(2, 1fr);
            }

            table {
                font-size: 14px;
            }

            table th,
            table td {
                padding: 10px;
            }
        }
    </style>
</head>
<body>
<!-- Header -->
<div class="dashboard-header">
    <div class="dashboard-header-content">
        <div class="dashboard-title">📊 MMO Market</div>
        <div class="user-info">
            <div class="user-details">
                <div class="user-name" id="userName">Đang tải...</div>
                <div class="user-email" id="userEmail">-</div>
                <div class="user-role" id="userRole">Customer</div>
            </div>
            <button class="btn btn-danger btn-small" onclick="handleLogout()">
                Đăng xuất
            </button>
        </div>
    </div>
</div>

<!-- Main Content -->
<div class="dashboard-content">
    <!-- Welcome Section -->
    <div class="welcome-section">
        <div class="welcome-text">
            <h1 id="welcomeText">Chào mừng trở lại! 👋</h1>
            <p>Quản lý tài sản số của bạn một cách an toàn và hiệu quả</p>
        </div>
    </div>

    <!-- Statistics -->
    <div class="stats-grid">
        <div class="stat-card">
            <div class="stat-number">₫ 0</div>
            <div class="stat-label">Số dư ví</div>
        </div>
        <div class="stat-card">
            <div class="stat-number">0</div>
            <div class="stat-label">Sản phẩm sở hữu</div>
        </div>
        <div class="stat-card">
            <div class="stat-number">0</div>
            <div class="stat-label">Giao dịch thành công</div>
        </div>
        <div class="stat-card">
            <div class="stat-number">0</div>
            <div class="stat-label">Điểm đánh giá</div>
        </div>
    </div>

    <!-- Quick Actions -->
    <div>
        <h2 style="margin-bottom: 20px; color: #333;">Hành động nhanh</h2>
        <div class="action-grid">
            <div class="action-card">
                <div class="action-icon">💰</div>
                <div class="action-title">Nạp tiền</div>
                <div class="action-description">Nạp tiền vào ví của bạn</div>
                <button class="btn btn-primary" onclick="alert('Chức năng đang phát triển')">
                    Nạp tiền
                </button>
            </div>

            <div class="action-card">
                <div class="action-icon">🛒</div>
                <div class="action-title">Mua hàng</div>
                <div class="action-description">Tìm kiếm và mua tài sản số</div>
                <button class="btn btn-primary" onclick="alert('Chức năng đang phát triển')">
                    Khám phá
                </button>
            </div>

            <div class="action-card">
                <div class="action-icon">📋</div>
                <div class="action-title">Lịch sử giao dịch</div>
                <div class="action-description">Xem chi tiết các giao dịch</div>
                <button class="btn btn-primary" onclick="alert('Chức năng đang phát triển')">
                    Xem lịch sử
                </button>
            </div>

            <div class="action-card">
                <div class="action-icon">⚙️</div>
                <div class="action-title">Cài đặt tài khoản</div>
                <div class="action-description">Cập nhật thông tin cá nhân</div>
                <button class="btn btn-primary" onclick="alert('Chức năng đang phát triển')">
                    Cài đặt
                </button>
            </div>
        </div>
    </div>

    <!-- Recent Transactions -->
    <div class="card">
        <div class="card-title">Giao dịch gần đây</div>
        <div class="table-container">
            <table>
                <thead>
                <tr>
                    <th>Ngày giờ</th>
                    <th>Loại</th>
                    <th>Số tiền</th>
                    <th>Trạng thái</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td colspan="4" style="text-align: center; color: #999; padding: 40px;">
                        Bạn chưa có giao dịch nào
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>

    <!-- User Info -->
    <div class="card">
        <div class="card-title">Thông tin tài khoản</div>
        <div>
            <div style="margin-bottom: 15px;">
                <strong>Email:</strong>
                <span id="infoEmail" style="margin-left: 10px;">-</span>
            </div>
            <div style="margin-bottom: 15px;">
                <strong>Họ tên:</strong>
                <span id="infoFullName" style="margin-left: 10px;">-</span>
            </div>
            <div style="margin-bottom: 15px;">
                <strong>Số điện thoại:</strong>
                <span id="infoPhone" style="margin-left: 10px;">-</span>
            </div>
            <div style="margin-bottom: 15px;">
                <strong>Vai trò:</strong>
                <span id="infoRole" style="margin-left: 10px;">Customer</span>
            </div>
            <div class="button-group" style="margin-top: 20px;">
                <button class="btn btn-secondary" onclick="alert('Chức năng đang phát triển')">
                    Sửa thông tin
                </button>
                <button class="btn btn-secondary" onclick="alert('Chức năng đang phát triển')">
                    Đổi mật khẩu
                </button>
            </div>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/auth.js"></script>
<script>
    // Load user information
    function loadUserInfo() {
        const user = TokenManager.getUser();

        if (!user) {
            window.location.href = '${pageContext.request.contextPath}/login';
            return;
        }

        // Update header
        document.getElementById('userName').textContent = user.fullName || 'Người dùng';
        document.getElementById('userEmail').textContent = user.email || '';

        // Update welcome text
        const firstName = (user.fullName || 'bạn').split(' ').pop();
        document.getElementById('welcomeText').textContent = `Chào mừng ${firstName}! 👋`;

        // Update role
        const roleObj = typeof user.role === 'string' ? JSON.parse(user.role) : user.role;
        const roleName = roleObj?.role || 'Customer';
        document.getElementById('userRole').textContent = roleName;

        // Update user info section
        document.getElementById('infoEmail').textContent = user.email || '-';
        document.getElementById('infoFullName').textContent = user.fullName || '-';
        document.getElementById('infoPhone').textContent = user.phone || '-';
        document.getElementById('infoRole').textContent = roleName;
    }

    // Handle logout
    async function handleLogout() {
        if (!confirm('Bạn chắc chắn muốn đăng xuất?')) {
            return;
        }

        try {
            await AuthAPI.logout();
            UIHelper.showMessage('messageContainer', '✓ Đăng xuất thành công!', 'success');
            setTimeout(() => {
                window.location.href = '${pageContext.request.contextPath}/login';
            }, 1000);
        } catch (error) {
            alert('Lỗi khi đăng xuất: ' + error.message);
            // Force logout even if API fails
            TokenManager.removeTokens();
            window.location.href = '${pageContext.request.contextPath}/login';
        }
    }

    // Initialize on page load
    window.addEventListener('DOMContentLoaded', function() {
        loadUserInfo();
    });

    // Prevent browser back button after logout
    window.addEventListener('pageshow', function(event) {
        if (event.persisted) {
            if (!TokenManager.isLoggedIn()) {
                window.location.href = '${pageContext.request.contextPath}/login';
            }
        }
    });
</script>
</body>
</html>

