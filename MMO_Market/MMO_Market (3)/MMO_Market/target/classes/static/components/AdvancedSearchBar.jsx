import React, { useState, useEffect } from 'react';
import '../css/AdvancedSearchBar.css';

// Giữ nguyên Quick Links của bạn
const QUICK_LINKS = ['Netflix', 'Tài khoản ChatGPT', 'Facebook', 'Youtube Premium'];

const AdvancedSearchBar = () => {
    // State quản lý danh mục từ Database
    const [mainCategories, setMainCategories] = useState([]);
    const [subCategories, setSubCategories] = useState([]);

    // State lưu trữ lựa chọn của người dùng
    const [selectedMainCat, setSelectedMainCat] = useState('');
    const [selectedSubCat, setSelectedSubCat] = useState('');
    const [mainSearchTerm, setMainSearchTerm] = useState('');

    // Fetch danh mục chính (parent_id IS NULL) khi component vừa load
    useEffect(() => {
        fetch('/api/categories/main')
            .then(res => res.json())
            .then(data => setMainCategories(data))
            .catch(err => console.error("Lỗi tải danh mục chính:", err));
    }, []);

    // Fetch danh mục con khi người dùng chọn danh mục chính
    useEffect(() => {
        if (selectedMainCat) {
            fetch(`/api/categories/${selectedMainCat}/sub`)
                .then(res => res.json())
                .then(data => {
                    setSubCategories(data);
                    setSelectedSubCat(''); // Reset danh mục con khi đổi cha
                })
                .catch(err => console.error("Lỗi tải danh mục con:", err));
        } else {
            setSubCategories([]);
            setSelectedSubCat('');
        }
    }, [selectedMainCat]);

    // Xử lý khi bấm nút TÌM
    const handleSearchSubmit = (e) => {
        e.preventDefault();
        // Lấy id của danh mục chi tiết nhất (ưu tiên danh mục con, nếu không có thì lấy danh mục cha)
        const finalCategoryId = selectedSubCat || selectedMainCat || '';
        // Chuyển hướng sang Spring Boot Controller
        window.location.href = `/search?keyword=${encodeURIComponent(mainSearchTerm)}&categoryId=${finalCategoryId}`;
    };

    return (
        <div style={{ width: '100%', maxWidth: '800px', margin: '0 auto' }}>
            <form className="search-container" onSubmit={handleSearchSubmit}>

                {/* Phần bên trái: 2 Dropdown liên kết */}
                {/* Dropdown 1: Danh mục chính */}
                <div className="search-category">
                    <select
                        value={selectedMainCat}
                        onChange={(e) => setSelectedMainCat(e.target.value)}
                        className="search-category__trigger"
                        style={{ border: 'none', background: 'transparent', outline: 'none', width: '100%', cursor: 'pointer', appearance: 'auto', padding: '0 10px' }}
                    >
                        <option value="">Chọn danh mục chính...</option>
                        {mainCategories.map(cat => (
                            <option key={cat.id} value={cat.id}>{cat.name}</option>
                        ))}
                    </select>
                </div>

                {/* Dropdown 2: Lọc chi tiết */}
                <div className="search-category">
                    <select
                        value={selectedSubCat}
                        onChange={(e) => setSelectedSubCat(e.target.value)}
                        className="search-category__trigger"
                        disabled={!selectedMainCat || subCategories.length === 0}
                        style={{ border: 'none', background: 'transparent', outline: 'none', width: '100%', cursor: 'pointer', appearance: 'auto', padding: '0 10px' }}
                    >
                        <option value="">Lọc chi tiết...</option>
                        {subCategories.map(sub => (
                            <option key={sub.id} value={sub.id}>{sub.name}</option>
                        ))}
                    </select>
                </div>

                {/* Phần trung tâm: Ô nhập từ khóa (Giữ nguyên) */}
                <div className="search-input-wrapper">
                    <input
                        type="text"
                        className="search-input"
                        placeholder="Từ khóa, Tài khoản, Mã nguồn,..."
                        value={mainSearchTerm}
                        onChange={(e) => setMainSearchTerm(e.target.value)}
                    />
                </div>

                {/* Phần bên phải: Nút tìm kiếm (Giữ nguyên) */}
                <button type="submit" className="search-button" aria-label="Tìm kiếm">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <circle cx="11" cy="11" r="8"></circle>
                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    </svg>
                </button>
            </form>

            {/* Phần bên dưới: Từ khóa gợi ý (Giữ nguyên thiết kế, update link) */}
            <div className="search-quick-links">
                {QUICK_LINKS.map((link, index) => (
                    <a
                        key={index}
                        href={`/search?keyword=${encodeURIComponent(link)}`}
                        className="quick-link-item"
                    >
                        {link}
                    </a>
                ))}
            </div>
        </div>
    );
};

export default AdvancedSearchBar;
