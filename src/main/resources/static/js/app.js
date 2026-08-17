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
        handleFiles(Array.from(fileInput.files));
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

// Uploads one file and returns its response, or null on failure (error already shown). Never
// navigates itself — handleFiles uploads every selected file first, then reloads once, so a
// multi-file selection doesn't have one file's success racing another's upload.
async function uploadInvoice(file) {
    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await fetch('/invoices', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const err = await response.json();
            showUploadError(`"${file.name}": ${err.message || 'Upload failed'}`);
            return null;
        }

        return await response.json();
    } catch (err) {
        showUploadError(`"${file.name}": Upload failed. Please try again.`);
        return null;
    }
}

// Validates every selected file before uploading any of them, then uploads them all and
// reloads once — the new rows show up in the "Currently processing" section from the fresh
// page load, and pollProcessingInvoices (below) picks up from there.
async function handleFiles(files) {
    if (files.length === 0) {
        return;
    }

    const oversized = files.filter((f) => f.size > MAX_UPLOAD_BYTES);
    if (oversized.length > 0) {
        showUploadError(`${oversized.map((f) => f.name).join(', ')} exceed the 10MB limit.`);
        return;
    }

    setUploadLoading(true);
    await Promise.allSettled(files.map(uploadInvoice));
    window.location.reload();
}

// Polls every currently-PROCESSING row in the main table (only visible when explicitly filtered
// to status=PROCESSING, since the list page otherwise excludes them) and updates it in place —
// never reloads, so it can't race with in-progress navigation, e.g. clicking "View" on another row.
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

// Polls the "Currently processing" widget's own items. Once every one of them has left
// PROCESSING, stashes a summary of the outcome and reloads automatically — no "refresh to
// view" prompt to click through. checkUploadSummary() (below) reads that stash back out after
// the reload and renders it as a banner.
function pollProcessingInvoices() {
    const ids = Array.from(document.querySelectorAll('[data-processing-id]'))
        .map((el) => el.dataset.processingId);
    if (ids.length === 0) {
        return;
    }

    const interval = setInterval(async () => {
        try {
            const results = await Promise.all(
                ids.map((id) =>
                    fetch(`/api/invoices/${id}`)
                        .then((r) => r.json())
                        .catch(() => ({ status: 'PROCESSING' }))
                )
            );

            const done = results.filter((i) => i.status !== 'PROCESSING');
            if (done.length === ids.length) {
                clearInterval(interval);

                const needsReview = done.filter((i) => i.status === 'NEEDS_REVIEW');
                const accepted = done.filter((i) => i.status === 'ACCEPTED');
                const failed = done.filter((i) => i.status === 'FAILED');

                sessionStorage.setItem('uploadSummary', JSON.stringify({
                    accepted: accepted.length,
                    failed: failed.length,
                    needsReview: needsReview.map((i) => ({
                        id: i.id,
                        vendorName: i.vendorName || i.originalFilename,
                        invoiceNumber: i.invoiceNumber
                    }))
                }));

                window.location.reload();
            }
        } catch (e) {
            // Transient network error — keep polling silently.
        }
    }, 3000);
}

pollProcessingInvoices();

// Renders a dismissible summary banner from the previous poll's outcome, if any — reads and
// clears sessionStorage so it only shows once, right after the reload that follows completion.
function checkUploadSummary() {
    const raw = sessionStorage.getItem('uploadSummary');
    if (!raw) {
        return;
    }
    sessionStorage.removeItem('uploadSummary');

    const s = JSON.parse(raw);
    const total = s.accepted + s.needsReview.length + s.failed;
    if (total === 0) {
        return;
    }

    let html = `<div class="alert alert-info alert-dismissible fade show mb-3" role="alert">
        <strong>Processing complete</strong> —
        ${total} invoice${total > 1 ? 's' : ''} processed.<br>`;

    if (s.accepted > 0) {
        html += `<span class="me-3 text-success">
            <i class="bi bi-check-circle-fill me-1"></i>
            ${s.accepted} accepted
        </span>`;
    }

    if (s.needsReview.length > 0) {
        const links = s.needsReview.map((i) => {
            const label = i.invoiceNumber
                ? `${i.vendorName} (${i.invoiceNumber})`
                : (i.vendorName || 'Unknown');
            return `<a href="/invoices/${i.id}" class="alert-link fw-semibold">${label}</a>`;
        }).join(', ');
        html += `<span class="me-3 text-warning">
            <i class="bi bi-exclamation-triangle-fill me-1"></i>
            ${s.needsReview.length} need${s.needsReview.length === 1 ? 's' : ''} review:
            ${links}
        </span>`;
    }

    if (s.failed > 0) {
        html += `<span class="text-danger">
            <i class="bi bi-x-circle-fill me-1"></i>
            ${s.failed} failed
        </span>`;
    }

    html += `<button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>`;

    const container = document.getElementById('invoice-list-container');
    container?.insertAdjacentHTML('beforebegin', html);
}

checkUploadSummary();

function updateInvoiceRow(invoiceId, invoice) {
    const row = document.getElementById('invoice-row-' + invoiceId);
    if (!row) {
        return;
    }

    row.dataset.status = invoice.status;
    row.classList.toggle('table-warning', invoice.status === 'NEEDS_REVIEW');

    setCellText(row, '.col-vendor', invoice.vendorName);
    setCellText(row, '.col-invoice-number', invoice.invoiceNumber);
    setCellText(row, '.col-date', invoice.invoiceDate ? formatDate(invoice.invoiceDate) : null);
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

// Matches the server-side "dd MMM yyyy" format (#temporals.format) so a row updated in place by
// the poller doesn't flip back to the raw "yyyy-MM-dd" the JSON API returns.
function formatDate(isoDate) {
    const [year, month, day] = isoDate.split('-');
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    return `${day} ${months[parseInt(month, 10) - 1]} ${year}`;
}

function formatAmount(amount) {
    return Number(amount).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
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
        handleFiles(Array.from(e.dataTransfer.files));
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
        const input = document.getElementById('correction-' + failureId);
        const newValue = input?.value?.trim();
        if (!newValue) {
            input.classList.add('is-invalid');
            return;
        }
        input.classList.remove('is-invalid');
        recordResolution(failureId, 'CORRECTED', null, null);
    });
});

// "I've fixed it" (SUBTOTAL_MISMATCH/TOTAL_RECONCILIATION/LINE_TOTAL_MISMATCH cards) starts
// disabled — there's nothing to confirm until the reviewer has actually edited one of the
// relevant fields above. The click itself is handled by the generic .resolve-btn listener
// above (it's an APPROVED resolve-btn); this only toggles the disabled state, since the
// actual edited value(s) travel separately as field corrections on Complete Review.
function wireFixedItButtons() {
    document.querySelectorAll('.ive-fixed-it-btn').forEach((btn) => {
        const scope = btn.dataset.scope;
        const lineItemId = btn.dataset.lineItemId;

        if (scope === 'INVOICE') {
            const watchFields = ['SUBTOTAL_AMOUNT', 'TAX_AMOUNT', 'TOTAL_AMOUNT'];
            watchFields.forEach((fieldName) => {
                const input = document.getElementById('field-' + fieldName);
                input?.addEventListener('input', () => {
                    const anyChanged = watchFields.some((f) => {
                        const el = document.getElementById('field-' + f);
                        return el && el.value.trim() !== (el.dataset.originalValue ?? '');
                    });
                    btn.disabled = !anyChanged;
                });
            });
        }

        if (scope === 'LINE_ITEM' && lineItemId) {
            const watchFields = ['QUANTITY', 'UNIT_PRICE', 'AMOUNT'];
            watchFields.forEach((fieldName) => {
                const input = document.querySelector(
                    `.line-item-field[data-line-item-id="${lineItemId}"][data-field="${fieldName}"]`);
                input?.addEventListener('input', () => {
                    const anyChanged = watchFields.some((f) => {
                        const el = document.querySelector(
                            `.line-item-field[data-line-item-id="${lineItemId}"][data-field="${f}"]`);
                        return el && el.value.trim() !== (el.dataset.originalValue ?? '');
                    });
                    btn.disabled = !anyChanged;
                });
            });
        }
    });
}

wireFixedItButtons();

// Scroll targets for the "fix the values in the fields panel ↑" / "fix this line item ↑" links
// on multi-field failure cards — replaces vague "above" text with something that actually takes
// the reviewer there and highlights what to look at.
function scrollToSection(id) {
    const el = document.getElementById(id);
    if (!el) {
        return;
    }
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    el.style.transition = 'box-shadow 0.3s ease';
    el.style.boxShadow = '0 0 0 2px #ffc107';
    setTimeout(() => {
        el.style.boxShadow = '';
    }, 2000);
}

function scrollToLineItem(lineItemId) {
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
    }, 2000);
}

document.querySelectorAll('.scroll-to-section-link').forEach((link) => {
    link.addEventListener('click', (e) => {
        e.preventDefault();
        scrollToSection(link.dataset.target);
    });
});

document.querySelectorAll('.scroll-to-line-link').forEach((link) => {
    link.addEventListener('click', (e) => {
        e.preventDefault();
        scrollToLineItem(link.dataset.lineItemId);
    });
});

function recordResolution(failureId, action, fieldName, lineItemId) {
    // Every card's correction, invoice-field or line-item, now comes from its own inline input —
    // the backend resolves which field a fieldless failure (e.g. LINE_TOTAL_MISMATCH) implies.
    const newValue = action === 'CORRECTED'
        ? document.getElementById('correction-' + failureId)?.value?.trim()
        : null;

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

// Lets a reviewer change their mind before submitting: clears the in-memory resolution and
// re-enables the card's inputs/buttons. Nothing has been sent to the server yet at this point
// (resolutions only go out on "Complete Review"), so this is a pure client-side undo. Delegated
// since every card has its own undo link, all present at load (not dynamically injected).
document.addEventListener('click', (e) => {
    if (!e.target.classList.contains('undo-link')) {
        return;
    }
    e.preventDefault();
    const failureId = e.target.dataset.failureId;
    delete resolutions[failureId];

    document.getElementById('resolved-indicator-' + failureId)?.classList.add('d-none');

    const card = document.getElementById('failure-' + failureId);
    if (card) {
        card.style.opacity = '';
        card.style.borderLeftColor = '';
        card.querySelectorAll('button, input').forEach((el) => {
            el.disabled = false;
        });
    }

    checkAllResolved();
});

function checkAllResolved() {
    const totalFailures = parseInt(
        document.getElementById('failure-count')?.dataset.count ?? '0'
    );
    const resolvedCount = Object.keys(resolutions).length;
    // Confirming a duplicate rejects the invoice outright regardless of any other open issue, so
    // that decision alone is enough to enable submission — no need to also resolve everything else.
    const duplicateConfirmed = Object.values(resolutions).some((r) => r.action === 'DUPLICATE_CONFIRMED');
    const submitBtn = document.getElementById('complete-review-btn');
    if (submitBtn) {
        submitBtn.disabled = !duplicateConfirmed && resolvedCount < totalFailures;
    }
}

// Collects direct edits to the extracted fields panel that weren't made via a failure card's
// own correction input — e.g. typing a corrected vendor name straight into the panel, then
// clicking "Yes, looks right" on that field's card rather than "Save". Compares each input's
// current value against the value it was rendered with (data-original-value) and only sends
// what actually changed.
function collectFieldCorrections() {
    const corrections = [];

    document.querySelectorAll('.invoice-field[data-field]').forEach((input) => {
        const orig = input.dataset.originalValue ?? '';
        const curr = input.value?.trim() ?? '';
        if (curr !== orig && curr !== '') {
            corrections.push({ fieldName: input.dataset.field, lineItemId: null, newValue: curr });
        }
    });

    document.querySelectorAll('.line-item-field[data-field]').forEach((input) => {
        const orig = input.dataset.originalValue ?? '';
        const curr = input.value?.trim() ?? '';
        if (curr !== orig && curr !== '') {
            corrections.push({ fieldName: input.dataset.field, lineItemId: input.dataset.lineItemId, newValue: curr });
        }
    });

    return corrections;
}

document.getElementById('complete-review-btn')
    ?.addEventListener('click', async () => {
        const invoiceId = document.getElementById('invoice-id')?.value;
        const payload = {
            resolutions: Object.values(resolutions),
            fieldCorrections: collectFieldCorrections()
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
