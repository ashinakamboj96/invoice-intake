# decisions.md

A running log of meaningful engineering decisions made during this
take-home. Each entry captures what was chosen, what was rejected,
why, and what was deliberately cut.

---

## Problem framing

**Decision:** Interpret "turn messy documents into structured,
queryable data" as an invoice-intelligence product, not a
generic document pipeline.

**Alternatives considered:** generic document converter
(any document type → any schema), contract parser, receipt tracker.

**Reasoning:** Invoices are a concrete, well-understood domain with
real messiness (inconsistent layouts, scanned photos, arithmetic
errors, duplicates). Narrowing to invoices allowed us to build a
product with a clear user (a finance/ops person) and a clear value
proposition: not just extraction, but trustworthy extraction.
A generic pipeline would produce more code and less product thinking.

---

## Core product idea: trust, not just extraction

**Decision:** The product's differentiator is making uncertainty
visible, not hiding it. Every extracted value has evidence. Every
failed check has a reason. Humans make the final call.

**Reasoning:** Any LLM can produce JSON from a PDF in an afternoon.
The hard problem is knowing which parts of that JSON to trust.
We solve this by separating reading (OCR), understanding (LLM),
and validation (deterministic code) into three distinct layers,
each with a clear responsibility.

---

## Extraction pipeline: PDF text layer → OCR fallback

**Decision:** Try PDFBox embedded text extraction first. Fall back
to Tesseract OCR only when the stripped extracted text length is
below `validation.min-text-length` (50 characters).

**Alternatives considered:** always use OCR (simpler, uniform),
vision LLM reading the image directly (one API call).

**Reasoning:** Digital PDFs have an embedded text layer that is
essentially 100% accurate — running OCR on them wastes time and
introduces unnecessary uncertainty. The 50-character threshold
is a documented heuristic, not a calibrated number; it handles
the common case where PDFBox extracts stray metadata characters
from a scanned PDF but no real content. `DocumentTypeDetector`
strips whitespace before comparing length, so a PDF that's mostly
blank space with a little embedded text doesn't get misrouted.

**Why not vision LLM:** a vision LLM reading the raw image
produces no per-field confidence signal. Our entire trust layer
depends on real OCR confidence scores. Without them, "evidence-based
trust" becomes theater.

---

## LLM role: semantic structuring only

**Decision:** The LLM receives extracted text and maps it to our
schema. It does not validate data, calculate totals, determine
confidence, detect duplicates, or make approval decisions.

**Reasoning:** LLMs are good at language understanding (resolving
that "Grand Total", "Amount Due", and "Total Payable" all mean the
same field). They are unreliable at arithmetic and self-assessment.
Keeping the boundary strict means every trust signal in the system
comes from code we can reason about and test.

---

## Evidence-based confidence: OCR confidence, not LLM self-reporting

**Decision:** Per-field confidence comes from Tesseract's native
word-level confidence score, not from asking the LLM how confident
it is.

**Reasoning:** LLM self-reported confidence is poorly calibrated —
models are frequently overconfident and the number does not track
actual accuracy. Tesseract's confidence measures pixel-recognition
certainty, which correlates with actual recognition error rates.
We match LLM field values back to OCR words via normalized text
matching to connect the two layers.

**Key implementation detail:** Tess4J returns confidence on a 0–100
scale. We normalize to 0–1 at the source (`OcrExtractor`) so every
downstream consumer — evidence mapping, validation, the UI — sees
the correct scale without needing to know about the conversion.

---

## Evidence mapping: text matching, bounding boxes for disambiguation

**Decision:** Match LLM-extracted field values back to OCR words
via normalized text matching (lowercase, strip currency symbols
and commas). Use bounding box Y-coordinate proximity only when
the same value appears multiple times (e.g. "125.00" in multiple
line items).

**Alternatives considered:** asking the LLM to report which part
of the text each value came from, running a second OCR pass with
layout analysis.

**Reasoning:** Start with the simplest approach that works. Exact
and contains matching handles the majority of cases. Bounding box
disambiguation is added only for the specific problem of repeated
values in line items — not as a general approach that would
require building a full layout analysis system. When no matching
OCR word can be found at all, we persist the evidence row anyway
with a null confidence (`OCR_SOURCE_NOT_FOUND` failure) rather than
silently omitting it — a missing evidence row and a low-confidence
one mean different things to a reviewer, and we didn't want to
collapse that distinction.

---

## Validation: deterministic rules, not LLM judgment

**Decision:** All validation is deterministic application code.
No validation rule uses an LLM call.

**Rules implemented:**
- MISSING_REQUIRED_FIELD (vendor name, total amount)
- INVALID_DATE (null invoice date)
- INVALID_CURRENCY (not in known ISO 4217 set)
- LOW_OCR_CONFIDENCE (below 0.90 threshold)
- OCR_SOURCE_NOT_FOUND (no OCR word could be matched to a field)
- LINE_TOTAL_MISMATCH (quantity × unit_price ≠ amount)
- MISSING_LINE_ITEM_AMOUNT (line item has no amount to validate against)
- SUBTOTAL_MISMATCH (sum of line items ≠ subtotal)
- TOTAL_RECONCILIATION (subtotal + tax ≠ total)
- EXACT_DUPLICATE (same vendor + invoice number)

**OCR confidence threshold:** 0.90 is a starting heuristic,
not a calibrated number. It is configurable via
`validation.ocr-confidence-threshold` in application.yml
without a code change.

---

## Duplicate detection: exact match only

**Decision:** Detect exact duplicates (same normalized vendor name
+ same invoice number). Cut possible duplicate detection.

**Alternatives considered:** possible duplicate detection using
vendor + amount + date window.

**Reasoning:** possible duplicate detection produces too many false
positives for recurring invoices (same vendor, same monthly retainer,
different invoice number). The false positive cost — training users
to dismiss warnings reflexively — is higher than the false negative
cost of occasionally missing a genuine duplicate that slipped through
with a different invoice number. A reviewer who dismisses a match
once (`DUPLICATE_DISMISSED`) isn't asked about that same pair again.

---

## Human review: approved rules skipped across sessions

**Decision:** When a human approves a validation failure (e.g.
accepts a known subtotal mismatch), that rule is suppressed in
all future revalidation passes for that invoice — not just the
current session.

**Reasoning:** discovered during UI testing that approving
LOW_OCR_CONFIDENCE in one review round and OCR_SOURCE_NOT_FOUND in
the next caused an infinite oscillating loop — each round's
revalidation had no memory of what was approved in a prior round,
so it could resurrect a rule the reviewer had already signed off
on, and the invoice could never reach ACCEPTED. Fixed by reading
previously-approved rules from the database
(`findByInvoiceIdAndAction`) before revalidation and unioning them
with the current round's approvals into the skip-set. APPROVED is
a durable human decision, not a session-scoped hint.

---

## Database: single schema, four tables

**Decision:** invoice, invoice_line_item, extraction_evidence,
validation_failure. Human review state is stored directly on
validation_failure (resolved, action, new_value) rather than
a separate audit table.

**Alternatives considered:** separate review_action audit table,
event sourcing.

**Reasoning:** the resolution relationship is 1:1 — each failure
is resolved exactly once. A separate table would add joins to
every review query without adding information. A production
financial system would use an immutable append-only audit log;
for a take-home, current resolution state is sufficient.
This is a deliberate scope cut, documented here.

---

## File storage: BYTEA column, not S3

**Decision:** Store original invoice files as BYTEA in Postgres.

**Alternatives considered:** S3, Cloudflare R2.

**Reasoning:** invoices are small (< 10MB each). A separate object
store adds an account, a client library, network failure modes,
and credential management for no benefit at this scale. The correct
production choice is S3; the correct take-home choice is to keep
the system to one datastore and note the upgrade path.

---

## OCR: Tesseract (local), not cloud OCR

**Decision:** Tesseract via Tess4J, running inside the Docker image.

**Alternatives considered:** AWS Textract, Google Document AI.

**Reasoning:** cloud OCR requires a cloud account and billing
just to run the evaluator's copy of the submission. Tesseract
runs locally inside Docker with no external dependencies beyond
the API key. In production, cloud OCR would offer meaningfully
better accuracy on low-quality scans — worth upgrading then.

**Key findings:**
- Tess4J returns confidence on a 0–100 scale, not 0–1. Normalized
  at source in `OcrExtractor`.
- `ITesseract` is not thread-safe — created fresh per extraction
  call, not as a Spring singleton bean.
- Ubuntu 22.04's `apt-get install tesseract-ocr` package installs
  Tesseract **4.1.1**, not 5 — its trained-data files live under
  `/usr/share/tesseract-ocr/4.00/tessdata`, not `.../5/tessdata`.
  Found by inspecting the built Docker image directly; the wrong
  path would have silently broken OCR in production with no local
  signal, since local development points at a Homebrew-installed
  Tesseract 5 with a different path.

---

## Async processing: @Async + polling, not a message queue

**Decision:** Spring @Async with a ThreadPoolTaskExecutor.
Client polls GET /api/invoices/{id} every 2 seconds.

**Alternatives considered:** RabbitMQ, Kafka, WebSockets.

**Reasoning:** OCR + LLM extraction takes 5–25 seconds.
A synchronous upload would hold the HTTP connection open
for that duration, likely timing out on Render's free tier.
@Async handles the concurrency correctly at this scale.
A message queue adds operational infrastructure (another
service to deploy and monitor) for no benefit with a single
app instance.

---

## UI: server-rendered Thymeleaf, not a JS framework

**Decision:** Thymeleaf + Bootstrap 5 (CDN) + a single vanilla
`app.js`, with two thin JSON endpoints (`POST /invoices` upload,
`GET /api/invoices/{id}` for polling) rather than a full SPA
consuming a REST API.

**Reasoning:** the review screen is form-heavy and page-navigation-
oriented — approve, correct, submit, reload — which server rendering
handles with less code than a client framework would. The one place
that genuinely needs to stay on the page without a reload (upload
progress, marking fields resolved) is handled with plain DOM
manipulation. Splitting the JSON API under `/api/invoices` was a
late, deliberate call: the HTML detail page and the original JSON
detail endpoint both wanted `GET /invoices/{id}`, and moving the
JSON side avoided relying on content negotiation via `Accept`
headers, which would have made the already-documented `curl`
examples from earlier in the build unpredictable.

**Key finding:** Thymeleaf 3.1 refuses to render a variable
expression inside `th:onclick` (and other DOM event-handler
attributes) as a security measure — it silently throws mid-render,
truncating the page. Fixed by moving the data into `data-*`
attributes and wiring the click handler in `app.js`, which is also
the pattern already used everywhere else in the review screen.

---

## What we deliberately did not build

- **Authentication / multi-tenancy:** no user model; all invoices
  are in one workspace. First thing to add in production.
- **Natural language querying:** filter UI covers all required
  query patterns. NL-to-SQL adds LLM complexity with no
  reliability guarantee.
- **Possible duplicate detection:** cuts too many false positives
  for recurring invoices. See duplicate detection entry above.
- **Cloud OCR:** adds external dependency that complicates
  evaluator setup. See OCR entry above.
- **Full audit event log:** combined review state on
  validation_failure is sufficient for this scope.
- **Re-opening accepted invoices:** ACCEPTED is terminal.
  Re-opening adds state machine complexity with no clear
  use case in the current scope.
- **Currency conversion:** single-currency assumption.
  Multi-currency would require an exchange rate service.
- **Vendor-specific parsers:** cross-extraction agreement
  via normalized text matching is sufficient and avoids
  maintaining a parser per vendor layout.

---

## UX: Failure cards ask one question, not show mechanism

**Decision:** Each failure card asks "Is this correct?" with two
answers: approve or correct. Removed scope badges (INVOICE_FIELD,
LINE_ITEM), "Go to field" buttons, and technical rule names from
the visible UI.

**Reasoning:** A finance user doesn't know what "OCR source word
could not be located" means. Showing mechanism instead of guidance
creates confusion. The rule name is still available as a badge
(small, right-aligned) for engineers who need it, but it's not
the primary information.

---

## UX: Field edits in the extracted fields panel are saved on Complete Review

**Decision:** The user can edit any extracted field directly in
the fields panel, not just through failure cards. All edits are
collected on Complete Review submission and applied before
revalidation.

**Reasoning:** Validation only surfaces fields the system is
uncertain about. A user may spot an error in a field that passed
validation — they should be able to fix it without the system
needing to have flagged it first. Restricting edits to failure
cards would mean the system's uncertainty defines the boundary
of what the user can do, which is backwards.

---

## Duplicate detection: one failure, one decision, permanent

**Decision:** DuplicateDetector creates at most one failure per
invoice (best matching candidate). Once a human makes any
duplicate decision (dismissed or confirmed), duplicate detection
never runs again for that invoice.

**Reasoning:** Multiple duplicate failures for the same invoice
(one per historical match) required the user to dismiss the same
warning repeatedly — once for each prior upload of the same file.
A single decision is sufficient: "this invoice is or isn't a
duplicate." The system should respect that decision permanently.

---

## Validation messages: field-specific, plain English

**Decision:** Every validation message names the specific field
or line item it refers to, states the problem plainly, and tells
the user what to do. Technical terms (OCR, confidence threshold,
source word) are avoided in user-facing messages.

**Reasoning:** "Our reading of this value wasn't very confident"
does not tell a finance user which value, why it matters, or
what to do next. "Total amount was read with 61% confidence —
please check it against the original document" does all three.

---

## Processing invoices: separate section, not hidden, not mixed

**Decision:** Invoices currently being processed appear in a
separate "Processing" section above the main list, showing
filename and upload time. They are excluded from the main ledger.
The section auto-refreshes silently every 3 seconds.

**Reasoning:** Hiding processing invoices entirely (original
approach) removed useful feedback — the user couldn't tell if
their upload was received. Showing them in the main list with
empty fields looked broken. A separate section sets correct
expectations: "something is happening" without polluting the
trusted ledger with unverified records.
