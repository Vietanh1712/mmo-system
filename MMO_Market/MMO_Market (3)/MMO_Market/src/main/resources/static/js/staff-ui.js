(function () {
    function showToast(message, type) {
        let container = document.getElementById('staffToastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'staffToastContainer';
            container.className = 'ds-toast-container';
            document.body.appendChild(container);
        }

        const toastClass = type === 'danger' ? 'ds-toast-error' : 'ds-toast-success';
        const title = type === 'danger' ? 'Thông báo' : 'Thành công';

        const toast = document.createElement('div');
        toast.className = 'ds-toast ' + toastClass;
        toast.innerHTML =
            '<div><p class="ds-toast-title">' + title + '</p>' +
            '<p class="ds-toast-message">' + message + '</p></div>' +
            '<button class="ds-toast-close" type="button" aria-label="Đóng">×</button>';

        toast.querySelector('.ds-toast-close').addEventListener('click', function () {
            toast.remove();
        });

        container.appendChild(toast);

        setTimeout(function () {
            toast.remove();
            if (!container.children.length) {
                container.remove();
            }
        }, 3200);
    }

    function bindActionButtons() {
        document.querySelectorAll('[data-staff-action]').forEach(function (button) {
            button.addEventListener('click', function () {
                const action = button.getAttribute('data-staff-action');
                const label = button.getAttribute('data-staff-label') || 'Thao tác';
                const type = action === 'reject' || action === 'delete' ? 'danger' : 'success';
                showToast(label + ' (demo frontend — chưa kết nối backend)', type);
            });
        });
    }

    function bindChatCompose() {
        const form = document.getElementById('staffChatForm');
        if (!form) return;

        form.addEventListener('submit', function (event) {
            event.preventDefault();
            const input = document.getElementById('staffChatInput');
            const messages = document.getElementById('staffChatMessages');
            if (!input || !messages || !input.value.trim()) return;

            const bubble = document.createElement('div');
            bubble.className = 'staff-chat-bubble staff-chat-bubble--staff';
            bubble.textContent = input.value.trim();
            messages.appendChild(bubble);
            messages.scrollTop = messages.scrollHeight;
            input.value = '';
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        bindActionButtons();
        bindChatCompose();
    });
})();
