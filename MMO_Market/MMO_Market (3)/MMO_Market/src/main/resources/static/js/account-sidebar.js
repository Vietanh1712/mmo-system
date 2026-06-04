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
