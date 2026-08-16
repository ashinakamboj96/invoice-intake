const MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

function showAlert(type, message) {
    const container = document.getElementById('upload-alerts');
    if (!container) {
        return;
    }
    const alert = document.createElement('div');
    alert.className = `alert alert-${type} alert-dismissible fade show`;
    alert.setAttribute('role', 'alert');
    alert.textContent = message;

    const closeBtn = document.createElement('button');
    closeBtn.type = 'button';
    closeBtn.className = 'btn-close';
    closeBtn.setAttribute('data-bs-dismiss', 'alert');
    closeBtn.setAttribute('aria-label', 'Close');
    alert.appendChild(closeBtn);

    container.prepend(alert);
}

function showUploadError(message) {
    showAlert('danger', message);
}

function showProcessingBanner(invoiceId) {
    showAlert('info', `Invoice uploaded — processing… (ID: ${invoiceId})`);
}

// Original dropzone markup, captured once at load so it can be restored after an upload error
// without a full page reload (which would also wipe the error alert before it's readable).
const dropzoneOriginalHtml = document.getElementById('dropzone')?.innerHTML ?? null;

function wireUploadControls() {
    const fileInput = document.getElementById('file-input');
    const uploadBtn = document.getElementById('upload-btn');
    if (!fileInput) {
        return;
    }

    uploadBtn?.addEventListener('click', (e) => {
        e.stopPropagation();
        fileInput.click();
    });

    fileInput.addEventListener('change', () => {
        if (fileInput.files.length > 0) {
            handleSelectedFile(fileInput.files[0]);
        }
        fileInput.value = '';
    });
}

function setUploadLoading(loading) {
    const dropzone = document.getElementById('dropzone');
    if (!dropzone) {
        return;
    }

    if (loading) {
        dropzone.innerHTML = `
            <div class="spinner-border text-primary mb-2" role="status"></div>
            <p class="text-muted mb-0">Processing your invoice...</p>`;
    } else if (dropzoneOriginalHtml != null) {
        dropzone.innerHTML = dropzoneOriginalHtml;
        wireUploadControls();
    }
}

async function uploadInvoice(file) {
    setUploadLoading(true);

    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await fetch('/invoices', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const err = await response.json();
            setUploadLoading(false);
            showUploadError(err.message || 'Upload failed');
            return;
        }

        const invoice = await response.json();
        showProcessingBanner(invoice.id);
        pollStatus(invoice.id);
    } catch (err) {
        setUploadLoading(false);
        showUploadError('Upload failed. Please try again.');
    }
}

function pollStatus(invoiceId) {
    const interval = setInterval(async () => {
        const response = await fetch(`/api/invoices/${invoiceId}`);
        const invoice = await response.json();

        if (invoice.status !== 'PROCESSING') {
            clearInterval(interval);
            window.location.reload();
        }
    }, 2000);
}

function handleSelectedFile(file) {
    if (!file) {
        return;
    }
    if (file.size > MAX_UPLOAD_BYTES) {
        showUploadError(`"${file.name}" is ${(file.size / (1024 * 1024)).toFixed(1)}MB — files must be 10MB or smaller.`);
        return;
    }
    uploadInvoice(file);
}

(function initUploadArea() {
    const dropzone = document.getElementById('dropzone');
    if (!dropzone) {
        return;
    }

    wireUploadControls();

    dropzone.addEventListener('click', () => document.getElementById('file-input')?.click());

    ['dragover', 'dragenter'].forEach((evt) => {
        dropzone.addEventListener(evt, (e) => {
            e.preventDefault();
            dropzone.classList.add('dragover');
        });
    });

    ['dragleave', 'drop'].forEach((evt) => {
        dropzone.addEventListener(evt, (e) => {
            e.preventDefault();
            dropzone.classList.remove('dragover');
        });
    });

    dropzone.addEventListener('drop', (e) => {
        const file = e.dataTransfer.files[0];
        if (file) {
            handleSelectedFile(file);
        }
    });
})();

// ---- Accepted toast ----
(function initAcceptedToast() {
    const toastEl = document.getElementById('acceptedToast');
    if (toastEl) {
        const toast = new bootstrap.Toast(toastEl, { delay: 4000 });
        toast.show();
    }
})();

// ---- Needs-review screen: track resolutions in memory, keyed by failureId ----
const resolutions = {};

document.querySelectorAll('.resolve-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
        const failureId = btn.dataset.failureId;
        const action = btn.dataset.action;
        const fieldName = btn.dataset.fieldName || null;
        const lineItemId = btn.dataset.lineItemId || null;
        recordResolution(failureId, action, fieldName, lineItemId);
    });
});

document.querySelectorAll('.correct-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
        const failureId = btn.dataset.failureId;
        const lineItemId = btn.dataset.lineItemId || null;
        const input = document.getElementById('correction-' + failureId);
        const newValue = input?.value?.trim();
        if (!newValue) {
            input.classList.add('is-invalid');
            return;
        }
        input.classList.remove('is-invalid');
        recordResolution(failureId, 'CORRECTED', null, lineItemId);
    });
});

function recordResolution(failureId, action, fieldName, lineItemId) {
    let newValue = null;

    if (action === 'CORRECTED') {
        if (fieldName) {
            // Invoice-level field — read the current value straight from the field input.
            newValue = document.getElementById('field-' + fieldName)?.value?.trim();
            if (!newValue) {
                showFieldError(fieldName, 'Please enter a value before marking as fixed.');
                return;
            }
        } else if (lineItemId) {
            // Line item field — value comes from the card's own correction input.
            newValue = document.getElementById('correction-' + failureId)?.value?.trim();
        }
    }

    resolutions[failureId] = { failureId, action, newValue };

    const indicator = document.getElementById('resolved-indicator-' + failureId);
    const text = indicator?.querySelector('.resolution-text');
    if (text) {
        text.textContent = action === 'CORRECTED'
            ? `Will be corrected to: "${newValue}"`
            : action === 'APPROVED' ? 'Accepted as correct'
            : action === 'DUPLICATE_DISMISSED' ? 'Marked as not a duplicate'
            : 'Confirmed as duplicate';
    }
    indicator?.classList.remove('d-none');

    document.querySelectorAll(`[data-failure-id="${failureId}"]`).forEach((el) => {
        if (el.tagName === 'BUTTON') el.disabled = true;
        if (el.tagName === 'INPUT') el.disabled = true;
    });

    checkAllResolved();
}

function showFieldError(fieldName, message) {
    const input = document.getElementById('field-' + fieldName);
    input?.classList.add('is-invalid');
    const feedback = input?.parentElement?.querySelector('.invalid-feedback');
    if (feedback) {
        feedback.textContent = message;
    }
    setTimeout(() => input?.classList.remove('is-invalid'), 3000);
}

document.querySelectorAll('.go-to-field-link').forEach((link) => {
    link.addEventListener('click', (e) => {
        e.preventDefault();
        highlightField(link.dataset.fieldName);
    });
});

document.querySelectorAll('.go-to-line-item-link').forEach((link) => {
    link.addEventListener('click', (e) => {
        e.preventDefault();
        highlightLineItem(link.dataset.lineItemId);
    });
});

function highlightField(fieldName) {
    const input = document.getElementById('field-' + fieldName);
    if (!input) {
        return;
    }
    input.scrollIntoView({ behavior: 'smooth', block: 'center' });
    input.classList.add('border-warning', 'border-2');
    input.focus();
    setTimeout(() => input.classList.remove('border-warning', 'border-2'), 3000);
}

function highlightLineItem(lineItemId) {
    const row = document.getElementById('line-item-' + lineItemId);
    if (!row) {
        return;
    }
    row.scrollIntoView({ behavior: 'smooth', block: 'center' });
    row.querySelectorAll('input').forEach((inp) => {
        inp.classList.add('border-warning', 'border-2');
    });
    setTimeout(() => {
        row.querySelectorAll('input').forEach((inp) =>
            inp.classList.remove('border-warning', 'border-2'));
    }, 3000);
}

function checkAllResolved() {
    const totalFailures = parseInt(
        document.getElementById('failure-count')?.dataset.count ?? '0'
    );
    const resolvedCount = Object.keys(resolutions).length;
    const submitBtn = document.getElementById('complete-review-btn');
    if (submitBtn) {
        submitBtn.disabled = resolvedCount < totalFailures;
    }
}

document.getElementById('complete-review-btn')
    ?.addEventListener('click', async () => {
        const invoiceId = document.getElementById('invoice-id')?.value;
        const payload = {
            resolutions: Object.values(resolutions)
        };

        const submitBtn = document.getElementById('complete-review-btn');
        submitBtn.disabled = true;
        submitBtn.textContent = 'Submitting...';

        try {
            const response = await fetch(`/invoices/${invoiceId}/complete-review`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();

            if (result.status === 'ACCEPTED') {
                window.location.href = `/invoices/${invoiceId}?accepted=true`;
            } else if (result.status === 'REJECTED') {
                window.location.href = `/invoices/${invoiceId}`;
            } else {
                // NEEDS_REVIEW — new failures found
                window.location.reload();
            }
        } catch (err) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="bi bi-check2-all me-2"></i>Complete Review';
            alert('Submission failed. Please try again.');
        }
    });
