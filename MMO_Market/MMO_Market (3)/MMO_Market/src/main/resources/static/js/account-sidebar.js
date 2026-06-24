class AccountSidebar {
    constructor(root = document.querySelector('.account-sidebar')) {
        this.root = root;
        this.renderCachedProfile();
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
        this.root.classList.add('account-sidebar--hydrated');
        this.cacheProfile(profile);
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

    renderCachedProfile() {
        const cachedProfile = this.readCachedProfile();
        if (cachedProfile) {
            this.render(cachedProfile);
            return;
        }

        if (this.root) {
            this.root.classList.add('account-sidebar--hydrated');
        }
    }

    readCachedProfile() {
        for (const key of ['userInfo', 'user']) {
            try {
                const value = sessionStorage.getItem(key) || localStorage.getItem(key);
                if (!value || value === 'null' || value === 'undefined') {
                    continue;
                }

                const profile = JSON.parse(value);
                if (profile && typeof profile === 'object') {
                    return profile;
                }
            } catch {
                // Bỏ qua cache lỗi và chờ API profile đồng bộ lại.
            }
        }

        return null;
    }

    cacheProfile(profile) {
        const cachedProfile = {
            id: profile.id,
            email: profile.email,
            fullName: profile.fullName,
            phone: profile.phone,
            role: profile.role,
            shopStatus: profile.shopStatus,
            balanceVnd: profile.balanceVnd
        };
        const serializedProfile = JSON.stringify(cachedProfile);

        sessionStorage.setItem('userInfo', serializedProfile);
        sessionStorage.setItem('user', serializedProfile);
        localStorage.setItem('userInfo', serializedProfile);
        localStorage.setItem('user', serializedProfile);
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

document.querySelectorAll('.account-sidebar').forEach(root => {
    new AccountSidebar(root);
});

(function initializeAccountNavigation() {
    const accountPaths = new Set([
        '/profile',
        '/account/kyc',
        '/account/security',
        '/account/register-shop',
        '/wallet',
        '/wallet/topup',
        '/wallet/transactions',
        '/account/orders',
        '/account/notifications'
    ]);
    let navigationController = null;

    window.AccountNavigation = {
        canHandle(target) {
            const url = new URL(target, window.location.href);
            return Boolean(document.querySelector('.profile-layout > section')) &&
                url.origin === window.location.origin &&
                accountPaths.has(url.pathname);
        },
        navigate(target, pushHistory = true) {
            const url = new URL(target, window.location.href);
            if (!this.canHandle(url.href)) {
                return false;
            }
            if (url.pathname === window.location.pathname && url.search === window.location.search) {
                document.getElementById('userDropdown')?.classList.remove('active');
                return true;
            }
            navigateAccountPage(url, pushHistory);
            return true;
        }
    };

    document.addEventListener('click', event => {
        const link = event.target.closest('.profile-layout a[href]');
        if (!link || !shouldHandleLink(event, link)) {
            return;
        }

        event.preventDefault();
        window.AccountNavigation.navigate(link.href);
    });

    window.addEventListener('popstate', () => {
        if (accountPaths.has(window.location.pathname)) {
            navigateAccountPage(new URL(window.location.href), false);
        }
    });

    function shouldHandleLink(event, link) {
        if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
            return false;
        }

        const url = new URL(link.href, window.location.href);
        return url.origin === window.location.origin &&
            accountPaths.has(url.pathname);
    }

    async function navigateAccountPage(url, pushHistory) {
        const currentContent = document.querySelector('.profile-layout > section');
        if (!currentContent) {
            window.location.assign(url.href);
            return;
        }

        navigationController?.abort();
        navigationController = new AbortController();
        currentContent.classList.add('account-content-loading');
        currentContent.setAttribute('aria-busy', 'true');
        let contentReplaced = false;

        try {
            const response = await fetch(url.pathname + url.search, {
                headers: { 'X-Requested-With': 'AccountPartialNavigation' },
                signal: navigationController.signal
            });
            if (!response.ok || response.redirected) {
                throw new Error('Không thể tải nội dung tài khoản.');
            }

            const html = await response.text();
            const nextDocument = new DOMParser().parseFromString(html, 'text/html');
            const nextContent = nextDocument.querySelector('.profile-layout > section');
            if (!nextContent) {
                throw new Error('Trang đích không có vùng nội dung tài khoản.');
            }

            await ensureTargetStyles(nextDocument);
            syncAccountLayoutShell(nextDocument);
            currentContent.replaceWith(nextContent);
            contentReplaced = true;
            document.title = nextDocument.title;
            updateActiveMenu(url.pathname);
            document.getElementById('userDropdown')?.classList.remove('active');

            if (pushHistory) {
                window.history.pushState({ accountPartial: true }, '', url.pathname + url.search);
            }

            await ensureUtilityScripts(nextDocument);
            await initializeTargetPage(nextDocument);
            window.initDsDatePickers?.();
            window.initDsDropdowns?.();
            window.scrollTo({ top: 0, behavior: 'instant' });
        } catch (error) {
            if (error.name === 'AbortError') {
                return;
            }
            console.error('Account partial navigation failed:', error);
            if (!contentReplaced) {
                window.location.assign(url.href);
            }
        } finally {
            document.querySelector('.profile-layout > section')?.classList.remove('account-content-loading');
            document.querySelector('.profile-layout > section')?.removeAttribute('aria-busy');
        }
    }

    function updateActiveMenu(pathname) {
        document.querySelectorAll('.account-sidebar__menu-link').forEach(link => {
            const linkPath = new URL(link.href, window.location.href).pathname;
            link.classList.toggle('account-sidebar__menu-link--active', linkPath === pathname);
        });
    }

    function syncAccountLayoutShell(nextDocument) {
        const currentMain = document.querySelector('main.profile-page');
        const nextMain = nextDocument.querySelector('main.profile-page');
        if (currentMain && nextMain) {
            currentMain.className = nextMain.className;
        }

        const currentLayout = document.querySelector('.profile-layout');
        const nextLayout = nextDocument.querySelector('.profile-layout');
        if (currentLayout && nextLayout) {
            currentLayout.className = nextLayout.className;
        }

        const currentContainer = currentLayout?.closest('.ds-container');
        const nextContainer = nextLayout?.closest('.ds-container');
        if (currentContainer && nextContainer) {
            currentContainer.className = nextContainer.className;
        }
    }

    async function ensureTargetStyles(nextDocument) {
        const existingStyles = new Set(
            Array.from(document.querySelectorAll('link[rel="stylesheet"]')).map(link => new URL(link.href).pathname)
        );
        const stylePaths = Array.from(nextDocument.querySelectorAll('link[rel="stylesheet"]'))
            .map(link => new URL(link.getAttribute('href'), window.location.origin).pathname)
            .filter(path => path.startsWith('/css/') && !existingStyles.has(path));

        await Promise.all(stylePaths.map(loadStylesheet));
    }

    function loadStylesheet(path) {
        return new Promise((resolve, reject) => {
            const link = document.createElement('link');
            link.rel = 'stylesheet';
            link.href = path;
            link.onload = resolve;
            link.onerror = reject;
            document.head.appendChild(link);
        });
    }

    async function ensureUtilityScripts(nextDocument) {
        const utilityPaths = Array.from(nextDocument.querySelectorAll('script[src]'))
            .map(script => new URL(script.getAttribute('src'), window.location.origin).pathname)
            .filter(path => path === '/js/datepicker.js');

        for (const path of utilityPaths) {
            if (!document.querySelector(`script[src="${path}"]`)) {
                await loadScript(path, false);
            }
        }
    }

    async function initializeTargetPage(nextDocument) {
        const pageScriptPath = Array.from(nextDocument.querySelectorAll('script[src]'))
            .map(script => new URL(script.getAttribute('src'), window.location.origin).pathname)
            .find(path => path === '/js/profile.js' || /^\/js\/account-(?!sidebar)[a-z-]+\.js$/.test(path));

        if (!pageScriptPath) {
            return;
        }

        if (!window.AccountPageInitializers?.[pageScriptPath]) {
            await loadScript(pageScriptPath, true);
        }

        const initializer = window.AccountPageInitializers?.[pageScriptPath];
        if (typeof initializer !== 'function') {
            throw new Error(`Không tìm thấy initializer cho ${pageScriptPath}.`);
        }
        initializer();
    }

    function loadScript(path, partial) {
        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = path;
            if (partial) {
                script.dataset.accountPartial = 'true';
            }
            script.onload = resolve;
            script.onerror = reject;
            document.body.appendChild(script);
        });
    }
})();
