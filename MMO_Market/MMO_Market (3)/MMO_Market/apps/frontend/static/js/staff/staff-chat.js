document.addEventListener('DOMContentLoaded', function () {
    let currentUserId = null;
    let currentUserName = '';
    let pendingAttachment = null;
    let currentContextProduct = null;

    // 3-dot Dropdown Actions
    window.toggleChatDropdown = function(event) {
        event.stopPropagation();
        const dropdown = document.getElementById('staff-chat-dropdown');
        if (dropdown) {
            const show = dropdown.style.display === 'none';
            dropdown.style.display = show ? 'block' : 'none';
        }
    };

    window.addEventListener('click', () => {
        const dropdown = document.getElementById('staff-chat-dropdown');
        if (dropdown) dropdown.style.display = 'none';
    });

    window.clearStaffChatHistory = function(event) {
        event.preventDefault();
        if (confirm('Bạn có chắc chắn muốn xóa toàn bộ lịch sử chat với người dùng này?')) {
            alert('Đã xóa lịch sử trò chuyện (giả lập).');
            messagesContainer.innerHTML = '<div style="text-align: center; padding: 2rem; color: var(--ds-text-tertiary);">Lịch sử chat đã được xóa.</div>';
        }
    };

    window.toggleStaffBlock = function(event) {
        event.preventDefault();
        alert('Đã thực hiện chặn/mở chặn liên hệ (giả lập).');
    };



    window.toggleStaffSearchMessages = function(event) {
        if (event) event.preventDefault();
        const container = document.getElementById('staff-message-search-container');
        const input = document.getElementById('staff-message-search-input');
        const isHidden = container.style.display === 'none';
        
        container.style.display = isHidden ? 'flex' : 'none';
        if (isHidden) {
            input.value = '';
            input.focus();
            window.clearStaffMessageHighlights();
        } else {
            window.clearStaffMessageHighlights();
        }
    };

    window.closeStaffMessageSearch = function() {
        const container = document.getElementById('staff-message-search-container');
        if (container) container.style.display = 'none';
        window.clearStaffMessageHighlights();
    };

    window.clearStaffMessageHighlights = function() {
        const bubbles = document.querySelectorAll('.staff-chat-bubble');
        bubbles.forEach(b => {
            b.style.border = '';
            b.style.background = '';
        });
    };

    window.performStaffMessageSearch = function(term) {
        window.clearStaffMessageHighlights();
        if (!term || !term.trim()) return;

        const kw = term.trim().toLowerCase();
        const bubbles = document.querySelectorAll('.staff-chat-bubble');
        let firstMatch = null;
        let foundCount = 0;

        bubbles.forEach(b => {
            const text = b.textContent.toLowerCase();
            if (text.includes(kw)) {
                b.style.border = '2px solid var(--brand-accent, #f97316)';
                b.style.background = '#fef3c7'; // yellow highlight background
                foundCount++;
                if (!firstMatch) {
                    firstMatch = b;
                }
            }
        });

        if (firstMatch) {
            firstMatch.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    };

    // Attachments Handling
    window.handleStaffImageUpload = function(event) {
        const file = event.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = function(e) {
            pendingAttachment = {
                type: 'image',
                name: file.name,
                size: file.size,
                dataUrl: e.target.result
            };
            renderAttachmentPreview();
        };
        reader.readAsDataURL(file);
    };

    window.handleStaffFileUpload = function(event) {
        const file = event.target.files[0];
        if (!file) return;
        pendingAttachment = {
            type: 'file',
            name: file.name,
            size: file.size,
            url: '#'
        };
        renderAttachmentPreview();
    };

    function renderAttachmentPreview() {
        const preview = document.getElementById('staff-attachment-preview');
        const content = document.getElementById('staff-attachment-content');
        if (!preview || !content) return;
        
        if (pendingAttachment) {
            preview.style.display = 'flex';
            if (pendingAttachment.type === 'image') {
                content.innerHTML = `<i class="fa fa-picture-o" style="color: #f97316; font-size: 16px;"></i> <span style="font-weight: 600; text-overflow: ellipsis; overflow: hidden; white-space: nowrap; max-width: 140px;" title="${pendingAttachment.name}">${pendingAttachment.name}</span> (${(pendingAttachment.size / 1024).toFixed(1)} KB)`;
            } else {
                content.innerHTML = `<i class="fa fa-file-text-o" style="color: #64748b; font-size: 16px;"></i> <span style="font-weight: 600; text-overflow: ellipsis; overflow: hidden; white-space: nowrap; max-width: 140px;" title="${pendingAttachment.name}">${pendingAttachment.name}</span> (${(pendingAttachment.size / 1024).toFixed(1)} KB)`;
            }
        } else {
            preview.style.display = 'none';
        }
    }

    window.cancelStaffAttachment = function() {
        pendingAttachment = null;
        renderAttachmentPreview();
        document.getElementById('staff-image-input').value = '';
        document.getElementById('staff-file-input').value = '';
    };

    const conversationsContainer = document.querySelector('.staff-chat-list__items');
    const messagesContainer = document.getElementById('staffChatMessages');
    const chatInput = document.getElementById('staffChatInput');
    const chatForm = document.getElementById('staffChatForm');
    const searchInput = document.querySelector('.staff-chat-list__header input[type="search"]');
    const panelHeaderTitle = document.querySelector('.staff-chat-panel__header .ds-entity-title');
    const panelHeaderSubtitle = document.querySelector('.staff-chat-panel__header .ds-entity-subtitle');
    const panelHeaderAvatar = document.querySelector('.staff-chat-panel__header .ds-avatar');

    // Authentication token handling for API calls
    const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');

    async function fetchApi(url, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        };
        const response = await fetch(url, { ...options, headers });
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || 'Lỗi kết nối máy chủ');
        }
        return response.json();
    }

    // Load Conversations
    async function loadConversations() {
        try {
            const conversations = await fetchApi('/api/v1/staff/chat/conversations');
            renderConversations(conversations);
        } catch (error) {
            console.error('Failed to load conversations:', error);
        }
    }

    function renderConversations(conversations) {
        conversationsContainer.innerHTML = '';
        
        let totalUnread = 0;
        
        if (conversations.length === 0) {
            conversationsContainer.innerHTML = '<div style="padding: 1rem; text-align: center; color: var(--ds-text-secondary);">Chưa có hội thoại nào. Tìm kiếm người dùng để bắt đầu chat.</div>';
        } else {
            conversations.forEach(conv => {
                if (conv.unreadCount > 0) totalUnread += 1;
                const initial = conv.userName ? conv.userName.substring(0, 2).toUpperCase() : 'NA';
                const badge = conv.unreadCount > 0 ? `<span class="ds-badge ds-badge-danger">${conv.unreadCount}</span>` : '';
                const isActive = currentUserId === conv.userId ? 'is-active' : '';

                const timeString = conv.lastMessageTime ? new Date(conv.lastMessageTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '';
                const isOnline = conv.online;

                const item = document.createElement('a');
                item.className = `staff-chat-item ${isActive}`;
                item.setAttribute('data-id', conv.userId);
                item.href = '#';
                item.onclick = (e) => {
                    e.preventDefault();
                    selectUser(conv.userId, conv.userName, conv.userEmail, conv.userRole, isOnline);
                };

                let avatarClass = 'ds-avatar-success';
                let avatarContent = initial;
                if (conv.userRole === 'Seller') {
                    avatarClass = 'ds-avatar-primary';
                } else if (conv.userRole === 'Dispute') {
                    avatarClass = 'ds-avatar-danger';
                    avatarContent = '<i class="fa fa-gavel" style="font-size:15px;"></i>';
                }

                item.innerHTML = `
                    <div class="staff-avatar-wrap">
                        <span class="ds-avatar ds-avatar-sm ${avatarClass}">${avatarContent}</span>
                        <span class="staff-chat-status ${isOnline ? '' : 'staff-chat-status--offline'}"></span>
                    </div>
                    <div style="flex: 1; overflow: hidden;">
                        <div class="ds-entity-title">${conv.userName} <span style="font-size:0.75rem; color:var(--ds-text-tertiary);">(${conv.userRole})</span></div>
                        <div class="ds-entity-subtitle" style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${conv.lastMessage || 'Bắt đầu chat...'}</div>
                    </div>
                    <div class="staff-chat-item__meta">
                        <div>${timeString}</div>
                        ${badge}
                    </div>
                `;
                conversationsContainer.appendChild(item);
            });
        }
        
        const badgeElement = document.getElementById('unreadConversationsBadge');
        if (badgeElement) {
            if (totalUnread > 0) {
                badgeElement.textContent = `${totalUnread} hội thoại chưa đọc`;
                badgeElement.style.display = 'inline-flex';
            } else {
                badgeElement.style.display = 'none';
            }
        }

        const staffUnreadBadge = document.getElementById('staff-unread-badge');
        if (staffUnreadBadge) {
            if (totalUnread > 0) {
                staffUnreadBadge.textContent = `${totalUnread} hội thoại chưa đọc`;
                staffUnreadBadge.style.display = 'inline-block';
            } else {
                staffUnreadBadge.style.display = 'none';
            }
        }
    }

    // Search Users
    let searchTimeout = null;
    searchInput.addEventListener('input', (e) => {
        clearTimeout(searchTimeout);
        const keyword = e.target.value.trim();
        if (keyword.length === 0) {
            loadConversations();
            return;
        }

        searchTimeout = setTimeout(async () => {
            try {
                const users = await fetchApi(`/api/v1/staff/chat/search?keyword=${encodeURIComponent(keyword)}`);
                renderSearchResults(users);
            } catch (error) {
                console.error('Failed to search users:', error);
            }
        }, 500);
    });

    function renderSearchResults(users) {
        conversationsContainer.innerHTML = '';
        if (users.length === 0) {
            conversationsContainer.innerHTML = '<div style="padding: 1rem; text-align: center; color: var(--ds-text-secondary);">Không tìm thấy kết quả.</div>';
            return;
        }

        users.forEach(user => {
            const initial = user.userName ? user.userName.substring(0, 2).toUpperCase() : 'NA';
            const isActive = currentUserId === user.userId ? 'is-active' : '';
            const isOnline = user.online;

            const item = document.createElement('a');
            item.className = `staff-chat-item ${isActive}`;
            item.href = '#';
            item.onclick = (e) => {
                e.preventDefault();
                selectUser(user.userId, user.userName, user.userEmail, user.userRole, isOnline);
                searchInput.value = '';
                // Optional: You could call loadConversations() here to reset the list, or keep search results
            };

            item.innerHTML = `
                <div class="staff-avatar-wrap">
                    <span class="ds-avatar ds-avatar-sm ${user.userRole === 'Seller' ? 'ds-avatar-primary' : 'ds-avatar-success'}">${initial}</span>
                    <span class="staff-chat-status ${isOnline ? '' : 'staff-chat-status--offline'}"></span>
                </div>
                <div style="flex: 1;">
                    <div class="ds-entity-title">${user.userName} <span style="font-size:0.75rem; color:var(--ds-text-tertiary);">(${user.userRole})</span></div>
                    <div class="ds-entity-subtitle">${user.userEmail}</div>
                </div>
            `;
            conversationsContainer.appendChild(item);
        });
    }

    // Select User
    function selectUser(userId, userName, userEmail, userRole, isOnline) {
        // Reset search state
        if (typeof window.closeStaffMessageSearch === 'function') {
            window.closeStaffMessageSearch();
        }

        currentUserId = userId;
        currentUserName = userName;

        // Update UI styles
        document.querySelectorAll('.staff-chat-item').forEach(el => el.classList.remove('is-active'));
        
        // Update Panel Header
        panelHeaderTitle.textContent = userName;
        if (userId < 0) {
            panelHeaderSubtitle.innerHTML = `${userEmail} · ${userRole}`;
            panelHeaderAvatar.innerHTML = '<i class="fa fa-gavel" style="font-size:15px;"></i>';
            panelHeaderAvatar.className = 'ds-avatar ds-avatar-sm ds-avatar-danger';
            
            chatInput.disabled = true;
            chatInput.placeholder = "Nhân viên chỉ có quyền Read-only đối với phòng chat đối chất.";
            const attachBtn = document.querySelector('.staff-input-attach');
            if (attachBtn) attachBtn.style.pointerEvents = 'none';
            const inputSub = document.querySelector('.staff-input-sub');
            if (inputSub) inputSub.style.display = 'none';
        } else {
            panelHeaderSubtitle.innerHTML = `${userEmail} · ${userRole} · <span style="color: ${isOnline ? '#10b981' : '#94a3b8'}; display: inline-flex; align-items: center; gap: 4px;"><i class="fa fa-circle" style="font-size: 8px;"></i> ${isOnline ? 'Online Now' : 'Offline'}</span>`;
            panelHeaderAvatar.textContent = userName ? userName.substring(0, 2).toUpperCase() : 'NA';
            panelHeaderAvatar.className = `ds-avatar ds-avatar-sm ${userRole === 'Seller' ? 'ds-avatar-primary' : 'ds-avatar-success'}`;
            
            chatInput.disabled = false;
            chatInput.placeholder = "Nhập tin nhắn...";
            const attachBtn = document.querySelector('.staff-input-attach');
            if (attachBtn) attachBtn.style.pointerEvents = 'auto';
            const inputSub = document.querySelector('.staff-input-sub');
            if (inputSub) inputSub.style.display = 'flex';
        }

        // Load Messages
        loadMessages(userId);
    }

    // Load Messages
    async function loadMessages(userId) {
        messagesContainer.innerHTML = '<div style="text-align: center; padding: 2rem; color: var(--ds-text-tertiary);">Đang tải tin nhắn...</div>';
        const viewStaffComplaintBtn = document.getElementById('view-staff-complaint-btn');
        if (viewStaffComplaintBtn) {
            if (userId < 0) {
                const complaintId = -userId;
                viewStaffComplaintBtn.href = `/staff/complaints/detail?id=${complaintId}`;
                viewStaffComplaintBtn.style.display = 'inline-flex';
            } else {
                viewStaffComplaintBtn.style.display = 'none';
            }
        }
        try {
            const data = await fetchApi(`/api/v1/staff/chat/${userId}`);
            const messages = Array.isArray(data) ? data : (data.messages || []);
            const contextProductId = Array.isArray(data) ? null : data.contextProductId;

            currentContextProduct = null;
            if (contextProductId) {
                try {
                    const prod = await fetchApi(`/api/search/products/${contextProductId}`);
                    if (prod) {
                        currentContextProduct = {
                            id: prod.id || contextProductId,
                            name: prod.name,
                            price: prod.price,
                            sellerId: prod.sellerId
                        };
                    }
                } catch (e) {
                    console.error('Cannot load context product details:', e);
                }
            }

            renderMessages(messages, currentContextProduct);
        } catch (error) {
            messagesContainer.innerHTML = `<div style="text-align: center; padding: 2rem; color: var(--ds-color-danger);">Lỗi: ${error.message}</div>`;
        }
    }

    function renderMessages(messages, currentContextProduct) {
        messagesContainer.innerHTML = '';

        if (currentContextProduct) {
            const cp = currentContextProduct;
            const contextCard = document.createElement('div');
            contextCard.className = 'messages-context-card';
            contextCard.style.marginBottom = '16px';
            contextCard.onclick = () => { window.open(`/products/${cp.id}`, '_blank'); };

            let formattedPrice = '—';
            try {
                formattedPrice = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })
                    .format(cp.price).replace('₫', 'VNĐ');
            } catch (e) { formattedPrice = cp.price + ' VNĐ'; }

            contextCard.innerHTML = `
                <div class="messages-context-card__thumb"><i class="fa fa-play-circle"></i></div>
                <div class="messages-context-card__info">
                    <div class="messages-context-card__label">SẢN PHẨM TRANH CHẤP</div>
                    <div class="messages-context-card__name">${cp.name}</div>
                    <div class="messages-context-card__meta">
                        <span class="messages-context-card__price">${formattedPrice}</span>
                        <span class="messages-context-card__stock">CÒN HÀNG</span>
                    </div>
                </div>
                <i class="fa fa-chevron-right messages-context-card__chevron"></i>
            `;
            messagesContainer.appendChild(contextCard);
        }

        if (messages.length === 0) {
            const emptyEl = document.createElement('div');
            emptyEl.style.cssText = 'text-align: center; padding: 2rem; color: var(--ds-text-tertiary);';
            emptyEl.textContent = 'Chưa có tin nhắn nào.';
            messagesContainer.appendChild(emptyEl);
            return;
        }

    function escapeHtmlText(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function getSenderAvatarHtml(senderName, role, isMe) {
        let initials = senderName ? senderName.trim().split(/\s+/).map(w => w[0]).join('').substring(0, 2).toUpperCase() : '??';
        let bg = '#2563eb'; // Customer blue
        let titleRole = role || 'Khách hàng';
        let iconHtml = escapeHtmlText(initials);

        if (role) {
            if (role.includes('Cửa hàng') || role.includes('Seller') || role.includes('Shop')) {
                bg = '#d97706'; // Seller amber/orange
                titleRole = 'Cửa hàng';
            } else if (role.includes('Khách hàng') || role.includes('Customer') || role.includes('Buyer')) {
                bg = '#2563eb'; // Customer blue
                titleRole = 'Khách hàng';
            } else if (role.includes('Staff') || role.includes('Admin') || role.includes('Nhân viên')) {
                bg = '#7c3aed'; // Staff purple
                titleRole = 'Nhân viên';
                iconHtml = '<i class="fa fa-user-shield" style="font-size:11px;"></i>';
            }
        } else if (isMe) {
            bg = '#7c3aed';
            titleRole = 'Nhân viên';
            iconHtml = '<i class="fa fa-user-shield" style="font-size:11px;"></i>';
        }

        return `<div style="width: 32px; height: 32px; border-radius: 50%; background: ${bg}; color: #ffffff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 11px; flex-shrink: 0; margin-bottom: 2px;" title="${escapeHtmlText(senderName || '')} (${titleRole})">${iconHtml}</div>`;
    }

        messages.forEach(msg => {
            if (msg.message && msg.message.startsWith('Hệ thống:')) {
                const systemRow = document.createElement('div');
                systemRow.style.cssText = 'width: 100%; display: flex; justify-content: center; align-items: center; gap: 8px; margin: 12px 0; box-sizing: border-box;';
                systemRow.innerHTML = `
                    <div style="width: 26px; height: 26px; border-radius: 50%; background: #64748b; color: #fff; display: inline-flex; align-items: center; justify-content: center; font-size: 11px; flex-shrink: 0;"><i class="fa fa-cog"></i></div>
                    <div style="font-size: 12px; background: #f1f5f9; color: #334155; padding: 6px 14px; border-radius: 6px; border: 1px solid #cbd5e1; display: inline-block; font-weight: 500; text-align: left;">
                        ${escapeHtmlText(msg.message)}
                    </div>
                `;
                messagesContainer.appendChild(systemRow);
                return;
            }

            // Backend sets type='out' for staff messages, 'in' for contact messages
            const isMe = msg.type === 'out';
            const rowClass = isMe ? 'staff-chat-row--staff' : 'staff-chat-row--user';
            
            let bubbleClass = isMe ? 'staff-chat-bubble--staff' : 'staff-chat-bubble--user';
            if (!isMe && msg.role) {
                if (msg.role.includes('Cửa hàng') || msg.role.includes('Seller')) {
                    bubbleClass = 'staff-chat-bubble--seller';
                } else if (msg.role.includes('Khách hàng') || msg.role.includes('Customer') || msg.role.includes('Buyer')) {
                    bubbleClass = 'staff-chat-bubble--customer';
                }
            }
            
            const timeObj = new Date(msg.createdAt);
            const timeStr = timeObj.getHours().toString().padStart(2, '0') + ':' + timeObj.getMinutes().toString().padStart(2, '0');

            const row = document.createElement('div');
            row.className = `staff-chat-row ${rowClass}`;
            row.style.display = 'flex';
            row.style.gap = '8px';
            row.style.alignItems = 'flex-end';

            const roleLabel = msg.role ? `<span style="font-size: 10.5px; font-weight: 700; color: var(--ds-text-secondary); display: block; margin-bottom: 3px;">${escapeHtmlText(msg.senderName)} <span style="opacity:0.8; font-weight:500;">(${msg.role})</span></span>` : '';
            const avatarHtml = getSenderAvatarHtml(msg.senderName, msg.role, isMe);

            if (isMe) {
                row.innerHTML = `
                    <div class="staff-chat-bubble-wrap">
                        ${roleLabel}
                        <div class="staff-chat-bubble ${bubbleClass}">${msg.message}</div>
                        <span class="staff-chat-meta">${timeStr}</span>
                    </div>
                    ${avatarHtml}
                `;
            } else {
                row.innerHTML = `
                    ${avatarHtml}
                    <div class="staff-chat-bubble-wrap">
                        ${roleLabel}
                        <div class="staff-chat-bubble ${bubbleClass}">${msg.message}</div>
                        <span class="staff-chat-meta">${timeStr}</span>
                    </div>
                `;
            }

            messagesContainer.appendChild(row);
        });

        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    // Send Message
    chatForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const messageText = chatInput.value.trim();
        if (!messageText && !pendingAttachment || !currentUserId) return;

        chatInput.value = '';
        chatInput.disabled = true;

        let finalMessage = messageText;
        if (pendingAttachment) {
            if (pendingAttachment.type === 'image') {
                finalMessage = `<img src="${pendingAttachment.dataUrl}" style="max-width: 200px; max-height: 150px; border-radius: 8px; cursor: pointer; display: block; margin-top: 5px;" onclick="window.open(this.src)" alt="Image">`;
            } else {
                finalMessage = `
                    <div style="display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: #f1f5f9; border-radius: 8px; border: 1px solid #cbd5e1; color: #1e293b; font-size: 13px; margin-top: 5px;">
                        <i class="fa fa-file-text-o" style="font-size: 20px; color: #64748b;"></i>
                        <div style="text-align: left; flex: 1; min-width: 0;">
                            <div style="font-weight: 600; text-overflow: ellipsis; overflow: hidden; white-space: nowrap; max-width: 140px;" title="${pendingAttachment.name}">${pendingAttachment.name}</div>
                            <div style="font-size: 11px; color: #64748b;">${(pendingAttachment.size / 1024).toFixed(1)} KB</div>
                        </div>
                    </div>
                `;
            }
        }

        try {
            const newMsg = await fetchApi(`/api/v1/staff/chat/${currentUserId}`, {
                method: 'POST',
                body: JSON.stringify({ message: finalMessage })
            });

            // Clear preview
            if (typeof cancelStaffAttachment === 'function') {
                cancelStaffAttachment();
            }

            // Append directly to UI
            const timeStr = new Date().getHours().toString().padStart(2, '0') + ':' + new Date().getMinutes().toString().padStart(2, '0');
            const row = document.createElement('div');
            row.className = `staff-chat-row staff-chat-row--staff`;
            row.innerHTML = `
                <div class="staff-chat-bubble-wrap">
                    <div class="staff-chat-bubble staff-chat-bubble--staff">${newMsg.message}</div>
                    <span class="staff-chat-meta">${timeStr} <i class="fa fa-check" style="color:#f97316;"></i></span>
                </div>
            `;
            messagesContainer.appendChild(row);
            messagesContainer.scrollTop = messagesContainer.scrollHeight;

            // Reload conversations to update "last message"
            loadConversations();
        } catch (error) {
            alert('Lỗi gửi tin nhắn: ' + error.message);
        } finally {
            chatInput.disabled = false;
            chatInput.focus();
        }
    });

    // Initial Load
    loadConversations().then(() => {
        // Select initial chat from URL params if present
        const urlParams = new URLSearchParams(window.location.search);
        const complaintIdParam = urlParams.get('complaintId');
        if (complaintIdParam) {
            const cId = parseInt(complaintIdParam);
            selectUser(-cId, `Tranh chấp #CMP-${cId}`, "Đối chất khiếu nại", "Dispute", true);
        }
    });

    // Auto-polling interval for real-time messages & sidebar updates
    setInterval(() => {
        if (currentUserId) {
            // Poll active messages
            fetchApi(`/api/v1/staff/chat/${currentUserId}`)
                .then(data => {
                    const messages = Array.isArray(data) ? data : (data.messages || []);
                    renderMessages(messages, currentContextProduct);
                })
                .catch(err => console.error('Failed to poll messages:', err));
        }
        // Poll sidebar conversations list
        fetchApi('/api/v1/staff/chat/conversations')
            .then(conversations => {
                // Render sidebar silently
                renderConversations(conversations);
            })
            .catch(err => console.error('Failed to poll conversations:', err));
    }, 4000);
});

