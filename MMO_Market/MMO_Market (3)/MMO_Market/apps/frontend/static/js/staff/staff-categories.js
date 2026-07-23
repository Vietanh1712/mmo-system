(function () {
    'use strict';

    let allCategoriesData = [];
    let parentCategoriesList = [];
    let currentPage = 0;
    let pageSize = 10;
    let totalPages = 1;
    let totalElements = 0;
    let currentSelectedCat = null;

    document.addEventListener('DOMContentLoaded', function () {
        initEvents();
        fetchCategoryStats();
        fetchParentCategories();
        loadCategories();
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

        // Search & Filter
        const btnSearch = document.getElementById('btnSearch');
        if (btnSearch) {
            btnSearch.addEventListener('click', function () {
                currentPage = 0;
                loadCategories();
            });
        }

        const btnResetFilter = document.getElementById('btnResetFilter');
        if (btnResetFilter) {
            btnResetFilter.addEventListener('click', function () {
                document.getElementById('keywordFilter').value = '';
                document.getElementById('typeFilter').value = 'ALL';
                document.getElementById('sortByFilter').value = 'newest';
                currentPage = 0;
                loadCategories();
            });
        }

        const keywordFilter = document.getElementById('keywordFilter');
        if (keywordFilter) {
            keywordFilter.addEventListener('keyup', function (e) {
                if (e.key === 'Enter') {
                    currentPage = 0;
                    loadCategories();
                }
            });
        }

        // Modal triggers
        const btnOpenCreateModal = document.getElementById('btnOpenCreateModal');
        if (btnOpenCreateModal) {
            btnOpenCreateModal.addEventListener('click', function () {
                openModal('create');
            });
        }

        const btnCloseModal = document.getElementById('btnCloseModal');
        if (btnCloseModal) {
            btnCloseModal.addEventListener('click', closeModal);
        }

        const btnCancelModal = document.getElementById('btnCancelModal');
        if (btnCancelModal) {
            btnCancelModal.addEventListener('click', closeModal);
        }

        // Modal status toggle
        const btnToggleStatusModal = document.getElementById('btnToggleStatusModal');
        if (btnToggleStatusModal) {
            btnToggleStatusModal.addEventListener('click', function () {
                if (currentSelectedCat) {
                    toggleCategoryStatus(currentSelectedCat.id, true);
                }
            });
        }

        // Form Submit
        const categoryForm = document.getElementById('categoryForm');
        if (categoryForm) {
            categoryForm.addEventListener('submit', handleFormSubmit);
        }
    }

    async function fetchCategoryStats() {
        try {
            const res = await customFetch('/v1/staff/categories/stats');
            if (res.ok) {
                const stats = await res.json();
                document.getElementById('stat-total-active').textContent = stats.totalActive || 0;
                document.getElementById('stat-total-parents').textContent = stats.totalParents || 0;
                document.getElementById('stat-total-children').textContent = stats.totalChildren || 0;
                document.getElementById('stat-total-deleted').textContent = stats.totalDeleted || 0;
            }
        } catch (e) {
            console.error('Lỗi tải thống kê danh mục:', e);
        }
    }

    async function fetchParentCategories() {
        try {
            const res = await customFetch('/v1/staff/categories/parents');
            if (res.ok) {
                parentCategoriesList = await res.json();
                renderParentDropdownMenu(parentCategoriesList);
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

    async function loadCategories() {
        const tbody = document.getElementById('categoryTableBody');
        if (!tbody) return;

        tbody.innerHTML = '<tr><td colspan="8" class="ds-table-center"><i class="fa fa-spinner fa-spin"></i> Đang tải dữ liệu danh mục...</td></tr>';

        const keyword = document.getElementById('keywordFilter')?.value?.trim() || '';
        const type = document.getElementById('typeFilter')?.value || 'ALL';
        const sortBy = document.getElementById('sortByFilter')?.value || 'newest';

        const params = new URLSearchParams();
        if (keyword) params.append('keyword', keyword);
        if (type !== 'ALL') params.append('type', type);
        if (sortBy) params.append('sortBy', sortBy);
        params.append('page', currentPage);
        params.append('size', pageSize);

        let url = `/v1/staff/categories?` + params.toString();

        try {
            const res = await customFetch(url);
            if (!res.ok) {
                throw new Error('Lấy danh sách danh mục thất bại. HTTP Status: ' + res.status);
            }
            const data = await res.json();
            allCategoriesData = data.content || [];
            totalElements = data.totalElements || 0;
            totalPages = data.totalPages || 1;
            currentPage = data.currentPage || 0;

            renderCategoryTable(allCategoriesData);
            renderPagination();
        } catch (e) {
            console.error('Lỗi load danh mục:', e);
            tbody.innerHTML = '<tr><td colspan="8" class="ds-table-center ds-text-danger">Không thể tải dữ liệu danh mục. Vui lòng thử lại.</td></tr>';
            if (typeof window.showErrorToast === 'function') {
                window.showErrorToast('Không thể tải dữ liệu danh mục.');
            }
        }
    }

    function renderCategoryTable(categories) {
        const tbody = document.getElementById('categoryTableBody');
        if (!tbody) return;

        if (!categories || categories.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="ds-table-center">Không tìm thấy danh mục nào phù hợp.</td></tr>';
            return;
        }

        let html = '';
        categories.forEach((cat, index) => {
            const stt = currentPage * pageSize + index + 1;
            const isParent = !cat.parentId;
            const parentCol = isParent
                ? `<span class="ds-text-subtle" style="color: #94a3b8; font-weight: 600;">—</span>`
                : `<span class="ds-badge ds-badge-warning" style="font-size: 12.5px;">${escapeHtml(cat.parentName)}</span>`;

            const childCol = `<strong class="ds-text-heading" style="font-size: 14px; color: #0f172a;">${escapeHtml(cat.name)}</strong>`;

            const statusBadge = cat.delete
                ? '<span class="ds-badge ds-badge-danger">Đã ẩn</span>'
                : '<span class="ds-badge ds-badge-info">Đang hoạt động</span>';

            html += `
                <tr class="${cat.delete ? 'ds-row-disabled' : ''}">
                    <td class="ds-table-center">${stt}</td>
                    <td><strong>#CAT-${cat.id}</strong></td>
                    <td>${parentCol}</td>
                    <td>${childCol}</td>
                    <td class="ds-text-subtle">${cat.description ? escapeHtml(cat.description) : '<em>Chưa có mô tả</em>'}</td>
                    <td class="ds-table-center">
                        <span class="ds-badge ds-badge-neutral">${cat.productCount} SP</span>
                    </td>
                    <td class="ds-table-center">${statusBadge}</td>
                    <td class="ds-table-center">
                        <div class="ds-table-actions" style="justify-content: center;">
                            <a class="ds-icon-btn ds-icon-btn-view" href="/staff/categories/detail?id=${cat.id}" title="Xem chi tiết danh mục" aria-label="Xem chi tiết">
                                <svg class="ds-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true" style="width: 16px; height: 16px;">
                                    <path d="M2.25 12C3.73 8.12 7.49 5.25 12 5.25C16.51 5.25 20.27 8.12 21.75 12C20.27 15.88 16.51 18.75 12 18.75C7.49 18.75 3.73 15.88 2.25 12Z" stroke="currentColor" stroke-width="2"/>
                                    <path d="M12 15.25C13.79 15.25 15.25 13.79 15.25 12C15.25 10.21 13.79 8.75 12 8.75C10.21 8.75 8.75 10.21 8.75 12C8.75 13.79 10.21 15.25 12 15.25Z" stroke="currentColor" stroke-width="2"/>
                                </svg>
                            </a>
                        </div>
                    </td>
                </tr>
            `;
        });

        tbody.innerHTML = html;
    }

    function renderPagination() {
        if (typeof window.mountStaffPagination === 'function') {
            window.mountStaffPagination('categoryPagination', {
                page: currentPage,
                totalPages: totalPages,
                totalElements: totalElements,
                pageSize: pageSize
            }, {
                onPage: function (newPage) {
                    currentPage = newPage;
                    loadCategories();
                },
                onSize: function (newSize) {
                    pageSize = newSize;
                    currentPage = 0;
                    loadCategories();
                }
            });
        }
    }

    function openModal(mode, data = null) {
        const modal = document.getElementById('categoryModal');
        const modalTitle = document.getElementById('modalTitle');
        const categoryId = document.getElementById('categoryId');
        const categoryName = document.getElementById('categoryName');
        const categoryParentInput = document.getElementById('categoryParentInput');
        const categoryDescription = document.getElementById('categoryDescription');

        const infoSummary = document.getElementById('categoryInfoSummary');
        const summaryCatId = document.getElementById('summaryCatId');
        const summaryProdCount = document.getElementById('summaryProdCount');
        const summaryStatusBadge = document.getElementById('summaryStatusBadge');
        const btnToggleStatusModal = document.getElementById('btnToggleStatusModal');
        const menu = document.getElementById('parentDropdownMenu');

        if (!modal) return;
        currentSelectedCat = data;
        if (menu) menu.style.display = 'none';

        if (mode === 'create') {
            modalTitle.textContent = 'Thêm danh mục mới';
            categoryId.value = '';
            categoryName.value = '';
            if (categoryParentInput) categoryParentInput.value = '';
            categoryDescription.value = '';

            renderParentDropdownMenu(parentCategoriesList, null);

            if (infoSummary) infoSummary.style.display = 'none';
            if (btnToggleStatusModal) btnToggleStatusModal.style.display = 'none';
        } else {
            modalTitle.textContent = 'Chi tiết & Chỉnh sửa danh mục';
            categoryId.value = data.id;
            categoryName.value = data.name || '';
            if (categoryParentInput) {
                categoryParentInput.value = (data.parentName && data.parentName !== '—') ? data.parentName : '';
            }
            categoryDescription.value = data.description || '';

            renderParentDropdownMenu(parentCategoriesList, data.id);

            if (infoSummary) {
                infoSummary.style.display = 'block';
                summaryCatId.textContent = `#CAT-${data.id}`;
                summaryProdCount.textContent = `${data.productCount} SP`;
                if (data.delete) {
                    summaryStatusBadge.className = 'ds-badge ds-badge-danger';
                    summaryStatusBadge.textContent = 'Đã ẩn';
                } else {
                    summaryStatusBadge.className = 'ds-badge ds-badge-info';
                    summaryStatusBadge.textContent = 'Đang hoạt động';
                }
            }

            if (btnToggleStatusModal) {
                btnToggleStatusModal.style.display = 'inline-block';
                if (data.delete) {
                    btnToggleStatusModal.className = 'ds-btn ds-btn-success';
                    btnToggleStatusModal.innerHTML = '<i class="fa fa-eye"></i> Hiện danh mục này';
                } else {
                    btnToggleStatusModal.className = 'ds-btn ds-btn-danger';
                    btnToggleStatusModal.innerHTML = '<i class="fa fa-eye-slash"></i> Ẩn danh mục này';
                }
            }
        }

        modal.style.display = 'grid';
        modal.removeAttribute('hidden');
    }

    function closeModal() {
        const modal = document.getElementById('categoryModal');
        const menu = document.getElementById('parentDropdownMenu');
        if (menu) menu.style.display = 'none';
        if (modal) {
            modal.style.display = 'none';
            modal.setAttribute('hidden', 'true');
        }
        currentSelectedCat = null;
    }

    async function handleFormSubmit(e) {
        e.preventDefault();

        const categoryId = document.getElementById('categoryId').value;
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

        const isEdit = !!categoryId;
        const url = isEdit ? `/v1/staff/categories/${categoryId}` : '/v1/staff/categories';
        const method = isEdit ? 'PUT' : 'POST';

        const btnSave = document.getElementById('btnSaveCategory');
        if (btnSave) {
            btnSave.disabled = true;
            btnSave.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang lưu...';
        }

        try {
            const res = await customFetch(url, {
                method: method,
                body: JSON.stringify(payload)
            });

            const result = await res.json();

            if (res.ok) {
                if (typeof window.showSuccessToast === 'function') {
                    window.showSuccessToast(result.message || (isEdit ? 'Cập nhật danh mục thành công!' : 'Tạo danh mục mới thành công!'));
                }
                closeModal();
                fetchCategoryStats();
                fetchParentCategories();
                loadCategories();
            } else {
                const errorMsg = result.error || result.message || 'Thao tác thất bại. Vui lòng kiểm tra lại.';
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
                btnSave.innerHTML = '<i class="fa fa-save"></i> Lưu thông tin';
            }
        }
    }

    async function toggleCategoryStatus(id, updateModal = false) {
        try {
            const res = await customFetch(`/v1/staff/categories/${id}/toggle-status`, {
                method: 'PATCH'
            });
            const result = await res.json();
            if (res.ok) {
                if (typeof window.showSuccessToast === 'function') {
                    window.showSuccessToast(result.message || 'Đã cập nhật trạng thái danh mục.');
                }
                fetchCategoryStats();
                await loadCategories();

                if (updateModal) {
                    const updatedCat = allCategoriesData.find(c => c.id === id);
                    if (updatedCat) {
                        openModal('view', updatedCat);
                    } else {
                        closeModal();
                    }
                }
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
