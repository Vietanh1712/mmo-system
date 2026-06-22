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

        // Ẩn/Hiện mục "Ticket của tôi" chỉ dành cho Customer và Seller
        let isCustomerOrSeller = false;
        if (profile.role) {
            try {
                const roleObj = JSON.parse(profile.role);
                const roleName = (roleObj.role || '').toLowerCase();
                isCustomerOrSeller = roleName.includes('customer') || roleName.includes('seller');
            } catch (e) {
                const roleLower = String(profile.role).toLowerCase();
                isCustomerOrSeller = roleLower.includes('customer') || roleLower.includes('seller');
            }
        }

        const ticketsMenuLink = this.root.querySelector('a[href*="/account/tickets"]');
        if (ticketsMenuLink) {
            ticketsMenuLink.style.display = isCustomerOrSeller ? '' : 'none';
        }
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
