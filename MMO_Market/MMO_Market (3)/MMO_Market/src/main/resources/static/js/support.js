let currentFaqFilter = 'all';

// FAQ Accordion
document.querySelectorAll('.support-faq-item__trigger').forEach(trigger => {
    trigger.addEventListener('click', () => {
        const item = trigger.closest('.support-faq-item');
        const isOpen = item.classList.contains('is-open');

        document.querySelectorAll('.support-faq-item.is-open').forEach(openItem => {
            openItem.classList.remove('is-open');
            openItem.querySelector('.support-faq-item__trigger').setAttribute('aria-expanded', 'false');
        });

        if (!isOpen) {
            item.classList.add('is-open');
            trigger.setAttribute('aria-expanded', 'true');
        }
    });
});

function setActiveFaqTab(filter) {
    document.querySelectorAll('.support-faq-tab').forEach(tab => {
        tab.classList.toggle('is-active', tab.getAttribute('data-filter') === filter);
    });
}

function setActiveTopicCard(filter) {
    document.querySelectorAll('.support-topic-card[data-topic]').forEach(card => {
        card.classList.toggle('is-active', card.getAttribute('data-topic') === filter);
    });
}

function applyFaqGroupFilter() {
    document.querySelectorAll('.support-faq-group').forEach(group => {
        const groupName = group.getAttribute('data-group');
        if (currentFaqFilter === 'all') {
            group.classList.remove('is-hidden');
        } else {
            group.classList.toggle('is-hidden', groupName !== currentFaqFilter);
        }
    });

    document.querySelectorAll('.support-faq-item').forEach(item => {
        if (item.style.display === 'none' && item.dataset.searchHidden === '1') return;
        if (currentFaqFilter === 'all') {
            if (item.dataset.searchHidden !== '1') item.style.display = '';
        } else {
            if (item.dataset.searchHidden !== '1') {
                item.style.display = item.getAttribute('data-group') === currentFaqFilter ? '' : 'none';
            }
        }
    });
}

function filterFaqGroup(group) {
    currentFaqFilter = group;
    document.getElementById('support-search-input').value = '';

    document.querySelectorAll('.support-faq-item').forEach(item => {
        item.dataset.searchHidden = '0';
        item.style.display = '';
    });

    setActiveFaqTab(group);
    if (group === 'buyer' || group === 'seller') {
        setActiveTopicCard(group);
    } else {
        document.querySelectorAll('.support-topic-card[data-topic]').forEach(c => c.classList.remove('is-active'));
    }

    applyFaqGroupFilter();

    document.getElementById('faq').scrollIntoView({ behavior: 'smooth' });

    const targetGroup = document.querySelector('.support-faq-group[data-group="' + group + '"]');
    if (targetGroup && group !== 'all') {
        setTimeout(() => targetGroup.scrollIntoView({ behavior: 'smooth', block: 'start' }), 200);
    }
}

function resetFaqSearch() {
    document.querySelectorAll('.support-faq-item').forEach(item => {
        item.dataset.searchHidden = '0';
        item.style.display = '';
    });
    applyFaqGroupFilter();
}

function handleSupportSearch(e) {
    e.preventDefault();
    const q = document.getElementById('support-search-input').value.trim().toLowerCase();
    if (!q) {
        resetFaqSearch();
        return;
    }

    currentFaqFilter = 'all';
    setActiveFaqTab('all');
    document.querySelectorAll('.support-topic-card[data-topic]').forEach(c => c.classList.remove('is-active'));

    document.querySelectorAll('.support-faq-group').forEach(g => g.classList.remove('is-hidden'));

    const items = document.querySelectorAll('.support-faq-item');
    let firstMatch = null;

    items.forEach(item => {
        const keywords = (item.getAttribute('data-keywords') || '').toLowerCase();
        const question = item.querySelector('.support-faq-item__question').innerText.toLowerCase();
        const answer = item.querySelector('.support-faq-item__answer').innerText.toLowerCase();
        const match = keywords.includes(q) || question.includes(q) || answer.includes(q);
        item.style.display = match ? '' : 'none';
        item.dataset.searchHidden = match ? '0' : '1';
        if (match && !firstMatch) firstMatch = item;
    });

    document.getElementById('faq').scrollIntoView({ behavior: 'smooth' });

    if (firstMatch) {
        document.querySelectorAll('.support-faq-item.is-open').forEach(el => {
            el.classList.remove('is-open');
            el.querySelector('.support-faq-item__trigger').setAttribute('aria-expanded', 'false');
        });
        firstMatch.classList.add('is-open');
        firstMatch.querySelector('.support-faq-item__trigger').setAttribute('aria-expanded', 'true');
    }
}

function scrollToFaq(keyword) {
    filterFaqGroup('all');
    document.getElementById('support-search-input').value = keyword;
    handleSupportSearch({ preventDefault: () => {} });
}

document.querySelectorAll('.support-hero__quick-links a[data-faq-query]').forEach(link => {
    link.addEventListener('click', (e) => {
        e.preventDefault();
        const q = link.getAttribute('data-faq-query');
        scrollToFaq(q);
    });
});

document.getElementById('support-search-input').addEventListener('input', function () {
    if (!this.value.trim()) resetFaqSearch();
});

function getUserEmail() {
    try {
        const userStr = sessionStorage.getItem('userInfo') || sessionStorage.getItem('user');
        if (userStr) {
            const user = JSON.parse(userStr);
            return user ? user.email : 'guest';
        }
    } catch(e) {}
    return 'guest';
}

function openTicketForm() {
    const token = sessionStorage.getItem('accessToken');
    if (!token) {
        showWarningToast('Vui lòng đăng nhập để gửi ticket hỗ trợ!');
        window.location.href = '/login?returnUrl=/support';
        return;
    }
    const modal = document.getElementById('support-ticket-modal');
    modal.classList.add('is-open');
    modal.setAttribute('aria-hidden', 'false');
    document.body.style.overflow = 'hidden';
}

function closeTicketForm() {
    const modal = document.getElementById('support-ticket-modal');
    modal.classList.remove('is-open');
    modal.setAttribute('aria-hidden', 'true');
    document.body.style.overflow = '';
}

async function submitTicketForm(e) {
    e.preventDefault();
    const category = document.getElementById('ticket-category').value;
    const subject = document.getElementById('ticket-subject').value.trim();
    const detail = document.getElementById('ticket-detail').value.trim();

    if (!subject || !detail) return;

    try {
        const res = await authFetch('/support-tickets', {
            method: 'POST',
            body: JSON.stringify({
                category: category,
                title: subject,
                description: detail
            })
        });

        if (!res.ok) {
            const data = await res.json();
            throw new Error(data.message || 'Lỗi gửi ticket');
        }

        closeTicketForm();
        document.getElementById('support-ticket-form').reset();
        
        showSuccessToast('Gửi ticket hỗ trợ thành công! Ticket của bạn đã được đưa lên hệ thống xử lý của Staff.');
        window.location.href = '/account/tickets';
    } catch(err) {
        console.error(err);
        showErrorToast('Không thể gửi ticket hỗ trợ: ' + err.message);
    }
}

function openLiveChat() {
    window.location.href = '/messages?sellerId=35';
}

// Mở FAQ khi vào từ menu "Câu hỏi thường gặp"
if (window.location.hash === '#faq') {
    setTimeout(() => {
        document.getElementById('faq').scrollIntoView({ behavior: 'smooth' });
    }, 300);
}
