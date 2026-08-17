# Sample Invoices

Use these to test the system end to end.

| File | Type | Expected result | What it tests |
|---|---|---|---|
| `invoice_clean.pdf` | Digital PDF | ACCEPTED immediately | Happy path — all fields present, arithmetic correct |
| `invoice_line_mismatch.pdf` | Digital PDF | NEEDS_REVIEW | LINE_TOTAL_MISMATCH on 2 lines, SUBTOTAL_MISMATCH, TOTAL_RECONCILIATION |
| `invoice_missing_fields.pdf` | Digital PDF | NEEDS_REVIEW | MISSING_REQUIRED_FIELD — no vendor name, no total |
| `invoice_scanned.jpg` | Scanned image | NEEDS_REVIEW | OCR path — confidence warnings + LINE_TOTAL_MISMATCH |

**To test duplicate detection:** upload `invoice_clean.pdf` twice.
The second upload will trigger EXACT_DUPLICATE.

**To test bulk upload:** select all four files at once on the upload screen.
