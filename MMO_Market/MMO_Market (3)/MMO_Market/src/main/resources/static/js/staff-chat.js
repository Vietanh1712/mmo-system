document.addEventListener('DOMContentLoaded', function () {
    let currentUserId = null;
    let currentUserName = '';

    const conversationsContainer = document.querySelector('.staff-chat-list__items');
    const messagesContainer = document.getElementById('staffChatMessages');
    const chatInput = document.getElementById('staffChatInput');
    const chatForm = document.getElementById('staffChatForm');
    const searchInput = document.querySelector('.staff-chat-list__header input[type="search"]');
    const panelHeaderTitle = document.querySelector('.staff-chat-panel__header .ds-entity-title');
    const panelHeaderSubtitle = document.querySelector('.staff-chat-panel__header .ds-entity-subtitle');
    const panelHeaderAvatar = document.querySelector('.staff-chat-panel__header .ds-avatar');

    // Authentication token handling for API calls
    const token = localStorage.getItem('jwt_token') || sessionStorage.getItem('jwt_token');

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

                const item = document.createElement('a');
                item.className = `staff-chat-item ${isActive}`;
                item.href = '#';
                item.onclick = (e) => {
                    e.preventDefault();
                    selectUser(conv.userId, conv.userName, conv.userEmail, conv.userRole);
                };

                item.innerHTML = `
                    <span class="ds-avatar ds-avatar-sm ${conv.userRole === 'Seller' ? 'ds-avatar-primary' : 'ds-avatar-success'}">${initial}</span>
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

            const item = document.createElement('a');
            item.className = `staff-chat-item ${isActive}`;
            item.href = '#';
            item.onclick = (e) => {
                e.preventDefault();
                selectUser(user.userId, user.userName, user.userEmail, user.userRole);
                searchInput.value = '';
                // Optional: You could call loadConversations() here to reset the list, or keep search results
            };

            item.innerHTML = `
                <span class="ds-avatar ds-avatar-sm ${user.userRole === 'Seller' ? 'ds-avatar-primary' : 'ds-avatar-success'}">${initial}</span>
                <div style="flex: 1;">
                    <div class="ds-entity-title">${user.userName} <span style="font-size:0.75rem; color:var(--ds-text-tertiary);">(${user.userRole})</span></div>
                    <div class="ds-entity-subtitle">${user.userEmail}</div>
                </div>
            `;
            conversationsContainer.appendChild(item);
        });
    }

    // Select User
    function selectUser(userId, userName, userEmail, userRole) {
        currentUserId = userId;
        currentUserName = userName;

        // Update UI styles
        document.querySelectorAll('.staff-chat-item').forEach(el => el.classList.remove('is-active'));
        // (If the user is in the list, we could set is-active, but we rely on re-rendering or manual toggling)
        loadConversations(); // Reload to clear badges and set active state

        // Update Panel Header
        panelHeaderTitle.textContent = userName;
        panelHeaderSubtitle.textContent = `${userEmail} · ${userRole}`;
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

        // We assume the user whose ID is NOT currentUserId is the staff (us)
        // However, a better way is to check the JWT payload or simply assume senderId != currentUserId means staff
        messages.forEach(msg => {
            const isMe = msg.senderId !== currentUserId;
            const bubbleClass = isMe ? 'staff-chat-bubble--staff' : 'staff-chat-bubble--user';
            
            const div = document.createElement('div');
            div.className = `staff-chat-bubble ${bubbleClass}`;
            div.textContent = msg.message;
            
            // Add tooltip with time
            const timeStr = new Date(msg.createdAt).toLocaleString();
            div.title = timeStr;

            messagesContainer.appendChild(div);
        });

        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    // Send Message
    chatForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const message = chatInput.value.trim();
        if (!message || !currentUserId) return;

        chatInput.value = '';
        chatInput.disabled = true;

        try {
            const newMsg = await fetchApi(`/api/v1/staff/chat/${currentUserId}`, {
                method: 'POST',
                body: JSON.stringify({ message })
            });

            // Append directly to UI
            const div = document.createElement('div');
            div.className = `staff-chat-bubble staff-chat-bubble--staff`;
            div.textContent = newMsg.message;
            messagesContainer.appendChild(div);
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
