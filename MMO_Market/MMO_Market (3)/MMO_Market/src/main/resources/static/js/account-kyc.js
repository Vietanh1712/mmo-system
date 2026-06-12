const KYC_STORAGE_KEY = 'mmoMarketKycMock';
const DEFAULT_KYC_STATE = { status: 'NOT_SUBMITTED' };

let accountSidebar = null;
let currentProfile = null;
let currentKyc = DEFAULT_KYC_STATE;

document.addEventListener('DOMContentLoaded', initializeKycPage);

function initializeKycPage() {
    accountSidebar = new AccountSidebar();
    bindKycEvents();
    loadKycPage();
}

function bindKycEvents() {
    document.getElementById('kycPrimaryAction').addEventListener('click', openKycFormMode);
    document.getElementById('kycCancelButton').addEventListener('click', closeKycFormMode);
    document.getElementById('kycForm').addEventListener('submit', submitKycForm);

    bindFileNamePreview('kycFrontFile', 'front');
    bindFileNamePreview('kycBackFile', 'back');
    bindFileNamePreview('kycSelfieFile', 'selfie');
}

async function loadKycPage() {
    const token = sessionStorage.getItem('accessToken');
    if (!token || token === 'null' || token === 'undefined') {
        window.location.href = '/login';
        return;
    }

    try {
        const response = await authFetch('/v1/profile');
        if (!response.ok) {
            throw new Error('Không thể tải thông tin tài khoản.');
        }

        currentProfile = await response.json();
        accountSidebar.render(currentProfile);
        currentKyc = readKycState();
        renderKycState();

        if (isKycFormMode()) {
            openKycFormMode();
        }
    } catch (error) {
        showKycMessage(error.message || 'Không thể tải màn định danh.', 'danger');
    }
}

function readKycState() {
    try {
        return JSON.parse(sessionStorage.getItem(KYC_STORAGE_KEY)) || DEFAULT_KYC_STATE;
    } catch {
        return DEFAULT_KYC_STATE;
    }
}

function saveKycState(nextState) {
    currentKyc = nextState;
    sessionStorage.setItem(KYC_STORAGE_KEY, JSON.stringify(nextState));
}

function renderKycState() {
    const statusConfig = getKycStatusConfig(currentKyc.status);
    const badge = document.getElementById('kycStatusBadge');
    badge.textContent = statusConfig.label;
    badge.className = `ds-badge ${statusConfig.badgeClass}`;

    document.getElementById('kycSummaryTitle').textContent = statusConfig.title;
    document.getElementById('kycSummaryDescription').textContent = statusConfig.description;

    renderKycMeta();
    renderKycPrimaryAction();
    renderKycSteps(currentKyc.status);
    showKycMessage(statusConfig.message, statusConfig.messageType);
}

function renderKycMeta() {
    const meta = document.getElementById('kycMeta');
    const hasRequest = Boolean(currentKyc.requestCode);
    meta.hidden = !hasRequest;

    if (!hasRequest) {
        return;
    }

    document.getElementById('kycRequestCode').textContent = currentKyc.requestCode;
    document.getElementById('kycSubmittedAt').textContent = currentKyc.submittedAt || '-';
    document.getElementById('kycDocumentType').textContent = currentKyc.documentType || '-';

    const rejectRow = document.getElementById('kycRejectReasonRow');
    rejectRow.hidden = currentKyc.status !== 'REJECTED';
    document.getElementById('kycRejectReason').textContent =
        currentKyc.rejectReason || 'Ảnh giấy tờ chưa rõ. Vui lòng gửi lại hồ sơ.';
}

function renderKycPrimaryAction() {
    const action = document.getElementById('kycPrimaryAction');
    action.hidden = false;
    action.className = 'ds-btn ds-btn-primary';

    if (currentKyc.status === 'APPROVED') {
        action.textContent = 'Đi tới ví';
        action.href = '/wallet';
        action.className = 'ds-btn ds-btn-outline';
        return;
    }

    if (currentKyc.status === 'PENDING') {
        action.hidden = true;
        return;
    }

    if (currentKyc.status === 'REJECTED') {
        action.textContent = 'Gửi lại hồ sơ';
        action.href = '/account/kyc?mode=resubmit';
        return;
    }

    action.textContent = 'Gửi yêu cầu định danh';
    action.href = '/account/kyc?mode=submit';
}

function renderKycSteps(status) {
    document.querySelectorAll('[data-kyc-step]').forEach(step => {
        step.classList.remove('kyc-step--active', 'kyc-step--success', 'kyc-step--danger');
    });

    if (status === 'APPROVED') {
        setStepClass(['prepare', 'submit', 'review', 'done'], 'kyc-step--success');
        return;
    }

    if (status === 'PENDING') {
        setStepClass(['prepare', 'submit'], 'kyc-step--success');
        setStepClass(['review'], 'kyc-step--active');
        return;
    }

    if (status === 'REJECTED') {
        setStepClass(['prepare', 'submit'], 'kyc-step--success');
        setStepClass(['review'], 'kyc-step--danger');
        return;
    }

    setStepClass(['prepare'], 'kyc-step--active');
}

function setStepClass(stepNames, className) {
    stepNames.forEach(stepName => {
        const step = document.querySelector(`[data-kyc-step="${stepName}"]`);
        if (step) {
            step.classList.add(className);
        }
    });
}

function openKycFormMode(event) {
    if (event) {
        event.preventDefault();
    }

    if (currentKyc.status === 'PENDING' || currentKyc.status === 'APPROVED') {
        return;
    }

    const mode = currentKyc.status === 'REJECTED' || getKycMode() === 'resubmit' ? 'resubmit' : 'submit';
    document.getElementById('kycFormTitle').textContent =
        mode === 'resubmit' ? 'Gửi lại hồ sơ định danh' : 'Gửi yêu cầu định danh';
    document.getElementById('kycSubmitButton').textContent =
        mode === 'resubmit' ? 'Gửi lại hồ sơ' : 'Gửi yêu cầu';

    prefillKycForm();
    clearKycFormErrors();
    document.getElementById('kycForm').hidden = false;
    setKycModeInUrl(mode);
    document.getElementById('kycForm').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function closeKycFormMode(event) {
    if (event) {
        event.preventDefault();
    }

    document.getElementById('kycForm').hidden = true;
    clearKycFormErrors();
    setKycModeInUrl('view');
}

function prefillKycForm() {
    document.getElementById('kycFullName').value = currentKyc.fullName || currentProfile?.fullName || '';
    document.getElementById('kycDocumentNumber').value = currentKyc.documentNumber || '';
    document.getElementById('kycDateOfBirth').value = currentKyc.dateOfBirth || '';
    document.getElementById('kycDocumentTypeInput').value = currentKyc.documentType || '';
    document.getElementById('kycAddress').value = currentKyc.address || '';
    document.getElementById('kycConfirm').checked = false;
}

function submitKycForm(event) {
    event.preventDefault();

    if (!validateKycForm()) {
        return;
    }

    const submittedAt = new Intl.DateTimeFormat('vi-VN', {
        dateStyle: 'short',
        timeStyle: 'short'
    }).format(new Date());

    saveKycState({
        status: 'PENDING',
        requestCode: currentKyc.requestCode || `KYC-${Date.now().toString().slice(-6)}`,
        submittedAt,
        fullName: document.getElementById('kycFullName').value.trim(),
        documentNumber: document.getElementById('kycDocumentNumber').value.trim(),
        dateOfBirth: document.getElementById('kycDateOfBirth').value,
        documentType: document.getElementById('kycDocumentTypeInput').value,
        address: document.getElementById('kycAddress').value.trim()
    });

    closeKycFormMode();
    renderKycState();
}

function validateKycForm() {
    clearKycFormErrors();
    let valid = true;

    const fullName = document.getElementById('kycFullName').value.trim();
    const documentNumber = document.getElementById('kycDocumentNumber').value.trim();
    const dateOfBirth = document.getElementById('kycDateOfBirth').value;
    const documentType = document.getElementById('kycDocumentTypeInput').value;
    const address = document.getElementById('kycAddress').value.trim();
    const confirmed = document.getElementById('kycConfirm').checked;

    if (fullName.length < 3) {
        setText('kycFullNameError', 'Họ và tên phải có ít nhất 3 ký tự.');
        valid = false;
    }

    if (!/^[A-Za-z0-9]{6,20}$/.test(documentNumber)) {
        setText('kycDocumentNumberError', 'Số giấy tờ phải gồm 6-20 chữ hoặc số.');
        valid = false;
    }

    if (!dateOfBirth) {
        setText('kycDateOfBirthError', 'Vui lòng chọn ngày sinh.');
        valid = false;
    }

    if (!documentType) {
        setText('kycDocumentTypeError', 'Vui lòng chọn loại giấy tờ.');
        valid = false;
    }

    if (address.length < 10) {
        setText('kycAddressError', 'Địa chỉ thường trú phải có ít nhất 10 ký tự.');
        valid = false;
    }

    if (!hasSelectedFiles()) {
        setText('kycFilesError', 'Vui lòng chọn đủ ảnh mặt trước, mặt sau và ảnh chân dung.');
        valid = false;
    }

    if (!confirmed) {
        setText('kycConfirmError', 'Bạn cần xác nhận thông tin trước khi gửi.');
        valid = false;
    }

    if (!valid) {
        showKycFormMessage('Vui lòng kiểm tra lại thông tin định danh.');
    }

    return valid;
}

function hasSelectedFiles() {
    return Boolean(
        document.getElementById('kycFrontFile').files.length &&
        document.getElementById('kycBackFile').files.length &&
        document.getElementById('kycSelfieFile').files.length
    );
}

function clearKycFormErrors() {
    [
        'kycFullNameError',
        'kycDocumentNumberError',
        'kycDateOfBirthError',
        'kycDocumentTypeError',
        'kycAddressError',
        'kycFilesError',
        'kycConfirmError'
    ].forEach(id => setText(id, ''));
    document.getElementById('kycFormMessage').hidden = true;
}

function showKycFormMessage(message) {
    const formMessage = document.getElementById('kycFormMessage');
    formMessage.textContent = message;
    formMessage.hidden = false;
}

function showKycMessage(message, type) {
    const messageElement = document.getElementById('kycMessage');
    messageElement.textContent = message;
    messageElement.hidden = false;
    messageElement.classList.remove('ds-alert-info', 'ds-alert-warning', 'ds-alert-danger', 'ds-alert-success');
    messageElement.classList.add(`ds-alert-${type}`);
}

function bindFileNamePreview(inputId, targetName) {
    document.getElementById(inputId).addEventListener('change', event => {
        const file = event.target.files[0];
        const label = document.querySelector(`[data-kyc-file-name="${targetName}"]`);
        label.textContent = file ? file.name : 'PNG, JPG hoặc PDF';
    });
}

function getKycStatusConfig(status) {
    const config = {
        NOT_SUBMITTED: {
            label: 'Chưa định danh',
            badgeClass: 'ds-badge-muted',
            title: 'Bạn chưa gửi hồ sơ KYC',
            description: 'Gửi hồ sơ định danh để hệ thống có thể xác minh tài khoản của bạn.',
            message: 'KYC giúp tăng độ tin cậy cho tài khoản và chuẩn bị cho các tính năng ví nâng cao.',
            messageType: 'info'
        },
        PENDING: {
            label: 'Đang chờ duyệt',
            badgeClass: 'ds-badge-warning',
            title: 'Hồ sơ của bạn đang được xét duyệt',
            description: 'Bạn không cần gửi lại hồ sơ trong khi staff đang xử lý.',
            message: 'Hồ sơ đã được gửi. Vui lòng chờ staff kiểm tra và cập nhật kết quả.',
            messageType: 'warning'
        },
        APPROVED: {
            label: 'Đã xác minh',
            badgeClass: 'ds-badge-success',
            title: 'Tài khoản đã được xác minh',
            description: 'Thông tin định danh của bạn đã được staff phê duyệt.',
            message: 'KYC đã hoàn tất. Bạn có thể tiếp tục sử dụng các tính năng tài khoản.',
            messageType: 'success'
        },
        REJECTED: {
            label: 'Bị từ chối',
            badgeClass: 'ds-badge-danger',
            title: 'Hồ sơ KYC bị từ chối',
            description: 'Vui lòng xem lý do từ chối và gửi lại hồ sơ chính xác hơn.',
            message: 'Hồ sơ chưa đạt yêu cầu. Bạn có thể gửi lại hồ sơ sau khi chỉnh thông tin.',
            messageType: 'danger'
        }
    };

    return config[status] || config.NOT_SUBMITTED;
}

function getKycMode() {
    return new URLSearchParams(window.location.search).get('mode');
}

function isKycFormMode() {
    const mode = getKycMode();
    return mode === 'submit' || mode === 'resubmit';
}

function setKycModeInUrl(mode) {
    const nextUrl = mode === 'view' ? '/account/kyc' : `/account/kyc?mode=${mode}`;
    window.history.pushState({ kycMode: mode }, '', nextUrl);
}

function setText(id, value) {
    document.getElementById(id).textContent = value;
}
