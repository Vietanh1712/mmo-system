/**
 * MMO Market - Global Toast Notification System
 * Thay thế toàn bộ alert() bằng toast notification có style đẹp.
 * Các hàm có thể gọi từ bất kỳ trang nào:
 *   showSuccessToast(message)
 *   showErrorToast(message)
 *   showWarningToast(message)
 *   showInfoToast(message)
 *   showToast(type, title, message, durationMs)
 */

(function () {
    'use strict';

    const TOAST_CONFIGS = {
        success: {
            title: 'Thành công',
            icon: 'fa-check-circle',
            cssClass: 'ds-toast-success'
        },
        error: {
            title: 'Lỗi',
            icon: 'fa-times-circle',
            cssClass: 'ds-toast-error'
        },
        warning: {
            title: 'Cảnh báo',
            icon: 'fa-exclamation-triangle',
            cssClass: 'ds-toast-warning'
        },
        info: {
            title: 'Thông tin',
            icon: 'fa-info-circle',
            cssClass: 'ds-toast-info'
        }
    };

    /**
     * Hiển thị toast notification
     * @param {string} type - 'success' | 'error' | 'warning' | 'info'
     * @param {string} title - Tiêu đề toast (nếu null sẽ dùng mặc định)
     * @param {string} message - Nội dung thông báo
     * @param {number} durationMs - Thời gian hiển thị (mặc định 3500ms)
     */
    function showToast(type, title, message, durationMs) {
        const config = TOAST_CONFIGS[type] || TOAST_CONFIGS.info;
        const finalTitle = title || config.title;
        const finalDuration = durationMs || (type === 'error' ? 5000 : 3500);

        // Lấy hoặc tạo container
        let container = document.querySelector('.ds-toast-container');
        if (!container) {
            container = document.createElement('div');
            container.className = 'ds-toast-container';
            document.body.appendChild(container);
        }

        // Tạo toast element
        const toast = document.createElement('div');
        toast.className = `ds-toast ${config.cssClass}`;
        toast.innerHTML = `
            <div style="display:flex;align-items:flex-start;gap:10px;flex:1;min-width:0;">
                <i class="fa ${config.icon}" style="font-size:18px;margin-top:2px;flex-shrink:0;"></i>
                <div style="flex:1;min-width:0;">
                    <h4 class="ds-toast-title">${finalTitle}</h4>
                    <p class="ds-toast-message">${message}</p>
                </div>
            </div>
            <button class="ds-toast-close" onclick="this.closest('.ds-toast').remove()" title="Đóng">&times;</button>
        `;

        container.appendChild(toast);

        // Tự đóng sau finalDuration ms
        const timer = setTimeout(() => {
            if (toast.parentElement) {
                toast.style.opacity = '0';
                toast.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
                toast.style.transform = 'translateX(20px)';
                setTimeout(() => toast.remove(), 400);
            }
        }, finalDuration);

        // Cho phép đóng tức thì khi click nút X, hủy timer
        toast.querySelector('.ds-toast-close').addEventListener('click', () => {
            clearTimeout(timer);
        });
    }

    /**
     * Toast thành công (xanh lá)
     */
    function showSuccessToast(message) {
        showToast('success', null, message);
    }

    /**
     * Toast lỗi (đỏ)
     */
    function showErrorToast(message) {
        showToast('error', null, message);
    }

    /**
     * Toast cảnh báo (vàng cam)
     */
    function showWarningToast(message) {
        showToast('warning', null, message);
    }

    /**
     * Toast thông tin (xanh dương)
     */
    function showInfoToast(message) {
        showToast('info', null, message);
    }

    // Expose ra global scope
    window.showToast = showToast;
    window.showSuccessToast = showSuccessToast;
    window.showErrorToast = showErrorToast;
    window.showWarningToast = showWarningToast;
    window.showInfoToast = showInfoToast;

})();
