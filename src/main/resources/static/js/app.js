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
        // Reload once, immediately, so the new row appears in the table as PROCESSING.
        // From there the general row poller (below) picks it up like any other in-flight
        // invoice — no further reloads, so it can't race with the user clicking elsewhere.
        window.location.href = '/?uploaded=' + encodeURIComponent(invoice.id);
    } catch (err) {
        setUploadLoading(false);
        showUploadError('Upload failed. Please try again.');
    }
}

// Polls every currently-PROCESSING row on the list page and updates it in place once its
// status changes — covers invoices still processing from an earlier visit, not just one just
// uploaded in this tab, and never reloads the page (a full-page reload here previously raced
// with in-progress navigation, e.g. clicking "View" on another row).
(function initProcessingRowPoller() {
    const pendingIds = new Set(
        Array.from(document.querySelectorAll('tr[data-status="PROCESSING"]'))
            .map((row) => row.dataset.invoiceId)
    );
    if (pendingIds.size === 0) {
        return;
    }

    const interval = setInterval(async () => {
        for (const id of Array.from(pendingIds)) {
            try {
                const response = await fetch(`/api/invoices/${id}`);
                const invoice = await response.json();
                if (invoice.status !== 'PROCESSING') {
                    pendingIds.delete(id);
                    updateInvoiceRow(id, invoice);
                }
            } catch (err) {
                // Transient network error — leave it pending and retry next tick.
            }
        }
        if (pendingIds.size === 0) {
            clearInterval(interval);
        }
    }, 3000);
})();

function updateInvoiceRow(invoiceId, invoice) {
    const row = document.getElementById('invoice-row-' + invoiceId);
    if (!row) {
        return;
    }

    row.dataset.status = invoice.status;
    row.classList.toggle('table-warning', invoice.status === 'NEEDS_REVIEW');

    setCellText(row, '.col-vendor', invoice.vendorName);
    setCellText(row, '.col-invoice-number', invoice.invoiceNumber);
    setCellText(row, '.col-date', invoice.invoiceDate);
    setCellText(row, '.col-total', invoice.totalAmount != null ? formatAmount(invoice.totalAmount) : null);
    setCellText(row, '.col-currency', invoice.currency);

    const statusBadge = row.querySelector('.col-status .badge');
    if (statusBadge) {
        statusBadge.className = 'badge status-badge-' + invoice.status;
        statusBadge.textContent = invoice.status;
    }

    const issuesCell = row.querySelector('.col-issues');
    const unresolvedCount = invoice.unresolvedFailures ? invoice.unresolvedFailures.length : 0;
    if (issuesCell) {
        issuesCell.innerHTML = '';
        const badge = document.createElement('span');
        if (unresolvedCount > 0) {
            badge.className = 'badge bg-warning text-dark';
            badge.textContent = String(unresolvedCount);
        } else {
            badge.className = 'text-muted';
            badge.textContent = '—';
        }
        issuesCell.appendChild(badge);
    }
}

function setCellText(row, selector, value) {
    const cell = row.querySelector(selector);
    if (cell) {
        cell.textContent = value ?? '—';
    }
}

function formatAmount(amount) {
    return Number(amount).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
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

    const card = document.getElementById('failure-' + failureId);
    if (card) {
        card.style.opacity = '0.55';
        card.style.borderLeftColor = '#198754';
        card.querySelectorAll('button, input').forEach((el) => {
            el.disabled = true;
        });
    }

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
