(function () {
    'use strict';

    let categoryId = null;
    let currentCategoryData = null;
    let parentCategoriesList = [];

    document.addEventListener('DOMContentLoaded', function () {
        const urlParams = new URLSearchParams(window.location.search);
        categoryId = urlParams.get('id');

        if (!categoryId) {
            window.location.href = '/staff/categories';
            return;
        }

        initEvents();
        fetchParentCategories().then(() => {
            loadCategoryDetail();
        });
    });

    function getAuthHeaders() {
        const token = sessionStorage.getItem('accessToken') || localStorage.getItem('accessToken');
        return {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        };
    }

    async function customFetch(url, options = {}) {
        let fetchUrl = url;
        if (typeof window.authFetch === 'function') {
            if (fetchUrl.startsWith('/api/')) {
                fetchUrl = fetchUrl.substring(4);
            }
            return await window.authFetch(fetchUrl, options);
        }
        if (!fetchUrl.startsWith('/api/')) {
            fetchUrl = '/api' + fetchUrl;
        }
        options.headers = Object.assign({}, getAuthHeaders(), options.headers || {});
        return await fetch(fetchUrl, options);
    }

    function initEvents() {
        // Dropdown toggle via Arrow icon ONLY
        const btnToggleParentDropdown = document.getElementById('btnToggleParentDropdown');
        const parentDropdownMenu = document.getElementById('parentDropdownMenu');

        if (btnToggleParentDropdown && parentDropdownMenu) {
            btnToggleParentDropdown.addEventListener('click', function (e) {
                e.stopPropagation();
                const isOpen = parentDropdownMenu.style.display === 'block';
                parentDropdownMenu.style.display = isOpen ? 'none' : 'block';
            });
        }

        // Close dropdown when clicking outside
        document.addEventListener('click', function (e) {
            const wrapper = document.querySelector('.ds-combobox-wrapper');
            if (wrapper && !wrapper.contains(e.target) && parentDropdownMenu) {
                parentDropdownMenu.style.display = 'none';
            }
        });

        const editForm = document.getElementById('editCategoryForm');
        if (editForm) {
            editForm.addEventListener('submit', handleFormSubmit);
        }

        const btnToggleStatus = document.getElementById('btnToggleStatus');
        if (btnToggleStatus) {
            btnToggleStatus.addEventListener('click', toggleStatus);
        }
    }

    async function fetchParentCategories() {
        try {
            const res = await customFetch('/v1/staff/categories/parents');
            if (res.ok) {
                parentCategoriesList = await res.json();
                renderParentDropdownMenu(parentCategoriesList, categoryId);
            }
        } catch (e) {
            console.error('Lỗi tải danh mục cha:', e);
        }
    }

    function renderParentDropdownMenu(parents, currentCatId = null) {
        const menu = document.getElementById('parentDropdownMenu');
        if (!menu) return;

        let html = '';
        if (parents && parents.length > 0) {
            parents.forEach(p => {
                if (currentCatId && String(p.id) === String(currentCatId)) {
                    return; // Skip self
                }
                html += `<li class="ds-combobox-item" data-id="${p.id}" data-name="${escapeHtml(p.name)}">${escapeHtml(p.name)}</li>`;
            });
        } else {
            html = '<li class="ds-combobox-item" style="color: #94a3b8; cursor: default;">Chưa có danh mục cha nào</li>';
        }
        menu.innerHTML = html;

        // Attach click listeners to menu items
        Array.from(menu.querySelectorAll('.ds-combobox-item')).forEach(item => {
            item.addEventListener('click', function () {
                const selectedName = this.getAttribute('data-name') || '';
                const input = document.getElementById('categoryParentInput');
                if (input) input.value = selectedName;
                menu.style.display = 'none';
            });
        });
    }

    async function loadCategoryDetail() {
        try {
            const res = await customFetch(`/v1/staff/categories/${categoryId}`);
            if (!res.ok) {
                if (typeof window.showErrorToast === 'function') {
                    window.showErrorToast('Không tìm thấy thông tin danh mục.');
                }
                setTimeout(() => { window.location.href = '/staff/categories'; }, 1500);
                return;
            }

            const data = await res.json();
            currentCategoryData = data;
            renderCategoryDetail(data);
        } catch (e) {
            console.error('Lỗi tải chi tiết danh mục:', e);
        }
    }

    function renderCategoryDetail(cat) {
        const isParent = !cat.parentId;

        // Breadcrumb & Page Subtitle
        document.getElementById('catCodeDisplay').textContent = `#CAT-${cat.id}`;
        document.getElementById('catCodeSubtitle').textContent = `#CAT-${cat.id}`;
        document.getElementById('catNameSubtitle').textContent = cat.name || '';

        // Status Badge Header
        const catStatusBadge = document.getElementById('catStatusBadge');
        if (cat.delete) {
            catStatusBadge.className = 'ds-badge ds-badge-danger';
            catStatusBadge.textContent = 'Đã ẩn';
        } else {
            catStatusBadge.className = 'ds-badge ds-badge-info';
            catStatusBadge.textContent = 'Đang hoạt động';
        }

        // Card 1: Info list
        document.getElementById('infoCatId').textContent = `#CAT-${cat.id}`;
        const parentHtml = isParent
            ? '<span class="ds-text-subtle" style="color: #94a3b8; font-weight: 600;">—</span>'
            : `<span class="ds-badge ds-badge-warning">${escapeHtml(cat.parentName)}</span>`;
        document.getElementById('infoParentName').innerHTML = parentHtml;

        document.getElementById('infoCatName').textContent = cat.name || '';
        document.getElementById('infoDescription').textContent = cat.description || 'Chưa có mô tả';
        document.getElementById('infoProductCount').innerHTML = `<span class="ds-badge ds-badge-neutral">${cat.productCount} SP</span>`;
        document.getElementById('infoCreatedAt').textContent = formatDate(cat.createdAt);
        document.getElementById('infoUpdatedAt').textContent = formatDate(cat.updatedAt);

        // Card 2: Form fields
        const categoryParentInput = document.getElementById('categoryParentInput');
        if (categoryParentInput) {
            categoryParentInput.value = (!isParent && cat.parentName && cat.parentName !== '—') ? cat.parentName : '';
        }

        document.getElementById('categoryName').value = cat.name || '';
        document.getElementById('categoryDescription').value = cat.description || '';

        renderParentDropdownMenu(parentCategoriesList, cat.id);

        // Right Column: Summary Card
        const summaryStatusBadge = document.getElementById('summaryStatusBadge');
        if (cat.delete) {
            summaryStatusBadge.className = 'ds-badge ds-badge-danger';
            summaryStatusBadge.textContent = 'Đã ẩn';
        } else {
            summaryStatusBadge.className = 'ds-badge ds-badge-info';
            summaryStatusBadge.textContent = 'Đang hoạt động';
        }

        document.getElementById('summaryProductCount').textContent = `${cat.productCount} SP`;

        // Right Column: Action Button
        const btnToggleStatus = document.getElementById('btnToggleStatus');
        if (btnToggleStatus) {
            if (cat.delete) {
                btnToggleStatus.className = 'ds-btn ds-btn-success';
                btnToggleStatus.innerHTML = '<i class="fa fa-eye"></i> Hiện danh mục này';
            } else {
                btnToggleStatus.className = 'ds-btn ds-btn-danger';
                btnToggleStatus.innerHTML = '<i class="fa fa-eye-slash"></i> Ẩn danh mục này';
            }
        }
    }

    async function handleFormSubmit(e) {
        e.preventDefault();

        const name = document.getElementById('categoryName').value.trim();
        const parentInputValue = document.getElementById('categoryParentInput')?.value?.trim() || '';
        const description = document.getElementById('categoryDescription').value.trim();

        if (!name) {
            if (typeof window.showErrorToast === 'function') {
                window.showErrorToast('Vui lòng nhập tên danh mục.');
            }
            return;
        }

        let parentId = null;
        let parentName = null;

        if (parentInputValue) {
            const matchedParent = parentCategoriesList.find(p => p.name.toLowerCase() === parentInputValue.toLowerCase());
            if (matchedParent) {
                parentId = matchedParent.id;
            } else {
                parentName = parentInputValue;
            }
        }

        const payload = {
            name: name,
            parentId: parentId,
            parentName: parentName,
            description: description
        };

        const btnSave = document.getElementById('btnSaveCategory');
        if (btnSave) {
            btnSave.disabled = true;
            btnSave.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang lưu...';
        }

        try {
            const res = await customFetch(`/v1/staff/categories/${categoryId}`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });

            const result = await res.json();

            if (res.ok) {
                if (typeof window.showSuccessToast === 'function') {
                    window.showSuccessToast(result.message || 'Cập nhật thông tin danh mục thành công!');
                }
                fetchParentCategories().then(() => {
                    loadCategoryDetail();
                });
            } else {
                const errorMsg = result.error || result.message || 'Cập nhật thất bại. Vui lòng kiểm tra lại.';
                if (typeof window.showErrorToast === 'function') {
                    window.showErrorToast(errorMsg);
                }
            }
        } catch (err) {
            console.error('Lỗi khi lưu danh mục:', err);
            if (typeof window.showErrorToast === 'function') {
                window.showErrorToast('Lỗi kết nối máy chủ. Vui lòng thử lại.');
            }
        } finally {
            if (btnSave) {
                btnSave.disabled = false;
                btnSave.innerHTML = '<i class="fa fa-save"></i> Lưu thay đổi';
            }
        }
    }

    async function toggleStatus() {
        if (!categoryId) return;
        try {
            const res = await customFetch(`/v1/staff/categories/${categoryId}/toggle-status`, {
                method: 'PATCH'
            });
            const result = await res.json();
            if (res.ok) {
                if (typeof window.showSuccessToast === 'function') {
                    window.showSuccessToast(result.message || 'Đã cập nhật trạng thái danh mục.');
                }
                loadCategoryDetail();
            } else {
                if (typeof window.showErrorToast === 'function') {
                    window.showErrorToast(result.error || result.message || 'Không thể cập nhật trạng thái.');
                }
            }
        } catch (e) {
            console.error('Lỗi đổi trạng thái danh mục:', e);
            if (typeof window.showErrorToast === 'function') {
                window.showErrorToast('Lỗi kết nối máy chủ.');
            }
        }
    }

    function formatDate(dateStr) {
        if (!dateStr) return '-';
        try {
            const d = new Date(dateStr);
            if (isNaN(d.getTime())) return dateStr;
            return d.toLocaleString('vi-VN');
        } catch (e) {
            return dateStr;
        }
    }

    function escapeHtml(text) {
        if (!text) return '';
        return text
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }
})();
