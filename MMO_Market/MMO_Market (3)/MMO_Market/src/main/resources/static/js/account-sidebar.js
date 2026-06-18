class AccountSidebar {
    constructor(root = document.querySelector('.account-sidebar')) {
        this.root = root;
    }

    render(profile) {
        if (!this.root || !profile) {
            return;
        }

        const fullName = profile.fullName || 'Người dùng';
        this.setText('avatar', fullName.charAt(0).toUpperCase());
        this.setText('name', fullName);
        this.setText('email', profile.email || '-');
        this.setText('balance', this.formatBalance(profile.balanceVnd));
        this.renderRoleActions(profile.role);
    }

    renderRoleActions(roleValue) {
        const role = this.normalizeRole(roleValue);
        this.root.querySelectorAll('[data-account-role="customer-only"]').forEach(element => {
            element.hidden = role !== 'Customer';
        });
        this.root.querySelectorAll('[data-account-role="seller-only"]').forEach(element => {
            element.hidden = role !== 'Seller';
        });
    }

    normalizeRole(roleValue) {
        if (typeof window.normalizeRole === 'function') {
            return window.normalizeRole(roleValue);
        }

        const normalized = String(roleValue || '').replaceAll('"', '').trim().toLowerCase();
        if (normalized.includes('admin')) return 'Admin';
        if (normalized.includes('staff')) return 'Staff';
        if (normalized.includes('seller')) return 'Seller';
        return 'Customer';
    }

    setText(field, value) {
        const element = this.root.querySelector(`[data-account-sidebar="${field}"]`);
        if (element) {
            element.textContent = value;
        }
    }

    formatBalance(balanceVnd) {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })
            .format(balanceVnd || 0);
    }
}
