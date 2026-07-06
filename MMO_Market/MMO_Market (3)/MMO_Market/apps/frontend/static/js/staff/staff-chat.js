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

    window.toggleStaffMute = function(event) {
        event.preventDefault();
        alert('Đã thay đổi trạng thái tắt/bật thông báo (giả lập).');
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
                if (conv.userRole === 'Seller') {
                    avatarClass = 'ds-avatar-primary';
                } else if (conv.userRole === 'Dispute') {
                    avatarClass = 'ds-avatar-danger';
                }

                item.innerHTML = `
                    <div class="staff-avatar-wrap">
                        <span class="ds-avatar ds-avatar-sm ${avatarClass}">${conv.userRole === 'Dispute' ? 'TC' : initial}</span>
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
        currentUserId = userId;
        currentUserName = userName;

        // Update UI styles
        document.querySelectorAll('.staff-chat-item').forEach(el => el.classList.remove('is-active'));
        
        // Update Panel Header
        panelHeaderTitle.textContent = userName;
        if (userId < 0) {
            panelHeaderSubtitle.innerHTML = `${userEmail} · ${userRole}`;
            panelHeaderAvatar.textContent = 'TC';
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

        messages.forEach(msg => {
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

            const roleLabel = msg.role ? `<span style="font-size: 10px; font-weight: bold; color: var(--ds-text-secondary); display: block; margin-bottom: 2px;">${msg.senderName} (${msg.role})</span>` : '';

            row.innerHTML = `
                <div class="staff-chat-bubble-wrap">
                    ${roleLabel}
                    <div class="staff-chat-bubble ${bubbleClass}">${msg.message}</div>
                    <span class="staff-chat-meta">${timeStr}</span>
                </div>
            `;

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

