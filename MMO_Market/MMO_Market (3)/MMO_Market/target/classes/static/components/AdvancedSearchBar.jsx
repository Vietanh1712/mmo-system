import React, { useState, useRef, useEffect } from 'react';
import '../css/AdvancedSearchBar.css';

// Dữ liệu danh mục mới
const CATEGORIES = [
    'Tất cả',
    'Tên người bán',
    'Email',
    'Tài khoản',
    'Phần mềm',
    'Tăng tương tác',
    'Dịch vụ phần mềm',
    'Blockchain',
    'Khác',
    'Dịch vụ khác'
];

const QUICK_LINKS = ['Netflix', 'Tài khoản ChatGPT', 'Clone FB', 'Youtube Premium'];

const AdvancedSearchBar = () => {
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    const [selectedCategory, setSelectedCategory] = useState('Tất cả');
    const [categorySearchTerm, setCategorySearchTerm] = useState('');
    const [mainSearchTerm, setMainSearchTerm] = useState('');

    const dropdownRef = useRef(null);

    // Xử lý click ra ngoài để đóng dropdown
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsDropdownOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, []);

    // Filter danh mục dựa trên từ khóa tìm kiếm nhỏ
    const filteredCategories = CATEGORIES.filter(cat => 
        cat.toLowerCase().includes(categorySearchTerm.toLowerCase())
    );

    const handleCategorySelect = (category) => {
        setSelectedCategory(category);
        setIsDropdownOpen(false);
        setCategorySearchTerm(''); // Reset search term sau khi chọn
    };

    const handleSearchSubmit = (e) => {
        e.preventDefault();
        console.log(`Tìm kiếm: "${mainSearchTerm}" trong danh mục "${selectedCategory}"`);
        // TODO: Chuyển hướng sang trang kết quả tìm kiếm (ví dụ: window.location.href = `/search?q=${mainSearchTerm}&category=${selectedCategory}`)
    };

    return (
        <div style={{ width: '100%', maxWidth: '800px', margin: '0 auto' }}>
            <form className="search-container" onSubmit={handleSearchSubmit}>
                
                {/* Phần bên trái: Dropdown chọn danh mục */}
                <div className="search-category" ref={dropdownRef}>
                    <div 
                        className="search-category__trigger"
                        onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                    >
                        <span>{selectedCategory}</span>
                        <svg 
                            className={`search-category__icon ${isDropdownOpen ? 'search-category__icon--open' : ''}`}
                            width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                        >
                            <polyline points="6 9 12 15 18 9"></polyline>
                        </svg>
                    </div>

                    {isDropdownOpen && (
                        <div className="dropdown-panel">
                            <input 
                                type="text" 
                                className="dropdown-panel__search"
                                placeholder="Tìm danh mục..."
                                value={categorySearchTerm}
                                onChange={(e) => setCategorySearchTerm(e.target.value)}
                                onClick={(e) => e.stopPropagation()} // Ngăn việc click vào input làm đóng dropdown
                            />
                            <ul className="dropdown-panel__list">
                                {filteredCategories.length > 0 ? (
                                    filteredCategories.map((cat, index) => (
                                        <li 
                                            key={index} 
                                            className="dropdown-panel__item"
                                            onClick={() => handleCategorySelect(cat)}
                                        >
                                            {cat}
                                        </li>
                                    ))
                                ) : (
                                    <li className="dropdown-panel__item" style={{ color: '#94a3b8', cursor: 'default' }}>
                                        Không tìm thấy danh mục
                                    </li>
                                )}
                            </ul>
                        </div>
                    )}
                </div>

                {/* Phần trung tâm: Ô nhập từ khóa */}
                <div className="search-input-wrapper">
                    <input 
                        type="text" 
                        className="search-input"
                        placeholder="Từ khóa, Tài khoản, Mã nguồn,..."
                        value={mainSearchTerm}
                        onChange={(e) => setMainSearchTerm(e.target.value)}
                    />
                </div>

                {/* Phần bên phải: Nút tìm kiếm */}
                <button type="submit" className="search-button" aria-label="Tìm kiếm">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <circle cx="11" cy="11" r="8"></circle>
                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    </svg>
                </button>
            </form>

            {/* Phần bên dưới: Từ khóa gợi ý */}
            <div className="search-quick-links">
                {QUICK_LINKS.map((link, index) => (
                    <a 
                        key={index} 
                        href={`/search?q=${encodeURIComponent(link)}`} 
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