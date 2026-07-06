document.addEventListener('DOMContentLoaded', function () {
    let currentUserId = null;
    let currentUserName = '';
    let pendingAttachment = null;

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
                item.href = '#';
                item.onclick = (e) => {
                    e.preventDefault();
                    selectUser(conv.userId, conv.userName, conv.userEmail, conv.userRole, isOnline);
                };

                item.innerHTML = `
                    <div class="staff-avatar-wrap">
                        <span class="ds-avatar ds-avatar-sm ${conv.userRole === 'Seller' ? 'ds-avatar-primary' : 'ds-avatar-success'}">${initial}</span>
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
        // (If the user is in the list, we could set is-active, but we rely on re-rendering or manual toggling)
        loadConversations(); // Reload to clear badges and set active state

        // Update Panel Header
        panelHeaderTitle.textContent = userName;
        panelHeaderSubtitle.innerHTML = `${userEmail} · ${userRole} · <span style="color: ${isOnline ? '#10b981' : '#94a3b8'}; display: inline-flex; align-items: center; gap: 4px;"><i class="fa fa-circle" style="font-size: 8px;"></i> ${isOnline ? 'Online Now' : 'Offline'}</span>`;
        panelHeaderAvatar.textContent = userName ? userName.substring(0, 2).toUpperCase() : 'NA';
        panelHeaderAvatar.className = `ds-avatar ds-avatar-sm ${userRole === 'Seller' ? 'ds-avatar-primary' : 'ds-avatar-success'}`;

        // Load Messages
        loadMessages(userId);
    }

    // Load Messages
    async function loadMessages(userId) {
        messagesContainer.innerHTML = '<div style="text-align: center; padding: 2rem; color: var(--ds-text-tertiary);">Đang tải tin nhắn...</div>';
        try {
            const messages = await fetchApi(`/api/v1/staff/chat/${userId}`);
            renderMessages(messages);
        } catch (error) {
            messagesContainer.innerHTML = `<div style="text-align: center; padding: 2rem; color: var(--ds-color-danger);">Lỗi: ${error.message}</div>`;
        }
    }

    function renderMessages(messages) {
        messagesContainer.innerHTML = '';
        if (messages.length === 0) {
            messagesContainer.innerHTML = '<div style="text-align: center; padding: 2rem; color: var(--ds-text-tertiary);">Chưa có tin nhắn nào. Bắt đầu hội thoại ngay!</div>';
            return;
        }

        messages.forEach(msg => {
            // Backend sets type='out' for staff messages, 'in' for contact messages
            const isMe = msg.type === 'out';
            const rowClass = isMe ? 'staff-chat-row--staff' : 'staff-chat-row--user';
            const bubbleClass = isMe ? 'staff-chat-bubble--staff' : 'staff-chat-bubble--user';
            
            const timeObj = new Date(msg.createdAt);
            const timeStr = timeObj.getHours().toString().padStart(2, '0') + ':' + timeObj.getMinutes().toString().padStart(2, '0');

            const row = document.createElement('div');
            row.className = `staff-chat-row ${rowClass}`;
            row.innerHTML = `
                <div class="staff-chat-bubble-wrap">
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
    loadConversations();
});
